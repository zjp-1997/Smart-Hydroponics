import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface Role {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  remark?: string
  status?: number
  sort?: number
  createBy?: number
  updateBy?: number
  createTime?: string
  updateTime?: string
  isDeleted?: number
}

export interface RolePayload {
  roleName: string
  roleCode: string
  description?: string
  remark?: string
  status?: number
  sort?: number
}

export interface UpdateRolePayload extends Partial<RolePayload> {
  id: number
}

export interface RoleListParams {
  roleName?: string
  roleCode?: string
  status?: number
  pageNum: number
  pageSize: number
}

export interface Permission {
  id?: number
  parentId?: number
  permissionName: string
  permissionCode: string
  type: number
  path?: string
  component?: string
  status?: number
  sort?: number
  createTime?: string
  updateTime?: string
}

export interface MenuPermissionTreeNode extends Permission {
  children: MenuPermissionTreeNode[]
}

export interface RoleDetailResponse {
  role: Role
  permissions: Permission[]
  userCount: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询角色列表，对接 smart_plant RoleController -> GET /role/list。 */
export const listRoles = (params: RoleListParams) => {
  return get<ApiResult<PageInfo<Role>>>('/role/list', params as unknown as Record<string, unknown>)
}

/** 根据角色 ID 查询详情。 */
export const getRoleById = (id: number) => {
  return get<ApiResult<Role>>(`/role/${id}`)
}

/** 查询角色详情，包含已分配权限和关联用户数量。 */
export const getRoleDetail = (id: number) => {
  return get<ApiResult<RoleDetailResponse>>(`/role/${id}/detail`)
}

/** 查询角色已分配权限。 */
export const listRolePermissions = (id: number) => {
  return get<ApiResult<Permission[]>>(`/role/${id}/permissions`)
}

/** 查询与后台左侧菜单一致的可分配菜单树。 */
export const listRolePermissionTree = () => {
  return get<ApiResult<MenuPermissionTreeNode[]>>('/role/permissions/tree')
}

/** 给角色分配权限。 */
export const assignRolePermissions = (id: number, permissionIds: number[]) => {
  return http<ApiResult<void>>({
    url: `/role/${id}/permissions`,
    method: 'put',
    data: { permissionIds },
  })
}

/** 新增角色，对接 POST /role/add。 */
export const addRole = (data: RolePayload) => {
  return post<ApiResult<Role>>('/role/add', data as unknown as Record<string, unknown>)
}

/** 编辑角色，对接 PUT /role。 */
export const updateRole = (data: UpdateRolePayload) => {
  return http<ApiResult<Role>>({ url: '/role', method: 'put', data })
}

/** 删除单个角色。后端会阻止删除已关联用户的角色。 */
export const deleteRole = (id: number) => {
  return http<ApiResult<void>>({ url: `/role/${id}`, method: 'delete' })
}

/** 批量删除角色。 */
export const batchDeleteRoles = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/role/batch', method: 'delete', data: ids })
}
