import { get } from '@/utils/request'

export interface AdminLoginLog {
  id: number
  adminId?: number
  username: string
  success: number
  failureReason?: string
  ip?: string
  userAgent?: string
  loginTime?: string
}

export interface LoginLogListParams {
  username?: string
  ip?: string
  success?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export interface PageInfo<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
  size?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listAdminLoginLogs = (params: LoginLogListParams) => {
  return get<ApiResult<PageInfo<AdminLoginLog>>>(
    '/admin/login-log/list',
    params as unknown as Record<string, unknown>,
  )
}
