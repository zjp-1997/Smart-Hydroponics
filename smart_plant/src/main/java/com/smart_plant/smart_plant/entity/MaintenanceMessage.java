package com.smart_plant.smart_plant.entity;

import lombok.Data;
import java.time.LocalDateTime;

/** 维护消息快照及当前查询范围内的送达统计，与两端接口字段一一对应。 */
@Data
public class MaintenanceMessage {
    /** 独立消息ID，不与 notification 明细ID混用。 */
    private Long id;
    /** 关联设备及发布时名称快照；编辑时不允许变更设备归属。 */
    private Long deviceId;
    private String deviceName;
    /** 并发编辑版本，更新时必须带回。 */
    private Integer version;
    /** 人工发布人ID，系统自动消息为空。 */
    private Long publisherId;
    /** 来源故障ID，手动发布时为空。 */
    private Long faultId;
    /** 发布时地块ID及名称。 */
    private Long plotId;
    private String plotName;
    /** 系统根据故障生成的标题、正文及级别。 */
    private String title;
    private String content;
    private Integer level;
    /** 自动发布人和发布时间，不随故障更新改变。 */
    private String publisherName;
    private LocalDateTime sendTime;
    /** 管理员查看全部送达数，其他角色仅查看自己的送达及阅读情况。 */
    private Long recipientCount;
    private Long readCount;
}
