import type { AxiosResponse } from 'axios'
import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface CropImage {
  id?: number
  userId: number
  username?: string
  nickname?: string
  imageUrl: string
  imageSize?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface CropImagePayload {
  userId?: number
  imageUrl: string
  imageSize?: number
  remark?: string
}

export interface CropImageUploadResult {
  imageUrl: string
  imageSize: number
}

export interface UpdateCropImagePayload extends Partial<CropImagePayload> {
  id: number
}

export interface CropImageListParams {
  userId?: number
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listCropImages = (params: CropImageListParams) => {
  return http<ApiResult<PageInfo<CropImage>>>({
    url: '/crop-image/list',
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

export const getCropImageById = (id: number) => {
  return get<ApiResult<CropImage>>(`/crop-image/${id}`)
}

export const addCropImage = (data: CropImagePayload) => {
  return post<ApiResult<CropImage>>('/crop-image/add', data as unknown as Record<string, unknown>)
}

export const uploadCropImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)

  return http<ApiResult<CropImageUploadResult>>({
    url: '/crop-image/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const updateCropImage = (data: UpdateCropImagePayload) => {
  return http<ApiResult<CropImage>>({ url: '/crop-image', method: 'put', data })
}

export const deleteCropImage = (id: number) => {
  return http<ApiResult<void>>({ url: `/crop-image/${id}`, method: 'delete' })
}

export const downloadCropImage = (id: number) => {
  return http<AxiosResponse<Blob>>({
    url: `/crop-image/${id}/download`,
    method: 'get',
    responseType: 'blob',
  })
}

export const batchDeleteCropImages = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/crop-image/batch', method: 'delete', data: ids })
}
