package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.dto.SystemSettingUpdateRequest;
import com.smart_plant.smart_plant.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 全局系统设置数据库访问接口。 */
@Mapper
public interface SystemSettingMapper {

    /** 读取固定主键为1的系统设置。 */
    SystemSetting selectSingleton();

    /** 通过版本号进行原子更新，返回0表示配置已被其他管理员修改。 */
    int updateSingleton(@Param("setting") SystemSettingUpdateRequest setting,
                        @Param("updateBy") Long updateBy);
}
