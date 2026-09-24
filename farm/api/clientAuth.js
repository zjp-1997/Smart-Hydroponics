import { get, post, put, uploadFile } from '@/utils/request.js'

// 匿名注册页只读取启用中的农场主 ID 和展示名称。
export function getRegisterFarmOwners() {
	return get('/smart_plant/client/auth/register/farm-owners', {}, { skipAuthRedirect: true })
}

// 用户端注册接口。这里只做字段映射，不放校验规则，校验留给页面和后端双层兜底。
export function registerAccount(data) {
	return post('/smart_plant/client/auth/register', {
		username: data.username,
		password: data.password,
		confirmPassword: data.confirmPassword,
		phone: data.phone,
		nickname: data.nickname,
		roleCode: data.roleCode,
		farmOwnerId: data.farmOwnerId
	})
}

// 用户端账号密码登录接口。skipAuthRedirect 用于避免登录失败 401 被当作登录态过期处理。
export function loginByPassword(data) {
	return post('/smart_plant/client/auth/login', {
		username: data.username,
		password: data.password,
		rememberMe: Boolean(data.rememberMe)
	}, {
		skipAuthRedirect: true
	})
}

// 获取验证码登录场景的模拟短信验证码；后端当前会直接返回六位验证码供联调。
export function sendLoginSmsCode(phone) {
	return post('/smart_plant/client/auth/sms-code/login', { phone }, {
		// 发码发生在登录前，业务失败不能被请求层误判为登录态过期。
		skipAuthRedirect: true
	})
}

// 使用手机号和验证码登录，成功响应与账号密码登录保持一致。
export function loginBySms(data) {
	return post('/smart_plant/client/auth/sms-login', {
		phone: data.phone,
		code: data.code,
		rememberMe: Boolean(data.rememberMe)
	}, {
		skipAuthRedirect: true
	})
}

// 应用重开或切回工作台时，从服务端读取当前角色和用户资料，避免依赖本地旧缓存。
export function getCurrentClientUser() {
	return get('/smart_plant/client/auth/me')
}

// 更新当前移动端用户个人信息；后端根据 token 确定用户，页面不传 userId。
export function updateCurrentClientProfile(data) {
	return put('/smart_plant/client/auth/profile', {
		username: data.username,
		phone: data.phone,
		nickname: data.nickname,
		gender: data.gender,
		email: data.email,
		avatar: data.avatar,
		remark: data.remark
	})
}

// 上传头像并返回后端相对地址，保存个人信息时再写入 user.avatar。
export function uploadCurrentClientAvatar(filePath) {
	return uploadFile({
		url: '/smart_plant/client/auth/profile/avatar',
		filePath,
		name: 'file'
	})
}

// 当前登录用户获取绑定手机号验证码，Authorization 由请求拦截器自动携带。
export function sendBindPhoneSmsCode(phone) {
	return post('/smart_plant/client/auth/phone/code', { phone })
}

// 校验手机号与验证码并绑定到当前 JWT 对应用户，不允许前端提交 userId。
export function bindPhone(data) {
	return post('/smart_plant/client/auth/phone/bind', {
		phone: data.phone,
		code: data.code
	})
}

// 当前登录用户获取新手机号的换绑验证码，后端会校验旧账号确实已有绑定号码。
export function sendChangePhoneSmsCode(phone) {
	return post('/smart_plant/client/auth/phone/change-code', { phone })
}

// 使用新手机号验证码完成换绑，并返回更新后的用户信息。
export function changePhone(data) {
	return post('/smart_plant/client/auth/phone/change', {
		phone: data.phone,
		code: data.code
	})
}

// 找回密码验证码只能发送到当前账号已经绑定的手机号。
export function sendPasswordResetCode(phone) {
	return post('/smart_plant/client/auth/password/code', { phone })
}

// 校验已绑定手机号与验证码后设置新密码，成功后服务端会撤销旧登录会话。
export function resetPassword(data) {
	return post('/smart_plant/client/auth/password/reset', {
		phone: data.phone,
		code: data.code,
		newPassword: data.newPassword,
		confirmPassword: data.confirmPassword
	})
}

// 通知后端注销当前访问令牌；页面随后会清理本地 token 和用户信息。
export function logout() {
	return post('/smart_plant/client/auth/logout', {}, {
		// 注销时即使令牌已失效，也由页面统一完成本地退出，避免重复触发登录过期跳转。
		skipAuthRedirect: true
	})
}
