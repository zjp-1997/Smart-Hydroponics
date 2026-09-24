import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export type SnapshotMetricValue = number | string

/**
 * 地块聚合传感器快照实体，对应 smart_plant 后端 IotPlotSensorSnapshot。
 */
export interface IotPlotSensorSnapshot {
  id?: number
  plotId?: number
  plotName?: string
  plotCode?: string
  userId?: number
  username?: string
  nickname?: string
  isCurrent?: number
  snapshotTime?: string
  airSourceDataId?: number
  airSourceDeviceId?: number
  airSourceDeviceCode?: string
  airSourceDeviceName?: string
  soilSourceDataId?: number
  soilSourceDeviceId?: number
  soilSourceDeviceCode?: string
  soilSourceDeviceName?: string
  waterSourceDataId?: number
  waterSourceDeviceId?: number
  waterSourceDeviceCode?: string
  waterSourceDeviceName?: string
  runtimeSourceDataId?: number
  runtimeSourceDeviceId?: number
  runtimeSourceDeviceCode?: string
  runtimeSourceDeviceName?: string
  cameraSourceDataId?: number
  cameraSourceDeviceId?: number
  cameraSourceDeviceCode?: string
  cameraSourceDeviceName?: string
  pumpSourceDataId?: number
  pumpSourceDeviceId?: number
  pumpSourceDeviceCode?: string
  pumpSourceDeviceName?: string
  lightSourceDataId?: number
  lightSourceDeviceId?: number
  lightSourceDeviceCode?: string
  lightSourceDeviceName?: string
  fanSourceDataId?: number
  fanSourceDeviceId?: number
  fanSourceDeviceCode?: string
  fanSourceDeviceName?: string
  airTemperature?: SnapshotMetricValue
  airHumidity?: SnapshotMetricValue
  lightIntensity?: SnapshotMetricValue
  soilTemperature?: SnapshotMetricValue
  soilMoisture?: SnapshotMetricValue
  waterTemperature?: SnapshotMetricValue
  phValue?: SnapshotMetricValue
  ecValue?: SnapshotMetricValue
  dissolvedOxygen?: SnapshotMetricValue
  waterLevel?: SnapshotMetricValue
  waterFlow?: SnapshotMetricValue
  waterPressure?: SnapshotMetricValue
  runningStatus?: number
  cameraVoltage?: SnapshotMetricValue
  cameraElectricCurrent?: SnapshotMetricValue
  cameraPowerValue?: SnapshotMetricValue
  cameraBatteryLevel?: SnapshotMetricValue
  cameraSignalStrength?: number
  cameraRuntimeSeconds?: number
  cameraRawData?: string
  pumpWaterFlow?: SnapshotMetricValue
  pumpWaterPressure?: SnapshotMetricValue
  pumpVoltage?: SnapshotMetricValue
  pumpElectricCurrent?: SnapshotMetricValue
  pumpPowerValue?: SnapshotMetricValue
  pumpSignalStrength?: number
  pumpRuntimeSeconds?: number
  pumpRawData?: string
  lightVoltage?: SnapshotMetricValue
  lightElectricCurrent?: SnapshotMetricValue
  lightPowerValue?: SnapshotMetricValue
  lightSignalStrength?: number
  lightRuntimeSeconds?: number
  lightRawData?: string
  fanVoltage?: SnapshotMetricValue
  fanElectricCurrent?: SnapshotMetricValue
  fanPowerValue?: SnapshotMetricValue
  fanSignalStrength?: number
  fanRuntimeSeconds?: number
  fanRawData?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface IotPlotSensorSnapshotStatistics {
  totalCount?: number
  currentCount?: number
  historyCount?: number
  normalCount?: number
  abnormalCount?: number
  invalidCount?: number
  coveredPlotCount?: number
  completeSevenDeviceSnapshotCount?: number
  avgAirTemperature?: SnapshotMetricValue
  avgAirHumidity?: SnapshotMetricValue
  avgLightIntensity?: SnapshotMetricValue
  avgSoilTemperature?: SnapshotMetricValue
  avgSoilMoisture?: SnapshotMetricValue
  avgWaterTemperature?: SnapshotMetricValue
  avgPhValue?: SnapshotMetricValue
  avgEcValue?: SnapshotMetricValue
  avgDissolvedOxygen?: SnapshotMetricValue
  avgPumpWaterFlow?: SnapshotMetricValue
  avgPumpPowerValue?: SnapshotMetricValue
  avgLightPowerValue?: SnapshotMetricValue
  avgFanPowerValue?: SnapshotMetricValue
}

/**
 * 地块数据列表查询参数，与 /iot-plot-sensor-snapshot/list 和 /statistics 保持一致。
 */
export interface IotPlotSensorSnapshotListParams {
  plotId?: number
  plotName?: string
  plotCode?: string
  userId?: number
  username?: string
  isCurrent?: number
  runningStatus?: number
  status?: number
  snapshotStartTime?: string
  snapshotEndTime?: string
  pageNum: number
  pageSize: number
}

export type IotPlotSensorSnapshotStatisticsParams = Omit<
  IotPlotSensorSnapshotListParams,
  'pageNum' | 'pageSize'
>

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询地块聚合传感器快照。 */
export const listIotPlotSensorSnapshots = (params: IotPlotSensorSnapshotListParams) => {
  return http<ApiResult<PageInfo<IotPlotSensorSnapshot>>>({
    url: '/iot-plot-sensor-snapshot/list',
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

/** 查询地块聚合快照统计数据。 */
export const getIotPlotSensorSnapshotStatistics = (
  params: IotPlotSensorSnapshotStatisticsParams,
) => {
  return http<ApiResult<IotPlotSensorSnapshotStatistics>>({
    url: '/iot-plot-sensor-snapshot/statistics',
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

/** 根据快照 ID 查询地块数据详情。 */
export const getIotPlotSensorSnapshotById = (id: number) => {
  return get<ApiResult<IotPlotSensorSnapshot>>(`/iot-plot-sensor-snapshot/${id}`)
}

/** 删除单条地块数据快照。 */
export const deleteIotPlotSensorSnapshot = (id: number) => {
  return http<ApiResult<void>>({ url: `/iot-plot-sensor-snapshot/${id}`, method: 'delete' })
}

/** 批量删除地块数据快照。 */
export const batchDeleteIotPlotSensorSnapshots = (ids: number[]) => {
  return http<ApiResult<number>>({
    url: '/iot-plot-sensor-snapshot/batch',
    method: 'delete',
    data: ids,
  })
}
