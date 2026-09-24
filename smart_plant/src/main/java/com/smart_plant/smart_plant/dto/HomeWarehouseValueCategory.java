package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 首页仓库物资分类库存价值占比数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeWarehouseValueCategory {

    private Integer category;
    private String categoryName;
    private BigDecimal totalValue;
}
