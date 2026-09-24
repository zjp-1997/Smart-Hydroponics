import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'
import type { Permission } from '@/api/role'

export interface PermissionPayload {
  parentId?: number
  permissionName: string
  permissionCode: string
  type: number
  path?: string
  component?: string
  status?: number
  sort?: number
}

export interface UpdatePermissionPayload extends Partial<PermissionPayload> {
  id: number
}

export interface PermissionListParams {
  permissionName?: string
  permissionCode?: string
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

export const listPermissions = (params: PermissionListParams) => {
  return get<ApiResult<PageInfo<Permission>>>(
    '/permission/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getPermissionById = (id: number) => {
  return get<ApiResult<Permission>>(`/permission/${id}`)
}

export const addPermission = (data: PermissionPayload) => {
  return post<ApiResult<Permission>>('/permission/add', data as unknown as Record<string, unknown>)
}

export const updatePermission = (data: UpdatePermissionPayload) => {
  return http<ApiResult<Permission>>({ url: '/permission', method: 'put', data })
}

export const deletePermission = (id: number) => {
  return http<ApiResult<void>>({ url: `/permission/${id}`, method: 'delete' })
}
