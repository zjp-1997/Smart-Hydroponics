package com.smart_plant.smart_plant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** farm 仓库流水只读分页结果，仅暴露移动端实际展示的字段。 */
public record ClientWarehouseRecordPage(List<Entry> list, boolean hasNextPage) {

    /** 一条入库或出库记录的展示快照，不包含成本、供应商及其他后台专用信息。 */
    public record Entry(Long id, String itemName, String itemCode, String itemUnit,
                        Integer recordType, BigDecimal quantity, BigDecimal afterQty,
                        String operatorName, String recipient, LocalDateTime recordTime, String remark) {
    }
}
