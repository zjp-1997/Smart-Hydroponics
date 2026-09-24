package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI模型配置实体类。
 *
 * <p>该类映射数据库 model 表，用于保存 OpenAI 兼容模型调用所需的 baseUrl、apiKey 和 model 名称。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModel {

    /** 模型配置主键ID。 */
    private Long id;

    /** 模型展示名称，供后台和 farm 用户端选择。 */
    private String modelName;

    /** 模型服务基础地址，例如 https://api.openai.com/v1。 */
    private String baseUrl;

    /** 模型服务 API Key，管理端列表会做脱敏展示。 */
    private String apiKey;

    /** 具体模型名称，例如 gpt-4o-mini 或 deepseek-chat。 */
    private String model;

    /** 状态，1启用、0禁用。 */
    private Integer status;

    /** 备注说明，用于记录模型用途、供应商或使用限制。 */
    private String remark;

    /** 创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;

    /** 关联对话数量，列表查询时通过子查询回填。 */
    private Integer chatCount;
}
