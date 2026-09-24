package com.smart_plant.smart_plant.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 故障维护记录页的一次性响应：故障快照和按生命周期排列的操作节点。 */
public record ClientDeviceFaultRecordResponse(
        ClientDeviceFaultResponse fault,
        List<Entry> records
) {
    /** 节点内容直接来自故障单已持久化的时间、处理说明和完成图片。 */
    public record Entry(String key, int status, String actionName, String content,
                        String operatorName, LocalDateTime executeTime, String imageUrl) {
    }
}
