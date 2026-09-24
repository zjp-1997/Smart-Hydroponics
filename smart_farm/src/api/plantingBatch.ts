import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface PlantingBatch {
  id?: number
  plotId: number
  plotName?: string
  cropId: number
  cropName?: string
  userId: number
  username?: string
  nickname?: string
  batchNo?: string
  plantingArea?: number
  areaUnit?: string
  plantedAt?: string
  expectedHarvestAt?: string
  actualHarvestAt?: string
  harvester?: string
  growthStage?: number
  growthStageId?: number
  growthStageName?: string
  expectedYieldAmount?: number
  grownDays?: number
  cropImage?: string
  status?: number
  yieldAmount?: number
  yieldUnit?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface PlantingBatchPayload {
  plotId: number
  cropId: number
  userId: number
  batchNo?: string
  plantingArea?: number
  areaUnit?: string
  plantedAt?: string
  expectedHarvestAt?: string
  actualHarvestAt?: string
  growthStageId?: number
  expectedYieldAmount?: number
  grownDays?: number
  cropImage?: string
  status?: number
  yieldAmount?: number
  yieldUnit?: string
  remark?: string
}

export interface UpdatePlantingBatchPayload extends Partial<PlantingBatchPayload> {
  id: number
}

export interface PlantingBatchListParams {
  plotId?: number
  cropId?: number
  userId?: number
  batchNo?: string
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listPlantingBatches = (params: PlantingBatchListParams) => {
  return get<ApiResult<PageInfo<PlantingBatch>>>(
    '/planting-batch/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getPlantingBatchById = (id: number) => {
  return get<ApiResult<PlantingBatch>>(`/planting-batch/${id}`)
}

export const addPlantingBatch = (data: PlantingBatchPayload) => {
  return post<ApiResult<PlantingBatch>>(
    '/planting-batch/add',
    data as unknown as Record<string, unknown>,
  )
}

export const updatePlantingBatch = (data: UpdatePlantingBatchPayload) => {
  return http<ApiResult<PlantingBatch>>({ url: '/planting-batch', method: 'put', data })
}

export const deletePlantingBatch = (id: number) => {
  return http<ApiResult<void>>({ url: `/planting-batch/${id}`, method: 'delete' })
}

export const batchDeletePlantingBatches = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/planting-batch/batch', method: 'delete', data: ids })
}

export const updatePlantingBatchStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/planting-batch/${id}/status`,
    method: 'put',
    params: { status },
  })
}
