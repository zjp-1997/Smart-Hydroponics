import { getCurrentClientUser } from '@/api/clientAuth.js'
import { getClientHome, getToken, setUserInfo } from '@/utils/auth.js'

/** 专家每次进入工作台时由后端确认角色；令牌失效交给统一请求层处理。 */
export async function ensureExpertAccess() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/pwd_index' })
    return false
  }
  try {
    const user = await getCurrentClientUser()
    setUserInfo(user)
    if (String(user.roleCode || '').toLowerCase() === 'expert') return true
    uni.reLaunch({ url: getClientHome(user.roleCode) })
  } catch { /* 401 时请求层会清理凭据并跳回登录页。 */ }
  return false
}

/** 农场主四栏页面只对农场主和普通用户开放，其他角色回到各自工作台。 */
export async function ensureNonExpertAccess() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/pwd_index' })
    return false
  }
  try {
    const user = await getCurrentClientUser()
    setUserInfo(user)
	if (['farm_owner', 'user'].includes(String(user.roleCode || '').toLowerCase())) return true
	uni.reLaunch({ url: getClientHome(user.roleCode) })
  } catch { /* 失效令牌由统一请求层处理。 */ }
  return false
}
