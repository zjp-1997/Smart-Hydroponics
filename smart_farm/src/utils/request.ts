import axios, {
  AxiosHeaders,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios'
import { ElMessage } from 'element-plus'
import Constants from '@/utils/constants'
import {
  getAuthRefreshToken,
  getAuthToken,
  isRememberedLogin,
  setAuthTokens,
  setCurrentUser,
} from '@/utils/auth'
import { clearLocalStorage } from '@/utils/utils'
import { createSingleFlight } from '@/utils/singleFlight'

interface ApiResponse<T = unknown> {
  code?: number
  message?: string
  data?: T
  [key: string]: unknown
}

type SmartFarmRequestConfig = AxiosRequestConfig & {
  preserveParamDateTime?: boolean
}

type SmartFarmInternalRequestConfig = InternalAxiosRequestConfig & {
  preserveParamDateTime?: boolean
  _retry?: boolean
}

interface RefreshLoginResponse {
  token: string
  refreshToken?: string
  admin?: {
    role?: string
    roleCode?: string
    [key: string]: unknown
  }
  roleCode?: string
  dataScope?: string
  permissions?: string[]
}

/**
 * token 请求头名称。
 *
 * 该名称需要与后端登录鉴权拦截器保持一致。
 */
const TOKEN_HEADER_NAME = 'pdmtoken'
const AUTHORIZATION_HEADER_NAME = 'Authorization'
const ISO_LOCAL_DATE_TIME_PATTERN = /^(\d{4}-\d{2}-\d{2})T(\d{2}:\d{2}:\d{2})(?:\.\d+)?$/
const DISPLAY_LOCAL_DATE_TIME_PATTERN = /^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2})$/
const MESSAGE_DEDUPLICATION_MS = 1200
const recentMessages = new Map<string, number>()
let logoutTimer: ReturnType<typeof setTimeout> | null = null

/**
 * 创建 Axios 实例，统一设置后端接口基础地址。
 */
const request = axios.create({
  baseURL: Constants.BASE_URL,
  timeout: 15000,
  withCredentials: true,
})

/**
 * 退出登录并回到登录页。
 */
const logout = () => {
  clearLocalStorage()
  location.href = '/'
}

/** 同一批并发失败只展示一次提示，页面无需再次弹出相同错误。 */
const showErrorOnce = (key: string, message: string) => {
  const now = Date.now()
  if (now - (recentMessages.get(key) || 0) < MESSAGE_DEDUPLICATION_MS) {
    return
  }
  recentMessages.set(key, now)
  ElMessage.error(message)
}

/** 401只触发一次提示和跳转，避免并发请求造成重复退出。 */
const expireSession = (message: string) => {
  if (logoutTimer) {
    return
  }
  ElMessage.closeAll()
  showErrorOnce('session-expired', message)
  logoutTimer = setTimeout(() => {
    logoutTimer = null
    logout()
  }, 300)
}

/** 403通知权限生命周期重新拉取用户信息，由路由决定是否离开当前页面。 */
const notifyPermissionDenied = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('admin-permission-denied'))
  }
}

const isLoginRequest = (url?: string) => {
  return Boolean(url?.includes('/admin/auth/login'))
}

const isRefreshRequest = (url?: string) => {
  return Boolean(url?.includes('/admin/auth/refresh'))
}

const isCaptchaRequest = (url?: string) => {
  return Boolean(url?.includes('/admin/auth/captcha'))
}

const isLogoutRequest = (url?: string) => {
  return Boolean(url?.includes('/admin/auth/logout'))
}

const shouldRefreshToken = (url?: string) => {
  return !isLoginRequest(url) && !isRefreshRequest(url) && !isCaptchaRequest(url) && !isLogoutRequest(url)
}

const getApiUrl = (path: string) => {
  return `${Constants.BASE_URL.replace(/\/+$/, '')}/${path.replace(/^\/+/, '')}`
}

