import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * AI对话记录实体，对应 smart_plant 后端 ai_chat 表。
 * 管理端只做查询和删除，不提供人工改写历史对话内容的入口。
 */
export interface AiChat {
  id?: number
  userId?: number
  username?: string
  nickname?: string
  modelId?: number
  modelName?: string
  model?: string
  imageUrl?: string
  userContent?: string
  aiContent?: string
  status?: number
  failReason?: string
  createTime?: string
  updateTime?: string
}

export interface AiChatListParams {
  userId?: number
  username?: string
  modelName?: string
  status?: number
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

export type AiChatStatisticsParams = Omit<AiChatListParams, 'pageNum' | 'pageSize'>

export interface AiChatStatistics {
  totalCount?: number
  successCount?: number
  failedCount?: number
  imageCount?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询AI对话记录，对接 GET /ai-chat/list。 */
export const listAiChats = (params: AiChatListParams) => {
  return get<ApiResult<PageInfo<AiChat>>>('/ai-chat/list', params as unknown as Record<string, unknown>)
}

/** 查询AI对话统计数据，对接 GET /ai-chat/statistics。 */
export const getAiChatStatistics = (params: AiChatStatisticsParams) => {
  return get<ApiResult<AiChatStatistics>>(
    '/ai-chat/statistics',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询对话详情，供详情弹窗展示完整消息。 */
export const getAiChatById = (id: number) => {
  return get<ApiResult<AiChat>>(`/ai-chat/${id}`)
}

/** 删除单条对话记录。 */
export const deleteAiChat = (id: number) => {
  return http<ApiResult<void>>({ url: `/ai-chat/${id}`, method: 'delete' })
}

/** 批量删除对话记录。 */
export const batchDeleteAiChats = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/ai-chat/batch', method: 'delete', data: ids })
}
