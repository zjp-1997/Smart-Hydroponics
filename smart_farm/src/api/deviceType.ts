import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 设备类型实体，对应 smart_plant 后端 device_type 表字段。
 * 页面统一使用该类型承接列表、详情和状态切换后的返回数据。
 */
export interface DeviceType {
  id?: number
  typeCode: string
  typeName: string
  category: number
  description?: string
  status?: number
  createTime?: string
  updateTime?: string
}

/**
 * 新增设备类型时提交给后端的字段。
 * id、createTime、updateTime 由数据库和后端维护，不在新增表单中提交。
 */
export interface DeviceTypePayload {
  typeCode: string
  typeName: string
  category: number
  description?: string
  status?: number
}

export interface UpdateDeviceTypePayload extends Partial<DeviceTypePayload> {
  id: number
}

export interface DeviceTypeListParams {
  typeCode?: string
  typeName?: string
  category?: number
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询设备类型列表，对接 smart_plant DeviceTypeController -> GET /device-type/list。 */
export const listDeviceTypes = (params: DeviceTypeListParams) => {
  return get<ApiResult<PageInfo<DeviceType>>>(
    '/device-type/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据设备类型 ID 查询详情，供编辑弹框回填表单使用。 */
export const getDeviceTypeById = (id: number) => {
  return get<ApiResult<DeviceType>>(`/device-type/${id}`)
}

/** 新增设备类型，对接 POST /device-type/add。 */
export const addDeviceType = (data: DeviceTypePayload) => {
  return post<ApiResult<DeviceType>>('/device-type/add', data as unknown as Record<string, unknown>)
}

/** 编辑设备类型基础信息，对接 PUT /device-type。 */
export const updateDeviceType = (data: UpdateDeviceTypePayload) => {
  return http<ApiResult<DeviceType>>({ url: '/device-type', method: 'put', data })
}

/** 删除单个设备类型，后端会阻止删除已被设备引用的类型。 */
export const deleteDeviceType = (id: number) => {
  return http<ApiResult<void>>({ url: `/device-type/${id}`, method: 'delete' })
}

/** 批量删除设备类型。 */
export const batchDeleteDeviceTypes = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/device-type/batch', method: 'delete', data: ids })
}

/** 启用或禁用设备类型，status 为 1 表示启用，0 表示禁用。 */
export const updateDeviceTypeStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/device-type/${id}/status`,
    method: 'put',
    params: { status },
  })
}
