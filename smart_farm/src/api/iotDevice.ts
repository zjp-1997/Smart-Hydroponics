import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 设备信息实体，对应 smart_plant 后端 IotDevice 返回字段。
 * 前端列表、详情弹框和状态操作统一使用该类型，避免页面重复维护字段结构。
 */
export interface IotDevice {
  id?: number
  plotId: number
  plotName?: string
  plotCode?: string
  plotStatus?: number
  userId?: number
  username?: string
  nickname?: string
  deviceCode: string
  name: string
  typeId: number
  typeName?: string
  controlStatus?: number
  onlineStatus?: number
  healthStatus?: number
  installTime?: string
  offlineTime?: string
  faultTime?: string
  lastHeartbeatTime?: string
  lastOnlineTime?: string
  onlineDuration?: number
  lastDataTime?: string
  location?: string
  createTime?: string
  updateTime?: string
}

/**
 * 新增设备时提交给后端的字段。
 * id、createTime、updateTime 等字段由数据库或后端维护，新增表单不提交。
 */
export interface IotDevicePayload {
  plotId: number
  deviceCode?: string
  name: string
  typeId: number
  controlStatus?: number
  onlineStatus?: number
  healthStatus?: number
  installTime?: string
  offlineTime?: string
  faultTime?: string
  lastHeartbeatTime?: string
  lastOnlineTime?: string
  onlineDuration?: number
  lastDataTime?: string
  location?: string
}

export interface UpdateIotDevicePayload extends Partial<IotDevicePayload> {
  id: number
}

export interface IotDeviceListParams {
  plotId?: number
  plotName?: string
  deviceCode?: string
  name?: string
  typeId?: number
  controlStatus?: number
  onlineStatus?: number
  healthStatus?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询设备信息列表，供设备信息管理表格和统计卡使用。 */
export const listIotDevices = (params: IotDeviceListParams) => {
  return get<ApiResult<PageInfo<IotDevice>>>(
    '/iot-device/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据设备 ID 查询详情，供编辑弹框回填。 */
export const getIotDeviceById = (id: number) => {
  return get<ApiResult<IotDevice>>(`/iot-device/${id}`)
}

/** 新增设备信息。 */
export const addIotDevice = (data: IotDevicePayload) => {
  return post<ApiResult<IotDevice>>('/iot-device/add', data as unknown as Record<string, unknown>)
}

/** 编辑设备基础信息和状态字段。 */
export const updateIotDevice = (data: UpdateIotDevicePayload) => {
  return http<ApiResult<IotDevice>>({ url: '/iot-device', method: 'put', data })
}

/** 删除单台设备。后端会在删除前检查是否存在业务数据引用。 */
export const deleteIotDevice = (id: number) => {
  return http<ApiResult<void>>({ url: `/iot-device/${id}`, method: 'delete' })
}

/** 批量删除设备。 */
export const batchDeleteIotDevices = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/iot-device/batch', method: 'delete', data: ids })
}

/** 保存设备期望控制状态，1 表示期望开启，0 表示期望关闭；返回成功不代表硬件已执行。 */
export const updateIotDeviceStatus = (id: number, controlStatus: number) => {
  return http<ApiResult<void>>({
    url: `/iot-device/${id}/control-status`,
    method: 'put',
    params: { controlStatus },
  })
}

/** 修改设备在线状态，1 表示在线，0 表示离线。 */
export const updateIotDeviceOnlineStatus = (id: number, onlineStatus: number) => {
  return http<ApiResult<void>>({
    url: `/iot-device/${id}/online-status`,
    method: 'put',
    params: { onlineStatus },
  })
}

/** 修改设备健康状态，0 正常，1 故障，2 维护中。 */
export const updateIotDeviceHealthStatus = (id: number, healthStatus: number) => {
  return http<ApiResult<void>>({
    url: `/iot-device/${id}/health-status`,
    method: 'put',
    params: { healthStatus },
  })
}

/** 更新设备心跳时间；未传时间时后端使用服务器当前时间，并将在线状态置为在线。 */
export const updateIotDeviceHeartbeat = (id: number, lastHeartbeatTime?: string) => {
  return http<ApiResult<void>>({
    url: `/iot-device/${id}/heartbeat`,
    method: 'put',
    params: lastHeartbeatTime ? { lastHeartbeatTime } : undefined,
  })
}
