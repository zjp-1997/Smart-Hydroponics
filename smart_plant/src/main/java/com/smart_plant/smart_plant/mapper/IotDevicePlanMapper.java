package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.IotDevicePlan;
import com.smart_plant.smart_plant.entity.IotDeviceSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 设备计划与运行时段的数据访问层。 */
@Mapper
public interface IotDevicePlanMapper {
    /** 查询设备和计划的合并视图，LEFT JOIN 保证未配置计划的设备仍可展示。 */
    List<IotDevicePlan> selectDevicePlans(@Param("userId") Long userId,
                                          @Param("plotId") Long plotId,
                                          @Param("typeCode") String typeCode,
                                          @Param("keyword") String keyword);

    /** 按设备读取计划；结果同时包含设备基础信息。 */
    IotDevicePlan selectByDeviceId(@Param("deviceId") Long deviceId);

    /** 批量读取计划下的自动运行时段，避免逐条查询。 */
    List<IotDeviceSchedule> selectSchedulesByPlanIds(@Param("planIds") List<Long> planIds);

    /** 新建一条设备计划。 */
    int insertPlan(IotDevicePlan plan);

    /** 使用版本号执行乐观锁更新。 */
    int updatePlan(IotDevicePlan plan);

    /** 更新计划前清理旧时段，随后由事务批量写入新时段。 */
    int deleteSchedulesByPlanId(@Param("planId") Long planId);

    /** 批量写入补光灯或水泵运行时段。 */
    int insertSchedules(@Param("schedules") List<IotDeviceSchedule> schedules);

    /** 按设备删除计划，数据库外键会级联删除关联时段。 */
    int deletePlanByDeviceId(@Param("deviceId") Long deviceId);
}
