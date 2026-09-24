import Constants from '@/utils/constants'

let memoryToken = ''
let memoryRefreshToken = ''
let memoryUser: AuthUser | null = null
const currentUserListeners = new Set<(user: AuthUser | null) => void>()

/** 通知头部、侧栏等长期挂载组件重新读取角色与权限。 */
const notifyCurrentUserChanged = () => {
  currentUserListeners.forEach((listener) => listener(memoryUser))
}

/** 直接删除旧密码缓存，不解码；启动和退出时均清理，不影响其他业务存储。 */
export const clearLegacyLoginForm = () => {
  localStorage.removeItem(Constants.LEGACY_REMEMBERED_LOGIN_FORM)
  sessionStorage.removeItem(Constants.LEGACY_REMEMBERED_LOGIN_FORM)
}

export interface AuthUser {
  id?: number
  adminId?: number
  username?: string
  phone?: string
  nickname?: string
  avatar?: string
  roleId?: number
  roleName?: string
  roleCode?: string
  role?: string
  dataScope?: string
  permissions?: string[]
  userType?: number
  status?: number
  email?: string
  gender?: number
  region?: string
  remark?: string
}

export const setAuthToken = (token: string, rememberMe = false) => {
  memoryToken = token

  if (rememberMe) {
    localStorage.setItem(Constants.USER_TOKEN, token)
    sessionStorage.removeItem(Constants.USER_TOKEN)
    return
  }

  sessionStorage.setItem(Constants.USER_TOKEN, token)
  localStorage.removeItem(Constants.USER_TOKEN)
}

export const setAuthTokens = (token: string, refreshToken = '', rememberMe = false) => {
  setAuthToken(token, rememberMe)
  memoryRefreshToken = refreshToken

  sessionStorage.removeItem(Constants.USER_REFRESH_TOKEN)
  localStorage.removeItem(Constants.USER_REFRESH_TOKEN)

  if (!refreshToken) {
    return
  }

  getStorage(rememberMe).setItem(Constants.USER_REFRESH_TOKEN, refreshToken)
}

export const getAuthToken = () => {
  if (memoryToken) {
    return memoryToken
  }

  memoryToken =
    sessionStorage.getItem(Constants.USER_TOKEN) || localStorage.getItem(Constants.USER_TOKEN) || ''

  return memoryToken
}

export const getAuthRefreshToken = () => {
  if (memoryRefreshToken) {
    return memoryRefreshToken
  }

  memoryRefreshToken =
    sessionStorage.getItem(Constants.USER_REFRESH_TOKEN) ||
    localStorage.getItem(Constants.USER_REFRESH_TOKEN) ||
    ''

  return memoryRefreshToken
}

export const isRememberedLogin = () => Boolean(localStorage.getItem(Constants.USER_REFRESH_TOKEN))

const getStorage = (rememberMe: boolean) => (rememberMe ? localStorage : sessionStorage)

const decodeBase64Url = (value: string) => {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = '='.repeat((4 - (base64.length % 4)) % 4)
  return decodeURIComponent(
    Array.from(atob(base64 + padding))
      .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, '0')}`)
      .join(''),
  )
}

const getUserFromToken = (): AuthUser | null => {
  const token = getAuthToken()
  const payload = token.split('.')[1]

  if (!payload) {
    return null
  }

  try {
    const parsed = JSON.parse(decodeBase64Url(payload)) as AuthUser
    return {
      id: parsed.id ?? parsed.adminId,
      adminId: parsed.adminId,
      username: parsed.username,
      roleCode: parsed.roleCode || parsed.role,
      role: parsed.role,
    }
  } catch {
    return null
  }
}

export const setCurrentUser = (user: AuthUser | undefined, rememberMe = false) => {
  memoryUser = user || null

  sessionStorage.removeItem(Constants.CURRENT_USER)
  localStorage.removeItem(Constants.CURRENT_USER)

  if (user) {
    getStorage(rememberMe).setItem(Constants.CURRENT_USER, JSON.stringify(user))
  }

  notifyCurrentUserChanged()
}

/** 订阅统一用户缓存，返回取消订阅函数供组件卸载时清理。 */
export const subscribeCurrentUser = (listener: (user: AuthUser | null) => void) => {
  currentUserListeners.add(listener)
  listener(getCurrentUser())
  return () => currentUserListeners.delete(listener)
}

export const getCurrentUser = () => {
  if (memoryUser) {
    return memoryUser
  }

  const raw =
    sessionStorage.getItem(Constants.CURRENT_USER) || localStorage.getItem(Constants.CURRENT_USER)

  if (raw) {
    try {
      memoryUser = JSON.parse(raw) as AuthUser
      return memoryUser
    } catch {
      sessionStorage.removeItem(Constants.CURRENT_USER)
      localStorage.removeItem(Constants.CURRENT_USER)
    }
  }

  memoryUser = getUserFromToken()
  return memoryUser
}

export const getCurrentUserId = () => getCurrentUser()?.id ?? getCurrentUser()?.adminId

export const getCurrentRoleCode = () => (getCurrentUser()?.roleCode || getCurrentUser()?.role || '').toLowerCase()

export const isAdminUser = (user = getCurrentUser()) => (user?.roleCode || user?.role || '').toLowerCase() === 'admin'

export const isFarmOwnerUser = (user = getCurrentUser()) =>
  (user?.roleCode || user?.role || '').toLowerCase() === 'farm_owner'

export const hasPermission = (permission: string, user = getCurrentUser()) => {
  const permissions = user?.permissions || []

  return permissions.some((owned) => permissionMatches(owned, permission))
}

const permissionMatches = (owned: string, required: string) => {
  if (!owned || !required) {
    return false
  }

  const normalizedOwned = owned.trim()
  const normalizedRequired = required.trim()

  return (
    normalizedOwned === '*:*:*' ||
    normalizedOwned === normalizedRequired ||
    moduleManageMatches(normalizedOwned, normalizedRequired) ||
    impliedMenuMatches(normalizedOwned, normalizedRequired)
  )
}

const moduleManageMatches = (owned: string, required: string) => {
  if (!owned.endsWith(':manage') || !required.includes(':')) {
    return false
  }

  const moduleCode = owned.slice(0, -':manage'.length)
  return required.startsWith(`${moduleCode}:`)
}

const impliedMenuMatches = (owned: string, required: string) => {
  return owned === 'farm_task:manage' && required.startsWith('farm_task_record:')
}

export const clearAuthToken = () => {
  memoryToken = ''
  memoryRefreshToken = ''
  memoryUser = null
  sessionStorage.removeItem(Constants.USER_TOKEN)
  localStorage.removeItem(Constants.USER_TOKEN)
  sessionStorage.removeItem(Constants.USER_REFRESH_TOKEN)
  localStorage.removeItem(Constants.USER_REFRESH_TOKEN)
  sessionStorage.removeItem(Constants.CURRENT_USER)
  localStorage.removeItem(Constants.CURRENT_USER)
  clearLegacyLoginForm()
  notifyCurrentUserChanged()
}
