import { getCurrentClientUser } from '@/api/clientAuth.js'
import { getClientHome, getToken, setUserInfo } from '@/utils/auth.js'

/** 技术人员页面每次显示时向服务端确认身份，防止本地缓存角色过期。 */
export async function ensureTechnicianAccess() {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/pwd_index' })
    return false
  }
  try {
    const user = await getCurrentClientUser()
    setUserInfo(user)
    if (String(user.roleCode || '').toLowerCase() === 'technician') return true
    uni.reLaunch({ url: getClientHome(user.roleCode) })
  } catch { /* 失效令牌由请求层清理。 */ }
  return false
}
