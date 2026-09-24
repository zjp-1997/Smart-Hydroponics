package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI对话记录实体类。
 *
 * <p>该类映射 ai_chat 表，用于保存用户输入、可选附件、模型回复和调用状态。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChat {

    /** 对话记录主键ID。 */
    private Long id;

    /** 发起对话的用户ID。 */
    private Long userId;

    /** 用户名，列表查询时通过 user 表关联回填。 */
    private String username;

    /** 用户昵称，列表查询时通过 user 表关联回填。 */
    private String nickname;

    /** 使用的模型配置ID，对应 model.id。 */
    private Long modelId;

    /** 模型展示名称，列表查询时通过 model 表关联回填。 */
    private String modelName;

    /** 实际调用的模型名称，列表查询时通过 model 表关联回填。 */
    private String model;

    /** 兼容数据库既有字段：保存图片或文档附件地址，纯文本咨询时为空。 */
    private String imageUrl;

    /** 文档附件的原始文件名，用于刷新历史后保持用户上传时的名称。 */
    private String fileName;

    /** 用户发送的咨询内容。 */
    private String userContent;

    /** AI返回的回复内容。 */
    private String aiContent;

    /** 调用状态，1成功、2失败。 */
    private Integer status;

    /** 失败原因，仅 status=2 时记录。 */
    private String failReason;

    /** 创建时间，由数据库默认写入。 */
    private LocalDateTime createTime;

    /** 更新时间，由数据库自动维护。 */
    private LocalDateTime updateTime;
}
