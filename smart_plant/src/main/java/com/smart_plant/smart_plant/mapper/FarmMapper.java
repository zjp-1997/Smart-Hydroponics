package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.dto.ClientFarmListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface FarmMapper {

    int insert(Farm farm);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(Farm farm);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    Farm selectById(Long id);

    List<Farm> selectList(@Param("farmName") String farmName,
                          @Param("farmCode") String farmCode,
                          @Param("userId") Long userId,
                          @Param("status") Integer status);

    Map<String, Object> selectStatistics(@Param("userId") Long userId);

    List<ClientFarmListResponse> selectClientFarmListByUserId(@Param("userId") Long userId);

    int countByUserIdAndFarmCode(@Param("userId") Long userId,
                                 @Param("farmCode") String farmCode,
                                 @Param("excludeId") Long excludeId);

    int countByFarmCode(@Param("farmCode") String farmCode,
                        @Param("excludeId") Long excludeId);

    int countPlotByFarmId(@Param("farmId") Long farmId);

    int countPlotByFarmIds(@Param("farmIds") List<Long> farmIds);
}
