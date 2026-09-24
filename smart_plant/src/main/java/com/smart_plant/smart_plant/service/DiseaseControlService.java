package com.smart_plant.smart_plant.service;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.entity.DiseaseControl;

import java.util.List;

/**
 * 病虫害防治措施业务接口。
 *
 * <p>该接口定义 disease_control 表对外暴露的管理能力。</p>
 */
public interface DiseaseControlService {

    /** 新增防治措施。 */
    DiseaseControl addDiseaseControl(DiseaseControl diseaseControl);

    /** 根据ID删除防治措施。 */
    void deleteDiseaseControl(Long id);

    /** 根据ID集合批量删除防治措施。 */
    int deleteDiseaseControls(List<Long> ids);

    /** 修改防治措施。 */
    DiseaseControl updateDiseaseControl(DiseaseControl diseaseControl);

    /** 修改防治措施状态。 */
    void updateStatus(Long id, Integer status);

    /** 根据ID查询防治措施详情。 */
    DiseaseControl getDiseaseControlById(Long id);

    /** 分页查询防治措施列表。 */
    PageInfo<DiseaseControl> listDiseaseControls(Long diseaseId, String diseaseName, Integer controlType,
                                                 Integer controlCategory, Integer status,
                                                 Integer pageNum, Integer pageSize);
}
