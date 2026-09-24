import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 作物信息接口封装，对应 smart_plant 后端 /crop 系列接口。
 */
export interface Crop {
  id?: number
  userId?: number
  username?: string
  nickname?: string
  typeId?: number | null
  typeName?: string
  cropName: string
  cropCode?: string
  variety?: string
  growthDays?: number
  suitableTemperature?: string
  suitableHumidity?: string
  suitablePh?: string
  imageUrl?: string
  description?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface CropPayload {
  typeId?: number | null
  cropName: string
  variety?: string
  growthDays?: number
  suitableTemperature?: string
  suitableHumidity?: string
  suitablePh?: string
  imageUrl?: string
  description?: string
  status?: number
  remark?: string
}

export interface UpdateCropPayload extends Partial<CropPayload> {
  id: number
}

export interface CropListParams {
  cropName?: string
  typeId?: number
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询作物信息列表。 */
export const listCrops = (params: CropListParams) => {
  return get<ApiResult<PageInfo<Crop>>>('/crop/list', params as unknown as Record<string, unknown>)
}

/** 根据 ID 查询作物详情。 */
export const getCropById = (id: number) => {
  return get<ApiResult<Crop>>(`/crop/${id}`)
}

/** 新增作物信息。 */
export const addCrop = (data: CropPayload) => {
  return post<ApiResult<Crop>>('/crop/add', data as unknown as Record<string, unknown>)
}

/** 编辑作物信息。 */
export const updateCrop = (data: UpdateCropPayload) => {
  return http<ApiResult<Crop>>({ url: '/crop', method: 'put', data })
}

/** 删除单个作物。 */
export const deleteCrop = (id: number) => {
  return http<ApiResult<void>>({ url: `/crop/${id}`, method: 'delete' })
}

/** 批量删除作物。 */
export const batchDeleteCrops = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/crop/batch', method: 'delete', data: ids })
}

/** 启用或禁用作物。 */
export const updateCropStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/crop/${id}/status`,
    method: 'put',
    params: { status },
  })
}
