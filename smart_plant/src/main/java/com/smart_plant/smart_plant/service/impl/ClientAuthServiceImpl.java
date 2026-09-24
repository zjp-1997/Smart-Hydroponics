package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ClientAuthResponse;
import com.smart_plant.smart_plant.dto.ClientLoginRequest;
import com.smart_plant.smart_plant.dto.ClientRegisterRequest;
import com.smart_plant.smart_plant.dto.ClientFarmOwnerOption;
import com.smart_plant.smart_plant.dto.ClientPasswordResetRequest;
import com.smart_plant.smart_plant.dto.ClientSmsCodeResponse;
import com.smart_plant.smart_plant.dto.ClientSmsLoginRequest;
import com.smart_plant.smart_plant.dto.ClientProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.ClientAvatarUploadResponse;
import com.smart_plant.smart_plant.dto.JwtPayload;
import com.smart_plant.smart_plant.entity.SmsCode;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.exception.RateLimitException;
import com.smart_plant.smart_plant.mapper.SmsCodeMapper;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.ClientAuthService;
import com.smart_plant.smart_plant.service.JwtService;
import com.smart_plant.smart_plant.service.LoginRateLimitService;
import com.smart_plant.smart_plant.service.LoginSessionService;
import com.smart_plant.smart_plant.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * farm 用户端认证服务。
 *
 * <p>该服务与 AdminAuthService 分离，核心原因是用户端和管理端的登录角色、
 * 风险控制和返回模型不同：管理端只允许 admin/farm_owner，farm 端只允许指定移动端角色。</p>
 */
@Service
@RequiredArgsConstructor
public class ClientAuthServiceImpl implements ClientAuthService {

    /** 短信登录场景，与数据库 sms_code.scene=1 保持一致。 */
    public static final int SMS_SCENE_LOGIN = 1;

    /** 找回密码场景，与数据库 sms_code.scene=3 保持一致。 */
    public static final int SMS_SCENE_RESET_PASSWORD = 3;

    /** 绑定手机号场景，与数据库 sms_code.scene=4 保持一致。 */
    public static final int SMS_SCENE_BIND_PHONE = 4;

    /** 修改手机号场景，与数据库 sms_code.scene=5 保持一致。 */
    public static final int SMS_SCENE_CHANGE_PHONE = 5;

