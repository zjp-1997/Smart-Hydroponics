const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('@vue/compiler-sfc')

const read = (path) => readFileSync(path, 'utf8')
const storage = () => {
  const entries = new Map()
  return {
    getItem: (key) => entries.get(key) ?? null,
    setItem: (key, value) => entries.set(key, String(value)),
    removeItem: (key) => entries.delete(key),
    entries,
  }
}

// 执行真实 TypeScript 模块，仅替换浏览器存储和外部接口，不访问业务数据库。
function load(source, globals = {}, imports = {}) {
  const exports = {}
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  vm.runInNewContext(code, {
    exports,
    ...globals,
    require(name) {
      assert.ok(name in imports, `Unexpected import: ${name}`)
      return imports[name]
    },
  })
  return exports
}

const constantsModule = load(read('src/utils/constants.ts').replace('import.meta.env.VITE_APP_API_URL', "''"))
const keys = constantsModule.default
function setup() {
  const globals = { localStorage: storage(), sessionStorage: storage() }
  const reloadAuth = () => load(read('src/utils/auth.ts'), globals, { '@/utils/constants': constantsModule })
  return { globals, reloadAuth, auth: reloadAuth() }
}

test('应用启动即删除旧密码缓存，保留已有令牌和业务偏好', () => {
  const { globals, auth } = setup()
  globals.localStorage.setItem(keys.LEGACY_REMEMBERED_LOGIN_FORM, 'invalid-legacy-value')
  globals.sessionStorage.setItem(keys.LEGACY_REMEMBERED_LOGIN_FORM, 'legacy-session-value')
  auth.setAuthTokens('existing-access', 'existing-refresh', true)
  globals.localStorage.setItem('theme', 'dark')
  const app = { component() {}, use() {}, mount() {} }
  load(read('src/main.ts'), globals, {
    'element-plus/theme-chalk/el-message.css': {},
    'element-plus/theme-chalk/el-message-box.css': {},
    './assets/main.css': {},
    vue: { createApp: () => app, defineAsyncComponent: () => ({}) },
    pinia: { createPinia: () => ({}) },
    './App.vue': { default: {} },
    './router': { default: {} },
    './utils/filterSearch': { setupFilterFormSearchInteractions() {} },
    './utils/auth': auth,
  })
  assert.equal(globals.localStorage.getItem(keys.LEGACY_REMEMBERED_LOGIN_FORM), null)
  assert.equal(globals.sessionStorage.getItem(keys.LEGACY_REMEMBERED_LOGIN_FORM), null)
  assert.equal(auth.getAuthToken(), 'existing-access')
  assert.equal(auth.getAuthRefreshToken(), 'existing-refresh')
  assert.equal(globals.localStorage.getItem('theme'), 'dark')
})

test('用户缓存更新和退出会通知长期挂载的权限UI', () => {
  const { auth } = setup()
  const roles = []
  const unsubscribe = auth.subscribeCurrentUser((user) => roles.push(user?.roleCode || 'anonymous'))
  auth.setCurrentUser({ id: 7, roleCode: 'farm_owner', permissions: ['plot:manage'] })
  auth.setCurrentUser({ id: 7, roleCode: 'farm_owner', permissions: ['warehouse:manage'] })
  auth.clearAuthToken()
  unsubscribe()
  auth.setCurrentUser({ id: 1, roleCode: 'admin', permissions: ['*:*:*'] })
  assert.deepEqual(roles, ['anonymous', 'farm_owner', 'farm_owner', 'anonymous'])
})

for (const rememberMe of [false, true]) {
  test(`登录保持选项 ${rememberMe}：只存令牌与用户信息，不存密码`, async () => {
    const { globals, auth, reloadAuth } = setup()
    // 模拟切换登录模式，确保旧模式中的持久令牌也被移除。
    auth.setAuthTokens('old-access', 'old-refresh', !rememberMe)
    const password = 'Only-for-this-unit-test-42!'
    let payload
    let destination
    const admin = { id: 1, username: 'test-user', roleCode: 'admin' }
    const source = parse(read('src/views/LoginView.vue')).descriptor.scriptSetup.content
    const login = load(`${source}\nexport { loginForm, loginFormRef, handleLogin }`, globals, {
      vue: {
        ref: (value) => ({ value }), reactive: (value) => value,
        computed: (get) => ({ get value() { return get() } }), onMounted() {},
      },
      'vue-router': { useRouter: () => ({ push: async (path) => { destination = path } }) },
      'element-plus': { ElMessage: { success() {}, error(message) { assert.fail(message) } } },
      '@element-plus/icons-vue': {},
      '@/api/auth': {
        loginAdmin: async (input) => { payload = input; return { data: { token: 'access', refreshToken: 'refresh', admin } } },
      },
      '@/utils/auth': auth,
      '@/services/adminSession': {
        refreshCurrentAdminSession: async () => {
          const current = auth.getCurrentUser()
          auth.setCurrentUser({ ...current, permissions: ['*:*:*'] }, rememberMe)
          return auth.getCurrentUser()
        },
      },
      '@/stores/systemSetting': { systemSetting: {} },
      '@/utils/utils': { getFileUrl: (url) => url },
    })
    assert.equal(login.loginForm.password, '')
    Object.assign(login.loginForm, { username: 'test-user', password, rememberMe })
    login.loginFormRef.value = { validate: async () => true }
    await login.handleLogin()
    assert.equal(payload.password, password)
    assert.equal(payload.rememberMe, rememberMe)
    assert.equal(destination, '/home')
    assert.equal(login.loginForm.password, '')
    const persisted = rememberMe ? globals.localStorage : globals.sessionStorage
    const other = rememberMe ? globals.sessionStorage : globals.localStorage
    assert.deepEqual([...persisted.entries.keys()].sort(), [keys.USER_TOKEN, keys.USER_REFRESH_TOKEN, keys.CURRENT_USER].sort())
    assert.equal(other.entries.size, 0)
    assert.ok(!JSON.stringify([...persisted.entries]).includes(password))
    const restored = reloadAuth()
    assert.equal(restored.getAuthToken(), 'access')
    assert.equal(restored.getAuthRefreshToken(), 'refresh')
    assert.equal(restored.getCurrentUser().username, 'test-user')
    assert.equal(restored.isRememberedLogin(), rememberMe)
    restored.clearAuthToken()
    assert.equal(restored.getAuthToken(), '')
    assert.equal(globals.localStorage.entries.size, 0)
    assert.equal(globals.sessionStorage.entries.size, 0)
  })
}
