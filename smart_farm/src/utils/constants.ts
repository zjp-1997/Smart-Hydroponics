/**
 * 项目通用常量配置。
 *
 * 这里集中维护跨页面复用的固定值，避免在业务组件中散落硬编码。
 */
const Constants = {
  /**
   * 管理员登录 token 在 localStorage 中使用的 key。
   * 请求拦截器会通过这个 key 读取 token 并放入请求头。
   */
  USER_TOKEN: 'admin_token',

  /**
   * 管理员刷新 token 在浏览器存储中使用的 key。
   */
  USER_REFRESH_TOKEN: 'admin_refresh_token',

  /**
   * 当前登录用户信息缓存 key。
   */
  CURRENT_USER: 'admin_user',

  /**
   * 旧版账号密码缓存 key，仅用于升级清理，禁止读取或写入。
   */
  LEGACY_REMEMBERED_LOGIN_FORM: 'smart_farm_remembered_login_form',

  /**
   * 后端接口基础地址。
   * Vite 会根据当前运行模式读取 .env.development 或 .env.production 中的配置。
   */
  BASE_URL: import.meta.env.VITE_APP_API_URL,

  /**
   * 列表页默认分页大小。
   */
  PAGE_SIZE: 10,

  /**
   * 管理端登录页路径。
   */
  PAGE_ADMIN_LOGIN: '/login',
} as const

export default Constants
