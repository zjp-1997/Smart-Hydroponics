package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.IotDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备信息 Mapper 接口。
 *
 * <p>该接口只定义数据库访问能力，具体 SQL 写在 IotDeviceMapper.xml 中，保持与用户管理模块一致的 MyBatis 分层风格。</p>
 */
@Mapper
public interface IotDeviceMapper {

    /** 新增设备信息，并回填数据库生成的 id。 */
    int insert(IotDevice iotDevice);

    /** 根据设备ID删除单条设备信息。 */
    int deleteById(Long id);

    /** 根据设备ID集合批量删除设备信息。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据设备ID动态修改设备信息。 */
    int updateById(IotDevice iotDevice);

    /** 单独修改业务控制状态，便于列表页快速开关设备。 */
    int updateControlStatus(@Param("id") Long id, @Param("controlStatus") Integer controlStatus);

    /** 单独修改在线状态，便于心跳或连接层逻辑更新设备在线情况。 */
    int updateOnlineStatus(@Param("id") Long id, @Param("onlineStatus") Integer onlineStatus);

    /** 单独修改健康状态，便于设备故障和维护状态管理。 */
    int updateHealthStatus(@Param("id") Long id, @Param("healthStatus") Integer healthStatus);

    /** 更新最后心跳时间，并同步将设备置为在线。 */
    int updateHeartbeat(@Param("id") Long id, @Param("lastHeartbeatTime") java.time.LocalDateTime lastHeartbeatTime);

    /** 故障全部处理完成后，将设备恢复为健康在线状态。 */
    int restoreOnlineAfterFaultResolved(@Param("id") Long id);

    /** 根据设备ID查询设备详情。 */
    IotDevice selectById(Long id);

    /** 根据设备编码查询设备，用于唯一性校验和外部编码定位。 */
    IotDevice selectByDeviceCode(String deviceCode);

    /** 分页列表查询，支持地块、名称、编码、类型和三类状态筛选。 */
    List<IotDevice> selectList(@Param("userId") Long userId,
                               @Param("plotId") Long plotId,
                               @Param("plotName") String plotName,
                               @Param("deviceCode") String deviceCode,
                               @Param("name") String name,
                               @Param("typeId") Long typeId,
                               @Param("controlStatus") Integer controlStatus,
                               @Param("onlineStatus") Integer onlineStatus,
                               @Param("healthStatus") Integer healthStatus);

    /** 查询健康状态为故障但尚未存在未完成故障单的设备，用于故障列表补偿同步。 */
    List<IotDevice> selectUnhealthyDevicesWithoutOpenFault(@Param("userId") Long userId);

    /** 统计设备编码是否重复，excludeId 用于修改时排除自身。 */
    int countByDeviceCode(@Param("deviceCode") String deviceCode, @Param("excludeId") Long excludeId);

    /** 查询 D_001 这类自动编码中的最大序号，用于新增设备时生成下一个编码。 */
    int selectMaxGeneratedDeviceCodeNumber();

    /** 统计业务表中是否存在设备引用，用于删除前保护历史业务数据。 */
    int countReferencesByDeviceId(@Param("deviceId") Long deviceId);
}
