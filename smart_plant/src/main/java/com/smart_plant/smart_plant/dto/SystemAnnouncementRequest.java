package com.smart_plant.smart_plant.dto;

import lombok.Data;

/**
 * 系统公告发布和编辑请求。
 *
 * <p>前端只允许提交公告业务字段，发布人、接收人、公告类型等敏感字段由后端根据登录态统一生成。</p>
 */
@Data
public class SystemAnnouncementRequest {

    /** 公告标题，用于列表检索和用户端消息摘要展示。 */
    private String title;

    /** 公告正文内容，用于承载完整通知文本。 */
    private String content;

    /** 公告级别：1普通，2重要，3紧急。 */
    private Integer level;
}
