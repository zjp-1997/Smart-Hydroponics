package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.Crop;

import java.util.List;

/**
 * 作物信息业务接口，定义作物信息管理能力。
 */
public interface CropService {

    /** 新增作物信息 */
    Crop addCrop(Crop crop);

    /** 编辑作物信息 */
    Crop updateCrop(Crop crop);

    /** 删除作物信息 */
    void deleteCrop(Long id);

    /** 批量删除作物信息 */
    int deleteCrops(List<Long> ids);

    /** 启用或禁用作物 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询作物信息 */
    Crop getCropById(Long id);

    /** 分页查询作物信息列表 */
    PageInfo<Crop> listCrops(String cropName, Long typeId, Integer status,
                             Integer pageNum, Integer pageSize);
}
