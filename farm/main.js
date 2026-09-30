import App from './App'
import pullRefreshMixin from './utils/pullRefresh.js'

// #ifdef H5
const DYNAMIC_IMPORT_RELOAD_KEY = 'farm:dynamic-import-reload'
window.addEventListener('unhandledrejection', event => {
  const reason = event.reason
  const message = String((reason && reason.message) || reason || '')
  if (!message.includes('Failed to fetch dynamically imported module')) return

  const lastReload = Number(window.sessionStorage.getItem(DYNAMIC_IMPORT_RELOAD_KEY) || 0)
  if (Date.now() - lastReload < 10000) return

  event.preventDefault()
  window.sessionStorage.setItem(DYNAMIC_IMPORT_RELOAD_KEY, String(Date.now()))
  window.location.reload()
})
// #endif

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
Vue.mixin(pullRefreshMixin)
App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  app.mixin(pullRefreshMixin)
  return {
    app
  }
}
// #endif
