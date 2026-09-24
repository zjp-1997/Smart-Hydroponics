package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Crop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作物信息持久层接口，负责 crop 表的增删改查。
 */
@Mapper
public interface CropMapper {

    /** 新增作物信息 */
    int insert(Crop crop);

    /** 根据ID删除作物信息 */
    int deleteById(Long id);

    /** 根据ID集合批量删除作物信息 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据ID动态更新作物信息 */
    int updateById(Crop crop);

    /** 新增成功后根据数据库主键写入系统生成的作物编码 */
    int updateCropCode(@Param("id") Long id, @Param("cropCode") String cropCode);

    /** 修改作物启用/禁用状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据ID查询作物信息 */
    Crop selectById(Long id);

    /** 分页条件查询作物信息列表 */
    List<Crop> selectList(@Param("cropName") String cropName,
                          @Param("typeId") Long typeId,
                          @Param("status") Integer status,
                          @Param("userId") Long userId);

    /** farm 编辑地块时可选的公共作物、本人作物及本人已使用的作物。 */
    List<Crop> selectClientEditableCrops(@Param("userId") Long userId);

    int countVisibleByUserId(@Param("id") Long id, @Param("userId") Long userId);

    int countOwnedByUserId(@Param("id") Long id, @Param("userId") Long userId);

}
