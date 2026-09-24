import { get, http, post } from '@/utils/request'
import type { Role } from '@/api/role'

export interface CreateSmartPlantUserPayload {
  username: string
  password: string
  phone: string
  email?: string
  avatar?: string
  nickname?: string
  gender?: number
  roleId?: number
  roleName?: string
  roleCode?: string
  /** 普通用户或技术人员所属的农场主 ID，关系由后端写入绑定表。 */
  farmOwnerId?: number
  status?: number
  region?: string
  remark?: string
}

export interface UpdateSmartPlantUserPayload extends Partial<CreateSmartPlantUserPayload> {
  id: number
}

export interface SmartPlantUser extends Omit<CreateSmartPlantUserPayload, 'password'> {
  id?: number
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface PageInfo<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
  size?: number
}

export interface UserListParams {
  username?: string
  phone?: string
  roleCode?: string
  status?: number
  pageNum: number
  pageSize: number
}

/** 分页查询用户列表，对接 smart_plant UserController -> GET /user/list。 */
export const listSmartPlantUsers = (params: UserListParams) => {
  return get<ApiResult<PageInfo<SmartPlantUser>>>(
    '/user/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 根据用户 ID 查询详情。 */
export const getSmartPlantUserById = (id: number) => {
  return get<ApiResult<SmartPlantUser>>(`/user/${id}`)
}

/** 用户表单只获取当前登录角色可分配的角色及农场主。 */
export const listAssignableUserRoles = () => get<ApiResult<Role[]>>('/user/assignable-roles')

/** 绑定农场主下拉列表由服务端按当前管理者范围过滤。 */
export const listAvailableFarmOwners = () => get<ApiResult<SmartPlantUser[]>>('/user/farm-owners')

/** 新增用户，对接 POST /user/add。 */
export const addSmartPlantUser = (data: CreateSmartPlantUserPayload) => {
  return post<ApiResult<SmartPlantUser>>('/user/add', data as unknown as Record<string, unknown>)
}

/** 编辑用户，对接 PUT /user。 */
export const updateSmartPlantUser = (data: UpdateSmartPlantUserPayload) => {
  return http<ApiResult<SmartPlantUser>>({ url: '/user', method: 'put', data })
}

/** 删除用户。 */
export const deleteSmartPlantUser = (id: number) => {
  return http<ApiResult<void>>({ url: `/user/${id}`, method: 'delete' })
}

/** 修改用户启用/禁用状态，status 为 1 表示启用，0 表示禁用。 */
export const updateSmartPlantUserStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/user/${id}/status`,
    method: 'put',
    params: { status },
  })
}

/** 重置密码，对接 PUT /user/{id}/reset-password；前端传明文，后端统一 BCrypt 加密保存。 */
export const resetSmartPlantUserPassword = (id: number, password: string) => {
  return http<ApiResult<void>>({
    url: `/user/${id}/reset-password`,
    method: 'put',
    params: { password: password.trim() },
  })
}

/** 批量删除用户。 */
export const batchDeleteSmartPlantUsers = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/user/batch', method: 'delete', data: ids })
}


export interface FarmJoinRequest {
  id: number
  userId: number
  ownerUserId: number
  username: string
  nickname?: string
  ownerName?: string
  requestedRole: 'user' | 'technician'
  status: number
  reviewerId?: number
  reviewedAt?: string
  reviewReason?: string
  revokedBy?: number
  revokedAt?: string
  revokeReason?: string
  createdAt: string
}

export const listFarmJoinRequests = (params: { status?: number; pageNum: number; pageSize: number }) =>
  get<ApiResult<PageInfo<FarmJoinRequest>>>('/user/farm-join-requests', params)

/** 获取当前登录人可审核的待处理数量，农场范围由后端鉴权确定。 */
export const getPendingFarmJoinRequestCount = () =>
  get<ApiResult<number>>('/user/farm-join-requests/count')

export const decideFarmJoinRequest = (userId: number, status: number, reason: string) =>
  http<ApiResult<void>>({ url: `/user/farm-join-requests/${userId}/decision`, method: 'put', data: { status, reason } })
