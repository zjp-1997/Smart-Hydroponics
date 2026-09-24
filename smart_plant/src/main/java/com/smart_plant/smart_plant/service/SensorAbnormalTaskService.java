package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.EnvironmentData;
import com.smart_plant.smart_plant.entity.LightData;
import com.smart_plant.smart_plant.entity.PumpData;
import com.smart_plant.smart_plant.entity.WaterQualityData;

/**
 * 采集数据异常判定与农事任务联动服务。
 *
 * <p>该服务根据作物当前生长期阈值判定数据是否异常，异常时生成 farm_task，
 * 并补发关联 task_id 的农事消息；消息仍保存到 notification，避免 farm_task 与 message_notice/farm_message 互相替代。</p>
 */
public interface SensorAbnormalTaskService {

    /** 根据当前地块作物生长期阈值补齐环境数据状态和异常详情。 */
    void prepareEnvironmentData(EnvironmentData data);

    /** 环境数据入库后，如果状态异常，则按数据ID幂等生成农事任务。 */
    void createEnvironmentTaskIfAbnormal(EnvironmentData data);

    /** 根据当前地块作物生长期阈值补齐水质数据状态和异常详情。 */
    void prepareWaterQualityData(WaterQualityData data);

    /** 水质数据入库后，如果状态异常，则按数据ID幂等生成农事任务。 */
    void createWaterQualityTaskIfAbnormal(WaterQualityData data);

    /** 根据当前地块作物生长期光照建议补齐补光灯数据状态和异常详情。 */
    void prepareLightData(LightData data);

    /** 补光灯数据入库后，如果状态异常，则按数据ID幂等生成农事任务。 */
    void createLightTaskIfAbnormal(LightData data);

    /** 根据当前地块作物生长期灌溉建议补齐水泵数据状态和异常详情。 */
    void preparePumpData(PumpData data);

    /** 水泵数据入库后，如果状态异常，则按数据ID幂等生成农事任务。 */
    void createPumpTaskIfAbnormal(PumpData data);
}
