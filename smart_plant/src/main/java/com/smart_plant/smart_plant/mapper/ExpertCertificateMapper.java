package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ExpertCertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专家资质证书持久层接口，负责 expert_certificate 表的维护。
 */
@Mapper
public interface ExpertCertificateMapper {

    /** 新增专家证书记录。 */
    int insert(ExpertCertificate expertCertificate);

    /** 根据专家ID删除证书记录。 */
    int deleteByExpertId(Long expertId);

    /** 根据专家ID集合批量删除证书记录。 */
    int deleteBatchByExpertIds(@Param("expertIds") List<Long> expertIds);

    /** 根据专家ID查询第一条证书记录，用于兼容专家管理列表的单证书展示。 */
    ExpertCertificate selectFirstByExpertId(Long expertId);
}
