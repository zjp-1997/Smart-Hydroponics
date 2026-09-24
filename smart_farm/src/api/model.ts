import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * AI模型实体，对应 smart_plant 后端 model 表。
 * 模型密钥由后端脱敏返回，前端只负责展示和提交，不在页面中长期保存明文。
 */
export interface AiModel {
  id?: number
  modelName: string
  baseUrl: string
  apiKey?: string
  model: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
  chatCount?: number
}

/** 新增模型时提交给后端的字段。 */
export interface AiModelPayload {
  modelName: string
  baseUrl: string
  apiKey: string
  model: string
  status?: number
  remark?: string
}

/** 编辑模型时允许只提交变化字段，apiKey 为空时后端保留原密钥。 */
export interface UpdateAiModelPayload extends Partial<AiModelPayload> {
  id: number
}

export interface AiModelListParams {
  modelName?: string
  model?: string
  status?: number
  pageNum: number
  pageSize: number
}

export type AiModelStatisticsParams = Omit<AiModelListParams, 'pageNum' | 'pageSize'>

export interface AiModelStatistics {
  totalCount?: number
  enabledCount?: number
  disabledCount?: number
  chatCount?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询模型列表，对接 GET /model/list。 */
export const listAiModels = (params: AiModelListParams) => {
  return get<ApiResult<PageInfo<AiModel>>>('/model/list', params as unknown as Record<string, unknown>)
}

/** 查询模型统计数据，对接 GET /model/statistics。 */
export const getAiModelStatistics = (params: AiModelStatisticsParams) => {
  return get<ApiResult<AiModelStatistics>>(
    '/model/statistics',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询模型详情，供编辑弹窗回填表单。 */
export const getAiModelById = (id: number) => {
  return get<ApiResult<AiModel>>(`/model/${id}`)
}

/** 新增模型配置，对接 POST /model/add。 */
export const addAiModel = (data: AiModelPayload) => {
  return post<ApiResult<AiModel>>('/model/add', data as unknown as Record<string, unknown>)
}

/** 编辑模型配置，对接 PUT /model。 */
export const updateAiModel = (data: UpdateAiModelPayload) => {
  return http<ApiResult<AiModel>>({ url: '/model', method: 'put', data })
}

/** 删除单个模型。存在历史对话时数据库会保留对话并将 model_id 置空。 */
export const deleteAiModel = (id: number) => {
  return http<ApiResult<void>>({ url: `/model/${id}`, method: 'delete' })
}

/** 批量删除模型。 */
export const batchDeleteAiModels = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/model/batch', method: 'delete', data: ids })
}
