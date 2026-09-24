import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 防治措施实体，对应 smart_plant 后端 disease_control 表返回字段。
 * diseaseName 是后端关联 disease_pest 表后带出的展示字段。
 */
export interface DiseaseControl {
  id?: number
  diseaseId: number
  diseaseName?: string
  controlType?: number
  controlCategory?: number
  method?: string
  drugName?: string
  usageMethod?: string
  dosageSpec?: string
  safetyIntervalDays?: number
  precautions?: string
  suitableStage?: string
  status?: number
  sortOrder?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

/**
 * 新增防治措施时提交给后端的字段。
 * createTime、updateTime、diseaseName 等展示字段不参与新增。
 */
export interface DiseaseControlPayload {
  diseaseId: number
  controlType?: number
  controlCategory?: number
  method?: string
  drugName?: string
  usageMethod?: string
  dosageSpec?: string
  safetyIntervalDays?: number
  precautions?: string
  suitableStage?: string
  status?: number
  sortOrder?: number
  remark?: string
}

export interface UpdateDiseaseControlPayload extends Partial<DiseaseControlPayload> {
  id: number
}

export interface DiseaseControlListParams {
  diseaseId?: number
  diseaseName?: string
  controlType?: number
  controlCategory?: number
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询防治措施列表，支持按病虫害名称、防治类型、防治手段和状态筛选。 */
export const listDiseaseControls = (params: DiseaseControlListParams) => {
  return get<ApiResult<PageInfo<DiseaseControl>>>(
    '/disease-control/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据 ID 查询防治措施详情，供编辑弹框回填。 */
export const getDiseaseControlById = (id: number) => {
  return get<ApiResult<DiseaseControl>>(`/disease-control/${id}`)
}

/** 新增防治措施。 */
export const addDiseaseControl = (data: DiseaseControlPayload) => {
  return post<ApiResult<DiseaseControl>>(
    '/disease-control/add',
    data as unknown as Record<string, unknown>,
  )
}

/** 编辑防治措施。 */
export const updateDiseaseControl = (data: UpdateDiseaseControlPayload) => {
  return http<ApiResult<DiseaseControl>>({ url: '/disease-control', method: 'put', data })
}

/** 删除单条防治措施。 */
export const deleteDiseaseControl = (id: number) => {
  return http<ApiResult<void>>({ url: `/disease-control/${id}`, method: 'delete' })
}

/** 批量删除防治措施。 */
export const batchDeleteDiseaseControls = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/disease-control/batch', method: 'delete', data: ids })
}

/** 启用或停用防治措施。 */
export const updateDiseaseControlStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/disease-control/${id}/status`,
    method: 'put',
    params: { status },
  })
}
