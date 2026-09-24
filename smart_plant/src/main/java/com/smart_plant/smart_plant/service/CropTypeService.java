package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.CropType;

import java.util.List;

/**
 * 作物分类业务接口，定义作物分类管理能力。
 */
public interface CropTypeService {

    /** 新增作物分类 */
    CropType addCropType(CropType cropType);

    /** 编辑作物分类 */
    CropType updateCropType(CropType cropType);

    /** 删除作物分类 */
    void deleteCropType(Long id);

    /** 批量删除作物分类 */
    int deleteCropTypes(List<Long> ids);

    /** 启用或禁用作物分类 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询作物分类 */
    CropType getCropTypeById(Long id);

    /** 分页查询作物分类列表 */
    PageInfo<CropType> listCropTypes(String typeName, Long parentId, Integer status,
                                     Integer pageNum, Integer pageSize);
}
