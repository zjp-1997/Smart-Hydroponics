package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.Notification;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.FarmMessageRequest;
import com.smart_plant.smart_plant.dto.SystemAnnouncementRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    Notification createIfAbsent(Notification notification);

    Notification getByRef(Long userId, Integer noticeType, String refType, Long refId);

    Notification getNotificationById(Long id);

    /** farm 端按登录用户获取系统公告分页列表和未读数。 */
    PageInfo<Notification> listClientAnnouncements(Integer pageNum, Integer pageSize);

    long countClientUnreadAnnouncements();

    /** farm 端阅读本人公告及一键已读。 */
    void markClientAnnouncementRead(Long id);

    int markAllClientAnnouncementsRead();

    PageInfo<Notification> listNotifications(String title, Integer noticeType, Integer level,
                                             Integer isRead, String refType, LocalDate startDate,
                                             LocalDate endDate, Integer pageNum, Integer pageSize);

    /** 发布系统公告，并为所有启用用户生成公告接收明细。 */
    Notification publishSystemAnnouncement(SystemAnnouncementRequest request);

    /** 编辑系统公告，同步修改该公告组下所有接收人的公告内容。 */
    Notification updateSystemAnnouncement(Long id, SystemAnnouncementRequest request);

    /** 查询系统公告详情，按公告组返回已读数量和接收人数。 */
    Notification getSystemAnnouncementById(Long id);

    /** 分页查询系统公告，列表按公告组聚合展示。 */
    PageInfo<Notification> listSystemAnnouncements(String title, String content,
                                                   LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize);

    Map<String, Object> statisticsNotifications();

    /** 查询系统公告统计卡片数据。 */
    Map<String, Object> statisticsSystemAnnouncements();

    /** 发布农事消息，消息本体保存到 notification，并通过 task_id 关联 farm_task。 */
    Notification publishFarmMessage(FarmMessageRequest request);

    /** 编辑农事消息，只修改消息本体和 task_id 关联，不修改农事任务。 */
    Notification updateFarmMessage(Long id, FarmMessageRequest request);

    /** 查询农事消息详情，按消息组返回关联任务、接收人数和已读数量。 */
    Notification getFarmMessageById(Long id);

    /** 分页查询农事消息，支持消息标题、消息内容和地块名称查询。 */
    PageInfo<Notification> listFarmMessages(String title, String content, String plotName,
                                            LocalDate startDate, LocalDate endDate,
                                            Integer pageNum, Integer pageSize);

    /** 查询农事消息统计卡片数据。 */
    Map<String, Object> statisticsFarmMessages();

    Notification updateReadStatus(Long id, Integer isRead);

    int markAllRead();

    void deleteNotification(Long id);

    int deleteNotifications(List<Long> ids);

    /** 删除单条系统公告，按公告组逻辑删除所有接收明细。 */
    void deleteSystemAnnouncement(Long id);

    /** 批量删除系统公告，按公告组逻辑删除所有接收明细。 */
    int deleteSystemAnnouncements(List<Long> ids);

    /** 删除单条农事消息，按消息组逻辑删除所有接收明细。 */
    void deleteFarmMessage(Long id);

    /** 批量删除农事消息，按消息组逻辑删除所有接收明细。 */
    int deleteFarmMessages(List<Long> ids);
}
