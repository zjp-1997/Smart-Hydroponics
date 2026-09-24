package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.DeviceType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备类型 Mapper 接口。
 *
 * <p>该接口声明 device_type 表的增删改查、唯一性校验和设备引用统计方法。</p>
 */
@Mapper
public interface DeviceTypeMapper {

    /** 新增设备类型，并回填自增主键。 */
    int insert(DeviceType deviceType);

    /** 根据主键删除单条设备类型记录。 */
    int deleteById(Long id);

    /** 根据主键集合批量删除设备类型记录。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据主键动态更新设备类型非空字段。 */
    int updateById(DeviceType deviceType);

    /** 单独更新设备类型启用或禁用状态。 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 根据主键查询设备类型详情。 */
    DeviceType selectById(Long id);

    /** 根据唯一类型编码查询设备类型。 */
    DeviceType selectByTypeCode(String typeCode);

    /** 按编码、名称、分类和状态分页前置查询设备类型列表。 */
    List<DeviceType> selectList(@Param("typeCode") String typeCode,
                                @Param("typeName") String typeName,
                                @Param("category") Integer category,
                                @Param("status") Integer status);

    /** 统计同编码设备类型数量，用于新增和修改时校验唯一性。 */
    int countByTypeCode(@Param("typeCode") String typeCode, @Param("excludeId") Long excludeId);

    /** 统计设备表中引用指定设备类型的数量，用于删除保护。 */
    int countDevicesByTypeId(Long typeId);
}
