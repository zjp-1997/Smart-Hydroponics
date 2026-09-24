package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.entity.FarmJoinRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.smart_plant.smart_plant.dto.AdminProfileUpdateRequest;
import com.smart_plant.smart_plant.dto.ClientProfileUpdateRequest;

@Mapper
public interface UserMapper {

    int insertFarmJoinRequest(@Param("userId") Long userId, @Param("ownerId") Long ownerId,
                             @Param("roleCode") String roleCode);

    FarmJoinRequest selectFarmJoinRequestByUserId(@Param("userId") Long userId);

    /** 所有审核及通用编辑均先锁申请，串行化权限变更。 */
    FarmJoinRequest lockFarmJoinRequest(@Param("userId") Long userId);

    List<FarmJoinRequest> selectFarmJoinRequests(
            @Param("ownerId") Long ownerId, @Param("status") Integer status);

    /** 仅统计待审核申请；ownerId 为空表示管理员的全局统计范围。 */
    long countPendingFarmJoinRequests(@Param("ownerId") Long ownerId);

    int decideFarmJoinRequest(@Param("userId") Long userId, @Param("expectedStatus") int expectedStatus,
                             @Param("status") int status, @Param("reviewerId") Long reviewerId,
                             @Param("reason") String reason);


    int insert(User user);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    User selectById(Long id);

    User selectByUsername(String username);

    /** 按已绑定手机号精确查询用户，供短信验证码登录使用。 */
    User selectByPhone(String phone);

    int updateLastLogin(@Param("id") Long id);

    /** 只更新当前用户手机号，避免绑定接口覆盖其他账号资料。 */
    int updatePhone(@Param("id") Long id, @Param("phone") String phone);

    /** 仅更新个人中心允许编辑的资料字段，禁止通过该入口修改角色或账号状态。 */
    int updateProfile(@Param("id") Long id, @Param("profile") AdminProfileUpdateRequest profile);

    /** 仅更新 farm 农场主个人信息页开放的七个字段。 */
    int updateClientProfile(@Param("id") Long id, @Param("profile") ClientProfileUpdateRequest profile);

    List<User> selectList(@Param("username") String username,
                          @Param("phone") String phone,
                          @Param("roleCode") String roleCode,
                          @Param("status") Integer status,
                          @Param("ownerId") Long ownerId);

    /** 查询所有启用用户，用于系统公告发布时生成接收明细。 */
    List<User> selectActiveUsers();

    int countByRoleId(Long roleId);

    int countByRoleIds(@Param("roleIds") List<Long> roleIds);

    int countByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    int countByPhone(@Param("phone") String phone, @Param("excludeId") Long excludeId);

    int countByEmail(@Param("email") String email, @Param("excludeId") Long excludeId);

    /** 查询与农场主绑定且启用的技术人员，用于设备故障自动派单。 */
    Long selectBoundTechnicianIdByOwnerId(@Param("ownerId") Long ownerId);

    /** 统计技术人员是否与指定农场主存在启用绑定关系。 */
    int countActiveTechnicianBinding(@Param("ownerId") Long ownerId, @Param("technicianId") Long technicianId);

    /** 注册页只展示启用中的农场主账号。 */
    List<User> selectActiveFarmOwners();

    /** 可信管理操作或审核通过后建立故障派单使用的绑定。 */
    int insertFarmOwnerTechnician(@Param("ownerId") Long ownerId, @Param("technicianId") Long technicianId);

    /** 可信管理操作或审核通过后建立普通用户所属关系。 */
    int insertFarmOwnerUser(@Param("ownerId") Long ownerId, @Param("userId") Long userId);

    /** 编辑角色、调整归属或删除账号时先清理旧绑定。 */
    int deleteFarmOwnerUserBindings(@Param("userId") Long userId);

    int deleteFarmOwnerTechnicianBindings(@Param("technicianId") Long technicianId);

    /** 农场主只可管理绑定到自己的普通用户和技术员。 */
    int countManagedUserByOwnerId(@Param("ownerId") Long ownerId, @Param("userId") Long userId);

    /** 按普通用户账号查询注册时绑定的农场主，服务端用它限定移动端数据范围。 */
    Long selectBoundOwnerIdByUserId(@Param("userId") Long userId);

    /** 判断指定普通用户是否属于农场主，用于校验农事任务的执行人。 */
    int countBoundUserByOwnerId(@Param("ownerId") Long ownerId, @Param("userId") Long userId);
}
