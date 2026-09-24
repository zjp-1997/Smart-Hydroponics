import { reactive } from 'vue'
import { getPublicSystemSetting, type SystemSetting } from '@/api/systemSetting'
import { getFileUrl } from '@/utils/utils'

/** 后端不可用时仍维持当前界面的安全默认值。 */
export const systemSetting = reactive<SystemSetting>({
  id: 1,
  systemName: 'Smart Plant',
  systemDescription: '家庭小型水培后台管理系统',
  copyrightInfo: '© 2026 Smart Plant',
  homeTitle: '智慧农业工作台',
  themeColor: '#239aaa',
  timezone: 'Asia/Shanghai',
  datetimeFormat: 'yyyy-MM-dd HH:mm:ss',
  defaultPageSize: 10,
  maxUploadSizeMb: 5,
  allowedUploadTypes: 'jpg,jpeg,png,webp,pdf,xlsx,xls,csv,doc,docx',
  version: 0,
})

/** 把配置同步到文档标题、favicon和Element Plus主题变量。 */
export const applySystemSetting = (value: SystemSetting) => {
  Object.assign(systemSetting, value)
  document.title = value.homeTitle || value.systemName
  document.documentElement.style.setProperty('--system-theme-color', value.themeColor)
  document.documentElement.style.setProperty('--el-color-primary', value.themeColor)

  if (value.faviconUrl) {
    let favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (!favicon) {
      favicon = document.createElement('link')
      favicon.rel = 'icon'
      document.head.appendChild(favicon)
    }
    favicon.href = getFileUrl(value.faviconUrl)
  }
}

/** 应用启动时加载一次；失败时沿用默认值，不阻断登录页面。 */
export const loadPublicSystemSetting = async () => {
  try {
    const result = await getPublicSystemSetting()
    if (result.data) applySystemSetting(result.data)
  } catch {
    applySystemSetting(systemSetting)
  }
}