const performAccessTokenRefresh = async () => {
  const refreshToken = getAuthRefreshToken()

  if (!refreshToken) {
    return false
  }

  const response = await axios.post<ApiResponse<RefreshLoginResponse>>(
    getApiUrl('/admin/auth/refresh'),
    { refreshToken },
    {
      timeout: 15000,
      withCredentials: true,
    },
  )

  const result = response.data
  const data = result.data

  if (result.code !== 200 || !data?.token) {
    return false
  }

  const rememberMe = isRememberedLogin()
  setAuthTokens(data.token, data.refreshToken || refreshToken, rememberMe)

  if (data.admin) {
    setCurrentUser(
      {
        ...data.admin,
        roleCode: data.roleCode || data.admin.roleCode || data.admin.role,
        dataScope: data.dataScope,
        permissions: data.permissions || [],
      },
      rememberMe,
    )
  }

  return true
}

// 所有并发401共享一次刷新操作，后续请求等待同一个结果后分别重放。
const refreshAccessToken = createSingleFlight(performAccessTokenRefresh)

const isPlainRecord = (value: unknown): value is Record<string, unknown> => {
  return Object.prototype.toString.call(value) === '[object Object]'
}

/**
 * 后端 LocalDateTime 默认返回 yyyy-MM-ddTHH:mm:ss。
 * 页面层统一展示为 yyyy-MM-dd HH:mm:ss，避免各个列表重复做 replace。
 */
const normalizeResponseDateTime = <T>(value: T): T => {
  if (typeof value === 'string') {
    return value.replace(ISO_LOCAL_DATE_TIME_PATTERN, '$1 $2') as T
  }

  if (Array.isArray(value)) {
    return value.map((item) => normalizeResponseDateTime(item)) as T
  }

  if (isPlainRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, normalizeResponseDateTime(item)]),
    ) as T
  }

  return value
}

/**
 * 表单字段使用无 T 的显示格式；发送请求前转换回后端 LocalDateTime 可解析的 ISO 格式。
 */
const normalizeRequestDateTime = <T>(value: T): T => {
  if (typeof value === 'string') {
    return value.replace(DISPLAY_LOCAL_DATE_TIME_PATTERN, '$1T$2') as T
  }

  if (Array.isArray(value)) {
    return value.map((item) => normalizeRequestDateTime(item)) as T
  }

  if (isPlainRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, normalizeRequestDateTime(item)]),
    ) as T
  }

  return value
}

