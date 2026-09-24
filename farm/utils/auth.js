const TOKEN_KEY = 'farm_client_token'
const REFRESH_TOKEN_KEY = 'farm_client_refresh_token'
const USER_INFO_KEY = 'farm_client_user_info'

// 从本地缓存读取访问令牌。所有需要登录态的接口都会由请求前置守卫自动带上它。
export function getToken() {
	return uni.getStorageSync(TOKEN_KEY) || ''
}

// 访问令牌由登录接口返回，保存后可用于后续业务接口 Authorization Bearer 鉴权。
export function setToken(token) {
	if (token) {
		uni.setStorageSync(TOKEN_KEY, token)
	}
}

// 刷新令牌暂时只做持久化，后续接入 refresh 接口时可直接复用。
export function getRefreshToken() {
	return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(refreshToken) {
	if (refreshToken) {
		uni.setStorageSync(REFRESH_TOKEN_KEY, refreshToken)
	}
}

// 用户信息用于页面展示和本地登录态判断，敏感字段由后端 User 实体序列化规则过滤。
export function getUserInfo() {
	return uni.getStorageSync(USER_INFO_KEY) || null
}

export function setUserInfo(userInfo) {
	if (userInfo) {
		uni.setStorageSync(USER_INFO_KEY, userInfo)
	}
}

// 统一保存登录成功后的认证信息，避免页面层散落多个 storage key。
export function saveAuth(authData) {
	setToken(authData && authData.token)
	setRefreshToken(authData && authData.refreshToken)
	setUserInfo(authData && authData.user)
}

// 退出登录、令牌过期或鉴权失败时统一清理本地认证态。
export function clearAuth() {
	uni.removeStorageSync(TOKEN_KEY)
	uni.removeStorageSync(REFRESH_TOKEN_KEY)
	uni.removeStorageSync(USER_INFO_KEY)
}

// 登录、冷启动和令牌恢复均使用同一角色映射，技术人员与专家不进入农场主四栏页面。
export function getClientHome(roleCode) {
	const role = String(roleCode || '').toLowerCase()
	if (role === 'expert') return '/pages/expert/news'
	if (role === 'technician') return '/pages/service/fault_handling'
	return '/pages/index/index'
}
