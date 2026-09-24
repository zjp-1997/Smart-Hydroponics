package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 移动端完成故障处理时提交的请求参数。
 */
@Data
public class ClientDeviceFaultCompleteRequest {

    /** 选填的维修说明。 */
    private String handleResult;

    /** 必填的本系统故障凭证图片地址。 */
    private String completionImageUrl;
}
