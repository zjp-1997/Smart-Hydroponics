import { get, http } from '@/utils/request'

/** 与 smart_plant SystemSetting 一一对应的前端配置类型。 */
export interface SystemSetting {
  id: number
  systemName: string
  logoUrl?: string
  faviconUrl?: string
  systemDescription?: string
  copyrightInfo?: string
  homeTitle: string
  loginBackgroundUrl?: string
  themeColor: string
  timezone: string
  datetimeFormat: string
  defaultPageSize: number
  maxUploadSizeMb: number
  allowedUploadTypes: string
  recordNumber?: string
  officialWebsite?: string
  contactEmail?: string
  servicePhone?: string
  version: number
  updateTime?: string
}

export type SystemSettingUpdatePayload = Omit<SystemSetting, 'id' | 'updateTime'>
export type SystemAssetType = 'logo' | 'favicon' | 'loginBackground'

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface SystemAssetUploadResult {
  type: SystemAssetType
  url: string
  size: number
}

/** 登录前读取公开配置，使品牌外观无需写死在前端包中。 */
export const getPublicSystemSetting = () => {
  return get<ApiResult<SystemSetting>>('/public/system-settings')
}

/** 管理员打开系统设置弹框时读取最新值和版本号。 */
export const getAdminSystemSetting = () => {
  return get<ApiResult<SystemSetting>>('/admin/system-settings')
}

/** 整体保存五个分区的系统设置。 */
export const updateSystemSetting = (data: SystemSettingUpdatePayload) => {
  return http<ApiResult<SystemSetting>>({ url: '/admin/system-settings', method: 'put', data })
}

/** 上传系统品牌图片，并由后端返回可持久化的相对地址。 */
export const uploadSystemAsset = (type: SystemAssetType, file: File) => {
  const data = new FormData()
  data.append('file', file)
  return http<ApiResult<SystemAssetUploadResult>>({
    url: '/admin/system-settings/assets',
    method: 'post',
    params: { type },
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
