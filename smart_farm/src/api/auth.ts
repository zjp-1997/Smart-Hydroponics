import type { AxiosResponse } from 'axios'
import { get, http, post } from '@/utils/request'

export interface AdminLoginPayload {
  username: string
  password: string
  captcha?: string
  captchaKey?: string
  rememberMe: boolean
}

export interface AdminUser {
  id: number
  username: string
  phone?: string
  nickname?: string
  avatar?: string
  roleId?: number
  roleName?: string
  roleCode?: string
  role?: string
  userType?: number
  status?: number
  email?: string
  gender?: number
  region?: string
  remark?: string
}

/** 个人中心允许修改的字段白名单，与后端 AdminProfileUpdateRequest 对应。 */
export interface AdminProfileUpdatePayload {
  phone: string
  email?: string
  avatar?: string
  gender?: number
  roleId?: number
  region?: string
  remark?: string
}

/** 修改密码参数，服务端仍会重复校验两次新密码。 */
export interface AdminPasswordChangePayload {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

export interface AdminAvatarUploadResponse {
  url: string
  size: number
}

export interface AdminLoginResponse {
  token: string
  refreshToken?: string
  tokenType: string
  expireTime: string
  refreshExpireTime?: string
  admin?: AdminUser
  roleCode?: string
  dataScope?: 'all' | 'self' | string
  permissions?: string[]
}

export interface AdminCaptchaResponse {
  captchaKey: string
  captchaImage: string
  expireSeconds: number
}

export interface AdminUserInfoResponse {
  admin?: AdminUser
  roleCode?: string
  dataScope?: 'all' | 'self' | string
  permissions?: string[]
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const loginAdmin = (data: AdminLoginPayload) => {
  return post<ApiResult<AdminLoginResponse>>(
    '/admin/auth/login',
    data as unknown as Record<string, unknown>,
  )
}

export const refreshAdminToken = (refreshToken: string) => {
  return post<ApiResult<AdminLoginResponse>>('/admin/auth/refresh', { refreshToken })
}

export const getAdminCaptcha = () => {
  return get<ApiResult<AdminCaptchaResponse>>('/admin/auth/captcha')
}

export const getAdminInfo = () => {
  return get<ApiResult<AdminUserInfoResponse>>('/admin/auth/info')
}

export const logoutAdmin = () => {
  return post<ApiResult<void>>('/admin/auth/logout')
}

/** 获取当前登录用户的最新个人资料。 */
export const getAdminProfile = () => {
  return get<ApiResult<AdminUser>>('/admin/auth/profile')
}

/** 更新当前登录用户的个人资料；后端会单独校验角色变更权限。 */
export const updateAdminProfile = (data: AdminProfileUpdatePayload) => {
  return http<ApiResult<AdminUser>>({ url: '/admin/auth/profile', method: 'put', data })
}

/** 上传个人头像，返回可写入 user.avatar 的后端相对地址。 */
export const uploadAdminAvatar = (file: File) => {
  const data = new FormData()
  data.append('file', file)
  return http<ApiResult<AdminAvatarUploadResponse>>({
    url: '/admin/auth/profile/avatar',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 使用管理端 JWT 下载受保护头像，供浏览器创建本地预览地址。 */
export const downloadAdminAvatar = (url: string) => {
  return http<AxiosResponse<Blob>>({ url, method: 'get', responseType: 'blob' })
}

/** 修改当前登录用户密码，成功后前端应清理旧登录态。 */
export const changeAdminPassword = (data: AdminPasswordChangePayload) => {
  return http<ApiResult<void>>({ url: '/admin/auth/password', method: 'put', data })
}
