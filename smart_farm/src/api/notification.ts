import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface Notification {
  id: number
  userId: number
  username?: string
  nickname?: string
  title: string
  content?: string
  noticeType: number
  refType?: string
  refId?: number
  alertId?: number
  alertTitle?: string
  alertLevel?: number
  level?: number
  isRead?: number
  readTime?: string
  sendTime?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface NotificationListParams {
  title?: string
  noticeType?: number
  level?: number
  isRead?: number
  refType?: string
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

export const listNotifications = (params: NotificationListParams) => {
  return get<ApiResult<PageInfo<Notification>>>('/notification/list', params as unknown as Record<string, unknown>)
}

export const getNotificationById = (id: number) => {
  return get<ApiResult<Notification>>(`/notification/${id}`)
}

export const updateNotificationReadStatus = (id: number, isRead: number) => {
  return http<ApiResult<Notification>>({
    url: `/notification/${id}/read-status`,
    method: 'put',
    params: { isRead },
  })
}

export const markAllNotificationsRead = () => {
  return http<ApiResult<number>>({ url: '/notification/mark-all-read', method: 'put' })
}

export const deleteNotification = (id: number) => {
  return http<ApiResult<void>>({ url: `/notification/${id}`, method: 'delete' })
}

export const batchDeleteNotifications = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/notification/batch', method: 'delete', data: ids })
}

export const getNotificationStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/notification/statistics')
}
