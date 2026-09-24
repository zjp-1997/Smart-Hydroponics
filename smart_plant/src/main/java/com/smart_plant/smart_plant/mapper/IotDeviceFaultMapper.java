package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.IotDeviceFault;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备故障 Mapper，负责 iot_device_fault 表读写。
 */
@Mapper
public interface IotDeviceFaultMapper {

    int insert(IotDeviceFault fault);

    int softDeleteById(Long id);

    int softDeleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(IotDeviceFault fault);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("handleResult") String handleResult);

    /** 将已上传的现场凭证绑定到刚完成的故障单。 */
    int updateCompletionImage(@Param("id") Long id, @Param("imageUrl") String imageUrl);

    /** 农场主处理自己设备的待处理故障时，原子认领并切换为处理中。 */
    int startOwnedFault(@Param("id") Long id, @Param("ownerId") Long ownerId);

    int updateAssignee(@Param("id") Long id, @Param("handleUserId") Long handleUserId);

    int updateAssignmentStatus(@Param("id") Long id,
                               @Param("assignStatus") Integer assignStatus,
                               @Param("status") Integer status,
                               @Param("handleResult") String handleResult);

    IotDeviceFault selectById(Long id);

    IotDeviceFault selectOpenByDeviceId(@Param("deviceId") Long deviceId);

    int countActiveOwnedFault(@Param("id") Long id, @Param("userId") Long userId);

    List<IotDeviceFault> selectList(@Param("userId") Long userId,
                                    @Param("deviceId") Long deviceId,
                                    @Param("deviceName") String deviceName,
                                    @Param("faultCode") String faultCode,
                                    @Param("faultName") String faultName,
                                    @Param("faultType") Integer faultType,
                                    @Param("severity") Integer severity,
                                    @Param("status") Integer status,
                                    @Param("handleUserId") Long handleUserId);

}
