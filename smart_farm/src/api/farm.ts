import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface Farm {
  id?: number
  userId: number
  username?: string
  nickname?: string
  farmName: string
  farmCode?: string
  /** 农场图片地址，对应后端 farm.img_url 字段。 */
  imgUrl?: string
  contactPhone?: string
  address?: string
  coordinate?: string
  totalArea?: number
  areaUnit?: string
  longitude?: number
  latitude?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
  plotCount?: number
}

export interface FarmPayload {
  userId: number
  farmName: string
  /** 农场图片地址；可填完整 URL，也可填后端 /uploads 相对地址。 */
  imgUrl?: string
  contactPhone?: string
  address?: string
  coordinate?: string
  totalArea?: number
  areaUnit?: string
  status?: number
  remark?: string
}

export interface UpdateFarmPayload extends Partial<FarmPayload> {
  id: number
}

export interface FarmListParams {
  farmName?: string
  farmCode?: string
  userId?: number
  status?: number
  pageNum: number
  pageSize: number
}

export interface FarmStatistics {
  farmTotalCount?: number
  plotTotalCount?: number
  farmTotalArea?: number
  enabledFarmCount?: number
  farmTotalTrend?: number
  plotTotalTrend?: number
  farmTotalAreaTrend?: number
  enabledFarmTrend?: number
}

export interface FarmImageUploadResult {
  url: string
  size: number
}

export interface FarmAddressResolveResult {
  coordinate: string
  address: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listFarms = (params: FarmListParams) => {
  return get<ApiResult<PageInfo<Farm>>>('/farm/list', params as unknown as Record<string, unknown>)
}

export const getFarmStatistics = () => {
  return get<ApiResult<FarmStatistics>>('/farm/statistics')
}

export const getFarmById = (id: number) => {
  return get<ApiResult<Farm>>(`/farm/${id}`)
}

export const addFarm = (data: FarmPayload) => {
  return post<ApiResult<Farm>>('/farm/add', data as unknown as Record<string, unknown>)
}

export const updateFarm = (data: UpdateFarmPayload) => {
  return http<ApiResult<Farm>>({ url: '/farm', method: 'put', data })
}

export const deleteFarm = (id: number) => {
  return http<ApiResult<void>>({ url: `/farm/${id}`, method: 'delete' })
}

export const batchDeleteFarms = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/farm/batch', method: 'delete', data: ids })
}

export const updateFarmStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/farm/${id}/status`,
    method: 'put',
    params: { status },
  })
}

export const uploadFarmImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return http<ApiResult<FarmImageUploadResult>>({
    url: '/farm/upload-image',
    method: 'post',
    data: formData,
  })
}

export const resolveFarmAddress = (coordinate: string, coordinateSystem = 'wgs84') => {
  return get<ApiResult<FarmAddressResolveResult>>('/farm/location/address', {
    coordinate,
    coordinateSystem,
  })
}
