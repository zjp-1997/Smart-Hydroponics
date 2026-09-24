package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.ClientDeviceFaultResponse;
import com.smart_plant.smart_plant.dto.ClientDeviceFaultRecordResponse;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * farm 移动端设备故障业务接口。
 */
public interface ClientDeviceFaultService {

    /** 按处理状态查询当前用户有权查看的故障。 */
    List<ClientDeviceFaultResponse> listFaults(Integer status);

    /** 技术人员首页返回本人被指派的全部故障，并按处理进度排序。 */
    List<ClientDeviceFaultResponse> listTechnicianFaults();

    /** 技术人员处理记录只返回本人被指派且已处理的故障。 */
    List<ClientDeviceFaultResponse> listTechnicianCompletedFaults();

    /** 读取单个故障的维护过程，并复用现有详情权限校验。 */
    ClientDeviceFaultRecordResponse getFaultRecord(Long id);

    /** 由被指派的维修人员接受故障。 */
    ClientDeviceFaultResponse acceptFault(Long id);

    /** 校验当前处理人和故障状态后上传现场图片。 */
    CropImageUploadResult uploadCompletionImage(Long id, MultipartFile image);

    /** 由处理人提交现场图片及选填说明并完成故障。 */
    ClientDeviceFaultResponse completeFault(Long id, String handleResult, String completionImageUrl);
}
