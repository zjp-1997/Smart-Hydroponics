package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.Role;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface UserService {

    User addUser(User user);

    void deleteUser(Long id);

    int deleteUsers(List<Long> ids);

    User updateUser(User user);

    void resetPassword(Long id, String password);

    void updateStatus(Long id, Integer status);

    User getUserById(Long id);

    PageInfo<User> listUsers(String username, String phone, String roleCode, Integer status,
                             Integer pageNum, Integer pageSize);

    /** 用户表单只读取当前管理者可分配的角色和农场主。 */
    List<Role> listAssignableRoles();

    List<User> listAvailableFarmOwners();
}
