package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ExpertProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 专家信息持久层接口，负责 expert_profile 表的增删改查。
 */
@Mapper
public interface ExpertProfileMapper {

    /** 新增专家信息 */
    int insert(ExpertProfile expertProfile);

    /** 根据ID删除专家信息 */
    int deleteById(Long id);

    /** 根据ID集合批量删除专家信息 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新专家信息 */
    int updateById(ExpertProfile expertProfile);

    /** 修改专家账号启用/禁用状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 修改专家咨询状态 */
    int updateConsultationStatus(@Param("id") Long id, @Param("consultationStatus") Integer consultationStatus);

    /** 用户账号禁用时同步禁用专家档案和咨询能力 */
    int disableByUserId(@Param("userId") Long userId);

    /** 修改专家入驻审核状态 */
    int updateAuditStatus(@Param("id") Long id, @Param("auditStatus") Integer auditStatus);

    /** 根据ID查询专家信息 */
    ExpertProfile selectById(Long id);

    /** 根据关联用户ID查询专家信息 */
    ExpertProfile selectByUserId(Long userId);

    /** 分页条件查询专家信息列表 */
    List<ExpertProfile> selectList(@Param("realName") String realName,
                                   @Param("organization") String organization,
                                   @Param("specialty") String specialty,
                                   @Param("auditStatus") Integer auditStatus,
                                   @Param("serviceStatus") Integer serviceStatus,
                                   @Param("status") Integer status);

    /** 查询用户端全部专家列表 */
    List<ExpertProfile> selectClientList(@Param("realName") String realName);

    /** 查询专家管理统计数据 */
    Map<String, Object> selectStatistics();

    List<ExpertProfile> selectAuditList(@Param("realName") String realName,
                                        @Param("organization") String organization,
                                        @Param("specialty") String specialty,
                                        @Param("auditStatus") Integer auditStatus,
                                        @Param("status") Integer status);

    /** 统计关联用户ID是否已绑定专家资料，用于唯一性校验 */
    int countByUserId(@Param("userId") Long userId, @Param("excludeId") Long excludeId);

    /** 统计用户ID集合中已绑定专家资料的数量 */
    int countByUserIds(@Param("userIds") List<Long> userIds);

    /** 统计关联用户ID已审核通过的专家资料数量 */
    int countApprovedByUserId(@Param("userId") Long userId);

    /** 统计用户ID集合中已审核通过的专家资料数量 */
    int countApprovedByUserIds(@Param("userIds") List<Long> userIds);

    /** 删除指定用户绑定的未审核通过专家资料，释放用户删除外键约束 */
    int deleteUnapprovedByUserId(@Param("userId") Long userId);

    /** 批量删除指定用户绑定的未审核通过专家资料，释放用户删除外键约束 */
    int deleteUnapprovedByUserIds(@Param("userIds") List<Long> userIds);

    /** 根据评价表重新计算专家平均评分和评价数量 */
    int updateRatingSummary(@Param("expertId") Long expertId);
}
