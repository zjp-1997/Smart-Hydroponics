package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientAiRecognitionTypeResponse;
import com.smart_plant.smart_plant.dto.ClientAiRecognitionRecordResponse;
import com.smart_plant.smart_plant.dto.ClientManualRecognitionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 用户端智能识别服务。
 *
 * <p>该接口面向移动端场景，隐藏后台管理表结构和权限细节。</p>
 */
public interface ClientAiRecognitionService {

    /**
     * 查询已启用的识别类型，供 farm 端智能策略页面选择。
     *
     * @return 用户可选择的识别类型列表
     */
    List<ClientAiRecognitionTypeResponse> listEnabledRecognitionTypes();

    /**
     * 查询当前登录用户的手工识别记录列表。
     *
     * @param pageNum 当前页码
     * @param pageSize 每页条数
     * @return 识别记录列表，按识别记录创建时间倒序返回
     */
    List<ClientAiRecognitionRecordResponse> listManualRecognitionRecords(Integer pageNum, Integer pageSize);

    /**
     * 创建一条手工上传识别记录，并返回结果页展示所需数据。
     *
     * @param recognitionType 识别类型ID
     * @param image 用户上传的识别图片
     * @return 识别结果展示数据
     */
    ClientManualRecognitionResponse recognizeManually(Long recognitionType, MultipartFile image);
}
