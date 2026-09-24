package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE_CODE = "user";

    private static final String EXPERT_ROLE_CODE = "expert";
    private static final Set<String> OWNER_CREATABLE_ROLES = Set.of("user", "technician");

    private static final int STATUS_DISABLED = 0;

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final ExpertProfileMapper expertProfileMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User addUser(User user) {
        User manager = requireManager();
        validateCreateUser(user);
        user.setPassword(encodePassword(user.getPassword()));
        normalizeRoleForCreate(user);
        Long ownerId = resolveBindingOwner(manager, user, null);
        checkUnique(user);
        userMapper.insert(user);
        insertBinding(user.getRoleCode(), ownerId, user.getId());
        return userMapper.selectById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        requireId(id);
        requireManagedUser(requireManager(), id);
        ensureUserHasNoApprovedExpertProfile(id);
        expertProfileMapper.deleteUnapprovedByUserId(id);
        deleteBindings(id);
        int rows = userMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的用户");
        }
        User manager = requireManager();
        // 批量删除先校验全部 ID，避免部分删除后才发现越权账号。
        ids.forEach(id -> requireManagedUser(manager, id));
        ensureUsersHaveNoApprovedExpertProfile(ids);
        expertProfileMapper.deleteUnapprovedByUserIds(ids);
        ids.forEach(this::deleteBindings);
        return userMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户ID不能为空");
        }
        var joinRequest = userMapper.lockFarmJoinRequest(user.getId());
        if (joinRequest != null && !Integer.valueOf(1).equals(joinRequest.getStatus())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "该账号入场申请未批准，请使用入场申请审核");
        }
        User oldUser = userMapper.selectById(user.getId());
        if (oldUser == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        User manager = requireManager();
        requireManagedUser(manager, user.getId());
        if (StringUtils.hasText(user.getPassword())) {
            validatePassword(user.getPassword());
            user.setPassword(encodePassword(user.getPassword()));
        }
        normalizeRoleForUpdate(user);
        String targetRole = user.getRoleCode() == null ? oldUser.getRoleCode() : user.getRoleCode();
        user.setRoleCode(targetRole);
        Long ownerId = resolveBindingOwner(manager, user, oldUser);
        if (joinRequest != null && (!targetRole.equals(joinRequest.getRequestedRole())
                || !java.util.Objects.equals(ownerId, joinRequest.getOwnerUserId()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "申请成员不能直接变更角色或农场，请先撤销入场授权");
        }
        ensureExpertRoleNotRemovedWhenProfileExists(user);
        checkUnique(user);
        int rows = userMapper.updateById(user);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "用户修改失败");
        }
        // 角色或农场主改变时在同一事务内调整绑定，避免权限数据与用户角色不一致。
        if (!targetRole.equalsIgnoreCase(oldUser.getRoleCode())
                || !java.util.Objects.equals(ownerId, oldUser.getFarmOwnerId())) {
            deleteBindings(user.getId());
            insertBinding(targetRole, ownerId, user.getId());
        }
        if (Integer.valueOf(STATUS_DISABLED).equals(user.getStatus())) {
            expertProfileMapper.disableByUserId(user.getId());
        }
        return userMapper.selectById(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String password) {
        requireId(id);
        requireManagedUser(requireManager(), id);
        validatePassword(password);
        int rows = userMapper.updatePassword(id, encodePassword(password));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        requireId(id);
        requireManagedUser(requireManager(), id);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户状态只能为0或1");
        }
        var joinRequest = userMapper.lockFarmJoinRequest(id);
        if (status == 1 && joinRequest != null && !Integer.valueOf(1).equals(joinRequest.getStatus())) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "该账号入场申请未批准，请使用入场申请审核");
        }
        int rows = userMapper.updateStatus(id, status);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        if (status == STATUS_DISABLED) {
            expertProfileMapper.disableByUserId(id);
        }
    }

    @Override
    public User getUserById(Long id) {
        requireId(id);
        requireManagedUser(requireManager(), id);
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    public PageInfo<User> listUsers(String username, String phone, String roleCode, Integer status,
                                    Integer pageNum, Integer pageSize) {
        User manager = requireManager();
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        PageHelper.startPage(currentPage, currentSize);
        String normalizedRoleCode = StringUtils.hasText(roleCode) ? roleCode.trim() : null;
        return new PageInfo<>(userMapper.selectList(username, phone, normalizedRoleCode, status,
                isOwner(manager) ? manager.getId() : null));
    }

    @Override
    public List<Role> listAssignableRoles() {
        User manager = requireManager();
        if (isOwner(manager)) {
            return java.util.stream.Stream.of(roleMapper.selectByRoleCode("user"),
                    roleMapper.selectByRoleCode("technician"))
                    .filter(java.util.Objects::nonNull).toList();
        }
        return roleMapper.selectList(null, null, 1);
    }

    @Override
    public List<User> listAvailableFarmOwners() {
        User manager = requireManager();
        return isOwner(manager) ? List.of(manager) : userMapper.selectActiveFarmOwners();
    }

    /** 用户管理只允许管理员和农场主；具体权限仍由 Controller 的 user:manage 注解校验。 */
    private User requireManager() {
        User manager = CurrentUserContext.get();
        if (manager == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "请先登录");
        }
        String roleCode = manager.getRoleCode();
        if (!"admin".equalsIgnoreCase(roleCode) && !"farm_owner".equalsIgnoreCase(roleCode)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权管理用户");
        }
        return manager;
    }

    private boolean isOwner(User manager) {
        return "farm_owner".equalsIgnoreCase(manager.getRoleCode());
    }

    private void requireManagedUser(User manager, Long userId) {
        if (userId == null || (isOwner(manager)
                && userMapper.countManagedUserByOwnerId(manager.getId(), userId) == 0)) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "只能管理本农场的普通用户和技术人员");
        }
    }

    /** 服务端决定归属，不能信任前端角色或农场主 ID；农场主创建的账号强制归属本人。 */
    private Long resolveBindingOwner(User manager, User target, User previous) {
        String roleCode = target.getRoleCode();
        if (isOwner(manager)) {
            if (!OWNER_CREATABLE_ROLES.contains(roleCode)) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "农场主只能新增或编辑普通用户、技术人员");
            }
            if (target.getFarmOwnerId() != null && !manager.getId().equals(target.getFarmOwnerId())) {
                throw new BusinessException(ResponseCode.FORBIDDEN, "不能绑定到其他农场主");
            }
            return manager.getId();
        }
        if (!OWNER_CREATABLE_ROLES.contains(roleCode)) {
            if (target.getFarmOwnerId() != null) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "该角色不需要绑定农场主");
            }
            return null;
        }
        Long ownerId = target.getFarmOwnerId() != null ? target.getFarmOwnerId()
                : previous == null ? null : previous.getFarmOwnerId();
        User owner = ownerId == null ? null : userMapper.selectById(ownerId);
        if (owner == null || !"farm_owner".equalsIgnoreCase(owner.getRoleCode())
                || !Integer.valueOf(1).equals(owner.getStatus())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择有效的绑定农场主");
        }
        return ownerId;
    }

    private void insertBinding(String roleCode, Long ownerId, Long userId) {
        if ("user".equalsIgnoreCase(roleCode)) {
            userMapper.insertFarmOwnerUser(ownerId, userId);
        } else if ("technician".equalsIgnoreCase(roleCode)) {
            userMapper.insertFarmOwnerTechnician(ownerId, userId);
        }
    }

    private void deleteBindings(Long userId) {
        userMapper.deleteFarmOwnerUserBindings(userId);
        userMapper.deleteFarmOwnerTechnicianBindings(userId);
    }

    private void validateCreateUser(User user) {
        if (user == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户信息不能为空");
        }
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "密码不能为空");
        }
        validatePassword(user.getPassword());
        if (!StringUtils.hasText(user.getPhone())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号不能为空");
        }
    }

    private void checkUnique(User user) {
        Long excludeId = user.getId();
        if (StringUtils.hasText(user.getUsername()) && userMapper.countByUsername(user.getUsername(), excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户名已存在");
        }
        if (StringUtils.hasText(user.getPhone()) && userMapper.countByPhone(user.getPhone(), excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已存在");
        }
        if (StringUtils.hasText(user.getEmail()) && userMapper.countByEmail(user.getEmail(), excludeId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱已存在");
        }
    }

    private void normalizeRoleForCreate(User user) {
        Role role = resolveRole(user.getRoleId(), user.getRoleCode(), DEFAULT_ROLE_CODE);
        user.setRoleId(role.getId());
        user.setRoleName(role.getRoleName());
        user.setRoleCode(role.getRoleCode());
    }

    private void normalizeRoleForUpdate(User user) {
        if (user.getRoleId() == null && !StringUtils.hasText(user.getRoleCode())) {
            return;
        }
        Role role = resolveRole(user.getRoleId(), user.getRoleCode(), null);
        user.setRoleId(role.getId());
        user.setRoleName(role.getRoleName());
        user.setRoleCode(role.getRoleCode());
    }

    private Role resolveRole(Long roleId, String roleCode, String defaultRoleCode) {
        if (roleId != null) {
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
            }
            if (StringUtils.hasText(roleCode) && !role.getRoleCode().equals(roleCode.trim())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "角色ID和角色编码不匹配");
            }
            return role;
        }

        String code = StringUtils.hasText(roleCode) ? roleCode.trim() : defaultRoleCode;
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "角色不能为空");
        }
        Role role = roleMapper.selectByRoleCode(code);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void ensureExpertRoleNotRemovedWhenProfileExists(User user) {
        if (user.getRoleId() == null) {
            return;
        }
        String roleCode = user.getRoleCode();
        if (EXPERT_ROLE_CODE.equalsIgnoreCase(roleCode)) {
            return;
        }
        if (expertProfileMapper.countByUserId(user.getId(), null) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该用户已绑定专家档案，请先处理专家信息后再变更为非专家角色");
        }
    }

    private void ensureUserHasNoApprovedExpertProfile(Long userId) {
        if (expertProfileMapper.countApprovedByUserId(userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该用户已通过专家认证审核，不可直接删除");
        }
    }

    private void ensureUsersHaveNoApprovedExpertProfile(List<Long> userIds) {
        if (expertProfileMapper.countApprovedByUserIds(userIds) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "选中用户中存在已通过专家认证审核的用户，不可直接批量删除");
        }
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户ID不能为空");
        }
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password.trim());
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "新密码不能为空");
        }
        String value = password.trim();
        if (value.length() < 8 || value.length() > 32
                || !value.matches(".*[A-Za-z].*")
                || !value.matches(".*\\d.*")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "密码需为8到32个字符，且同时包含字母和数字");
        }
    }
}