/**
 * 请求拦截器：
 * 在每次请求发出前，从 localStorage 读取 token，并写入请求头。
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const smartFarmConfig = config as SmartFarmInternalRequestConfig
    const token = getAuthToken()

    config.headers = AxiosHeaders.from(config.headers)

    if (token) {
      config.headers.set(TOKEN_HEADER_NAME, token)
      config.headers.set(AUTHORIZATION_HEADER_NAME, `Bearer ${token}`)
    } else {
      config.headers.delete(TOKEN_HEADER_NAME)
      config.headers.delete(AUTHORIZATION_HEADER_NAME)
    }

    if (config.params && !smartFarmConfig.preserveParamDateTime) {
      config.params = normalizeRequestDateTime(config.params)
    }

    if (config.data) {
      config.data = normalizeRequestDateTime(config.data)
    }

    return config
  },
  (error: unknown) => Promise.reject(error),
)

/**
 * 响应拦截器：
 * 统一处理业务状态码、登录过期、网络异常和非 JSON 响应。
 */
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const contentType = String(
      response.headers['content-type'] ?? response.headers['Content-Type'] ?? '',
    )

    /**
     * 文件下载等响应通常不是 application/json。
     * 这类响应保留 Axios 原始 response，方便调用方读取 header、blob 等信息。
     */
    if (!contentType.includes('application/json')) {
      return Promise.resolve(response)
    }

    /**
     * Blob 响应通常表示文件流，不走业务 code 判断。
     */
    if (response.data && response.data instanceof Blob) {
      return Promise.reject(response.data)
    }

    const result = response.data

    if (result.data !== undefined) {
      result.data = normalizeResponseDateTime(result.data)
    }

    if (result.code && result.code !== 200) {
      if (isLoginRequest(response.config.url)) {
        return Promise.reject(response)
      }

      /**
       * token 失效、未登录或账号在其他地方登录。
       */
      if (result.code === 11012 || result.code === 11011) {
        expireSession('您没有登录，请重新登录')
        return Promise.reject(response)
      }

      /**
       * 长时间未操作，需要用户确认后重新登录。
       */
      if (result.code === 30001) {
        expireSession('登录状态已过期，请重新登录')
        return Promise.reject(response)
      }

      if (result.code === 401) {
        expireSession(result.message || '登录状态已过期，请重新登录')
        return Promise.reject(response)
      }

      if (result.code === 403) {
        showErrorOnce('forbidden', result.message || '您没有权限执行此操作')
        notifyPermissionDenied()
        return Promise.reject(response)
      }

      if (result.message) {
        showErrorOnce(`business:${result.code}:${result.message}`, result.message)
      }

      return Promise.reject(response)
    }

    /**
     * Axios 拦截器类型默认期望返回 AxiosResponse。
     * 这里运行时返回的是后端业务数据，http/get/post 方法会用泛型暴露真实返回类型。
     */
    return Promise.resolve(result as unknown as AxiosResponse)
  },
  async (error: any) => {
    const status = error?.response?.status
    const data = error?.response?.data as ApiResponse | undefined
    const originalConfig = error?.config as SmartFarmInternalRequestConfig | undefined

    if (
      status === 401 &&
      originalConfig &&
      !originalConfig._retry &&
      shouldRefreshToken(originalConfig.url)
    ) {
      originalConfig._retry = true

      try {
        const refreshed = await refreshAccessToken()

        if (refreshed) {
          return request.request(originalConfig)
        }
      } catch {
        // 刷新失败后走统一退出逻辑。
      }
    }

    if (status === 401 && !isLoginRequest(error?.config?.url)) {
      expireSession(data?.message || '登录状态已过期，请重新登录')
      return Promise.reject(error.response)
    }

    if (status === 403) {
      showErrorOnce('forbidden', data?.message || '您没有权限执行此操作')
      notifyPermissionDenied()
      return Promise.reject(error.response)
    }

    if (status === 401 || status === 429) {
      if (status === 429) showErrorOnce('rate-limit', data?.message || '请求过于频繁，请稍后重试')
      return Promise.reject(error.response)
    }

    if (isLoginRequest(error?.config?.url)) {
      return Promise.reject(error)
    }

    if (data?.message) {
      showErrorOnce(`http:${status}:${data.message}`, data.message)
      return Promise.reject(error.response)
    }

    /**
     * Axios 网络层异常统一提示，减少业务页面重复处理。
     */
    if (error.message?.includes('timeout')) {
      showErrorOnce('network-timeout', '网络超时')
    } else if (error.message === 'Network Error') {
      showErrorOnce('network-error', '网络连接错误')
    } else if (error.message?.includes('Request')) {
      showErrorOnce('network-request', '网络发生错误')
    }

    return Promise.reject(error)
  },
)

/**
 * 通用请求方法。
 *
 * @param config Axios 请求配置
 */
export const http = <T = ApiResponse>(config: SmartFarmRequestConfig) => {
  return request.request<unknown, T>(config)
}

/**
 * GET 请求封装。
 *
 * @param url 请求地址
 * @param params 查询参数
 */
export const get = <T = ApiResponse>(url: string, params?: Record<string, unknown>) => {
  return http<T>({ url, method: 'get', params })
}

/**
 * POST 请求封装。
 *
 * @param url 请求地址
 * @param data 请求体数据
 */
export const post = <T = ApiResponse>(url: string, data?: Record<string, unknown>) => {
  return http<T>({ url, method: 'post', data })
}

export default request
