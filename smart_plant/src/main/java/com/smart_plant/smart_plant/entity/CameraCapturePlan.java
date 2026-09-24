package com.smart_plant.smart_plant.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** 摄像头作物图像采集计划，同时承载列表联查得到的摄像头与地块展示字段。 */
@Data
public class CameraCapturePlan {
    /** 计划主键。 */ private Long id;
    /** 关联 camera_device.id。 */ private Long deviceId;
    /** 数据所有者，由服务端根据地块写入。 */ private Long userId;
    /** 所属地块，由服务端根据摄像头写入。 */ private Long plotId;
    /** 计划名称。 */ private String planName;
    /** 摄像头名称，联查字段。 */ private String cameraName;
    /** 地块名称，联查字段。 */ private String plotName;
    /** 地块编码，联查字段。 */ private String plotCode;
    /** 摄像头在线状态，联查字段。 */ private Integer onlineStatus;
    /** 抓拍间隔，单位分钟。 */ private Integer intervalMinutes;
    /** 星期位掩码，周一bit0至周日bit6。 */ private Integer weekdaysMask;
    /** 每日采集窗口开始时间。 */ private LocalTime startTime;
    /** 每日采集窗口结束时间。 */ private LocalTime endTime;
    /** IANA时区名称。 */ private String timezone;
    /** 是否启用：0否，1是。 */ private Integer enabled;
    /** 硬件应用状态：0待下发，1下发中，2已应用，3失败。 */ private Integer applyStatus;
    /** 最近一次实际抓拍时间。 */ private LocalDateTime lastCaptureTime;
    /** 按当前配置推算的下一次抓拍时间。 */ private LocalDateTime nextCaptureTime;
    /** 最近一次下发失败原因。 */ private String lastApplyError;
    /** 乐观锁版本号。 */ private Integer version;
    /** 业务备注。 */ private String remark;
    /** 创建人。 */ private Long createBy;
    /** 最后修改人。 */ private Long updateBy;
    /** 创建时间。 */ private LocalDateTime createTime;
    /** 更新时间。 */ private LocalDateTime updateTime;
}
