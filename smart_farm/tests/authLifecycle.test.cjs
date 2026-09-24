const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')

function load(source, imports = {}, globals = {}) {
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

test('single-flight合并并发刷新，并在完成后允许下一次刷新', async () => {
  const module = load(readFileSync('src/utils/singleFlight.ts', 'utf8'))
  let calls = 0
  let release
  const run = module.createSingleFlight(() => {
    calls += 1
    return new Promise((resolve) => { release = resolve })
  })

  const first = run()
  const second = run()
  assert.equal(first, second)
  assert.equal(calls, 1)
  release('refreshed')
  assert.equal(await first, 'refreshed')

  const third = run()
  assert.equal(calls, 2)
  release('next-refresh')
  assert.equal(await third, 'next-refresh')
})

test('用户信息同步合并并发请求并发布最新权限', async () => {
  const singleFlight = load(readFileSync('src/utils/singleFlight.ts', 'utf8'))
  let infoCalls = 0
  let release
  let savedUser = { id: 7, roleCode: 'farm_owner', permissions: ['plot:manage'] }
  const session = load(readFileSync('src/services/adminSession.ts', 'utf8'), {
    '@/api/auth': {
      getAdminInfo: () => {
        infoCalls += 1
        return new Promise((resolve) => { release = resolve })
      },
    },
    '@/utils/auth': {
      getAuthToken: () => 'access-token',
      getCurrentUser: () => savedUser,
      isRememberedLogin: () => false,
      setCurrentUser: (user) => { savedUser = user },
    },
    '@/utils/singleFlight': singleFlight,
  })

  const first = session.refreshCurrentAdminSession(true)
  const second = session.refreshCurrentAdminSession(true)
  assert.equal(infoCalls, 1)
  release({ data: { admin: { id: 7 }, roleCode: 'farm_owner', permissions: ['warehouse:manage'] } })
  await Promise.all([first, second])
  assert.deepEqual(savedUser.permissions, ['warehouse:manage'])
})
