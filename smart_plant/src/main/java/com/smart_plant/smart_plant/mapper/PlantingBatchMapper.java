package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.PlantingBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PlantingBatchMapper {

    int insert(PlantingBatch plantingBatch);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int updateById(PlantingBatch plantingBatch);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int completeHarvest(@Param("id") Long id,
                        @Param("harvester") String harvester,
                        @Param("yieldAmount") BigDecimal yieldAmount,
                        @Param("yieldUnit") String yieldUnit,
                        @Param("actualHarvestAt") LocalDate actualHarvestAt);

    PlantingBatch selectById(Long id);

    PlantingBatch selectActiveByPlotIdForUpdate(Long plotId);

    List<PlantingBatch> selectList(@Param("plotId") Long plotId,
                                   @Param("cropId") Long cropId,
                                   @Param("userId") Long userId,
                                   @Param("batchNo") String batchNo,
                                   @Param("status") Integer status);

    int countByUserIdAndBatchNo(@Param("userId") Long userId,
                                @Param("batchNo") String batchNo,
                                @Param("excludeId") Long excludeId);

    int countActiveByPlotId(@Param("plotId") Long plotId,
                            @Param("excludeId") Long excludeId);
}
