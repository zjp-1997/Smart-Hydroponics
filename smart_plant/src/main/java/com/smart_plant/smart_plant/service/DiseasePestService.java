package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseasePest;

import java.util.List;

/**
 * 病虫害基础信息业务接口。
 *
 * <p>该接口定义 disease_pest 表对外暴露的管理能力。</p>
 */
public interface DiseasePestService {

    /** 新增病虫害基础信息。 */
    DiseasePest addDiseasePest(DiseasePest diseasePest);

    /** 根据ID删除病虫害基础信息。 */
    void deleteDiseasePest(Long id);

    /** 根据ID集合批量删除病虫害基础信息。 */
    int deleteDiseasePests(List<Long> ids);

    /** 修改病虫害基础信息。 */
    DiseasePest updateDiseasePest(DiseasePest diseasePest);

    /** 修改病虫害基础信息状态。 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询病虫害详情，详情中会组装防治措施列表。 */
    DiseasePest getDiseasePestById(Long id);

    /** 根据ID查询 farm 用户端可见详情，只返回启用的病虫害和启用的防治措施。 */
    DiseasePest getEnabledDiseasePestById(Long id);

    /** 分页查询病虫害基础信息列表。 */
    PageInfo<DiseasePest> listDiseasePests(String name, Long cropTypeId, Integer type,
                                           Integer status, Integer pageNum, Integer pageSize);

}
