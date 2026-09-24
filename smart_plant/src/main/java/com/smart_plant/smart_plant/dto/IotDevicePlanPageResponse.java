package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** 避免修改 PageHelper 内部集合类型的稳定分页响应。 */
@Data
@AllArgsConstructor
public class IotDevicePlanPageResponse {
    private List<IotDevicePlanResponse> list;
    private long total;
    private int pageNum;
    private int pageSize;
}
