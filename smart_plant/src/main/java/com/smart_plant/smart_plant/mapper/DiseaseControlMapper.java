package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.DiseaseControl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 病虫害防治措施持久层接口。
 *
 * <p>该 Mapper 负责 disease_control 表的增删改查，并为列表查询提供筛选条件。</p>
 */
@Mapper
public interface DiseaseControlMapper {

    /** 新增一条防治措施记录。 */
    int insert(DiseaseControl diseaseControl);

    /** 根据ID删除一条防治措施记录。 */
    int deleteById(Long id);

    /** 根据ID集合批量删除防治措施记录。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新防治措施记录。 */
    int updateById(DiseaseControl diseaseControl);

    /** 单独修改防治措施启用/停用状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据ID查询防治措施详情，并带出病虫害名称。 */
    DiseaseControl selectById(Long id);

    /** 按条件查询防治措施列表。 */
    List<DiseaseControl> selectList(@Param("diseaseId") Long diseaseId,
                                    @Param("diseaseName") String diseaseName,
                                    @Param("controlType") Integer controlType,
                                    @Param("controlCategory") Integer controlCategory,
                                    @Param("status") Integer status);
}
