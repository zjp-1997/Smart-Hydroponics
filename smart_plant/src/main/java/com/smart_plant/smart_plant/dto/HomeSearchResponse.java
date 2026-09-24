package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 首页全局搜索响应，保留原始关键字便于前后端排查搜索条件。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeSearchResponse {

    /** 去除首尾空格后的实际搜索关键字。 */
    private String keyword;

    /** 按任务、设备、地块顺序合并后的有限结果集。 */
    private List<HomeSearchResultItem> items;
}
