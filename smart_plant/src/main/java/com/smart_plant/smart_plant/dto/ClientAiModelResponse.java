package com.smart_plant.smart_plant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * farm 用户端模型选项响应对象。
 *
 * <p>用户端只能看到模型展示字段，不能暴露 API Key 等敏感配置。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientAiModelResponse {

    /** 模型配置ID，发送消息时作为 modelId 提交。 */
    private Long id;

    /** 模型展示名称。 */
    private String modelName;

    /** 具体模型名称。 */
    private String model;
}
