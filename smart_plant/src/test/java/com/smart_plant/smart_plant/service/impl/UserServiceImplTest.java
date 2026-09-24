package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.RoleMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class UserServiceImplTest {

    private final UserMapper userMapper = mock(UserMapper.class);

    private final RoleMapper roleMapper = mock(RoleMapper.class);

    private final ExpertProfileMapper expertProfileMapper = mock(ExpertProfileMapper.class);

    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final UserServiceImpl userService = new UserServiceImpl(
            userMapper,
            roleMapper,
            expertProfileMapper,
            passwordEncoder
    );

    @BeforeEach
    void logInAsAdmin() {
        CurrentUserContext.set(user(1L, "admin"));
    }

    @AfterEach
    void clearLogin() {
        CurrentUserContext.clear();
    }

    @Test
    void adminCreatesBoundOrdinaryUser() {
        User request = newUser(21L, 5L);
        User owner = user(21L, "farm_owner");
        owner.setStatus(1);
        when(roleMapper.selectById(5L)).thenReturn(role(5L, "user"));
        when(userMapper.selectById(21L)).thenReturn(owner);
        doAnswer(call -> { ((User) call.getArgument(0)).setId(31L); return 1; })
                .when(userMapper).insert(request);

        userService.addUser(request);

        verify(userMapper).insertFarmOwnerUser(21L, 31L);
        verify(userMapper, never()).insertFarmOwnerTechnician(21L, 31L);
    }

    @Test
    void farmOwnerCreatesOnlyOwnTechnician() {
        CurrentUserContext.set(user(21L, "farm_owner"));
        User request = newUser(null, 3L);
        when(roleMapper.selectById(3L)).thenReturn(role(3L, "technician"));
        doAnswer(call -> { ((User) call.getArgument(0)).setId(32L); return 1; })
                .when(userMapper).insert(request);

        userService.addUser(request);

        verify(userMapper).insertFarmOwnerTechnician(21L, 32L);
    }

    @Test
    void farmOwnerCannotCreateAdminOrBindAnotherOwner() {
        CurrentUserContext.set(user(21L, "farm_owner"));
        User adminRequest = newUser(null, 2L);
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "admin"));
        assertThrows(BusinessException.class, () -> userService.addUser(adminRequest));

        User otherFarmRequest = newUser(22L, 5L);
        when(roleMapper.selectById(5L)).thenReturn(role(5L, "user"));
        assertThrows(BusinessException.class, () -> userService.addUser(otherFarmRequest));
        verify(userMapper, never()).insert(otherFarmRequest);
    }

    @Test
    void farmOwnerCannotDeleteAnotherFarmUser() {
        CurrentUserContext.set(user(21L, "farm_owner"));
        assertThrows(BusinessException.class, () -> userService.deleteUser(41L));
        verify(userMapper, never()).deleteById(41L);
    }

    @Test
    void farmOwnerUserListIsScopedToOwnBindings() {
        CurrentUserContext.set(user(21L, "farm_owner"));
        when(userMapper.selectList(null, null, null, null, 21L)).thenReturn(List.of());

        userService.listUsers(null, null, null, null, 1, 10);

        verify(userMapper).selectList(null, null, null, null, 21L);
    }

    @Test
    void farmOwnerCannotEditAnotherFarmUser() {
        CurrentUserContext.set(user(21L, "farm_owner"));
        User existing = user(41L, "user");
        when(userMapper.selectById(41L)).thenReturn(existing);
        User update = user(41L, null);
        update.setNickname("unauthorized");

        assertThrows(BusinessException.class, () -> userService.updateUser(update));

        verify(userMapper, never()).updateById(update);
    }

    private User newUser(Long farmOwnerId, Long roleId) {
        User user = user(null, null);
        user.setUsername("new-worker");
        user.setPassword("Password123");
        user.setPhone("13800000000");
        user.setRoleId(roleId);
        user.setFarmOwnerId(farmOwnerId);
        return user;
    }

    private User user(Long id, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setRoleCode(roleCode);
        return user;
    }

    private Role role(Long id, String code) {
        Role role = new Role();
        role.setId(id);
        role.setRoleCode(code);
        role.setRoleName(code);
        return role;
    }

    @Test
    void deleteUserRejectsApprovedExpertProfile() {
        Long userId = 8L;
        when(expertProfileMapper.countApprovedByUserId(userId)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(userId));

        assertEquals("该用户已通过专家认证审核，不可直接删除", exception.getMessage());
        verify(expertProfileMapper, never()).deleteUnapprovedByUserId(userId);
        verify(userMapper, never()).deleteById(userId);
    }

    @Test
    void deleteUserRemovesUnapprovedExpertProfileBeforeUser() {
        Long userId = 9L;
        when(expertProfileMapper.countApprovedByUserId(userId)).thenReturn(0);
        when(userMapper.deleteById(userId)).thenReturn(1);

        userService.deleteUser(userId);

        verify(expertProfileMapper).deleteUnapprovedByUserId(userId);
        verify(userMapper).deleteFarmOwnerUserBindings(userId);
        verify(userMapper).deleteFarmOwnerTechnicianBindings(userId);
        verify(userMapper).deleteById(userId);
    }

    @Test
    void deleteUsersRejectsWhenAnyApprovedExpertProfileExists() {
        List<Long> userIds = List.of(10L, 11L);
        when(expertProfileMapper.countApprovedByUserIds(userIds)).thenReturn(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUsers(userIds));

        assertEquals("选中用户中存在已通过专家认证审核的用户，不可直接批量删除", exception.getMessage());
        verify(expertProfileMapper, never()).deleteUnapprovedByUserIds(userIds);
        verify(userMapper, never()).deleteBatchByIds(userIds);
    }

    @Test
    void deleteUsersRemoveUnapprovedExpertProfilesBeforeUsers() {
        List<Long> userIds = List.of(12L, 13L);
        when(expertProfileMapper.countApprovedByUserIds(userIds)).thenReturn(0);
        when(userMapper.deleteBatchByIds(userIds)).thenReturn(2);

        int deletedCount = userService.deleteUsers(userIds);

        assertEquals(2, deletedCount);
        verify(expertProfileMapper).deleteUnapprovedByUserIds(userIds);
        verify(userMapper).deleteBatchByIds(userIds);
    }
}
