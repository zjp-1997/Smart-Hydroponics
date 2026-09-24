import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface OauthAccountPayload {
  userId: number
  provider: number
  openId: string
  unionId?: string
  nickname?: string
  avatar?: string
  accessToken?: string
  refreshToken?: string
  tokenExpireTime?: string
  bindTime?: string
  lastLoginTime?: string
  status?: number
  remark?: string
}

export interface UpdateOauthAccountPayload extends Partial<OauthAccountPayload> {
  id: number
}

export interface OauthAccount extends Omit<OauthAccountPayload, 'accessToken' | 'refreshToken'> {
  id?: number
  createTime?: string
  updateTime?: string
}

export interface OauthAccountListParams {
  userId?: number
  provider?: number
  openId?: string
  nickname?: string
  status?: number
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listOauthAccounts = (params: OauthAccountListParams) => {
  return get<ApiResult<PageInfo<OauthAccount>>>(
    '/oauth-account/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getOauthAccountById = (id: number) => {
  return get<ApiResult<OauthAccount>>(`/oauth-account/${id}`)
}

export const addOauthAccount = (data: OauthAccountPayload) => {
  return post<ApiResult<OauthAccount>>('/oauth-account/add', data as unknown as Record<string, unknown>)
}

export const updateOauthAccount = (data: UpdateOauthAccountPayload) => {
  return http<ApiResult<OauthAccount>>({ url: '/oauth-account', method: 'put', data })
}

export const deleteOauthAccount = (id: number) => {
  return http<ApiResult<void>>({ url: `/oauth-account/${id}`, method: 'delete' })
}

export const batchDeleteOauthAccounts = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/oauth-account/batch', method: 'delete', data: ids })
}

export const updateOauthAccountStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/oauth-account/${id}/status`,
    method: 'put',
    params: { status },
  })
}
