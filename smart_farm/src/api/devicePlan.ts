import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 补光灯或水泵的一段每周自动运行时间。 */
export interface IotDeviceSchedule {
  id?: number
  planId?: number
  scheduleName: string
  weekdaysMask: number
  startTime: string
  endTime: string
  enabled: number
  sortOrder?: number
}

/** IoT 设备计划列表响应；id 为空表示该设备尚未保存计划。 */
export interface IotDevicePlan {
  id?: number
  deviceId: number
  plotId?: number
  plotName?: string
  deviceCode?: string
  deviceName?: string
  typeCode?: string
  typeName?: string
  onlineStatus?: number
  healthStatus?: number
  collectionEnabled: boolean
  collectionIntervalMinutes: number
  controlEnabled: boolean
  timezone: string
  effectiveFrom?: string
  effectiveTo?: string
  applyStatus?: number
  lastAppliedAt?: string
  lastApplyError?: string
  version?: number
  updateTime?: string
  schedules: IotDeviceSchedule[]
}

/** 保存 IoT 设备计划时允许提交的业务字段。 */
export interface IotDevicePlanPayload {
  deviceId: number
  collectionEnabled: boolean
  collectionIntervalMinutes: number
  controlEnabled: boolean
  timezone: string
  effectiveFrom?: string
  effectiveTo?: string
  version?: number
  schedules: Omit<IotDeviceSchedule, 'id' | 'planId'>[]
}

/** 后端为设备计划列表提供的稳定分页结构。 */
export interface IotDevicePlanPage {
  list: IotDevicePlan[]
  total: number
  pageNum: number
  pageSize: number
}

/** 摄像头作物图像采集计划。 */
export interface CameraCapturePlan {
  id?: number
  deviceId: number
  userId?: number
  plotId?: number
  planName: string
  cameraName?: string
  plotName?: string
  plotCode?: string
  onlineStatus?: number
  intervalMinutes: number
  weekdaysMask: number
  startTime: string
  endTime: string
  timezone: string
  enabled: number
  applyStatus?: number
  lastCaptureTime?: string
  nextCaptureTime?: string
  lastApplyError?: string
  version?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 摄像头计划保存白名单。 */
export interface CameraCapturePlanPayload {
  id?: number
  deviceId: number
  planName: string
  intervalMinutes: number
  weekdaysMask: number
  startTime: string
  endTime: string
  timezone: string
  enabled: boolean
  remark?: string
  version?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询四类 IoT 设备及其当前计划。 */
export const listIotDevicePlans = (params: {
  plotId?: number
  typeCode?: string
  keyword?: string
  pageNum: number
  pageSize: number
}) => get<ApiResult<IotDevicePlanPage>>('/iot-device-plan/list', params)

/** 查询指定设备的计划详情或服务端默认计划。 */
export const getIotDevicePlan = (deviceId: number) =>
  get<ApiResult<IotDevicePlan>>(`/iot-device-plan/device/${deviceId}`)

/** 新增和修改统一使用设备维度的幂等保存接口。 */
export const saveIotDevicePlan = (data: IotDevicePlanPayload) =>
  http<ApiResult<IotDevicePlan>>({ url: '/iot-device-plan', method: 'put', data })

/** 删除设备当前计划，使页面恢复服务端默认建议值。 */
export const deleteIotDevicePlan = (deviceId: number) =>
  http<ApiResult<void>>({ url: `/iot-device-plan/device/${deviceId}`, method: 'delete' })

/** 分页查询摄像头采集计划。 */
export const listCameraCapturePlans = (params: {
  plotId?: number
  keyword?: string
  enabled?: number
  pageNum: number
  pageSize: number
}) => get<ApiResult<PageInfo<CameraCapturePlan>>>('/camera-capture-plan/list', params)

/** 查询摄像头采集计划详情。 */
export const getCameraCapturePlan = (id: number) =>
  get<ApiResult<CameraCapturePlan>>(`/camera-capture-plan/${id}`)

/** 新增摄像头采集计划。 */
export const addCameraCapturePlan = (data: CameraCapturePlanPayload) =>
  post<ApiResult<CameraCapturePlan>>('/camera-capture-plan/add', data as unknown as Record<string, unknown>)

/** 修改摄像头采集计划。 */
export const updateCameraCapturePlan = (data: CameraCapturePlanPayload) =>
  http<ApiResult<CameraCapturePlan>>({ url: '/camera-capture-plan', method: 'put', data })

/** 删除摄像头采集计划。 */
export const deleteCameraCapturePlan = (id: number) =>
  http<ApiResult<void>>({ url: `/camera-capture-plan/${id}`, method: 'delete' })
