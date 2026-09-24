import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 专家咨询消息实体，对应 smart_plant 后端 consult_message 表。
 */
export interface ConsultationMessage {
  id?: number
  sessionId?: number
  sessionNo?: string
  senderId?: number
  receiverId?: number
  senderName?: string
  receiverName?: string
  username?: string
  nickname?: string
  expertName?: string
  messageType?: number
  content?: string
  mediaUrl?: string
  isRead?: number
  readTime?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface ConsultationListParams {
  username?: string
  expertName?: string
  sessionNo?: string
  messageType?: number
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

export type ConsultationStatisticsParams = Omit<ConsultationListParams, 'pageNum' | 'pageSize'>

export interface ConsultationStatistics {
  totalCount?: number
  textCount?: number
  imageCount?: number
  unreadCount?: number
  sessionCount?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询专家咨询聊天内容，对接 GET /consultation/list。 */
export const listConsultationMessages = (params: ConsultationListParams) => {
  return get<ApiResult<PageInfo<ConsultationMessage>>>(
    '/consultation/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 查询专家咨询统计数据，对接 GET /consultation/statistics。 */
export const getConsultationStatistics = (params: ConsultationStatisticsParams) => {
  return get<ApiResult<ConsultationStatistics>>(
    '/consultation/statistics',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据消息ID查询专家咨询详情。 */
export const getConsultationMessageById = (id: number) => {
  return get<ApiResult<ConsultationMessage>>(`/consultation/${id}`)
}

/** 删除单条专家咨询消息，后端执行逻辑删除。 */
export const deleteConsultationMessage = (id: number) => {
  return http<ApiResult<void>>({ url: `/consultation/${id}`, method: 'delete' })
}

/** 批量删除专家咨询消息。 */
export const batchDeleteConsultationMessages = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/consultation/batch', method: 'delete', data: ids })
}
