package com.smart_plant.smart_plant.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.FarmMessageRequest;
import com.smart_plant.smart_plant.dto.SystemAnnouncementRequest;
import com.smart_plant.smart_plant.entity.FarmTask;
import com.smart_plant.smart_plant.entity.Notification;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.NotificationMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.response.ResponseCode;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.FarmTaskService;
import com.smart_plant.smart_plant.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** notice_type=1 统一表示系统公告。 */
    private static final int NOTICE_TYPE_SYSTEM_ANNOUNCEMENT = 1;

    /** notice_type=2 统一表示农事任务相关消息。 */
    private static final int NOTICE_TYPE_FARM_TASK = 2;

    /** 系统公告的业务关联类型，配合 ref_id 组成公告分组。 */
    private static final String REF_TYPE_SYSTEM_ANNOUNCEMENT = "system_announcement";

    /** 后台发布的农事消息组类型，task_id 才是真正的农事任务外键。 */
    private static final String REF_TYPE_FARM_TASK_MESSAGE = "farm_task_message";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final DataPermissionService dataPermissionService;
    private final FarmTaskService farmTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification createIfAbsent(Notification notification) {
        validateCreate(notification);
        Notification existing = getByRef(
                notification.getUserId(),
                notification.getNoticeType(),
                notification.getRefType(),
                notification.getRefId());
        if (existing != null) {
            // 自动生成只负责首次提供默认文案；已存在消息可能已被用户编辑，禁止再次覆盖。
            return existing;
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(0);
        }
        if (notification.getStatus() == null) {
            notification.setStatus(1);
        }
        if (notification.getLevel() == null) {
            notification.setLevel(1);
        }
        notification.setDedupKey(buildDedupKey(notification));
        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException duplicate) {
            Notification concurrent = notificationMapper.selectByDedupKeyForUpdate(notification.getDedupKey());
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }
        return notificationMapper.selectById(notification.getId());
    }

    @Override
    public Notification getByRef(Long userId, Integer noticeType, String refType, Long refId) {
        if (userId == null || noticeType == null || refType == null || refId == null) {
            return null;
        }
        return notificationMapper.selectByRef(userId, noticeType, refType, refId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageInfo<Notification> listClientAnnouncements(Integer pageNum, Integer pageSize) {
        Long userId = dataPermissionService.currentUser().getId();
        // 先补齐旧公告缺失的本人投递，再分页，确保列表总数和内容立即一致。
        syncClientAnnouncementDeliveries(userId);
        // 限制分页大小，避免移动端一次拉取过多公告。
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(notificationMapper.selectClientAnnouncements(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long countClientUnreadAnnouncements() {
        Long userId = dataPermissionService.currentUser().getId();
        // 消息首页只请求统计接口时也执行同步，无需先进入公告列表才能看到未读数。
        syncClientAnnouncementDeliveries(userId);
        return notificationMapper.countClientUnreadAnnouncements(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markClientAnnouncementRead(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Invalid announcement ID");
        }
        Long userId = dataPermissionService.currentUser().getId();
        // 更新条件同时校验接收人和公告类型；已读消息重复调用保持幂等。
        int updated = notificationMapper.markClientAnnouncementRead(id, userId, LocalDateTime.now());
        if (updated == 0) {
            Notification announcement = notificationMapper.selectById(id);
            if (announcement == null || !userId.equals(announcement.getUserId())
                    || !Integer.valueOf(NOTICE_TYPE_SYSTEM_ANNOUNCEMENT).equals(announcement.getNoticeType())
                    || (announcement.getRefType() != null
                        && !REF_TYPE_SYSTEM_ANNOUNCEMENT.equals(announcement.getRefType()))
                    || !Integer.valueOf(1).equals(announcement.getStatus())) {
                throw new BusinessException(ResponseCode.NOT_FOUND, "Announcement does not exist");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllClientAnnouncementsRead() {
        Long userId = dataPermissionService.currentUser().getId();
        syncClientAnnouncementDeliveries(userId);
        return notificationMapper.markAllClientAnnouncementsRead(userId, LocalDateTime.now());
    }

    /** 幂等补齐当前用户缺失的历史公告接收明细，已有记录不会被重复插入或重置为未读。 */
    private void syncClientAnnouncementDeliveries(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "登录状态已失效");
        }
        notificationMapper.insertMissingClientAnnouncementDeliveries(userId);
    }

    @Override
    public Notification getNotificationById(Long id) {
        requireId(id);
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || Integer.valueOf(0).equals(notification.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Notification does not exist");
        }
        dataPermissionService.requireOwnedResource(notification.getUserId());
        return notification;
    }

    @Override
    public PageInfo<Notification> listNotifications(String title, Integer noticeType, Integer level,
                                                    Integer isRead, String refType, LocalDate startDate,
                                                    LocalDate endDate, Integer pageNum, Integer pageSize) {
        validateNoticeType(noticeType);
        validateLevel(level);
        validateReadStatus(isRead);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Start date cannot be after end date");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        return new PageInfo<>(notificationMapper.selectList(
                scopedUserId,
                normalizeOptionalText(title),
                noticeType,
                level,
                isRead,
                normalizeOptionalText(refType),
                startTime,
                endTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification publishSystemAnnouncement(SystemAnnouncementRequest request) {
        validateSystemAnnouncementRequest(request);
        User publisher = dataPermissionService.currentUser();
        List<User> receivers = resolveAnnouncementReceivers(publisher);
        LocalDateTime sendTime = LocalDateTime.now();
        Long announcementRefId = null;

        for (User receiver : receivers) {
            Notification notification = buildSystemAnnouncementNotification(request, publisher, receiver, sendTime, announcementRefId);
            notificationMapper.insert(notification);
            if (announcementRefId == null) {
                announcementRefId = notification.getId();
                notificationMapper.updateRefIdById(notification.getId(), announcementRefId);
            }
        }

        return getSystemAnnouncementById(announcementRefId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification updateSystemAnnouncement(Long id, SystemAnnouncementRequest request) {
        validateSystemAnnouncementRequest(request);
        Notification oldAnnouncement = getSystemAnnouncementById(id);
        int rows = notificationMapper.updateSystemAnnouncementGroup(
                oldAnnouncement.getRefId(),
                request.getTitle().trim(),
                request.getContent().trim(),
                request.getLevel());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "系统公告修改失败");
        }
        return getSystemAnnouncementById(oldAnnouncement.getRefId());
    }

    @Override
    public Notification getSystemAnnouncementById(Long id) {
        requireId(id);
        Notification announcement = notificationMapper.selectSystemAnnouncementGroupById(id);
        if (announcement == null || Integer.valueOf(0).equals(announcement.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "系统公告不存在");
        }
        return normalizeSystemAnnouncement(announcement);
    }

    @Override
    public PageInfo<Notification> listSystemAnnouncements(String title, String content,
                                                          LocalDate startDate, LocalDate endDate,
                                                          Integer pageNum, Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        PageHelper.startPage(currentPage, currentSize);
        List<Notification> announcements = notificationMapper.selectSystemAnnouncementList(
                normalizeOptionalText(title),
                normalizeOptionalText(content),
                startTime,
                endTime);
        announcements.forEach(this::normalizeSystemAnnouncement);
        return new PageInfo<>(announcements);
    }

    @Override
    public Map<String, Object> statisticsNotifications() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = notificationMapper.selectStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    public Map<String, Object> statisticsSystemAnnouncements() {
        Map<String, Object> statistics = notificationMapper.selectSystemAnnouncementStatistics();
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification publishFarmMessage(FarmMessageRequest request) {
        validateFarmMessageRequest(request);
        FarmTask task = farmTaskService.getFarmTaskById(request.getTaskId());
        User publisher = dataPermissionService.currentUser();
        List<User> receivers = resolveFarmMessageReceivers(request, publisher, task);
        LocalDateTime sendTime = LocalDateTime.now();
        Long messageRefId = null;

        for (User receiver : receivers) {
            Notification notification = buildFarmMessageNotification(request, publisher, receiver, task, sendTime, messageRefId);
            notificationMapper.insert(notification);
            if (messageRefId == null) {
                messageRefId = notification.getId();
                notificationMapper.updateRefIdById(notification.getId(), messageRefId);
            }
        }

        return getFarmMessageById(messageRefId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification updateFarmMessage(Long id, FarmMessageRequest request) {
        validateFarmMessageRequest(request);
        Notification oldMessage = getFarmMessageById(id);
        farmTaskService.getFarmTaskById(request.getTaskId());
        int rows = notificationMapper.updateFarmMessageGroup(
                oldMessage.getRefId(),
                request.getTaskId(),
                request.getTitle().trim(),
                request.getContent().trim(),
                request.getLevel());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "农事消息修改失败");
        }
        return getFarmMessageById(oldMessage.getRefId());
    }

    @Override
    public Notification getFarmMessageById(Long id) {
        requireId(id);
        Notification message = notificationMapper.selectFarmMessageGroupById(id);
        if (message == null || Integer.valueOf(0).equals(message.getStatus())) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农事消息不存在");
        }
        return normalizeFarmMessage(message);
    }

    @Override
    public PageInfo<Notification> listFarmMessages(String title, String content, String plotName,
                                                   LocalDate startDate, LocalDate endDate,
                                                   Integer pageNum, Integer pageSize) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        LocalDateTime startTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        PageHelper.startPage(currentPage, currentSize);
        List<Notification> messages = notificationMapper.selectFarmMessageList(
                scopedUserId,
                normalizeOptionalText(title),
                normalizeOptionalText(content),
                normalizeOptionalText(plotName),
                startTime,
                endTime);
        messages.forEach(this::normalizeFarmMessage);
        return new PageInfo<>(messages);
    }

    @Override
    public Map<String, Object> statisticsFarmMessages() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        Map<String, Object> statistics = notificationMapper.selectFarmMessageStatistics(scopedUserId);
        return statistics == null ? new HashMap<>() : statistics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notification updateReadStatus(Long id, Integer isRead) {
        Notification oldNotification = getNotificationById(id);
        validateReadStatus(isRead);
        LocalDateTime readTime = Integer.valueOf(1).equals(isRead) ? LocalDateTime.now() : null;
        int rows = notificationMapper.updateReadStatus(oldNotification.getId(), isRead, readTime);
        if (rows == 0) {
            throw new BusinessException(ResponseCode.FAIL, "Failed to update notification read status");
        }
        return notificationMapper.selectById(oldNotification.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markAllRead() {
        Long scopedUserId = dataPermissionService.restrictUserId(null);
        return notificationMapper.markAllRead(scopedUserId, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id) {
        Notification oldNotification = getNotificationById(id);
        int rows = notificationMapper.deleteById(oldNotification.getId());
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "Notification does not exist");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNotifications(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Please select notifications to delete");
        }
        for (Long id : ids) {
            getNotificationById(id);
        }
        return notificationMapper.deleteBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSystemAnnouncement(Long id) {
        Notification announcement = getSystemAnnouncementById(id);
        int rows = notificationMapper.deleteSystemAnnouncementGroups(List.of(announcement.getRefId()));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "系统公告不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSystemAnnouncements(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的系统公告");
        }
        List<Long> refIds = new ArrayList<>();
        for (Long id : ids) {
            refIds.add(getSystemAnnouncementById(id).getRefId());
        }
        return notificationMapper.deleteSystemAnnouncementGroups(refIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFarmMessage(Long id) {
        Notification message = getFarmMessageById(id);
        int rows = notificationMapper.deleteFarmMessageGroups(List.of(message.getRefId()));
        if (rows == 0) {
            throw new BusinessException(ResponseCode.NOT_FOUND, "农事消息不存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteFarmMessages(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择需要删除的农事消息");
        }
        List<Long> refIds = new ArrayList<>();
        for (Long id : ids) {
            refIds.add(getFarmMessageById(id).getRefId());
        }
        return notificationMapper.deleteFarmMessageGroups(refIds);
    }

    private List<User> resolveAnnouncementReceivers(User publisher) {
        List<User> activeUsers = userMapper.selectActiveUsers();
        if (activeUsers == null || activeUsers.isEmpty()) {
            return List.of(publisher);
        }
        return activeUsers;
    }

    private Notification buildSystemAnnouncementNotification(SystemAnnouncementRequest request, User publisher,
                                                            User receiver, LocalDateTime sendTime,
                                                            Long announcementRefId) {
        Notification notification = new Notification();
        notification.setUserId(receiver.getId());
        notification.setTitle(request.getTitle().trim());
        notification.setContent(request.getContent().trim());
        notification.setNoticeType(NOTICE_TYPE_SYSTEM_ANNOUNCEMENT);
        notification.setRefType(REF_TYPE_SYSTEM_ANNOUNCEMENT);
        notification.setRefId(announcementRefId);
        notification.setLevel(request.getLevel());
        notification.setIsRead(0);
        notification.setSendTime(sendTime);
        notification.setPublisherId(publisher.getId());
        notification.setPublisherName(resolvePublisherName(publisher));
        notification.setStatus(1);
        return notification;
    }

    private Notification normalizeSystemAnnouncement(Notification announcement) {
        if (announcement.getRefId() == null) {
            announcement.setRefId(announcement.getId());
        }
        if (announcement.getRecipientCount() == null) {
            announcement.setRecipientCount(0L);
        }
        if (announcement.getReadCount() == null) {
            announcement.setReadCount(0L);
        }
        return announcement;
    }

    private List<User> resolveFarmMessageReceivers(FarmMessageRequest request, User publisher, FarmTask task) {
        Set<Long> receiverIds = new LinkedHashSet<>();
        if (request.getRecipientIds() != null) {
            request.getRecipientIds().stream()
                    .filter(id -> id != null && id > 0)
                    .forEach(receiverIds::add);
        }
        if (receiverIds.isEmpty()) {
            receiverIds.add(task.getExecutorId());
            receiverIds.add(task.getUserId());
            receiverIds.add(publisher.getId());
        }

        List<User> receivers = new ArrayList<>();
        for (Long receiverId : receiverIds) {
            if (receiverId == null) {
                continue;
            }
            User receiver = userMapper.selectById(receiverId);
            if (receiver == null || !Integer.valueOf(1).equals(receiver.getStatus())) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "接收人不存在或已禁用");
            }
            receivers.add(receiver);
        }
        if (receivers.isEmpty()) {
            receivers.add(publisher);
        }
        return receivers;
    }

    private Notification buildFarmMessageNotification(FarmMessageRequest request, User publisher, User receiver,
                                                      FarmTask task, LocalDateTime sendTime, Long messageRefId) {
        Notification notification = new Notification();
        notification.setUserId(receiver.getId());
        notification.setTitle(request.getTitle().trim());
        notification.setContent(request.getContent().trim());
        notification.setNoticeType(NOTICE_TYPE_FARM_TASK);
        notification.setRefType(REF_TYPE_FARM_TASK_MESSAGE);
        notification.setRefId(messageRefId);
        notification.setTaskId(task.getId());
        notification.setLevel(request.getLevel());
        notification.setIsRead(0);
        notification.setSendTime(sendTime);
        notification.setPublisherId(publisher.getId());
        notification.setPublisherName(resolvePublisherName(publisher));
        notification.setStatus(1);
        return notification;
    }

    private Notification normalizeFarmMessage(Notification message) {
        if (message.getRefId() == null) {
            message.setRefId(message.getId());
        }
        if (message.getTaskId() == null && REF_TYPE_FARM_TASK_MESSAGE.equals(message.getRefType())) {
            message.setTaskId(message.getRefId());
        }
        if (message.getRecipientCount() == null) {
            message.setRecipientCount(0L);
        }
        if (message.getReadCount() == null) {
            message.setReadCount(0L);
        }
        return message;
    }

    private void validateSystemAnnouncementRequest(SystemAnnouncementRequest request) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "系统公告信息不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "公告标题不能为空");
        }
        if (request.getTitle().trim().length() > 100) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "公告标题不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "公告内容不能为空");
        }
        if (request.getLevel() == null) {
            request.setLevel(1);
        }
        validateLevel(request.getLevel());
    }

    private void validateFarmMessageRequest(FarmMessageRequest request) {
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事消息信息不能为空");
        }
        if (request.getTaskId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "关联农事任务不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事消息标题不能为空");
        }
        if (request.getTitle().trim().length() > 100) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事消息标题不能超过100个字符");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "农事消息内容不能为空");
        }
        if (request.getLevel() == null) {
            request.setLevel(1);
        }
        validateLevel(request.getLevel());
    }

    private String resolvePublisherName(User publisher) {
        if (publisher == null) {
            return "";
        }
        return StringUtils.hasText(publisher.getNickname()) ? publisher.getNickname() : publisher.getUsername();
    }

    private void validateCreate(Notification notification) {
        if (notification == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification is required");
        }
        if (notification.getUserId() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification user is required");
        }
        if (notification.getNoticeType() == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification type is required");
        }
        if (!StringUtils.hasText(notification.getTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification title is required");
        }
    }

    private void validateNoticeType(Integer noticeType) {
        if (noticeType != null && (noticeType < 1 || noticeType > 7)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification type must be between 1 and 7");
        }
    }

    private void validateLevel(Integer level) {
        if (level != null && (level < 1 || level > 3)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification level must be between 1 and 3");
        }
    }

    private void validateReadStatus(Integer isRead) {
        if (isRead != null && isRead != 0 && isRead != 1) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Read status must be 0 or 1");
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildDedupKey(Notification notification) {
        return notification.getUserId() != null
                && notification.getNoticeType() != null
                && StringUtils.hasText(notification.getRefType())
                && notification.getRefId() != null
                ? "notification:" + notification.getUserId() + ":" + notification.getNoticeType()
                    + ":" + notification.getRefType().trim() + ":" + notification.getRefId()
                : null;
    }

    private void requireId(Long id) {
        if (id == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "Notification id is required");
        }
    }
}
