package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.FarmJoinRequest;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.*;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.impl.DataPermissionServiceImpl;
import com.smart_plant.smart_plant.service.impl.UserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FarmJoinRequestServiceTest {
    private final UserMapper mapper = mock(UserMapper.class);
    private final LoginSessionService sessions = mock(LoginSessionService.class);
    private final FarmJoinRequestService service = new FarmJoinRequestService(mapper, sessions);
    private final FarmJoinRequest request = new FarmJoinRequest();
    private final User applicant = user(12L, "user", 0);

    @BeforeEach
    void setup() {
        CurrentUserContext.set(user(7L, "farm_owner", 1));
        request.setUserId(12L);
        request.setOwnerUserId(7L);
        request.setRequestedRole("user");
        request.setStatus(0);
        when(mapper.lockFarmJoinRequest(12L)).thenReturn(request);
        when(mapper.selectById(12L)).thenReturn(applicant);
        when(mapper.selectById(7L)).thenReturn(user(7L, "farm_owner", 1));
        when(mapper.updateStatus(eq(12L), anyInt())).thenReturn(1);
        when(mapper.decideFarmJoinRequest(eq(12L), anyInt(), anyInt(), anyLong(), anyString())).thenReturn(1);
    }

    @AfterEach
    void cleanup() { CurrentUserContext.clear(); }

    /** 数量接口沿用列表的数据权限，农场主不能统计其他农场的申请。 */
    @Test
    void ownerCountsOnlyOwnPendingRequests() {
        when(mapper.countPendingFarmJoinRequests(7L)).thenReturn(3L);
        assertEquals(3L, service.countPending());
        verify(mapper).countPendingFarmJoinRequests(7L);
        verify(mapper, never()).countPendingFarmJoinRequests(isNull());
    }

    /** 管理员可统计全部农场，并使用long承载数据库COUNT返回值。 */
    @Test
    void adminCountsAllPendingRequests() {
        CurrentUserContext.set(user(1L, "admin", 1));
        when(mapper.countPendingFarmJoinRequests(null)).thenReturn(2147483648L);
        assertEquals(2147483648L, service.countPending());
        verify(mapper).countPendingFarmJoinRequests(null);
    }

    /** 没有待处理申请时返回数值0，前端据此显示空待办状态。 */
    @Test
    void emptyPendingCountReturnsZero() {
        when(mapper.countPendingFarmJoinRequests(7L)).thenReturn(0L);
        assertEquals(0L, service.countPending());
    }

    /** 未登录的统计请求必须在查询数据库之前拒绝。 */
    @Test
    void anonymousCannotCountPendingRequests() {
        CurrentUserContext.clear();
        var error = assertThrows(BusinessException.class, service::countPending);
        assertEquals(401, error.getCode());
        verify(mapper, never()).countPendingFarmJoinRequests(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "technician"})
    void approvalIsOnlyPlaceThatEnablesAndBinds(String role) {
        request.setRequestedRole(role);
        applicant.setRoleCode(role);
        service.decide(12L, 1, "已核实身份");
        if ("user".equals(role)) verify(mapper).insertFarmOwnerUser(7L, 12L);
        else verify(mapper).insertFarmOwnerTechnician(7L, 12L);
        verify(mapper).updateStatus(12L, 1);
        verify(mapper).decideFarmJoinRequest(12L, 0, 1, 7L, "已核实身份");
    }

    @Test
    void adminCanReviewOtherOwnersApplication() {
        CurrentUserContext.set(user(1L, "admin", 1));
        service.decide(12L, 1, "");
        verify(mapper).decideFarmJoinRequest(12L, 0, 1, 1L, "");
    }

    @Test
    void cannotReviewAnotherFarmEvenWithKnownApplicantId() {
        CurrentUserContext.set(user(8L, "farm_owner", 1));
        assertThrows(BusinessException.class, () -> service.decide(12L, 1, ""));
        verify(mapper, never()).updateStatus(anyLong(), anyInt());
        verify(mapper, never()).insertFarmOwnerUser(anyLong(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "technician", "expert"})
    void nonManagersCannotApproveOrList(String role) {
        CurrentUserContext.set(user(12L, role, 1));
        assertThrows(BusinessException.class, () -> service.decide(12L, 1, ""));
        assertThrows(BusinessException.class, () -> service.list(null, 1, 10));
        // 普通用户、技术人员和专家即使知道统计地址，也不能读取待审核数量。
        assertEquals(403, assertThrows(BusinessException.class, service::countPending).getCode());
        verify(mapper, never()).countPendingFarmJoinRequests(any());
        verify(mapper, never()).lockFarmJoinRequest(anyLong());
    }

    @Test
    void rejectionNeverCreatesBinding() {
        service.decide(12L, 2, "身份不符");
        verify(mapper).updateStatus(12L, 0);
        verify(mapper, never()).insertFarmOwnerUser(anyLong(), anyLong());
        verify(mapper, never()).insertFarmOwnerTechnician(anyLong(), anyLong());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void processedApplicationCannotBeApprovedAgain(int status) {
        request.setStatus(status);
        assertThrows(BusinessException.class, () -> service.decide(12L, 1, ""));
        verify(mapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    void invalidOwnerAndChangedApplicantAreRejected() {
        when(mapper.selectById(7L)).thenReturn(user(7L, "farm_owner", 0));
        assertThrows(BusinessException.class, () -> service.decide(12L, 1, ""));
        applicant.setRoleCode("admin");
        assertThrows(BusinessException.class, () -> service.decide(12L, 1, ""));
        verify(mapper, never()).updateStatus(anyLong(), anyInt());
    }

    @Test
    void revocationDeletesBindingDisablesAccountAndRevokesSession() {
        request.setStatus(1);
        applicant.setStatus(1);
        applicant.setFarmOwnerId(7L);
        service.decide(12L, 3, "成员离场");
        verify(mapper).deleteFarmOwnerUserBindings(12L);
        verify(mapper).deleteFarmOwnerTechnicianBindings(12L);
        verify(mapper).updateStatus(12L, 0);
        verify(sessions).revoke(12L);
        verify(mapper).decideFarmJoinRequest(12L, 1, 3, 7L, "成员离场");
    }

    @Test
    void staleOwnerCannotRevokeTransferredAccount() {
        request.setStatus(1);
        applicant.setFarmOwnerId(8L);
        assertThrows(BusinessException.class, () -> service.decide(12L, 3, "撤销"));
        verify(mapper, never()).deleteFarmOwnerUserBindings(anyLong());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2, 3})
    void genericUpdateAndEnableCannotBypassReview(int status) {
        request.setStatus(status);
        CurrentUserContext.set(user(1L, "admin", 1));
        var users = new UserServiceImpl(mapper, mock(RoleMapper.class), mock(ExpertProfileMapper.class), mock(PasswordEncoder.class));
        assertThrows(BusinessException.class, () -> users.updateStatus(12L, 1));
        applicant.setStatus(1);
        applicant.setFarmOwnerId(7L);
        assertThrows(BusinessException.class, () -> users.updateUser(applicant));
        verify(mapper, never()).updateStatus(anyLong(), anyInt());
        verify(mapper, never()).updateById(any());
    }

    @Test
    void pendingOrRevokedApplicantCannotReadFarmAndApprovedMemberCannotReadAnotherFarm() {
        CurrentUserContext.set(applicant);
        var permissions = new DataPermissionServiceImpl(mapper);
        assertThrows(BusinessException.class, () -> permissions.requireClientFarmReader(7L));
        when(mapper.selectBoundOwnerIdByUserId(12L)).thenReturn(7L);
        assertDoesNotThrow(() -> permissions.requireClientFarmReader(7L));
        assertThrows(BusinessException.class, () -> permissions.requireClientFarmReader(8L));
        when(mapper.selectBoundOwnerIdByUserId(12L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> permissions.requireClientFarmReader(7L));
    }

    private static User user(Long id, String role, int status) {
        User user = new User(); user.setId(id); user.setRoleCode(role); user.setStatus(status); return user;
    }
}
