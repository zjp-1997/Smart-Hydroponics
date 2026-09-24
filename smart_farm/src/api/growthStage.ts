import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 作物生长期接口封装，对应 smart_plant 后端 /growth-stage 系列接口。 */
export interface GrowthStage {
  id?: number
  cropId: number
  cropName?: string
  stageName: string
  stageCode?: string
  stageOrder: number
  startDay?: number
  endDay?: number
  duration?: number
  lightHours?: number
  tempMin?: number
  tempMax?: number
  humidityMin?: number
  humidityMax?: number
  phMin?: number
  phMax?: number
  ecMin?: number
  ecMax?: number
  waterIntervalDays?: number
  fertilizerIntervalDays?: number
  managementAdvice?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface GrowthStagePayload {
  cropId: number
  stageName: string
  stageCode?: string
  stageOrder: number
  startDay?: number
  endDay?: number
  duration?: number
  lightHours?: number
  tempMin?: number
  tempMax?: number
  humidityMin?: number
  humidityMax?: number
  phMin?: number
  phMax?: number
  ecMin?: number
  ecMax?: number
  waterIntervalDays?: number
  fertilizerIntervalDays?: number
  managementAdvice?: string
  status?: number
  remark?: string
}

export interface UpdateGrowthStagePayload extends Partial<GrowthStagePayload> {
  id: number
}

export interface GrowthStageListParams {
  cropId?: number
  stageName?: string
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listGrowthStages = (params: GrowthStageListParams) => {
  return get<ApiResult<PageInfo<GrowthStage>>>(
    '/growth-stage/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getGrowthStageById = (id: number) => {
  return get<ApiResult<GrowthStage>>(`/growth-stage/${id}`)
}

export const addGrowthStage = (data: GrowthStagePayload) => {
  return post<ApiResult<GrowthStage>>(
    '/growth-stage/add',
    data as unknown as Record<string, unknown>,
  )
}

export const updateGrowthStage = (data: UpdateGrowthStagePayload) => {
  return http<ApiResult<GrowthStage>>({ url: '/growth-stage', method: 'put', data })
}

export const deleteGrowthStage = (id: number) => {
  return http<ApiResult<void>>({ url: `/growth-stage/${id}`, method: 'delete' })
}

export const batchDeleteGrowthStages = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/growth-stage/batch', method: 'delete', data: ids })
}

export const updateGrowthStageStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/growth-stage/${id}/status`,
    method: 'put',
    params: { status },
  })
}
