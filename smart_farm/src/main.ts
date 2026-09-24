// ElMessage 与 MessageBox/Prompt 通过编程式 API 创建，不会被组件自动导入插件收集样式。
// 显式加载官方样式，保证路由懒加载后操作结果提示和确认弹框仍与拆分前一致。
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import './assets/main.css'

import { createApp, defineAsyncComponent } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setupFilterFormSearchInteractions } from './utils/filterSearch'
import { clearLegacyLoginForm } from './utils/auth'

// 已登录用户直接打开业务页时也要清理旧版本保存的密码。
clearLegacyLoginForm()
const app = createApp(App)

// 后台框架组件仅在业务路由真正渲染时加载，避免登录首屏同步下载菜单、
// 页签、通知和相关 Element Plus 组件；异步注册不改变组件名称或页面结构。
app.component('Header', defineAsyncComponent(() => import('./components/Header.vue')))
app.component('LeftMenu', defineAsyncComponent(() => import('./components/LeftMenu.vue')))

app.use(createPinia())
app.use(router)

// 为所有列表筛选表单注册统一查询交互，包括回车查询和清除条件后自动刷新。
setupFilterFormSearchInteractions()

app.mount('#app')
