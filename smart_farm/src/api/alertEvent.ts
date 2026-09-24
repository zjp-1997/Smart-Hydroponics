import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface AlertEvent {
  id: number
  userId: number
  username?: string
  nickname?: string
  plotId?: number
  plotName?: string
  plotCode?: string
  deviceId?: number
  deviceName?: string
  deviceCode?: string
  batchId?: number
  batchNo?: string
  alertType: number
  alertTitle: string
  alertContent?: string
  metricCode?: string
  metricName?: string
  metricValue?: number
  metricUnit?: string
  thresholdMin?: number
  thresholdMax?: number
  alertLevel?: number
  sourceType?: string
  sourceId?: number
  processStatus?: number
  handlerId?: number
  handleTime?: string
  handleResult?: string
  triggerTime?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface AlertEventListParams {
  plotName?: string
  alertType?: number
  alertLevel?: number
  processStatus?: number
  sourceType?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listAlertEvents = (params: AlertEventListParams) => {
  return get<ApiResult<PageInfo<AlertEvent>>>('/alert-event/list', params as unknown as Record<string, unknown>)
}

export const getAlertEventById = (id: number) => {
  return get<ApiResult<AlertEvent>>(`/alert-event/${id}`)
}

export const updateAlertProcessStatus = (id: number, processStatus: number, handleResult?: string) => {
  return http<ApiResult<AlertEvent>>({
    url: `/alert-event/${id}/process-status`,
    method: 'put',
    params: { processStatus, handleResult },
  })
}

export const deleteAlertEvent = (id: number) => {
  return http<ApiResult<void>>({ url: `/alert-event/${id}`, method: 'delete' })
}

export const batchDeleteAlertEvents = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/alert-event/batch', method: 'delete', data: ids })
}

export const getAlertEventStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/alert-event/statistics')
}
