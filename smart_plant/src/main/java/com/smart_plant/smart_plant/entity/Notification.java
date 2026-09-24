package com.smart_plant.smart_plant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /** 通知明细ID；系统公告列表按公告组展示时取组内最小ID作为公告ID。 */
    private Long id;

    /** 接收用户ID；同一条系统公告会为多个接收人生成多条明细。 */
    private Long userId;

    /** 接收用户账号，列表查询时通过 user 表联查得到。 */
    private String username;

    /** 接收用户昵称，列表查询时通过 user 表联查得到。 */
    private String nickname;

    /** 公告或通知标题。 */
    private String title;

    /** 公告或通知正文内容。 */
    private String content;

    /** 消息类型：1系统公告，2农事任务，3设备消息，4环境预警，5专家咨询，6AI识别，7库存预警。 */
    private Integer noticeType;

    /** 通知投递分组类型；只负责幂等与聚合，不代替 taskId/alertId 等受外键保护的业务引用。 */
    private String refType;

    /** 通知投递分组ID；同一消息的所有接收明细共享同一个 refId。 */
    private Long refId;

    /** 自动通知的业务幂等键；人工发布消息可为空。 */
    private String dedupKey;

    /** 关联农事任务ID；农事消息通过该字段与 farm_task 建立业务关联。 */
    private Long taskId;

    /** 关联农事任务标题，农事消息列表通过 farm_task 表联查得到。 */
    private String taskTitle;

    /** 关联农事任务类型，农事消息列表通过 farm_task 表联查得到。 */
    private Integer taskType;

    /** 关联农事任务状态，便于消息列表展示任务当前处理进度。 */
    private Integer taskStatus;

    /** 关联地块名称，农事消息列表通过 farm_task -> plot 表联查得到。 */
    private String plotName;

    /** 关联预警ID，预警类通知使用。 */
    private Long alertId;

    /** 关联预警标题，列表查询时通过 alert_event 表联查得到。 */
    private String alertTitle;

    /** 关联预警级别，列表查询时通过 alert_event 表联查得到。 */
    private Integer alertLevel;

    /** 公告级别：1普通，2重要，3紧急。 */
    private Integer level;

    /** 阅读状态：0未读，1已读。 */
    private Integer isRead;

    /** 阅读时间。 */
    private LocalDateTime readTime;

    /** 发布时间。 */
    private LocalDateTime sendTime;

    /** 发布人ID；系统公告由后台用户发布。 */
    private Long publisherId;

    /** 发布人名称，冗余保存用于公告列表稳定展示历史发布人。 */
    private String publisherName;

    /** 系统公告接收人数，公告分组列表中由 SQL 聚合得到。 */
    private Long recipientCount;

    /** 系统公告已读人数，公告分组列表中由 SQL 聚合得到。 */
    private Long readCount;

    /** 记录状态：1正常，0删除。 */
    private Integer status;

    /** 备注。 */
    private String remark;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
