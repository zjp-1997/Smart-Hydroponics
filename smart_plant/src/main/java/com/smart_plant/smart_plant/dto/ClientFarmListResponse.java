package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * farm 用户端农场列表响应对象。
 *
 * <p>用户端只需要展示农场图片、地块数量、种植面积和地址等轻量信息，
 * 因此使用 DTO 隔离后台管理字段，避免联系人、经纬度、备注等管理字段被过度暴露。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientFarmListResponse {

    /** 农场主键，供用户端点击农场后跳转到对应地块列表使用。 */
    private Long id;

    /** 农场名称，用户端列表中用于展示该农场的基础识别信息。 */
    private String farmName;

    /** 农场图片地址，对应 farm.img_url 字段。 */
    private String imgUrl;

    /** 当前农场下关联的地块数量。 */
    private Integer plotCount;

    /** 农场种植面积数值。 */
    private BigDecimal totalArea;

    /** 面积单位，默认为“亩”。 */
    private String areaUnit;

    /** 已格式化的种植面积文本，便于 uni-app 端直接渲染。 */
    private String plantArea;

    /** 农场种植地址。 */
    private String address;
}
