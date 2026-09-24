import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface IotDeviceFault {
  id?: number
  deviceId: number
  deviceCode?: string
  deviceName?: string
  plotName?: string
  faultCode: string
  faultName: string
  faultType: number
  severity?: number
  faultDesc?: string
  startTime?: string
  endTime?: string
  duration?: number
  status?: number
  assignStatus?: number
  handleUserId?: number
  handleUsername?: string
  handleNickname?: string
  handleTime?: string
  handleResult?: string
  createTime?: string
  updateTime?: string
}

export interface IotDeviceFaultPayload {
  deviceId: number
  faultCode: string
  faultName: string
  faultType: number
  severity?: number
  faultDesc?: string
  startTime?: string
  endTime?: string
  duration?: number
  status?: number
  assignStatus?: number
  handleUserId?: number
  handleTime?: string
  handleResult?: string
}

export interface UpdateIotDeviceFaultPayload extends Partial<IotDeviceFaultPayload> {
  id: number
}

export interface IotDeviceFaultListParams {
  deviceId?: number
  deviceName?: string
  faultCode?: string
  faultName?: string
  faultType?: number
  severity?: number
  status?: number
  handleUserId?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listIotDeviceFaults = (params: IotDeviceFaultListParams) => {
  return get<ApiResult<PageInfo<IotDeviceFault>>>(
    '/iot-device-fault/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getIotDeviceFaultById = (id: number) => {
  return get<ApiResult<IotDeviceFault>>(`/iot-device-fault/${id}`)
}

export const addIotDeviceFault = (data: IotDeviceFaultPayload) => {
  return post<ApiResult<IotDeviceFault>>(
    '/iot-device-fault/add',
    data as unknown as Record<string, unknown>,
  )
}

export const updateIotDeviceFault = (data: UpdateIotDeviceFaultPayload) => {
  return http<ApiResult<IotDeviceFault>>({ url: '/iot-device-fault', method: 'put', data })
}

export const deleteIotDeviceFault = (id: number) => {
  return http<ApiResult<void>>({ url: `/iot-device-fault/${id}`, method: 'delete' })
}

export const batchDeleteIotDeviceFaults = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/iot-device-fault/batch', method: 'delete', data: ids })
}

export const updateIotDeviceFaultStatus = (id: number, status: number, handleResult?: string) => {
  return http<ApiResult<void>>({
    url: `/iot-device-fault/${id}/status`,
    method: 'put',
    params: handleResult ? { status, handleResult } : { status },
  })
}

export const assignIotDeviceFault = (id: number, handleUserId: number) => {
  return http<ApiResult<void>>({
    url: `/iot-device-fault/${id}/assign`,
    method: 'put',
    params: { handleUserId },
  })
}

export const acceptIotDeviceFaultAssignment = (id: number) => {
  return http<ApiResult<void>>({
    url: `/iot-device-fault/${id}/assignment/accept`,
    method: 'put',
  })
}

export const rejectIotDeviceFaultAssignment = (id: number, rejectReason?: string) => {
  return http<ApiResult<void>>({
    url: `/iot-device-fault/${id}/assignment/reject`,
    method: 'put',
    params: rejectReason ? { rejectReason } : undefined,
  })
}
