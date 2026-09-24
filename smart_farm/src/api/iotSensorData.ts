import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export type SensorMetricValue = number | string

/**
 * 环境监测数据实体，对齐 smart_plant 后端当前 IotSensorData。
 */
export interface IotSensorData {
  id?: number
  deviceId?: number
  deviceCode?: string
  deviceName?: string
  typeId?: number
  typeName?: string
  plotId?: number
  plotName?: string
  plotCode?: string
  userId?: number
  dataCategory?: number
  username?: string
  nickname?: string
  isCurrent?: number
  collectTime?: string
  airTemperature?: SensorMetricValue
  airHumidity?: SensorMetricValue
  lightIntensity?: SensorMetricValue
  waterTemperature?: SensorMetricValue
  phValue?: SensorMetricValue
  ecValue?: SensorMetricValue
  dissolvedOxygen?: SensorMetricValue
  soilTemperature?: SensorMetricValue
  soilMoisture?: SensorMetricValue
  waterLevel?: SensorMetricValue
  waterFlow?: SensorMetricValue
  waterPressure?: SensorMetricValue
  voltage?: SensorMetricValue
  electricCurrent?: SensorMetricValue
  powerValue?: SensorMetricValue
  batteryLevel?: SensorMetricValue
  signalStrength?: number
  runtimeSeconds?: number
  rawData?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 环境监测列表查询参数，与 /iot-sensor-data/list 当前接口保持一致。
 */
export interface IotSensorDataListParams {
  deviceId?: number
  deviceCode?: string
  deviceName?: string
  dataCategory?: number
  plotId?: number
  plotName?: string
  plotCode?: string
  userId?: number
  username?: string
  isCurrent?: number
  status?: number
  collectStartTime?: string
  collectEndTime?: string
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询环境监测数据。 */
export const listIotSensorData = (params: IotSensorDataListParams) => {
  return http<ApiResult<PageInfo<IotSensorData>>>({
    url: '/iot-sensor-data/list',
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

/** 根据监测数据 ID 查询详情。 */
export const getIotSensorDataById = (id: number) => {
  return get<ApiResult<IotSensorData>>(`/iot-sensor-data/${id}`)
}

/** 删除单条环境监测数据。 */
export const deleteIotSensorData = (id: number) => {
  return http<ApiResult<void>>({ url: `/iot-sensor-data/${id}`, method: 'delete' })
}

/** 批量删除环境监测数据。 */
export const batchDeleteIotSensorData = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/iot-sensor-data/batch', method: 'delete', data: ids })
}
