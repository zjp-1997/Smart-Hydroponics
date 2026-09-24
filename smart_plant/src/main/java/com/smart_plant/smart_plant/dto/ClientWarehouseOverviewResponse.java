package com.smart_plant.smart_plant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * farm 用户端仓库首页聚合数据。
 *
 * @param totalItems 所属农场仓库的物资种类数
 * @param stockQuantity 所属农场仓库的库存总量
 * @param todayInboundQuantity 当前自然日的有效入库总量
 * @param items farm 用户可查看的当前页启用物资
 * @param hasNextPage 当前筛选条件下是否还有下一页
 */
public record ClientWarehouseOverviewResponse(
        long totalItems,
        BigDecimal stockQuantity,
        BigDecimal todayInboundQuantity,
        List<Item> items,
        boolean hasNextPage
) {
    /** 移动端只读物资卡片，不暴露入库单价、操作者等后台管理字段。 */
    public record Item(Long id, Long farmOwnerId, String farmOwnerName, String itemName, String itemCode,
                       String imageUrl, Integer category, String specification, String unit,
                       BigDecimal stockQty, BigDecimal warningQty, LocalDateTime createTime) {
    }
}
