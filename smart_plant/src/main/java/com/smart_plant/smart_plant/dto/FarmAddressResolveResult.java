package com.smart_plant.smart_plant.dto;

/**
 * 农场经纬度反查地址结果。
 *
 * @param coordinate 标准化经纬度，格式为 经度,纬度
 * @param address    反查得到的地址
 */
public record FarmAddressResolveResult(String coordinate, String address) {
}
