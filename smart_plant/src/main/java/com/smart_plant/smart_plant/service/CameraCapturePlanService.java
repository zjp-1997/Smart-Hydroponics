package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraCapturePlanSaveRequest;
import com.smart_plant.smart_plant.entity.CameraCapturePlan;

/** 摄像头作物图像采集计划业务接口。 */
public interface CameraCapturePlanService {
    /** 新增计划。 */ CameraCapturePlan add(CameraCapturePlanSaveRequest request);
    /** 修改计划。 */ CameraCapturePlan update(CameraCapturePlanSaveRequest request);
    /** 删除计划。 */ void delete(Long id);
    /** 查询详情。 */ CameraCapturePlan get(Long id);
    /** 分页查询。 */
    PageInfo<CameraCapturePlan> list(Long plotId, String keyword, Integer enabled,
                                     Integer pageNum, Integer pageSize);
}
