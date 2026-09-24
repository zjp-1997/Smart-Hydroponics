package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientFarmTaskDateSummaryResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskActionRequest;
import com.smart_plant.smart_plant.dto.ClientFarmTaskResponse;
import com.smart_plant.smart_plant.dto.ClientFarmTaskStatisticsResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.FarmTaskRecord;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/**
 * farm 用户端农事任务服务。
 *
 * <p>该服务只处理当前登录用户的数据，避免移动端直接传 userId 带来的越权风险。</p>
 */
public interface ClientFarmTaskService {

    /**
     * 查询连续 7 天的任务数量摘要。
     *
     * @param startDate 起始日期，为空时默认从当天开始
     * @return 7 天日期摘要
     */
    List<ClientFarmTaskDateSummaryResponse> listDateSummaries(LocalDate startDate);

    /**
     * 根据日期和任务类型查询当前用户的任务列表。
     *
     * @param taskDate 任务日期，为空时默认当天
     * @param taskType 任务类型，为空表示全部
     * @return 当前日期下的任务列表
     */
    List<ClientFarmTaskResponse> listTasks(LocalDate taskDate, Integer taskType);

    /**
     * 按状态查询当前用户的全部农事任务。
     *
     * @param status 1未开始、2进行中、3已完成、4已逾期，为空表示全部
     * @return 当前用户未取消的任务列表
     */
    List<ClientFarmTaskResponse> listMyTasks(Integer status);

    /**
     * 查询当前用户指定地块的全部农事任务，供地块农事时间序列页面展示。
     *
     * @param plotId 地块 ID
     * @return 按截止时间由近到远排列的任务列表
     */
    List<ClientFarmTaskResponse> listTasksByPlot(Long plotId);

    /**
     * 查询当前用户指定任务的详情。
     *
     * @param id 农事任务 ID
     * @return 聚合地块、作物和任务状态的详情
     */
    ClientFarmTaskResponse getTaskDetail(Long id);

    /** 上传当前用户指定任务的完成凭证图片。 */
    CropImageUploadResult uploadCompletionImage(Long id, MultipartFile image);

    /**
     * 查询当前用户的首页任务状态统计。
     *
     * @return 未开始、进行中、已完成、已逾期任务数
     */
    ClientFarmTaskStatisticsResponse statistics();

    /**
     * 将未开始任务更新为进行中。
     *
     * @param id 农事任务 ID
     * @return 更新后的任务信息
     */
    ClientFarmTaskResponse executeTask(Long id, ClientFarmTaskActionRequest request);

    /** 记录进行中任务的阶段性进展，任务状态保持为进行中。 */
    ClientFarmTaskResponse submitProgress(Long id, ClientFarmTaskActionRequest request);

    /** 完成进行中的任务，并同时写入实际完成时间和完成事件。 */
    ClientFarmTaskResponse completeTask(Long id, ClientFarmTaskActionRequest request);

    /** 查询当前用户指定任务的完整执行时间线。 */
    List<FarmTaskRecord> listTimeline(Long id);

    /** 查询当前用户指定地块下全部任务的完整操作时间线。 */
    List<FarmTaskRecord> listPlotTimeline(Long plotId);
}
