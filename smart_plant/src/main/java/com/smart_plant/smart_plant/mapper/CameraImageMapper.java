package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.CameraImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CameraImageMapper {

    int insert(CameraImage cameraImage);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(CameraImage cameraImage);

    CameraImage selectById(Long id);

    List<CameraImage> selectList(@Param("userId") Long userId,
                                 @Param("cameraId") Long cameraId,
                                 @Param("plotId") Long plotId,
                                 @Param("cameraName") String cameraName,
                                 @Param("plotName") String plotName,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
}
