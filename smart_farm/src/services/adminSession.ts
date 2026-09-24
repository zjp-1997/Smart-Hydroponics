import { getAdminInfo } from '@/api/auth'
import {
  getAuthToken,
  getCurrentUser,
  isRememberedLogin,
  setCurrentUser,
  type AuthUser,
} from '@/utils/auth'
import { createSingleFlight } from '@/utils/singleFlight'

// 路由频繁切换时复用近期结果；窗口重新获得焦点或收到403时仍可强制校准。
const SESSION_SYNC_INTERVAL_MS = 30_000
let lastSyncedAt = 0
let lastSyncedToken = ''

const performCurrentUserRefresh = async (): Promise<AuthUser> => {
  const result = await getAdminInfo()
  const currentUser = getCurrentUser() || {}
  const refreshedUser: AuthUser = {
    ...currentUser,
    ...result.data.admin,
    roleCode: result.data.roleCode || result.data.admin?.roleCode || result.data.admin?.role,
    dataScope: result.data.dataScope,
    permissions: result.data.permissions || [],
  }

  setCurrentUser(refreshedUser, isRememberedLogin())
  lastSyncedAt = Date.now()
  lastSyncedToken = getAuthToken()
  return refreshedUser
}

// 多个路由守卫、403回调或窗口焦点事件同时触发时，只发送一次用户信息请求。
const runCurrentUserRefresh = createSingleFlight(performCurrentUserRefresh)

/** 拉取后台最新角色与权限，使本地菜单和路由状态及时收敛。 */
export const refreshCurrentAdminSession = (force = false) => {
  const token = getAuthToken()
  if (!token) {
    return Promise.resolve<AuthUser | null>(null)
  }
  if (!force && token === lastSyncedToken && lastSyncedAt && Date.now() - lastSyncedAt < SESSION_SYNC_INTERVAL_MS) {
    return Promise.resolve(getCurrentUser())
  }
  return runCurrentUserRefresh()
}
