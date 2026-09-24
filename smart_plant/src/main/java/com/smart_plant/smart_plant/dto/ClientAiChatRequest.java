package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * farm 用户端 AI 咨询请求对象。
 *
 * <p>图片与文件地址均来自本人上传接口，由服务端核对归属后读取内容。</p>
 */
@Data
public class ClientAiChatRequest {

    /** 指定调用的模型配置ID；为空时后端自动选择一个启用模型。 */
    private Long modelId;

    /** 用户发送的咨询内容。 */
    private String content;

    /** 用户上传图片地址，可为空。 */
    private String imageUrl;

    /** 用户上传文档地址，可为空，和 imageUrl 不能同时填写。 */
    private String fileUrl;

    /** 用户可见的原始文件名称，仅用于消息气泡展示。 */
    private String fileName;
}
