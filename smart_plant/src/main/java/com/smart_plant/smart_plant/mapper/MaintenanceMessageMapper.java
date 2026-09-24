package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.MaintenanceMessage;
import com.smart_plant.smart_plant.entity.IotDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/** 维护消息持久层；发布锁和接收明细插入由同一事务串行执行。 */
@Mapper
public interface MaintenanceMessageMapper {
    /** 从数据库内已保存的故障生成快照，避免使用请求中的伪造归属字段。 */
    int insertFromFault(@Param("faultId") Long faultId);
    /** 锁住唯一消息行，保证并发调用下接收明细不重复。 */
    Long lockByFaultId(@Param("faultId") Long faultId);
    /** 仅向设备农场主及当前有效绑定技术人员投递，SQL 内去重。 */
    int deliver(@Param("id") Long id);
    /** 分页/详情复用相同的接收人范围及查询条件。 */
    List<MaintenanceMessage> selectList(@Param("userId") Long userId, @Param("id") Long id,
            @Param("title") String title, @Param("content") String content, @Param("plotName") String plotName);
    /** 消息数按消息本体统计，已读/未读按接收明细统计。 */
    Map<String, Object> statistics(@Param("userId") Long userId);
    /** 只能更新登录用户自己的接收明细，不允许管理员代读。 */
    int markRead(@Param("id") Long id, @Param("userId") Long userId);
    /** 设备候选按设备所属农场主和绑定技术人员隔离；指定ID时锁住设备归属。 */
    List<IotDevice> selectDevices(@Param("userId") Long userId, @Param("id") Long id, @Param("keyword") String keyword);
    /** 手动发布独立消息，不创建故障，也不消耗自动故障消息唯一键。 */
    int insertManual(MaintenanceMessage message);
    /** 编辑/删除前锁住本体，避免与通知投递并发产生残留明细。 */
    Long lockById(@Param("id") Long id);
    /** 乐观锁编辑消息，返回0代表版本已变化或消息已删除。 */
    int updateMessage(MaintenanceMessage message);
    /** 同步所有活跃接收人的消息本体字段，保留各自阅读状态。 */
    int syncDeliveries(@Param("id") Long id);
    /** 整组逻辑删除，保留自动故障唯一键，防止重启补发已删除消息。 */
    int deleteMessage(@Param("id") Long id);
    int deleteDeliveries(@Param("id") Long id);
}

