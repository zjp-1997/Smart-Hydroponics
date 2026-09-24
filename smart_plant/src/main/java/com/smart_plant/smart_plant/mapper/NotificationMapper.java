package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    /** 发布公告后回填公告组ID，使同一公告的所有接收明细可按 ref_id 聚合。 */
    int updateRefIdById(@Param("id") Long id, @Param("refId") Long refId);

    Notification selectById(Long id);

    Notification selectByRef(@Param("userId") Long userId,
                             @Param("noticeType") Integer noticeType,
                             @Param("refType") String refType,
                             @Param("refId") Long refId);

    Notification selectByDedupKeyForUpdate(@Param("dedupKey") String dedupKey);

    List<Notification> selectList(@Param("userId") Long userId,
                                  @Param("title") String title,
                                  @Param("noticeType") Integer noticeType,
                                  @Param("level") Integer level,
                                  @Param("isRead") Integer isRead,
                                  @Param("refType") String refType,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /** farm 端仅查询当前用户收到的系统公告明细。 */
    List<Notification> selectClientAnnouncements(@Param("userId") Long userId);

    /** 为当前用户补齐历史有效公告中缺失的接收明细，已有明细保持原阅读状态。 */
    int insertMissingClientAnnouncementDeliveries(@Param("userId") Long userId);

    /** 统计当前用户尚未阅读的系统公告，不受列表分页影响。 */
    long countClientUnreadAnnouncements(@Param("userId") Long userId);

    /** 使用用户 ID 和公告类型约束已读更新，防止跨用户修改。 */
    int markClientAnnouncementRead(@Param("id") Long id, @Param("userId") Long userId,
                                   @Param("readTime") LocalDateTime readTime);

    /** 一次性标记当前用户的全部公告，保留已读记录原有的阅读时间。 */
    int markAllClientAnnouncementsRead(@Param("userId") Long userId,
                                       @Param("readTime") LocalDateTime readTime);

    /** 按公告组查询系统公告列表，避免后台重复展示每个接收人的通知明细。 */
    List<Notification> selectSystemAnnouncementList(@Param("title") String title,
                                                    @Param("content") String content,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /** 根据公告列表ID解析公告组详情，编辑和删除时用它定位整组接收明细。 */
    Notification selectSystemAnnouncementGroupById(@Param("id") Long id);

    Map<String, Object> selectStatistics(@Param("userId") Long userId);

    /** 查询系统公告统计卡片数据，仅统计 notice_type=1 且未逻辑删除的公告组。 */
    Map<String, Object> selectSystemAnnouncementStatistics();

    /** 按消息组查询农事消息列表，消息本体来自 notification，任务信息通过 task_id 关联。 */
    List<Notification> selectFarmMessageList(@Param("userId") Long userId,
                                             @Param("title") String title,
                                             @Param("content") String content,
                                             @Param("plotName") String plotName,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /** 根据列表ID解析农事消息组详情，编辑和删除时用它定位整组接收明细。 */
    Notification selectFarmMessageGroupById(@Param("id") Long id);

    /** 查询农事消息统计卡片数据，按消息组聚合而不是按接收明细统计。 */
    Map<String, Object> selectFarmMessageStatistics(@Param("userId") Long userId);

    /** 编辑公告时同步更新同组所有接收人的公告标题、内容和级别。 */
    int updateSystemAnnouncementGroup(@Param("refId") Long refId,
                                      @Param("title") String title,
                                      @Param("content") String content,
                                      @Param("level") Integer level);

    /** 编辑农事消息时只更新消息本体和 task_id 关联，不修改 farm_task 任务本体。 */
    int updateFarmMessageGroup(@Param("refId") Long refId,
                               @Param("taskId") Long taskId,
                               @Param("title") String title,
                               @Param("content") String content,
                               @Param("level") Integer level);

    int updateReadStatus(@Param("id") Long id,
                         @Param("isRead") Integer isRead,
                         @Param("readTime") LocalDateTime readTime);

    int markAllRead(@Param("userId") Long userId,
                    @Param("readTime") LocalDateTime readTime);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 删除公告时按公告组批量逻辑删除，避免残留部分接收人的公告明细。 */
    int deleteSystemAnnouncementGroups(@Param("refIds") List<Long> refIds);

    /** 删除农事消息时按消息组批量逻辑删除，保留关联农事任务和执行反馈。 */
    int deleteFarmMessageGroups(@Param("refIds") List<Long> refIds);
}
