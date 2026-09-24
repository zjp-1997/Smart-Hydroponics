import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface CameraDevice {
  id?: number
  deviceId: number
  plotId: number
  plotName?: string
  plotCode?: string
  userId?: number
  username?: string
  nickname?: string
  name: string
  streamProtocol?: string
  streamUrl?: string
  snapshotUrl?: string
  resolution?: string
  onlineStatus?: number
  direction?: string
  createTime?: string
}

export interface CameraDevicePayload {
  deviceId: number
  plotId: number
  name: string
  streamProtocol?: string
  streamUrl?: string
  snapshotUrl?: string
  resolution?: string
  onlineStatus?: number
  direction?: string
}

export interface UpdateCameraDevicePayload extends Partial<CameraDevicePayload> {
  id: number
}

export interface CameraDeviceListParams {
  plotId?: number
  plotName?: string
  name?: string
  streamProtocol?: string
  onlineStatus?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询监控设备列表。 */
export const listCameraDevices = (params: CameraDeviceListParams) => {
  return get<ApiResult<PageInfo<CameraDevice>>>(
    '/camera-device/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 查询监控设备详情。 */
export const getCameraDeviceById = (id: number) => {
  return get<ApiResult<CameraDevice>>(`/camera-device/${id}`)
}

/** 新增监控设备。 */
export const addCameraDevice = (data: CameraDevicePayload) => {
  return post<ApiResult<CameraDevice>>(
    '/camera-device/add',
    data as unknown as Record<string, unknown>,
  )
}

/** 编辑监控设备。 */
export const updateCameraDevice = (data: UpdateCameraDevicePayload) => {
  return http<ApiResult<CameraDevice>>({ url: '/camera-device', method: 'put', data })
}

/** 删除单个监控设备。 */
export const deleteCameraDevice = (id: number) => {
  return http<ApiResult<void>>({ url: `/camera-device/${id}`, method: 'delete' })
}

/** 批量删除监控设备。 */
export const batchDeleteCameraDevices = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/camera-device/batch', method: 'delete', data: ids })
}

/** 修改在线状态，1 表示在线，0 表示离线。 */
export const updateCameraDeviceOnlineStatus = (id: number, onlineStatus: number) => {
  return http<ApiResult<void>>({
    url: `/camera-device/${id}/online-status`,
    method: 'put',
    params: { onlineStatus },
  })
}
