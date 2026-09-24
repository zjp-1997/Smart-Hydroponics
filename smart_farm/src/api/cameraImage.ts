import type { AxiosResponse } from 'axios'
import { http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface CameraImage {
  id?: number
  cameraId: number
  cameraName?: string
  plotId: number
  plotName?: string
  plotCode?: string
  userId?: number
  username?: string
  nickname?: string
  imageUrl: string
  imageSize?: number
  captureTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface CameraImageListParams {
  cameraId?: number
  plotId?: number
  cameraName?: string
  plotName?: string
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

export const listCameraImages = (params: CameraImageListParams) => {
  return http<ApiResult<PageInfo<CameraImage>>>({
    url: '/camera-image/list',
    method: 'get',
    params: params as unknown as Record<string, unknown>,
    preserveParamDateTime: true,
  })
}

export const deleteCameraImage = (id: number) => {
  return http<ApiResult<void>>({ url: `/camera-image/${id}`, method: 'delete' })
}

export const batchDeleteCameraImages = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/camera-image/batch', method: 'delete', data: ids })
}

export const downloadCameraImage = (id: number) => {
  return http<AxiosResponse<Blob>>({
    url: `/camera-image/${id}/download`,
    method: 'get',
    responseType: 'blob',
  })
}
