package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CropImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CropImageMapper {

    int insert(CropImage cropImage);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(CropImage cropImage);

    CropImage selectById(Long id);

    List<CropImage> selectList(@Param("userId") Long userId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);
}
