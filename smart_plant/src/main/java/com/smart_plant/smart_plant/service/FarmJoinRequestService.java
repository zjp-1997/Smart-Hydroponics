package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.FarmJoinRequest;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FarmJoinRequestService {
    private final UserMapper userMapper;
    private final LoginSessionService loginSessionService;

    /** 复用审核权限：农场主仅统计本人农场，管理员统计全部待审核申请。 */
    public long countPending() {
        User manager = requireManager();
        return userMapper.countPendingFarmJoinRequests(
                "farm_owner".equalsIgnoreCase(manager.getRoleCode()) ? manager.getId() : null);
    }

    public PageInfo<FarmJoinRequest> list(Integer status, int pageNum, int pageSize) {
        User manager = requireManager();
        if (status != null && (status < 0 || status > 3)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "申请状态无效");
        }
        PageHelper.startPage(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize)));
        return new PageInfo<>(userMapper.selectFarmJoinRequests(
                "farm_owner".equalsIgnoreCase(manager.getRoleCode()) ? manager.getId() : null, status));
    }

    /** 路径使用申请人 ID，和通用用户编辑竞争同一把申请行锁。 */
    @Transactional(rollbackFor = Exception.class)
    public void decide(Long userId, Integer status, String reason) {
        User manager = requireManager();
        if (userId == null || status == null || status < 1 || status > 3) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "审核参数无效");
        }
        String note = reason == null ? "" : reason.trim();
        if (note.length() > 500 || (status != 1 && note.isEmpty())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "拒绝或撤销请填写原因，最多500字");
        }
        FarmJoinRequest request = userMapper.lockFarmJoinRequest(userId);
        if (request == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "入场申请不存在");
        }
        if ("farm_owner".equalsIgnoreCase(manager.getRoleCode())
                && !Objects.equals(manager.getId(), request.getOwnerUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能审核本农场的入场申请");
        }
        int expectedStatus = status == 3 ? 1 : 0;
        if (!Integer.valueOf(expectedStatus).equals(request.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "申请状态已变化，请刷新后重试");
        }
        User applicant = userMapper.selectById(userId);
        if (applicant == null || !Objects.equals(applicant.getRoleCode(), request.getRequestedRole())
                || !("user".equals(request.getRequestedRole()) || "technician".equals(request.getRequestedRole()))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "申请账号或角色已变化，不能执行审核");
        }
        if (status == 1) {
            User owner = userMapper.selectById(request.getOwnerUserId());
            if (owner == null || !"farm_owner".equalsIgnoreCase(owner.getRoleCode())
                    || !Integer.valueOf(1).equals(owner.getStatus()) || applicant.getFarmOwnerId() != null) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "农场主已失效或申请人已有绑定，请刷新核实");
            }
            if ("user".equals(request.getRequestedRole())) {
                userMapper.insertFarmOwnerUser(request.getOwnerUserId(), userId);
            } else {
                userMapper.insertFarmOwnerTechnician(request.getOwnerUserId(), userId);
            }
        } else if (status == 3) {
            if (!Objects.equals(applicant.getFarmOwnerId(), request.getOwnerUserId())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "成员归属已变化，不能撤销旧申请");
            }
            userMapper.deleteFarmOwnerUserBindings(userId);
            userMapper.deleteFarmOwnerTechnicianBindings(userId);
        }
        if (userMapper.updateStatus(userId, status == 1 ? 1 : 0) != 1
                || userMapper.decideFarmJoinRequest(userId, expectedStatus, status, manager.getId(), note) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "审核失败，请刷新后重试");
        }
        // 撤销先使旧令牌失效；事务回滚时最多要求重新登录，不会保留越权会话。
        if (status == 3) {
            loginSessionService.revoke(userId);
        }
    }

    private User requireManager() {
        User manager = CurrentUserContext.get();
        if (manager == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "请先登录");
        }
        if (!"admin".equalsIgnoreCase(manager.getRoleCode())
                && !"farm_owner".equalsIgnoreCase(manager.getRoleCode())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权审核入场申请");
        }
        return manager;
    }
}
