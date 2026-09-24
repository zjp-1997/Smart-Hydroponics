package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.Role;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@RequirePermission("user:manage")
public class UserController {

    private final UserService userService;

    /** 新增表单使用当前账号可分配的角色，农场主无需获得角色管理权限。 */
    @GetMapping("/assignable-roles")
    public R<List<Role>> listAssignableRoles() {
        return R.success(userService.listAssignableRoles());
    }

    /** 管理员可选全部有效农场主，农场主只能选择自己。 */
    @GetMapping("/farm-owners")
    public R<List<User>> listAvailableFarmOwners() {
        return R.success(userService.listAvailableFarmOwners());
    }

    @PostMapping("/add")
    public R<User> addUser(@RequestBody User user) {
        return R.success(userService.addUser(user));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteUsers(@RequestBody List<Long> ids) {
        return R.success(userService.deleteUsers(ids));
    }

    @PutMapping
    public R<User> updateUser(@RequestBody User user) {
        return R.success(userService.updateUser(user));
    }

    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id,
                                 @RequestParam String password) {
        userService.resetPassword(id, password);
        return R.success();
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.success();
    }

    @GetMapping("/{id}")
    public R<User> getUserById(@PathVariable Long id) {
        return R.success(userService.getUserById(id));
    }

    @GetMapping("/list")
    public R<PageInfo<User>> listUsers(@RequestParam(required = false) String username,
                                       @RequestParam(required = false) String phone,
                                       @RequestParam(required = false) String roleCode,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(userService.listUsers(username, phone, roleCode, status, pageNum, pageSize));
    }
}
