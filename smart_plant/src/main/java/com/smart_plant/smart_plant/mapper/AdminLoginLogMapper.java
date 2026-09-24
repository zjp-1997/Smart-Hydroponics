package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AdminLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminLoginLogMapper {

    int insert(AdminLoginLog loginLog);

    List<AdminLoginLog> selectList(@Param("username") String username,
                                   @Param("ip") String ip,
                                   @Param("success") Integer success,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
