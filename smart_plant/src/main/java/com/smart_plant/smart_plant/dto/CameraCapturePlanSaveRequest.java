package com.smart_plant.smart_plant.dto;

import lombok.Data;
import java.time.LocalTime;

/** 摄像头采集计划保存白名单，防止客户端修改数据归属和硬件回执字段。 */
@Data
public class CameraCapturePlanSaveRequest {
    /** 修改时携带计划ID，新增时为空。 */ private Long id;
    /** 目标摄像头ID。 */ private Long deviceId;
    /** 便于运营识别的计划名称。 */ private String planName;
    /** 抓拍间隔，单位分钟。 */ private Integer intervalMinutes;
    /** 星期位掩码。 */ private Integer weekdaysMask;
    /** 每日开始时间。 */ private LocalTime startTime;
    /** 每日结束时间。 */ private LocalTime endTime;
    /** IANA时区名称。 */ private String timezone;
    /** 是否启用。 */ private Boolean enabled;
    /** 备注。 */ private String remark;
    /** 修改时必须回传的乐观锁版本。 */ private Integer version;
}
