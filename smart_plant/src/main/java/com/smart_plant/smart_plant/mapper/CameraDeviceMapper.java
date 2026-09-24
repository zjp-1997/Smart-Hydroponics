package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CameraDevice;
import com.smart_plant.smart_plant.dto.ClientMonitorResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监控设备 Mapper 接口。
 *
 * <p>该接口声明 camera_device 表的增删改查、状态维护和删除引用保护方法。</p>
 */
@Mapper
public interface CameraDeviceMapper {

    /** 新增监控设备。 */
    int insert(CameraDevice cameraDevice);

    /** 根据主键删除监控设备。 */
    int deleteById(Long id);

    /** 批量删除监控设备。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据主键动态更新监控设备。 */
    int updateById(CameraDevice cameraDevice);

    /** 更新在线状态。 */
    int updateOnlineStatus(@Param("id") Long id, @Param("onlineStatus") Integer onlineStatus);

    /** 保存最近一次云台控制方向，便于设备网关读取和状态追踪。 */
    int updateDirection(@Param("id") Long id,
                        @Param("userId") Long userId,
                        @Param("direction") String direction);

    /** 根据主键查询详情。 */
    CameraDevice selectById(Long id);

    /** 分页前置列表查询。 */
    List<CameraDevice> selectList(@Param("userId") Long userId,
                                  @Param("plotId") Long plotId,
                                  @Param("plotName") String plotName,
                                  @Param("name") String name,
                                  @Param("streamProtocol") String streamProtocol,
                                  @Param("onlineStatus") Integer onlineStatus);

    /** 查询 farm 当前用户的监控列表，并聚合地块当前作物和封面。 */
    List<ClientMonitorResponse> selectClientMonitors(@Param("userId") Long userId,
                                                     @Param("plotId") Long plotId,
                                                     @Param("keyword") String keyword);

    /** 在用户数据范围内查询单个监控播放详情。 */
    ClientMonitorResponse selectClientMonitorById(@Param("id") Long id, @Param("userId") Long userId);

    /** 统计同来源设备编号数量，用于新增和修改时校验唯一性。 */
    int countByDeviceId(@Param("deviceId") Long deviceId, @Param("excludeId") Long excludeId);

    /** 统计摄像头拍照计划、抓拍记录等业务引用数量，用于删除保护。 */
    int countReferencesByCameraId(@Param("cameraId") Long cameraId);
}
