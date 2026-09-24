package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.MaintenanceMessage;
import com.smart_plant.smart_plant.entity.IotDevice;
import com.smart_plant.smart_plant.dto.MaintenanceMessageRequest;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.MaintenanceMessageMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;

/** 独立维护消息服务：与故障创建同事务提交，失败时回滚故障，避免有故障无通知。 */
@Service
@RequiredArgsConstructor
public class MaintenanceMessageService {
    private final MaintenanceMessageMapper mapper;
    private final DataPermissionService permissions;

    /** 三个故障创建入口共用此方法；唯一消息行的数据库锁覆盖接收明细去重。 */
    @Transactional(rollbackFor = Exception.class)
    public void publishForFault(Long faultId) {
        if (faultId == null) throw new BusinessException(ResponseCode.PARAM_ERROR, "故障ID不能为空");
        mapper.insertFromFault(faultId);
        Long id = mapper.lockByFaultId(faultId);
        if (id == null) throw new BusinessException(ResponseCode.NOT_FOUND, "来源故障不存在");
        mapper.deliver(id);
    }

    /** 管理端按权限范围查询；用户端强制只查登录接收人。限制页大小以控制数据库压力。 */
    public PageInfo<MaintenanceMessage> list(String title, String content, String plotName,
                                            int pageNum, int pageSize, boolean personal) {
        Long scope = scope(personal);
        PageHelper.startPage(Math.max(1, pageNum), Math.max(1, Math.min(100, pageSize)));
        return new PageInfo<>(mapper.selectList(scope, null, clean(title), clean(content), clean(plotName)));
    }

    /** 统计范围与列表完全一致。 */
    public Map<String, Object> statistics(boolean personal) {
        return mapper.statistics(scope(personal));
    }

    /** 不存在与无权查看统一返回404，防止通过ID枚举其他农场消息。 */
    public MaintenanceMessage detail(Long id, boolean personal) {
        List<MaintenanceMessage> rows = mapper.selectList(scope(personal), id, null, null, null);
        if (rows.isEmpty()) throw new BusinessException(ResponseCode.NOT_FOUND, "维护消息不存在");
        return rows.getFirst();
    }

    /** 已读操作幂等且仅作用于当前用户；先验证个人范围，避免代其他接收人标记。 */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        detail(id, true);
        mapper.markRead(id, permissions.currentUser().getId());
    }

    /** 发布下拉候选支持远程搜索，使用与提交时相同的数据权限范围。 */
    public List<IotDevice> devices(String keyword) {
        return mapper.selectDevices(scope(false), null, clean(keyword));
    }

    /** 人工发布独立维护消息，发布人取登录态，接收人仍由设备归属自动确定。 */
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceMessage publish(MaintenanceMessageRequest request) {
        validate(request);
        if (request.getDeviceId() == null) throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择设备");
        var devices = mapper.selectDevices(scope(false), request.getDeviceId(), null);
        if (devices.isEmpty()) throw new BusinessException(ResponseCode.NOT_FOUND, "设备不存在或无权发布");
        var user = permissions.currentUser();
        MaintenanceMessage message = new MaintenanceMessage();
        message.setDeviceId(request.getDeviceId());
        message.setTitle(request.getTitle().trim());
        message.setLevel(request.getLevel());
        // 保留用户正文和换行；默认模板仅在自动生成消息时使用。
        message.setContent(request.getContent());
        message.setPublisherId(user.getId());
        String publisher = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        message.setPublisherName(StringUtils.hasText(publisher) ? publisher.substring(0, Math.min(50, publisher.length())) : "用户" + user.getId());
        if (mapper.insertManual(message) != 1) throw new BusinessException(ResponseCode.FAIL, "发布维护消息失败");
        mapper.deliver(message.getId());
        return detail(message.getId(), false);
    }

    /** 自动及人工消息均允许编辑标题、正文和级别；接收人的阅读状态不被重置。 */
    @Transactional(rollbackFor = Exception.class)
    public MaintenanceMessage update(Long id, MaintenanceMessageRequest request) {
        validate(request);
        lockAndAuthorize(id);
        MaintenanceMessage message = detail(id, false);
        if (request.getVersion() == null) throw new BusinessException(ResponseCode.PARAM_ERROR, "消息版本不能为空，请重新打开编辑窗口");
        if (!java.util.Objects.equals(request.getDeviceId(), message.getDeviceId()))
            throw new BusinessException(ResponseCode.PARAM_ERROR, "编辑时不能更换关联设备");
        message.setTitle(request.getTitle().trim());
        message.setLevel(request.getLevel());
        // 保留用户正文和换行；默认模板仅在自动生成消息时使用。
        message.setContent(request.getContent());
        message.setVersion(request.getVersion());
        if (mapper.updateMessage(message) != 1)
            throw new BusinessException(ResponseCode.FAIL, "消息已被修改，请刷新后重试");
        mapper.syncDeliveries(id);
        return detail(id, false);
    }

    /** 批量删除先完成全部权限校验，再整组软删除；任一越权请求使整批回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public int delete(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择1至100条有效消息");
        var ordered = ids.stream().distinct().sorted().toList();
        ordered.forEach(this::lockAndAuthorize);
        int count = 0;
        for (Long id : ordered) {
            count += mapper.deleteMessage(id);
            mapper.deleteDeliveries(id);
        }
        return count;
    }

    /** 锁顺序由调用方统一，避免批量操作按不同顺序获取锁造成死锁。 */
    private void lockAndAuthorize(Long id) {
        if (id == null || mapper.lockById(id) == null)
            throw new BusinessException(ResponseCode.NOT_FOUND, "维护消息不存在");
        detail(id, false);
    }

    /** 后端再次校验标题、正文长度和级别，不能仅依赖前端表单校验。 */
    private void validate(MaintenanceMessageRequest request) {
        if (request == null || !StringUtils.hasText(request.getTitle()) || request.getTitle().trim().length() > 100)
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息标题不能为空且不能超过100个字符");
        if (!StringUtils.hasText(request.getContent()) || request.getContent().length() > 10000)
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息内容不能为空且不能超过10000个字符");
        if (request.getLevel() == null || request.getLevel() < 1 || request.getLevel() > 3)
            throw new BusinessException(ResponseCode.PARAM_ERROR, "消息级别必须为1至3");
    }

    /** 在分页启动前解析登录态，避免权限查询误用PageHelper分页上下文。 */
    private Long scope(boolean personal) {
        return personal ? permissions.currentUser().getId() : permissions.restrictUserId(null);
    }

    /** 空白查询条件归一化，不拼接用户输入SQL。 */
    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
