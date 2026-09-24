package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Farm {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String farmName;

    private String farmCode;

    /**
     * 农场图片访问地址。
     *
     * <p>可以保存完整 http 地址，也可以保存 /uploads 开头的相对地址；
     * 前端展示时会根据自身环境补齐完整访问路径。</p>
     */
    private String imgUrl;

    private String contactPhone;

    private String address;

    /**
     * 兼容前端的只读坐标文本，由数据库根据 longitude/latitude 自动生成，禁止作为持久化事实源。
     */
    private String coordinate;

    private BigDecimal totalArea;

    private String areaUnit;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer plotCount;
}
