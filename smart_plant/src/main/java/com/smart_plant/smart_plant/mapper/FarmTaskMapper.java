package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.ClientFarmTaskDateSummaryResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskResponse;
import com.smart_plant.smart_plant.entity.FarmTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FarmTaskMapper {

    int insert(FarmTask farmTask);

    int updateById(FarmTask farmTask);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("actualStartTime") LocalDateTime actualStartTime,
                     @Param("actualEndTime") LocalDateTime actualEndTime,
                     @Param("completeRemark") String completeRemark);

    /**
     * 原子领取并开始任务；状态条件位于 SQL 中，避免多人同时点击造成重复领取。
     */
    int startTask(@Param("id") Long id,
                  @Param("executorId") Long executorId,
                  @Param("actualStartTime") LocalDateTime actualStartTime);

    /**
     * 原子完成进行中的任务，只有状态为进行中的记录才能被更新。
     */
    int completeTask(@Param("id") Long id,
                     @Param("actualEndTime") LocalDateTime actualEndTime,
                     @Param("completeRemark") String completeRemark);

    FarmTask selectById(Long id);

    FarmTask selectBySource(@Param("sourceType") String sourceType,
                            @Param("sourceId") Long sourceId);

    List<FarmTask> selectList(@Param("userId") Long userId,
                              @Param("plotName") String plotName,
                              @Param("taskTitle") String taskTitle,
                              @Param("taskContent") String taskContent,
                              @Param("taskType") Integer taskType,
                              @Param("status") Integer status,
                              @Param("sourceType") String sourceType,
                              @Param("deadlineStart") LocalDateTime deadlineStart,
                              @Param("deadlineEnd") LocalDateTime deadlineEnd);

    /**
     * 查询 farm 用户端任务列表，按当前用户、日期和任务类型过滤。
     */
    List<ClientFarmTaskResponse> selectClientTasks(@Param("userId") Long userId,
                                                   @Param("taskType") Integer taskType,
                                                   @Param("deadlineStart") LocalDateTime deadlineStart,
                                                   @Param("deadlineEnd") LocalDateTime deadlineEnd);

    /** 按业务状态查询当前用户全部未取消的移动端农事任务。 */
    List<ClientFarmTaskResponse> selectClientTasksByStatus(@Param("userId") Long userId,
                                                           @Param("status") Integer status);

    /** 查询指定地块的真实农事任务，并按截至时间由近到远排序。 */
    List<FarmTask> selectClientTasksByPlotId(@Param("userId") Long userId,
                                             @Param("plotId") Long plotId);

    /** 查询指定地块的移动端任务视图，包含作物图片、执行人和任务状态。 */
    List<ClientFarmTaskResponse> selectClientTaskViewsByPlotId(@Param("userId") Long userId,
                                                               @Param("plotId") Long plotId);

    /**
     * 查询单个 farm 用户端任务详情，用于执行任务后返回最新状态。
     */
    ClientFarmTaskResponse selectClientTaskById(@Param("id") Long id,
                                                @Param("userId") Long userId);

    /**
     * 按日期统计 farm 用户端任务数量，供移动端 7 天日期条展示。
     */
    List<ClientFarmTaskDateSummaryResponse> selectClientDateCounts(@Param("userId") Long userId,
                                                                   @Param("deadlineStart") LocalDateTime deadlineStart,
                                                                   @Param("deadlineEnd") LocalDateTime deadlineEnd);

    /**
     * 按状态聚合当前 farm 用户的四类移动端农事任务。
     */
    Map<String, Object> selectClientStatistics(@Param("userId") Long userId);

    Long selectActiveBatchIdByPlotId(Long plotId);

    Map<String, Object> selectStatistics(@Param("userId") Long userId);
}
