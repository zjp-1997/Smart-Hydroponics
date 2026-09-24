import Constants from '@/utils/constants'
import { clearAuthToken } from '@/utils/auth'

export interface FileListItem {
  name: string
  url: string
}

/**
 * 保存字符串数据到浏览器本地存储。
 *
 * @param key 存储键名
 * @param value 存储值
 */
export const saveLocalStorage = (key: string, value: string) => {
  localStorage.setItem(key, value)
}

/**
 * 从浏览器本地存储中读取数据。
 *
 * @param key 存储键名
 * @returns 若不存在对应数据，则返回空字符串，方便调用方直接判空。
 */
export const getLocalStorage = (key: string) => {
  return localStorage.getItem(key) || ''
}

/**
 * 删除指定本地存储数据。
 *
 * @param key 需要删除的存储键名
 */
export const removeLocalStorage = (key: string) => {
  localStorage.removeItem(key)
}

/**
 * 清理当前系统的登录态。
 *
 * 这里不再调用 localStorage.clear()，避免误删主题、筛选条件等其他业务缓存。
 */
export const clearLocalStorage = () => {
  clearAuthToken()
  removeLocalStorage(Constants.USER_TOKEN)
  removeLocalStorage(Constants.USER_REFRESH_TOKEN)
}

/**
 * 生成图片或文件的完整访问地址。
 *
 * - 后端返回完整 http/https 地址时直接返回
 * - 后端返回相对路径时，自动拼接环境变量中的接口基础地址
 * - 兼容 BASE_URL 或文件路径多写 / 的情况
 *
 * @param url 后端返回的文件相对路径或完整地址
 */
export const getFileUrl = (url: string) => {
  if (!url) {
    return ''
  }

  if (/^(https?:\/\/|blob:|data:)/i.test(url)) {
    return url
  }

  const baseUrl = Constants.BASE_URL.replace(/\/+$/, '')
  const normalizedPath = `/${url.replace(/^\/+/, '')}`

  // 兼容数据库历史值 /smart_plant/uploads/...，避免与 BASE_URL 的上下文路径重复拼接。
  try {
    const apiUrl = new URL(baseUrl)
    const contextPath = apiUrl.pathname.replace(/\/+$/, '')
    if (contextPath && (normalizedPath === contextPath || normalizedPath.startsWith(`${contextPath}/`))) {
      return `${apiUrl.origin}${normalizedPath}`
    }
  } catch {
    // 非标准 BASE_URL 继续走字符串拼接，构建期和相对代理地址均可兼容。
  }

  return `${baseUrl}${normalizedPath}`
}

/**
 * 将逗号分隔的文件路径转换成上传组件常用的 fileList 格式。
 *
 * 示例：
 * "a.png,b.png" -> [{ name: "a.png", url: "完整地址/a.png" }, ...]
 *
 * @param urls 后端返回的逗号分隔文件路径
 */
export const urls2Filelist = (urls?: string | null): FileListItem[] => {
  if (!urls) {
    return []
  }

  return urls
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => ({
      name: item,
      url: getFileUrl(item),
    }))
}
