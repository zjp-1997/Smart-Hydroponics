package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.ErrorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 错误日志持久层，列表查询刻意不加载大字段堆栈。 */
@Mapper
public interface ErrorLogMapper {

    int insert(ErrorLog errorLog);

    ErrorLog selectById(@Param("id") Long id);

    List<ErrorLog> selectList(@Param("traceId") String traceId,
                              @Param("operatorName") String operatorName,
                              @Param("requestUri") String requestUri,
                              @Param("handleStatus") Integer handleStatus,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    int updateHandle(@Param("id") Long id,
                     @Param("handleStatus") Integer handleStatus,
                     @Param("handleRemark") String handleRemark,
                     @Param("handlerId") Long handlerId,
                     @Param("handlerName") String handlerName,
                     @Param("version") Integer version);
}
