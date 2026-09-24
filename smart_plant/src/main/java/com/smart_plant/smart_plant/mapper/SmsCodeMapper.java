package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.SmsCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 短信验证码持久化接口。
 */
@Mapper
public interface SmsCodeMapper {

    /** 写入一条新验证码记录。 */
    int insert(SmsCode smsCode);

    /** 查询指定手机号和场景最近一次发码时间，用于限制重复发送。 */
    SmsCode selectLatest(@Param("phone") String phone, @Param("scene") Integer scene);

    /** 锁定并读取最新有效验证码，避免并发请求重复消费同一个验证码。 */
    SmsCode selectLatestValidForUpdate(@Param("phone") String phone, @Param("scene") Integer scene);

    /** 发送新验证码前使旧验证码失效，保证同一场景只有最后一条可用。 */
    int invalidateActive(@Param("phone") String phone, @Param("scene") Integer scene);

    /** 校验成功后将验证码原子标记为已使用。 */
    int markUsed(@Param("id") Long id);
}
