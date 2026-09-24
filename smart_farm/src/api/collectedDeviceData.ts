import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export type DeviceDataMode = 'environment' | 'waterQuality' | 'light' | 'pump'
export type DeviceDataMetricValue = number | string

/**
 * 四类设备采集数据共有的关联展示字段。
 * 后端查询时会联查 iot_device、device_type、plot，因此列表可直接展示设备和地块信息。
 */
export interface BaseCollectedDeviceData {
  id?: number
  deviceId?: number
  deviceCode?: string
  deviceName?: string
  typeId?: number
  typeCode?: string
  typeName?: string
  plotId?: number
  plotName?: string
  plotCode?: string
  dataStatus?: number
  abnormalDetail?: string
  cumulativeRuntime?: DeviceDataMetricValue
  collectTime?: string
  remark?: string
  createTime?: string
}

export interface EnvironmentData extends BaseCollectedDeviceData {
  airTemperature?: DeviceDataMetricValue
  airHumidity?: DeviceDataMetricValue
  /** 风速，单位 m/s，对应后端 environment_data.wind_speed。 */
  windSpeed?: DeviceDataMetricValue
  /** 气压，单位 hPa，对应后端 environment_data.air_pressure。 */
  airPressure?: DeviceDataMetricValue
  co2Concentration?: DeviceDataMetricValue
  pm25?: DeviceDataMetricValue
}

export interface WaterQualityData extends BaseCollectedDeviceData {
  waterTemperature?: DeviceDataMetricValue
  ph?: DeviceDataMetricValue
  ecValue?: DeviceDataMetricValue
  dissolvedOxygen?: DeviceDataMetricValue
}

export interface LightData extends BaseCollectedDeviceData {
  lightIntensity?: DeviceDataMetricValue
}

export interface PumpData extends BaseCollectedDeviceData {
  waterFlow?: DeviceDataMetricValue
  waterPressure?: DeviceDataMetricValue
}

export type CollectedDeviceData = EnvironmentData | WaterQualityData | LightData | PumpData

export interface CollectedDeviceDataListParams {
  plotId?: number
  deviceId?: number
  deviceCode?: string
  dataStatus?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export type CollectedDeviceDataStatisticsParams = Omit<
  CollectedDeviceDataListParams,
  'pageNum' | 'pageSize'
>

/**
 * 统计接口返回字段由各数据 Mapper 聚合生成。
 * 不同数据类型的平均值字段不同，因此保留索引签名供页面按配置读取。
 */
export interface CollectedDeviceDataStatistics {
  totalCount?: number
  normalCount?: number
  abnormalCount?: number
  deviceCount?: number
  plotCount?: number
  firstCollectTime?: string
  latestCollectTime?: string
  avgCumulativeRuntime?: DeviceDataMetricValue
  [key: string]: DeviceDataMetricValue | undefined
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

const endpointMap: Record<DeviceDataMode, string> = {
  environment: '/environment-data',
  waterQuality: '/water-quality-data',
  light: '/light-data',
  pump: '/pump-data',
}

const getEndpoint = (mode: DeviceDataMode) => endpointMap[mode]

/** 查询指定类型的设备采集数据列表。 */
export const listCollectedDeviceData = (
  mode: DeviceDataMode,
  params: CollectedDeviceDataListParams,
) => {
  return http<ApiResult<PageInfo<CollectedDeviceData>>>({
    url: `${getEndpoint(mode)}/list`,
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

/** 查询指定类型的设备采集数据统计值，用于页面顶部统计卡片。 */
export const getCollectedDeviceDataStatistics = (
  mode: DeviceDataMode,
  params: CollectedDeviceDataStatisticsParams,
) => {
  return http<ApiResult<CollectedDeviceDataStatistics>>({
    url: `${getEndpoint(mode)}/statistics`,
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

/** 根据 ID 查询采集数据详情。 */
export const getCollectedDeviceDataById = (mode: DeviceDataMode, id: number) => {
  return get<ApiResult<CollectedDeviceData>>(`${getEndpoint(mode)}/${id}`)
}

/** 删除单条采集数据。 */
export const deleteCollectedDeviceData = (mode: DeviceDataMode, id: number) => {
  return http<ApiResult<void>>({ url: `${getEndpoint(mode)}/${id}`, method: 'delete' })
}

/** 批量删除采集数据。 */
export const batchDeleteCollectedDeviceData = (mode: DeviceDataMode, ids: number[]) => {
  return http<ApiResult<number>>({ url: `${getEndpoint(mode)}/batch`, method: 'delete', data: ids })
}
