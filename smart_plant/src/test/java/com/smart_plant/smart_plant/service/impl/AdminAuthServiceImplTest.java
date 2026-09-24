package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.AdminPasswordChangeRequest;
import com.smart_plant.smart_plant.dto.AdminProfileUpdateRequest;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.AdminLoginLogService;
import com.smart_plant.smart_plant.service.CaptchaService;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginMetricsService;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.PermissionAuthorizationService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证个人中心涉及的角色越权和密码会话安全边界。 */
class AdminAuthServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LoginSessionService loginSessionService = mock(LoginSessionService.class);
    private final AdminAuthServiceImpl service = new AdminAuthServiceImpl(
            userMapper,
            roleMapper,
            passwordEncoder,
            mock(JwtService.class),
            mock(CaptchaService.class),
            loginSessionService,
            mock(TokenBlacklistService.class),
            mock(LoginRateLimitService.class),
            mock(AdminLoginLogService.class),
            mock(LoginMetricsService.class),
            mock(PermissionAuthorizationService.class)
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void farmOwnerCannotChangeOwnRole() {
        User currentUser = activeUser(7L, 2L, "farm_owner");
        CurrentUserContext.set(currentUser);
        when(userMapper.selectById(7L)).thenReturn(currentUser);

        AdminProfileUpdateRequest request = validProfile(1L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateCurrentProfile(request)
        );

        assertEquals("只有管理员可以修改角色", exception.getMessage());
        verify(userMapper, never()).updateProfile(7L, request);
    }

    @Test
    void administratorCanSelectEnabledBackendRole() {
        User currentUser = activeUser(1L, 1L, "admin");
        Role farmOwnerRole = new Role();
        farmOwnerRole.setId(2L);
        farmOwnerRole.setRoleCode("farm_owner");
        farmOwnerRole.setStatus(1);
        CurrentUserContext.set(currentUser);
        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(roleMapper.selectById(2L)).thenReturn(farmOwnerRole);
        when(userMapper.updateProfile(1L, validProfile(2L))).thenReturn(1);

        AdminProfileUpdateRequest request = validProfile(2L);
        service.updateCurrentProfile(request);

        assertEquals(2L, request.getRoleId());
        verify(userMapper).updateProfile(1L, request);
    }

    @Test
    void passwordChangeRevokesExistingRefreshSessions() {
        User currentUser = activeUser(3L, 1L, "admin");
        currentUser.setPassword("$2a$encoded-current-password");
        CurrentUserContext.set(currentUser);
        when(userMapper.selectById(3L)).thenReturn(currentUser);
        when(passwordEncoder.matches("OldPass1", currentUser.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("NewPass2", currentUser.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass2")).thenReturn("encoded-new-password");
        when(userMapper.updatePassword(3L, "encoded-new-password")).thenReturn(1);

        AdminPasswordChangeRequest request = new AdminPasswordChangeRequest();
        request.setCurrentPassword("OldPass1");
        request.setNewPassword("NewPass2");
        request.setConfirmPassword("NewPass2");
        service.changeCurrentPassword(request);

        verify(loginSessionService).revoke(3L);
    }

    /** 构造通过后台鉴权所需最小字段的启用用户。 */
    private User activeUser(Long id, Long roleId, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setRoleId(roleId);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        return user;
    }

    /** 构造符合基础资料校验规则的请求对象。 */
    private AdminProfileUpdateRequest validProfile(Long roleId) {
        AdminProfileUpdateRequest request = new AdminProfileUpdateRequest();
        request.setPhone("13800000000");
        request.setRoleId(roleId);
        request.setGender(0);
        return request;
    }
}
