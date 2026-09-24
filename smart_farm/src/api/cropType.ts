import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 作物类型接口封装，对应 smart_plant 后端 /crop-type 系列接口。
 * 页面层只调用这里的函数，避免在组件中散落后端 URL。
 */
export interface CropType {
  id?: number
  parentId?: number | null
  parentName?: string
  typeName: string
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

export interface CropTypePayload {
  parentId?: number | null
  typeName: string
  description?: string
  status?: number
}

export interface UpdateCropTypePayload extends Partial<CropTypePayload> {
  id: number
}

export interface CropTypeListParams {
  typeName?: string
  parentId?: number
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询作物类型列表。 */
export const listCropTypes = (params: CropTypeListParams) => {
  return get<ApiResult<PageInfo<CropType>>>(
    '/crop-type/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询作物类型详情。 */
export const getCropTypeById = (id: number) => {
  return get<ApiResult<CropType>>(`/crop-type/${id}`)
}

/** 新增作物类型。 */
export const addCropType = (data: CropTypePayload) => {
  return post<ApiResult<CropType>>('/crop-type/add', data as unknown as Record<string, unknown>)
}

/** 编辑作物类型。 */
export const updateCropType = (data: UpdateCropTypePayload) => {
  return http<ApiResult<CropType>>({ url: '/crop-type', method: 'put', data })
}

/** 删除单个作物类型。 */
export const deleteCropType = (id: number) => {
  return http<ApiResult<void>>({ url: `/crop-type/${id}`, method: 'delete' })
}

/** 批量删除作物类型。 */
export const batchDeleteCropTypes = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/crop-type/batch', method: 'delete', data: ids })
}

/** 启用或禁用作物类型。 */
export const updateCropTypeStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/crop-type/${id}/status`,
    method: 'put',
    params: { status },
  })
}
