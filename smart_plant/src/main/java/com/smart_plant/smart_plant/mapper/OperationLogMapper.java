package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {

    int insert(OperationLog operationLog);

    List<OperationLog> selectList(@Param("operatorId") Long operatorId,
                                  @Param("operatorName") String operatorName,
                                  @Param("operation") String operation,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
}
