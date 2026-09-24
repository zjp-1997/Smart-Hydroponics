package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.dto.ClientSmsCodeResponse;
import com.smart_plant.smart_plant.dto.ClientPasswordResetRequest;
import com.smart_plant.smart_plant.dto.ClientRegisterRequest;
import com.smart_plant.smart_plant.dto.ClientLoginRequest;
import com.smart_plant.smart_plant.dto.ClientSmsLoginRequest;
import com.smart_plant.smart_plant.dto.ClientProfileUpdateRequest;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.SmsCode;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.SmsCodeMapper;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.doAnswer;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 验证 farm 注册身份和绑定，以及认证流程中的关键安全规则。 */
class ClientAuthServiceImplTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 2, 3})
    void unapprovedApplicantCannotLoginWithPasswordOrSms(int status) {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        LoginSessionService sessions = mock(LoginSessionService.class);
        JwtService jwt = mock(JwtService.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(mapper, mock(ExpertProfileMapper.class),
                mock(SmsCodeMapper.class), mock(RoleMapper.class), encoder, jwt, sessions,
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        User user = new User();
        user.setId(12L); user.setRoleCode("user"); user.setStatus(0); user.setPassword("hashed");
        var join = new com.smart_plant.smart_plant.entity.FarmJoinRequest();
        join.setStatus(status);
        when(mapper.selectFarmJoinRequestByUserId(12L)).thenReturn(join);
        when(mapper.selectByUsername("applicant")).thenReturn(user);
        when(mapper.selectByPhone("13800138000")).thenReturn(user);
        when(encoder.matches("Farm2026", "hashed")).thenReturn(true);
        ClientLoginRequest password = new ClientLoginRequest();
        password.setUsername("applicant"); password.setPassword("Farm2026");
        ClientSmsLoginRequest sms = new ClientSmsLoginRequest();
        sms.setPhone("13800138000"); sms.setCode("123456");
        assertThrows(BusinessException.class, () -> service.login(password, "127.0.0.1"));
        assertThrows(BusinessException.class, () -> service.loginBySms(sms, "127.0.0.1"));
        org.mockito.Mockito.verifyNoInteractions(jwt, sessions);
    }

    @Test
    void farmOwnerCanUpdateWhitelistedProfileFields() {
        UserMapper userMapper = mock(UserMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper, mock(ExpertProfileMapper.class), mock(SmsCodeMapper.class), mock(RoleMapper.class),
                mock(PasswordEncoder.class), mock(JwtService.class), mock(LoginSessionService.class),
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        User current = new User();
        current.setId(7L);
        current.setRoleCode("farm_owner");
        current.setPhone("U-internal-phone");
        User updated = new User();
        updated.setId(7L);
        updated.setUsername("new-owner");
        when(userMapper.selectById(7L)).thenReturn(current, updated);
        when(userMapper.updateClientProfile(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.any(ClientProfileUpdateRequest.class))).thenReturn(1);
        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest();
        request.setUsername(" new-owner ");
        request.setPhone("");
        request.setNickname(" 新昵称 ");
        request.setGender(1);
        request.setEmail("owner@example.com");
        request.setAvatar("/uploads/profile-avatars/avatar.jpg");
        request.setRemark(" 智慧种植 ");

        User result = service.updateProfile(7L, request);

        ArgumentCaptor<ClientProfileUpdateRequest> profile = ArgumentCaptor.forClass(ClientProfileUpdateRequest.class);
        verify(userMapper).updateClientProfile(org.mockito.ArgumentMatchers.eq(7L), profile.capture());
        // 空手机号应保留数据库占位值，其他文本去除首尾空格后再入库。
        assertEquals("U-internal-phone", profile.getValue().getPhone());
        assertEquals("新昵称", profile.getValue().getNickname());
        assertEquals("智慧种植", profile.getValue().getRemark());
        assertEquals("new-owner", result.getUsername());
    }

    @Test
    void profileUpdateRejectsBackendOnlyRole() {
        UserMapper userMapper = mock(UserMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper, mock(ExpertProfileMapper.class), mock(SmsCodeMapper.class), mock(RoleMapper.class),
                mock(PasswordEncoder.class), mock(JwtService.class), mock(LoginSessionService.class),
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        User admin = new User();
        admin.setId(8L);
        admin.setRoleCode("admin");
        when(userMapper.selectById(8L)).thenReturn(admin);

        assertThrows(BusinessException.class, () -> service.updateProfile(8L, new ClientProfileUpdateRequest()));
        // 角色校验失败后不能执行任何个人资料写入。
        verify(userMapper, never()).updateClientProfile(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(ClientProfileUpdateRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "technician", "expert"})
    void profileUpdateAllowsOtherFarmClientRoles(String roleCode) {
        UserMapper userMapper = mock(UserMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper, mock(ExpertProfileMapper.class), mock(SmsCodeMapper.class), mock(RoleMapper.class),
                mock(PasswordEncoder.class), mock(JwtService.class), mock(LoginSessionService.class),
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        User current = new User();
        current.setId(9L);
        current.setRoleCode(roleCode);
        current.setPhone("U-internal-phone");
        when(userMapper.selectById(9L)).thenReturn(current);
        when(userMapper.updateClientProfile(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any(ClientProfileUpdateRequest.class))).thenReturn(1);
        ClientProfileUpdateRequest request = new ClientProfileUpdateRequest();
        request.setUsername(roleCode + "-account");

        service.updateProfile(9L, request);

        // 三类新增角色均复用字段白名单更新，不能因角色不同走管理端资料逻辑。
        verify(userMapper).updateClientProfile(org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.any(ClientProfileUpdateRequest.class));
    }

    @Test
    void expertUserAndTechnicianCanLoginWithPasswordAndSmsCode() {
        UserMapper users = mock(UserMapper.class);
        RoleMapper roles = mock(RoleMapper.class);
        SmsCodeMapper codes = mock(SmsCodeMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        User expert = new User();
        expert.setId(15L);
        expert.setUsername("expert1");
        expert.setPassword("hash");
        expert.setPhone("13800138000");
        expert.setRoleId(4L);
        expert.setRoleCode("expert");
        expert.setStatus(1);
        Role activeRole = new Role();
        activeRole.setId(4L);
        activeRole.setStatus(1);
        when(users.selectByUsername("expert1")).thenReturn(expert);
        when(users.selectByPhone("13800138000")).thenReturn(expert);
        when(users.selectById(15L)).thenReturn(expert);
        when(roles.selectById(4L)).thenReturn(activeRole);
        when(encoder.matches("secret", "hash")).thenReturn(true);
        when(jwt.generateAccessToken(expert, false)).thenReturn("access");
        when(jwt.generateRefreshToken(expert, false)).thenReturn("refresh");
        JwtPayload access = new JwtPayload();
        access.setJti("access-id");
        access.setExp(2_000_000_000L);
        JwtPayload refresh = new JwtPayload();
        refresh.setJti("refresh-id");
        refresh.setExp(2_100_000_000L);
        when(jwt.parseToken("access")).thenReturn(access);
        when(jwt.parseToken("refresh")).thenReturn(refresh);
        SmsCode code = new SmsCode();
        code.setId(22L);
        code.setCode("123456");
        when(codes.selectLatestValidForUpdate("13800138000", ClientAuthServiceImpl.SMS_SCENE_LOGIN))
                .thenReturn(code);
        when(codes.markUsed(22L)).thenReturn(1);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(users, mock(ExpertProfileMapper.class), codes,
                roles, encoder, jwt, mock(LoginSessionService.class),
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));

        // 两种登录都返回专家角色，前端据此进入双入口工作台。
        ClientLoginRequest password = new ClientLoginRequest();
        password.setUsername("expert1");
        password.setPassword("secret");
        assertEquals("expert", service.login(password, "127.0.0.1").getRoleCode());
        ClientSmsLoginRequest sms = new ClientSmsLoginRequest();
        sms.setPhone("13800138000");
        sms.setCode("123456");
        assertEquals("expert", service.loginBySms(sms, "127.0.0.1").getRoleCode());
        // 相同认证流程也必须放行普通用户，并把角色交给 farm 决定工作台入口。
        expert.setRoleCode("user");
        assertEquals("user", service.login(password, "127.0.0.1").getRoleCode());
        assertEquals("user", service.loginBySms(sms, "127.0.0.1").getRoleCode());
        // 技术人员使用同一登录页的密码和短信流程，角色原样返回供 farm 分流。
        expert.setRoleCode("technician");
        assertEquals("technician", service.login(password, "127.0.0.1").getRoleCode());
        assertEquals("technician", service.loginBySms(sms, "127.0.0.1").getRoleCode());
    }

    @Test
    void disabledRoleCannotLoginWithPasswordOrSmsCode() {
        UserMapper users = mock(UserMapper.class);
        RoleMapper roles = mock(RoleMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwt = mock(JwtService.class);
        LoginSessionService sessions = mock(LoginSessionService.class);
        User technician = new User();
        technician.setId(16L);
        technician.setRoleId(3L);
        technician.setRoleCode("technician");
        technician.setStatus(1);
        technician.setPassword("hash");
        Role disabledRole = new Role();
        disabledRole.setId(3L);
        disabledRole.setStatus(0);
        when(users.selectByUsername("technicist_02")).thenReturn(technician);
        when(users.selectByPhone("13800138001")).thenReturn(technician);
        when(roles.selectById(3L)).thenReturn(disabledRole);
        when(encoder.matches("secret", "hash")).thenReturn(true);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(users, mock(ExpertProfileMapper.class),
                mock(SmsCodeMapper.class), roles, encoder, jwt, sessions,
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        ClientLoginRequest password = new ClientLoginRequest();
        password.setUsername("technicist_02");
        password.setPassword("secret");
        ClientSmsLoginRequest sms = new ClientSmsLoginRequest();
        sms.setPhone("13800138001");
        sms.setCode("123456");

        BusinessException passwordError = assertThrows(BusinessException.class,
                () -> service.login(password, "127.0.0.1"));
        BusinessException smsError = assertThrows(BusinessException.class,
                () -> service.loginBySms(sms, "127.0.0.1"));

        assertEquals("当前角色已禁用，请联系管理员", passwordError.getMessage());
        assertEquals("当前角色已禁用，请联系管理员", smsError.getMessage());
        org.mockito.Mockito.verifyNoInteractions(jwt, sessions);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"user", "technician"})
    void registrationOnlyCreatesDisabledApplicant(String roleCode) {
        UserMapper userMapper = mock(UserMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(userMapper, mock(ExpertProfileMapper.class),
                mock(SmsCodeMapper.class), roleMapper, encoder, mock(JwtService.class),
                mock(LoginSessionService.class), mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        User owner = new User();
        owner.setId(7L);
        owner.setRoleCode("farm_owner");
        owner.setStatus(1);
        Role technician = new Role();
        technician.setId(3L);
        technician.setRoleCode(roleCode);
        when(userMapper.selectById(7L)).thenReturn(owner);
        when(roleMapper.selectByRoleCode(roleCode)).thenReturn(technician);
        when(encoder.encode("Farm2026")).thenReturn("hashed");
        doAnswer(invocation -> {
            ((User) invocation.getArgument(0)).setId(12L);
            return 1;
        }).when(userMapper).insert(org.mockito.ArgumentMatchers.any(User.class));
        ClientRegisterRequest request = registerRequest(roleCode, 7L);

        service.register(request);

        ArgumentCaptor<User> applicant = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(applicant.capture());
        assertEquals(0, applicant.getValue().getStatus());
        verify(userMapper).insertFarmJoinRequest(12L, 7L, roleCode);
        verify(userMapper, never()).insertFarmOwnerTechnician(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        verify(userMapper, never()).insertFarmOwnerUser(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void registerExpertCreatesProfileWithoutFarmOwner() {
        UserMapper userMapper = mock(UserMapper.class);
        ExpertProfileMapper expertProfileMapper = mock(ExpertProfileMapper.class);
        RoleMapper roleMapper = mock(RoleMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(userMapper, expertProfileMapper,
                mock(SmsCodeMapper.class), roleMapper, mock(PasswordEncoder.class), mock(JwtService.class),
                mock(LoginSessionService.class), mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));
        Role expert = new Role();
        expert.setId(4L);
        expert.setRoleCode("expert");
        when(roleMapper.selectByRoleCode("expert")).thenReturn(expert);
        doAnswer(invocation -> {
            ((User) invocation.getArgument(0)).setId(15L);
            return 1;
        }).when(userMapper).insert(org.mockito.ArgumentMatchers.any(User.class));

        service.register(registerRequest("expert", null));

        ArgumentCaptor<ExpertProfile> profile = ArgumentCaptor.forClass(ExpertProfile.class);
        verify(expertProfileMapper).insert(profile.capture());
        assertEquals(15L, profile.getValue().getUserId());
        assertEquals(0, profile.getValue().getAuditStatus());
        assertEquals(0, profile.getValue().getConsultationStatus());
        verify(userMapper, never()).insertFarmOwnerTechnician(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void registerRejectsAdminRoleAndInvalidFarmOwner() {
        UserMapper userMapper = mock(UserMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(userMapper, mock(ExpertProfileMapper.class),
                mock(SmsCodeMapper.class), mock(RoleMapper.class), mock(PasswordEncoder.class),
                mock(JwtService.class), mock(LoginSessionService.class),
                mock(TokenBlacklistService.class), mock(LoginRateLimitService.class));

        // 匿名请求即使伪造角色，也不能创建管理员账号。
        assertThrows(BusinessException.class, () -> service.register(registerRequest("admin", null)));
        // 普通用户必须绑定实际存在且启用的农场主。
        assertThrows(BusinessException.class, () -> service.register(registerRequest("user", 99L)));
        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.any(User.class));
    }

    /** 构造三种角色共用的有效注册输入，测试只聚焦角色与归属差异。 */
    private ClientRegisterRequest registerRequest(String roleCode, Long farmOwnerId) {
        ClientRegisterRequest request = new ClientRegisterRequest();
        request.setUsername("new-account");
        request.setPassword("Farm2026");
        request.setConfirmPassword("Farm2026");
        request.setRoleCode(roleCode);
        request.setFarmOwnerId(farmOwnerId);
        return request;
    }

    @Test
    void sendSmsCodeCreatesSixDigitLoginCode() {
        SmsCodeMapper smsCodeMapper = mock(SmsCodeMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                mock(UserMapper.class),
                mock(ExpertProfileMapper.class),
                smsCodeMapper,
                mock(RoleMapper.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(LoginSessionService.class),
                mock(TokenBlacklistService.class),
                mock(LoginRateLimitService.class)
        );

        ClientSmsCodeResponse response = service.sendSmsCode(
                null,
                "13800138000",
                ClientAuthServiceImpl.SMS_SCENE_LOGIN,
                "127.0.0.1",
                "test-agent"
        );

        ArgumentCaptor<SmsCode> captor = ArgumentCaptor.forClass(SmsCode.class);
        verify(smsCodeMapper).insert(captor.capture());
        // 响应验证码必须与入库验证码一致，且始终保留前导零形成六位数字。
        assertTrue(response.getCode().matches("\\d{6}"));
        assertEquals(response.getCode(), captor.getValue().getCode());
        assertEquals(300L, response.getExpiresIn());
    }

    @Test
    void logoutBlacklistsAccessTokenAndRevokesSession() {
        JwtService jwtService = mock(JwtService.class);
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                mock(UserMapper.class),
                mock(ExpertProfileMapper.class),
                mock(SmsCodeMapper.class),
                mock(RoleMapper.class),
                mock(PasswordEncoder.class),
                jwtService,
                loginSessionService,
                tokenBlacklistService,
                mock(LoginRateLimitService.class)
        );
        JwtPayload payload = new JwtPayload(9L, "farm-user", "user", "access", false,
                1_000L, 2_000L, "access-jti");
        when(jwtService.parseToken("access-token")).thenReturn(payload);

        service.logout("access-token");

        // 黑名单阻止旧 access token 再次访问，会话撤销同时阻止 refresh token 续期。
        verify(tokenBlacklistService).blacklist("access-jti", 2_000L);
        verify(loginSessionService).revoke(9L);
    }

    @Test
    void bindPhoneDoesNotUpdateUserWhenCodeIsWrong() {
        UserMapper userMapper = mock(UserMapper.class);
        SmsCodeMapper smsCodeMapper = mock(SmsCodeMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper,
                mock(ExpertProfileMapper.class),
                smsCodeMapper,
                mock(RoleMapper.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(LoginSessionService.class),
                mock(TokenBlacklistService.class),
                mock(LoginRateLimitService.class)
        );
        User user = new User();
        user.setId(9L);
        SmsCode storedCode = new SmsCode();
        storedCode.setId(12L);
        storedCode.setCode("123456");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(smsCodeMapper.selectLatestValidForUpdate(
                "13800138000",
                ClientAuthServiceImpl.SMS_SCENE_BIND_PHONE
        )).thenReturn(storedCode);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.bindPhone(9L, "13800138000", "654321")
        );
        // 错误验证码应中止绑定，既不消费验证码也不修改用户手机号。
        assertEquals("验证码错误或已过期", exception.getMessage());

        verify(smsCodeMapper, never()).markUsed(12L);
        verify(userMapper, never()).updatePhone(9L, "13800138000");
    }

    @Test
    void resetPasswordEncryptsPasswordAndRevokesSession() {
        UserMapper userMapper = mock(UserMapper.class);
        SmsCodeMapper smsCodeMapper = mock(SmsCodeMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        LoginSessionService loginSessionService = mock(LoginSessionService.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper,
                mock(ExpertProfileMapper.class),
                smsCodeMapper,
                mock(RoleMapper.class),
                passwordEncoder,
                mock(JwtService.class),
                loginSessionService,
                mock(TokenBlacklistService.class),
                mock(LoginRateLimitService.class)
        );
        User user = new User();
        user.setId(9L);
        user.setPhone("13800138000");
        SmsCode storedCode = new SmsCode();
        storedCode.setId(21L);
        storedCode.setCode("123456");
        ClientPasswordResetRequest request = new ClientPasswordResetRequest();
        request.setPhone("13800138000");
        request.setCode("123456");
        request.setNewPassword("Farm2026");
        request.setConfirmPassword("Farm2026");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(smsCodeMapper.selectLatestValidForUpdate(
                "13800138000",
                ClientAuthServiceImpl.SMS_SCENE_RESET_PASSWORD
        )).thenReturn(storedCode);
        when(smsCodeMapper.markUsed(21L)).thenReturn(1);
        when(passwordEncoder.encode("Farm2026")).thenReturn("bcrypt-password");

        service.resetPassword(9L, request);

        // 密码只以编码后结果入库，随后撤销当前账号所有旧登录会话。
        verify(userMapper).updatePassword(9L, "bcrypt-password");
        verify(loginSessionService).revoke(9L);
    }

    @Test
    void changePhoneConsumesChangeCodeBeforeUpdatingPhone() {
        UserMapper userMapper = mock(UserMapper.class);
        SmsCodeMapper smsCodeMapper = mock(SmsCodeMapper.class);
        ClientAuthServiceImpl service = new ClientAuthServiceImpl(
                userMapper,
                mock(ExpertProfileMapper.class),
                smsCodeMapper,
                mock(RoleMapper.class),
                mock(PasswordEncoder.class),
                mock(JwtService.class),
                mock(LoginSessionService.class),
                mock(TokenBlacklistService.class),
                mock(LoginRateLimitService.class)
        );
        User currentUser = new User();
        currentUser.setId(9L);
        currentUser.setPhone("13800138000");
        User updatedUser = new User();
        updatedUser.setId(9L);
        updatedUser.setPhone("13900139000");
        SmsCode storedCode = new SmsCode();
        storedCode.setId(31L);
        storedCode.setCode("654321");
        when(userMapper.selectById(9L)).thenReturn(currentUser, updatedUser);
        when(smsCodeMapper.selectLatestValidForUpdate(
                "13900139000",
                ClientAuthServiceImpl.SMS_SCENE_CHANGE_PHONE
        )).thenReturn(storedCode);
        when(smsCodeMapper.markUsed(31L)).thenReturn(1);

        User result = service.changePhone(9L, "13900139000", "654321");

        // 换绑只能消费 scene=5 的验证码，并将数据库与响应都更新为新手机号。
        verify(userMapper).updatePhone(9L, "13900139000");
        assertEquals("13900139000", result.getPhone());
    }
}
