import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 病虫害基本信息实体，对应 smart_plant 后端 disease_pest 表返回字段。
 * 前端列表、详情弹框和防治措施关联选择都复用该类型，避免字段散落在页面中。
 */
export interface DiseasePest {
  id?: number
  cropTypeId?: number | null
  cropTypeName?: string
  affectedCrops?: string
  name: string
  type?: number
  symptom?: string
  cause?: string
  suitableStage?: string
  occurrencePeriod?: string
  livingHabits?: string
  suitableEnvironment?: string
  transmissionRoute?: string
  coverImage?: string
  imageUrls?: string[]
  status?: number
  sortOrder?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 新增病虫害基本信息时提交给后端的字段。
 * id、createTime、updateTime 等字段由数据库维护，新增表单不提交。
 */
export interface DiseasePestPayload {
  cropTypeId?: number | null
  affectedCrops?: string
  name: string
  type?: number
  symptom?: string
  cause?: string
  suitableStage?: string
  occurrencePeriod?: string
  livingHabits?: string
  suitableEnvironment?: string
  transmissionRoute?: string
  coverImage?: string
  imageUrls?: string[]
  status?: number
  sortOrder?: number
  remark?: string
}

export interface UpdateDiseasePestPayload extends Partial<DiseasePestPayload> {
  id: number
}

export interface DiseasePestListParams {
  name?: string
  cropTypeId?: number
  type?: number
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询病虫害基本信息列表，供列表页表格和下拉选择使用。 */
export const listDiseasePests = (params: DiseasePestListParams) => {
  return get<ApiResult<PageInfo<DiseasePest>>>(
    '/disease-pest/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询病虫害基本信息详情，供编辑弹框回填。 */
export const getDiseasePestById = (id: number) => {
  return get<ApiResult<DiseasePest>>(`/disease-pest/${id}`)
}

/** 新增病虫害基本信息。 */
export const addDiseasePest = (data: DiseasePestPayload) => {
  return post<ApiResult<DiseasePest>>(
    '/disease-pest/add',
    data as unknown as Record<string, unknown>,
  )
}

/** 编辑病虫害基本信息。 */
export const updateDiseasePest = (data: UpdateDiseasePestPayload) => {
  return http<ApiResult<DiseasePest>>({ url: '/disease-pest', method: 'put', data })
}

/** 删除单条病虫害基本信息，后端会级联删除该病虫害下的防治措施。 */
export const deleteDiseasePest = (id: number) => {
  return http<ApiResult<void>>({ url: `/disease-pest/${id}`, method: 'delete' })
}

/** 批量删除病虫害基本信息。 */
export const batchDeleteDiseasePests = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/disease-pest/batch', method: 'delete', data: ids })
}

/** 启用或停用病虫害基本信息。 */
export const updateDiseasePestStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/disease-pest/${id}/status`,
    method: 'put',
    params: { status },
  })
}
