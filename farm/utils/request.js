import { clearAuth, getToken } from './auth.js'

// App 真机无法读取浏览器地址，默认使用当前开发机局域网 IP；仍可通过 farm_api_base_url 覆盖。
const APP_DEFAULT_BASE_URL = 'http://10.98.81.227:8080'
const SUCCESS_CODE = 200

// 简化版拦截器队列：保持 axios 风格的前置/后置扩展点，同时兼容 uni-app 全端。
const requestInterceptors = []
const responseInterceptors = []

export function getBaseUrl() {
	const storedBaseUrl = uni.getStorageSync('farm_api_base_url')
	if (storedBaseUrl) {
		return storedBaseUrl
	}
	// H5 调试时跟随页面主机，避免电脑 IP 变化后仍请求过期地址而被浏览器判定为网络/CORS 异常。
	if (typeof window !== 'undefined' && window.location && window.location.hostname) {
		return `http://${window.location.hostname}:8080`
	}
	return APP_DEFAULT_BASE_URL
}

// 图片等静态资源可能只保存 /uploads 相对路径，这里统一补齐后端上下文路径。
export function resolveFileUrl(url) {
	if (!url) {
		return ''
	}
	if (/^https?:\/\//i.test(url)) {
		return url
	}
	const normalizedUrl = url.startsWith('/') ? url : `/${url}`
	const resourcePath = normalizedUrl.startsWith('/smart_plant/')
		? normalizedUrl
		: `/smart_plant${normalizedUrl}`
	return `${getBaseUrl()}${resourcePath}`
}

// 支持传入完整 URL，也支持业务层只传相对路径，降低 api 层重复拼接成本。
function normalizeUrl(url) {
	if (/^https?:\/\//i.test(url)) {
		return url
	}
	return `${getBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`
}

// 按注册顺序执行拦截器；拦截器返回空值时沿用上一个结果，方便只做副作用扩展。
function runInterceptors(interceptors, value) {
	return interceptors.reduce((result, interceptor) => {
		if (typeof interceptor !== 'function') {
			return result
		}
		return interceptor(result) || result
	}, value)
}

// 非登录接口遇到 401 时统一清理登录态并回到账号密码登录页。
function handleUnauthorized() {
	clearAuth()
	uni.showToast({
		title: '登录已过期，请重新登录',
		icon: 'none'
	})
	setTimeout(() => {
		uni.reLaunch({
			url: '/pages/login/pwd_index'
		})
	}, 600)
}

// 请求前置守卫：统一超时、URL、JSON 头和 Bearer Token，页面/API 层无需关心鉴权细节。
requestInterceptors.push((config) => {
	const token = getToken()
	const header = {
		'Content-Type': 'application/json',
		...(config.header || {})
	}

	if (token) {
		header.Authorization = `Bearer ${token}`
	}

	return {
		timeout: 15000,
		// H5 登录响应需要保存 HttpOnly 资源 Cookie，供头像等受保护图片元素使用。
		withCredentials: true,
		...config,
		url: normalizeUrl(config.url),
		header
	}
})

// 响应后置守卫：统一识别后端 R<T> 包装结构，只把真正的 data 返回给业务页面。
responseInterceptors.push((response) => {
	const { statusCode, data, config } = response
	const businessCode = data && data.code

	if (statusCode >= 200 && statusCode < 300 && businessCode === SUCCESS_CODE) {
		return data.data
	}

	const message = (data && data.message) || '请求失败，请稍后重试'
	// 登录接口本身返回 401 代表账号密码错误，不应该触发“登录过期”跳转。
	const shouldRedirectToLogin = (statusCode === 401 || businessCode === 401) && !(config && config.skipAuthRedirect)

	if (shouldRedirectToLogin) {
		handleUnauthorized()
	}

	throw {
		statusCode,
		code: businessCode,
		message,
		silentToast: shouldRedirectToLogin,
		response
	}
})

// 暴露扩展点，后续可追加 traceId、设备信息、灰度标记等企业级横切逻辑。
export function addRequestInterceptor(interceptor) {
	requestInterceptors.push(interceptor)
}

export function addResponseInterceptor(interceptor) {
	responseInterceptors.push(interceptor)
}

// 统一请求入口。所有 uni.request 细节都收敛在这里，页面只处理业务成功/失败。
export function request(options) {
	const config = runInterceptors(requestInterceptors, options || {})

	return new Promise((resolve, reject) => {
		uni.request({
			...config,
			success(response) {
				try {
					resolve(runInterceptors(responseInterceptors, {
						...response,
						config
					}))
				} catch (error) {
					if (error && error.message && !error.silentToast) {
						uni.showToast({
							title: error.message,
							icon: 'none'
						})
					}
					reject(error)
				}
			},
			fail(error) {
				const requestError = {
					message: '网络异常，请检查连接',
					error
				}
				uni.showToast({
					title: requestError.message,
					icon: 'none'
				})
				reject(requestError)
			}
		})
	})
}

// 常用 GET 快捷方法，列表和详情查询统一从这里进入请求封装。
export function get(url, data, options = {}) {
	return request({
		...options,
		url,
		data,
		method: 'GET'
	})
}

// 常用 POST 快捷方法，保持 api 层代码简洁。
export function post(url, data, options = {}) {
	return request({
		...options,
		url,
		data,
		method: 'POST'
	})
}

export function put(url, data, options = {}) {
	return request({
		...options,
		url,
		data,
		method: 'PUT'
	})
}

/**
 * 统一文件上传入口，复用请求层的后端地址、Bearer Token 和 R<T> 响应协议。
 * 页面只传本地临时文件路径，避免重复实现鉴权与错误提示。
 */
export function uploadFile({ url, filePath, name = 'file', formData = {} }) {
	const token = getToken()
	return new Promise((resolve, reject) => {
		uni.uploadFile({
			url: normalizeUrl(url),
			filePath,
			name,
			formData,
			header: token ? { Authorization: `Bearer ${token}` } : {},
			timeout: 60000,
			success(response) {
				let body = response.data
				try {
					body = typeof body === 'string' ? JSON.parse(body) : body
				} catch (error) {
					body = null
				}
				if (response.statusCode >= 200 && response.statusCode < 300 && body && body.code === SUCCESS_CODE) {
					resolve(body.data)
					return
				}
				const message = (body && body.message) || '文件上传失败，请稍后重试'
				if (response.statusCode === 401 || (body && body.code === 401)) {
					handleUnauthorized()
				}
				uni.showToast({ title: message, icon: 'none' })
				reject({ statusCode: response.statusCode, message, response })
			},
			fail(error) {
				const uploadError = { message: '文件上传失败，请检查网络', error }
				uni.showToast({ title: uploadError.message, icon: 'none' })
				reject(uploadError)
			}
		})
	})
}

/** 下载受保护资源；图片组件和 downloadFile 不经过普通请求拦截器，因此在此补齐令牌。 */
export function downloadProtectedFile(url) {
	const token = getToken()
	return new Promise((resolve, reject) => {
		uni.downloadFile({
			url: normalizeUrl(url),
			header: token ? { Authorization: `Bearer ${token}` } : {},
			timeout: 60000,
			success(result) {
				if (result.statusCode === 200 && result.tempFilePath) {
					resolve(result.tempFilePath)
					return
				}
				if (result.statusCode === 401) handleUnauthorized()
				reject({ statusCode: result.statusCode, message: '文件下载失败' })
			},
			fail(error) { reject({ message: '文件下载失败', error }) }
		})
	})
}
