package com.smart_plant.smart_plant.dto;

import lombok.Data;

/** 发布/编辑维护消息。正文支持自定义；接收人仍由后端根据设备归属确定。 */
@Data
public class MaintenanceMessageRequest {
    /** 发布时选择的设备；编辑时只允许保留原设备。 */
    private Long deviceId;
    /** 可编辑的消息标题与级别（1普通、2重要、3紧急）。 */
    private String title;
    /** 实际保存的正文，支持多行文本，不能只保存默认模板。 */
    private String content;
    private Integer level;
    /** 编辑时必填的版本号，防止并发覆盖。 */
    private Integer version;
}