    /** 模拟验证码五分钟后失效。 */
    private static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);

    /** 同一手机号、同一场景一分钟内不能重复发码。 */
    private static final Duration SMS_SEND_INTERVAL = Duration.ofSeconds(60);

    /** 中国大陆手机号格式；前后端使用同一规则。 */
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    /** 邮箱只做通用结构校验，具体收件能力由用户自行确认。 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /** 头像最大 5MB，并限制为移动端能够直接展示的常见格式。 */
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /** 使用密码学安全随机数，避免验证码序列容易被预测。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 自助注册只能创建这三种账号，绝不接受客户端传入 roleId 或管理员角色。 */
    private static final Set<String> CLIENT_REGISTER_ROLE_CODES = Set.of("user", "technician", "expert");

    /** farm 共用登录页按角色进入工作台；管理员仍不具备移动端登录权限。 */
    private static final Set<String> CLIENT_LOGIN_ROLE_CODES = Set.of("user", "farm_owner", "technician", "expert");

    /** 个人信息页与 farm 登录角色保持一致，管理员等后台角色不能使用该入口。 */
    private static final Set<String> CLIENT_PROFILE_ROLE_CODES = Set.of("user", "farm_owner", "technician", "expert");

    /** 登录失败统一返回同一文案，避免攻击者通过提示枚举账号是否存在。 */
    private static final String LOGIN_FAILED_MESSAGE = "账号或密码错误";

    /**
     * 兼容当前 user.phone NOT NULL 约束的内部占位前缀。
     *
     * <p>farm 当前注册 UI 没有手机号字段，后续如果数据库允许 phone 为空，
     * 可以移除占位逻辑，直接存 null 或改为真正手机号注册。</p>
     */
    private static final String SYNTHETIC_PHONE_PREFIX = "U";

    private final UserMapper userMapper;

    private final ExpertProfileMapper expertProfileMapper;

    private final SmsCodeMapper smsCodeMapper;

    private final RoleMapper roleMapper;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final LoginSessionService loginSessionService;

    private final TokenBlacklistService tokenBlacklistService;

    private final LoginRateLimitService rateLimitService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(ClientRegisterRequest request) {
        // 所有外部输入先 trim，避免同一账号因为首尾空格产生重复身份。
        String username = trim(request == null ? null : request.getUsername());
        String password = trim(request == null ? null : request.getPassword());
        String confirmPassword = trim(request == null ? null : request.getConfirmPassword());
        String phone = trim(request == null ? null : request.getPhone());
        String nickname = trim(request == null ? null : request.getNickname());
        String roleCode = normalizeRoleCode(request == null ? null : request.getRoleCode());
        Long farmOwnerId = request == null ? null : request.getFarmOwnerId();

        validateRegister(username, password, confirmPassword, phone);
        // 角色及农场主归属只由服务端白名单和数据库验证，避免伪造管理员或绑定无效账号。
        if (!CLIENT_REGISTER_ROLE_CODES.contains(roleCode)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择普通用户、技术人员或专家角色");
        }
        if ("expert".equals(roleCode)) {
            if (farmOwnerId != null) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "专家无需绑定农场主");
            }
        } else {
            User owner = farmOwnerId == null ? null : userMapper.selectById(farmOwnerId);
            if (owner == null || !"farm_owner".equals(normalizeRoleCode(owner.getRoleCode()))
                    || !Integer.valueOf(1).equals(owner.getStatus())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择有效的农场主");
            }
        }
        // 账号唯一性先查一次，给客户端返回明确错误；数据库唯一约束仍应作为最终兜底。
        if (userMapper.countByUsername(username, null) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "账号已存在");
        }

        // 当前表结构要求 phone 非空且唯一，因此这里统一解析最终入库手机号。
        String storedPhone = resolveStoredPhone(username, phone);
        if (userMapper.countByPhone(storedPhone, null) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已存在");
        }

        // 角色 ID 由数据库按受限角色编码解析，不能由匿名请求直接指定。
        Role role = resolveClientRole(roleCode);
        User user = new User();
        user.setUsername(username);
        // BCrypt 加密后入库，绝不保存明文密码。
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(storedPhone);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setGender(0);
        user.setRoleId(role.getId());
        user.setRoleName(role.getRoleName());
        user.setRoleCode(role.getRoleCode());
        user.setStatus("expert".equals(roleCode) ? 1 : 0);
        user.setLoginCount(0);

        userMapper.insert(user);
        // 注册只提交申请，审核前不建立任何可用于数据授权的绑定。
        if (!"expert".equals(roleCode)) {
            userMapper.insertFarmJoinRequest(user.getId(), farmOwnerId, roleCode);
        } else {
            ExpertProfile profile = new ExpertProfile();
            profile.setUserId(user.getId());
            profile.setUsername(username);
            profile.setRealName(user.getNickname());
            // 注册只创建认证草稿；专家补齐材料并主动提交后才进入后台审核队列。
            profile.setAuditStatus(0);
            profile.setServiceStatus(0);
            profile.setConsultationStatus(0);
            profile.setStatus(0);
            expertProfileMapper.insert(profile);
        }
        // 重新查询可带出 roleName/roleCode 等关联字段，并依赖 User.password 的序列化规则避免泄露密码。
        return userMapper.selectById(user.getId());
    }

    @Override
    public List<ClientFarmOwnerOption> listFarmOwners() {
        // 只返回最小展示信息，不把 user 表中的电话、邮箱等资料交给匿名页面。
        return userMapper.selectActiveFarmOwners().stream()
                .map(owner -> new ClientFarmOwnerOption(owner.getId(),
                        StringUtils.hasText(owner.getNickname()) ? owner.getNickname() : owner.getUsername()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientAuthResponse login(ClientLoginRequest request, String ip) {
        // username 参与限流 key 计算，保持和注册同样的 trim 规则。
        String username = trim(request == null ? null : request.getUsername());
        String password = request == null ? null : request.getPassword();
        boolean rememberMe = request != null && Boolean.TRUE.equals(request.getRememberMe());

        // 复用现有登录限流服务，按 IP 和账号维度降低暴力破解风险。
        try {
            rateLimitService.check(ip, username);
        } catch (RateLimitException exception) {
            throw exception;
        }

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return fail(ip, username);
        }

        User user = userMapper.selectByUsername(username);
        // BCrypt matches 内部会处理盐值比较，不需要也不应该手动解密密码。
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return fail(ip, username);
        }
        validateClientUser(user, ip, username);

        rateLimitService.recordSuccess(ip, username);
        // 登录成功后更新最后登录时间和次数，便于运营审计和账号安全分析。
        userMapper.updateLastLogin(user.getId());
        User refreshedUser = userMapper.selectByUsername(username);
        return issueTokenPair(refreshedUser, rememberMe);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientSmsCodeResponse sendSmsCode(Long userId, String rawPhone, int scene, String ip, String userAgent) {
        String phone = validatePhone(rawPhone);
        if (scene != SMS_SCENE_LOGIN && scene != SMS_SCENE_RESET_PASSWORD
                && scene != SMS_SCENE_BIND_PHONE && scene != SMS_SCENE_CHANGE_PHONE) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "不支持的验证码场景");
        }
        // 登录发码允许匿名；账号设置中的其他场景必须校验 JWT 对应用户及手机号归属。
        if (scene != SMS_SCENE_LOGIN) {
            validateAuthenticatedSmsScene(userId, phone, scene);
        }

        SmsCode latest = smsCodeMapper.selectLatest(phone, scene);
        LocalDateTime now = LocalDateTime.now();
        // 返回剩余等待秒数能让 farm 端准确提示用户，而不是静默失败。
        if (latest != null && latest.getCreateTime() != null
                && latest.getCreateTime().plus(SMS_SEND_INTERVAL).isAfter(now)) {
            long seconds = Duration.between(now, latest.getCreateTime().plus(SMS_SEND_INTERVAL)).toSeconds() + 1;
            throw new BusinessException(ResponseCode.TOO_MANY_REQUESTS, "请在" + seconds + "秒后重新获取验证码");
        }

        // 新码发送前废弃同场景旧码，用户只能使用最后一次收到的验证码。
        smsCodeMapper.invalidateActive(phone, scene);
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setScene(scene);
        smsCode.setExpireTime(now.plus(SMS_CODE_TTL));
        smsCode.setSendIp(trimToLength(ip, 50));
        smsCode.setUserAgent(trimToLength(userAgent, 255));
        smsCode.setRemark("模拟短信验证码，接入真实短信服务后不得向客户端返回明文验证码");
        smsCodeMapper.insert(smsCode);
        return new ClientSmsCodeResponse(code, SMS_CODE_TTL.toSeconds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientAuthResponse loginBySms(ClientSmsLoginRequest request, String ip) {
        String phone = validatePhone(request == null ? null : request.getPhone());
        String code = validateSmsCode(request == null ? null : request.getCode());
        boolean rememberMe = request != null && Boolean.TRUE.equals(request.getRememberMe());

        // 手机号同样进入账号维度限流，避免绕过密码登录的基础风控。
        rateLimitService.check(ip, phone);
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            rateLimitService.recordFailure(ip, phone);
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "手机号或验证码错误");
        }
        validateClientUser(user, ip, phone);
        try {
            consumeSmsCode(phone, code, SMS_SCENE_LOGIN);
        } catch (BusinessException exception) {
            // 错误验证码计入登录限流，并使用统一提示避免泄露手机号是否已绑定。
            rateLimitService.recordFailure(ip, phone);
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "手机号或验证码错误");
        }
        rateLimitService.recordSuccess(ip, phone);
        userMapper.updateLastLogin(user.getId());
        return issueTokenPair(userMapper.selectById(user.getId()), rememberMe);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User bindPhone(Long userId, String rawPhone, String rawCode) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "登录状态已失效");
        }
        String phone = validatePhone(rawPhone);
        String code = validateSmsCode(rawCode);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        // 首次绑定和换绑是不同安全场景；已有真实手机号时必须走未来的旧手机号验证流程。
        if (StringUtils.hasText(user.getPhone()) && user.getPhone().matches(PHONE_PATTERN)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已绑定，如需更换请使用换绑流程");
        }
        if (userMapper.countByPhone(phone, userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该手机号已绑定其他账号");
        }

        consumeSmsCode(phone, code, SMS_SCENE_BIND_PHONE);
        userMapper.updatePhone(userId, phone);
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User changePhone(Long userId, String rawPhone, String rawCode) {
        String phone = validatePhone(rawPhone);
        String code = validateSmsCode(rawCode);
        User user = requireUser(userId);
        if (!isRealPhone(user.getPhone())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "当前账号尚未绑定手机号");
        }
        if (phone.equals(user.getPhone())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "新手机号不能与当前手机号相同");
        }
        if (userMapper.countByPhone(phone, userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该手机号已绑定其他账号");
        }

        // 换绑使用独立 scene=5，不能拿登录码或首次绑定码跨场景操作。
        consumeSmsCode(phone, code, SMS_SCENE_CHANGE_PHONE);
        userMapper.updatePhone(userId, phone);
        return userMapper.selectById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, ClientPasswordResetRequest request) {
        User user = requireUser(userId);
        String phone = validatePhone(request == null ? null : request.getPhone());
        String code = validateSmsCode(request == null ? null : request.getCode());
        String newPassword = trim(request == null ? null : request.getNewPassword());
        String confirmPassword = trim(request == null ? null : request.getConfirmPassword());
        if (!phone.equals(user.getPhone())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入当前账号已绑定的手机号");
        }
        validatePassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "两次输入的新密码不一致");
        }

        // 验证码先校验并消费，密码只保存 BCrypt 密文；成功后旧 Token 会话立即失效。
        consumeSmsCode(phone, code, SMS_SCENE_RESET_PASSWORD);
        userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
        loginSessionService.revoke(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateProfile(Long userId, ClientProfileUpdateRequest request) {
        User currentUser = requireClientProfileUser(userId);
        validateProfile(request);

        // 页面未填写手机号时保留注册阶段的内部占位值，修改其他字段不会被数据库非空约束阻断。
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : currentUser.getPhone();
        if (StringUtils.hasText(request.getPhone()) && !phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入正确的11位手机号");
        }
        request.setUsername(request.getUsername().trim());
        request.setPhone(phone);
        request.setNickname(trimToNull(request.getNickname()));
        request.setEmail(trimToNull(request.getEmail()));
        request.setAvatar(trimToNull(request.getAvatar()));
        request.setRemark(trimToNull(request.getRemark()));
        request.setGender(request.getGender() == null ? 0 : request.getGender());

        if (userMapper.countByUsername(request.getUsername(), userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户名已存在");
        }
        if (userMapper.countByPhone(phone, userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已绑定其他账号");
        }
        if (StringUtils.hasText(request.getEmail()) && userMapper.countByEmail(request.getEmail(), userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱已存在");
        }
        if (userMapper.updateClientProfile(userId, request) != 1) {
            throw new BusinessException(ResponseCode.FAIL, "个人信息保存失败");
        }
        return userMapper.selectById(userId);
    }

    @Override
    public ClientAvatarUploadResponse uploadProfileAvatar(Long userId, MultipartFile file) {
        requireClientProfileUser(userId);
        validateAvatar(file);
        String contentType = file.getContentType().trim().toLowerCase(Locale.ROOT);
        String extension = "image/jpeg".equals(contentType) ? "jpg" : contentType.substring("image/".length());
        LocalDate today = LocalDate.now();
        Path uploadDirectory = Paths.get(System.getProperty("user.dir"))
                .resolve("uploads").resolve("profile-avatars").resolve(today.toString());
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDirectory);
            Path targetPath = uploadDirectory.resolve(fileName).normalize();
            if (!targetPath.startsWith(uploadDirectory.normalize())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "头像文件名不合法");
            }
            file.transferTo(targetPath);
        } catch (IOException exception) {
            throw new BusinessException(ResponseCode.FAIL, "头像上传失败");
        }
        return new ClientAvatarUploadResponse("/uploads/profile-avatars/" + today + "/" + fileName, file.getSize());
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "令牌不能为空");
        }
        // 将当前访问令牌加入黑名单，并撤销该账号的服务端会话，阻止退出后继续访问或刷新令牌。
        JwtPayload payload = jwtService.parseToken(token);
        tokenBlacklistService.blacklist(payload.getJti(), payload.getExp());
        loginSessionService.revoke(payload.getAdminId());
    }

    /**
     * 注册参数校验。
     *
     * <p>这里做服务端强校验，即使前端校验被绕过，也能保证入库数据满足基本质量要求。</p>
     */
    private void validateRegister(String username, String password, String confirmPassword, String phone) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "账号不能为空");
        }
        if (username.length() > 50) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "账号长度不能超过50个字符");
        }
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "两次密码不一致");
        }
        if (StringUtils.hasText(phone) && phone.length() > 20) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号长度不能超过20个字符");
        }
    }

    /**
     * 解析最终写入 user.phone 的值。
     *
     * <p>当前数据库 user.phone 是 NOT NULL + UNIQUE，而 farm 注册页只有账号密码。
     * 因此策略是：优先使用真实 phone；账号本身是手机号时复用账号；否则生成内部唯一占位值。</p>
     */
    private String resolveStoredPhone(String username, String phone) {
        if (StringUtils.hasText(phone)) {
            return phone;
        }
        if (username.matches("^1[3-9]\\d{9}$")) {
            return username;
        }
        for (int i = 0; i < 3; i++) {
            String syntheticPhone = SYNTHETIC_PHONE_PREFIX + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(10000, 100000);
            if (userMapper.countByPhone(syntheticPhone, null) == 0) {
                return syntheticPhone;
            }
        }
        throw new BusinessException(ResponseCode.FAIL, "注册繁忙，请稍后重试");
    }

    /** 从角色表读取经白名单校验的自助注册角色，保证与 RBAC 数据一致。 */
    private Role resolveClientRole(String roleCode) {
        Role role = roleMapper.selectByRoleCode(roleCode);
        if (role == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "注册角色不存在");
        }
        return role;
    }

    /**
     * 登录失败统一出口。
     *
     * <p>无论账号不存在还是密码错误，都记录失败并返回统一文案，减少账号枚举风险。</p>
     */
    private ClientAuthResponse fail(String ip, String username) {
        rateLimitService.recordFailure(ip, username);
        throw new BusinessException(ResponseCode.UNAUTHORIZED, LOGIN_FAILED_MESSAGE);
    }

    /** 校验账号状态和用户端角色，密码与验证码登录共享同一安全边界。 */
    private String disabledAccountMessage(User user) {
        var request = user == null ? null : userMapper.selectFarmJoinRequestByUserId(user.getId());
        if (request != null) {
            return switch (request.getStatus()) {
                case 0 -> "入场申请待审核，请等待农场主或管理员批准";
                case 2 -> "入场申请已拒绝，请联系农场主或管理员";
                case 3 -> "入场授权已撤销，请联系农场主或管理员";
                default -> "账号已冻结请联系管理员";
            };
        }
        return "账号已冻结请联系管理员";
    }

    private void validateClientUser(User user, String ip, String account) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            rateLimitService.recordFailure(ip, account);
            throw new BusinessException(ResponseCode.FORBIDDEN, disabledAccountMessage(user));
        }
        if (!CLIENT_LOGIN_ROLE_CODES.contains(normalizeRoleCode(user.getRoleCode()))) {
            rateLimitService.recordFailure(ip, account);
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前账号无用户端登录权限");
        }
        Role role = roleMapper.selectById(user.getRoleId());
        if (role == null || !Integer.valueOf(1).equals(role.getStatus())) {
            rateLimitService.recordFailure(ip, account);
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前角色已禁用，请联系管理员");
        }
    }

    /** 锁定、比对并一次性消费验证码，错误时不暴露是过期还是数字不匹配。 */
    private void consumeSmsCode(String phone, String code, int scene) {
        SmsCode storedCode = smsCodeMapper.selectLatestValidForUpdate(phone, scene);
        if (storedCode == null || !storedCode.getCode().equals(code) || smsCodeMapper.markUsed(storedCode.getId()) != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "验证码错误或已过期");
        }
    }

    /** 获取当前用户，所有账号设置接口统一通过此方法处理失效身份。 */
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "登录状态已失效");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /** 个人信息编辑页只向 farm 移动端角色开放，服务端验证角色以防绕过页面访问。 */
    private User requireClientProfileUser(Long userId) {
        User user = requireUser(userId);
        if (!CLIENT_PROFILE_ROLE_CODES.contains(normalizeRoleCode(user.getRoleCode()))) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "当前角色不可修改移动端个人信息");
        }
        return user;
    }

    /** 对个人信息字段执行与 user 表长度和约束一致的校验。 */
    private void validateProfile(ClientProfileUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "用户名不能为空");
        }
        validateLength(request.getUsername(), 50, "用户名");
        validateLength(request.getNickname(), 50, "昵称");
        validateLength(request.getEmail(), 100, "邮箱");
        validateLength(request.getAvatar(), 255, "头像地址");
        validateLength(request.getRemark(), 255, "个性签名");
        if (StringUtils.hasText(request.getEmail())
                && !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (request.getGender() != null && request.getGender() != 0
                && request.getGender() != 1 && request.getGender() != 2) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "性别只能为0、1或2");
        }
    }

    /** 头像文件在写盘前校验大小和媒体类型。 */
    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要上传的头像");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "头像大小不能超过5MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "头像仅支持JPG、PNG或WEBP格式");
        }
    }

    private void validateLength(String value, int maxLength, String fieldName) {
        if (value != null && value.trim().length() > maxLength) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, fieldName + "不能超过" + maxLength + "个字符");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 内部 U 前缀占位值不属于真实手机号，不能用于换绑或找回密码。 */
    private boolean isRealPhone(String phone) {
        return StringUtils.hasText(phone) && phone.matches(PHONE_PATTERN);
    }

    /** 校验受保护发码场景，避免账号操作验证码被发送到不符合业务条件的号码。 */
    private void validateAuthenticatedSmsScene(Long userId, String phone, int scene) {
        User user = requireUser(userId);
        boolean hasBoundPhone = isRealPhone(user.getPhone());
        if (scene == SMS_SCENE_RESET_PASSWORD && (!hasBoundPhone || !phone.equals(user.getPhone()))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入当前账号已绑定的手机号");
        }
        if (scene == SMS_SCENE_BIND_PHONE && hasBoundPhone) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "手机号已绑定，请使用修改手机号功能");
        }
        if (scene == SMS_SCENE_CHANGE_PHONE) {
            if (!hasBoundPhone) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "当前账号尚未绑定手机号");
            }
            if (phone.equals(user.getPhone())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "新手机号不能与当前手机号相同");
            }
        }
        if ((scene == SMS_SCENE_BIND_PHONE || scene == SMS_SCENE_CHANGE_PHONE)
                && userMapper.countByPhone(phone, userId) > 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "该手机号已绑定其他账号");
        }
    }

    /** 注册与找回密码共享相同强度规则，避免两个入口产生不一致密码质量。 */
    private void validatePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "密码不能为空");
        }
        if (password.length() < 8 || password.length() > 32
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*\\d.*")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "密码需为8到32个字符，且同时包含字母和数字");
        }
    }

    /** 手机号属于外部身份数据，必须在服务端再次进行格式校验。 */
    private String validatePhone(String rawPhone) {
        String phone = trim(rawPhone);
        if (!phone.matches(PHONE_PATTERN)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入正确的11位手机号");
        }
        return phone;
    }

    /** 验证码固定为六位数字，拒绝空格和其他字符。 */
    private String validateSmsCode(String rawCode) {
        String code = trim(rawCode);
        if (!code.matches("^\\d{6}$")) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请输入6位验证码");
        }
        return code;
    }

    /** 审计字段按数据库长度截断，防止超长请求头导致发码失败。 */
    private String trimToLength(String value, int maxLength) {
        String normalized = trim(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    /**
     * 签发 accessToken/refreshToken 并保存当前有效会话。
     *
     * <p>JwtService 负责令牌生成和签名，LoginSessionService 负责服务端会话有效性控制，
     * 两者结合可以支持退出登录、单账号会话覆盖和 token 黑名单等能力。</p>
     */
    private ClientAuthResponse issueTokenPair(User user, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(user, rememberMe);
        String refreshToken = jwtService.generateRefreshToken(user, rememberMe);
        JwtPayload accessPayload = jwtService.parseToken(accessToken);
        JwtPayload refreshPayload = jwtService.parseToken(refreshToken);
        loginSessionService.save(
                user.getId(),
                accessPayload.getJti(),
                accessPayload.getExp(),
                refreshPayload.getJti(),
                refreshPayload.getExp()
        );
        return new ClientAuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                LocalDateTime.ofInstant(Instant.ofEpochSecond(accessPayload.getExp()), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(Instant.ofEpochSecond(refreshPayload.getExp()), ZoneId.systemDefault()),
                user,
                normalizeRoleCode(user.getRoleCode())
        );
    }

    /** 角色编码统一小写比较，避免数据库大小写差异导致权限判断失效。 */
    private String normalizeRoleCode(String roleCode) {
        return StringUtils.hasText(roleCode) ? roleCode.trim().toLowerCase(Locale.ROOT) : "";
    }

    /** 对可空字符串做统一清洗，减少各处重复 null 判断。 */
    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
