package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 首页全局搜索结果项，与 smart_farm 顶部搜索下拉项字段一一对应。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeSearchResultItem {

    /** 业务类型：farmTask、iotDevice 或 plot。 */
    private String type;

    /** 业务主键，用作前端列表渲染的稳定标识。 */
    private Long id;

    /** 搜索结果主标题。 */
    private String title;

    /** 编号、地块等辅助描述。 */
    private String description;
}
