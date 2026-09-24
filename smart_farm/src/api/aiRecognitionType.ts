import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * AI识别类型实体，对应 smart_plant 后端 ai_recognition_type 表。
 * 该类型用于维护作物识别、病害识别、虫害识别等模型识别分类。
 */
export interface AiRecognitionType {
  id?: number
  typeCode: string
  typeName: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 新增 AI 识别类型时提交给后端的业务字段。
 * id、createTime、updateTime 由数据库和后端维护，表单不主动提交。
 */
export interface AiRecognitionTypePayload {
  typeCode: string
  typeName: string
  description?: string
  status?: number
}

export interface UpdateAiRecognitionTypePayload extends Partial<AiRecognitionTypePayload> {
  id: number
}

export interface AiRecognitionTypeListParams {
  typeCode?: string
  typeName?: string
  status?: number
  pageNum: number
  pageSize: number
}

export type AiRecognitionTypeStatisticsParams = Omit<
  AiRecognitionTypeListParams,
  'pageNum' | 'pageSize'
>

export interface AiRecognitionTypeStatistics {
  totalCount?: number
  enabledCount?: number
  disabledCount?: number
  resultCount?: number
  firstCreateTime?: string
  latestCreateTime?: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询 AI 识别类型列表，对接 GET /ai-recognition-type/list。 */
export const listAiRecognitionTypes = (params: AiRecognitionTypeListParams) => {
  return get<ApiResult<PageInfo<AiRecognitionType>>>(
    '/ai-recognition-type/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 查询 AI 识别类型统计数据，对接 GET /ai-recognition-type/statistics。 */
export const getAiRecognitionTypeStatistics = (params: AiRecognitionTypeStatisticsParams) => {
  return get<ApiResult<AiRecognitionTypeStatistics>>(
    '/ai-recognition-type/statistics',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询 AI 识别类型详情，供编辑弹框回填。 */
export const getAiRecognitionTypeById = (id: number) => {
  return get<ApiResult<AiRecognitionType>>(`/ai-recognition-type/${id}`)
}

/** 新增 AI 识别类型，对接 POST /ai-recognition-type/add。 */
export const addAiRecognitionType = (data: AiRecognitionTypePayload) => {
  return post<ApiResult<AiRecognitionType>>(
    '/ai-recognition-type/add',
    data as unknown as Record<string, unknown>,
  )
}

/** 编辑 AI 识别类型，对接 PUT /ai-recognition-type。 */
export const updateAiRecognitionType = (data: UpdateAiRecognitionTypePayload) => {
  return http<ApiResult<AiRecognitionType>>({ url: '/ai-recognition-type', method: 'put', data })
}

/** 删除单个 AI 识别类型，后端会阻止删除已被识别结果引用的类型。 */
export const deleteAiRecognitionType = (id: number) => {
  return http<ApiResult<void>>({ url: `/ai-recognition-type/${id}`, method: 'delete' })
}

/** 批量删除 AI 识别类型。 */
export const batchDeleteAiRecognitionTypes = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/ai-recognition-type/batch', method: 'delete', data: ids })
}

/** 启用或禁用 AI 识别类型，status 为 1 表示启用，0 表示禁用。 */
export const updateAiRecognitionTypeStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/ai-recognition-type/${id}/status`,
    method: 'put',
    params: { status },
  })
}
