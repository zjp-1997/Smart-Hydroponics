const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')
const { parse } = require('@vue/compiler-sfc')

// 沿用项目现有测试方式执行真实页面脚本，仅替换网络与UI依赖，无需访问业务数据。
function setup(getCount) {
  const source = parse(readFileSync('src/views/user/ListView.vue', 'utf8')).descriptor.scriptSetup.content
  const exports = {}
  const imports = {
    vue: {
      ref: (value) => ({ value }),
      computed: (get) => ({ get value() { return get() } }),
      onMounted() {},
    },
    'element-plus': {},
    '@element-plus/icons-vue': {},
    '@/api/user': { getPendingFarmJoinRequestCount: getCount },
    '@/utils/utils': {},
    './AddOrUpdate.vue': {},
    './FarmJoinRequests.vue': {},
  }
  const code = ts.transpileModule(`${source}\nexport { fetchPendingJoinCount, pendingJoinCount, joinCountLoading, joinCountLabel }`, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  }).outputText
  vm.runInNewContext(code, {
    exports,
    require(name) {
      assert.ok(name in imports, `Unexpected import: ${name}`)
      return imports[name]
    },
  })
  return exports
}

test('待审核统计保留0及完整大数量，读屏提示包含业务含义', async () => {
  let count = 0
  const page = setup(async () => ({ data: count }))
  await page.fetchPendingJoinCount()
  assert.equal(page.pendingJoinCount.value, 0)
  assert.equal(page.joinCountLabel.value, '入场申请，0条待审核')
  count = 128
  await page.fetchPendingJoinCount()
  assert.equal(page.pendingJoinCount.value, 128)
  assert.equal(page.joinCountLabel.value, '入场申请，128条待审核')
})

test('审核后的新数量不会被旧请求的成功或失败结果覆盖', async () => {
  // 分别模拟旧请求迟到成功与迟到失败，两者均不能覆盖已刷新为0的结果。
  for (const staleFails of [false, true]) {
    const requests = []
    const page = setup(() => new Promise((resolve, reject) => requests.push({ resolve, reject })))
    const older = page.fetchPendingJoinCount()
    const newer = page.fetchPendingJoinCount()
    assert.equal(page.joinCountLoading.value, true)
    requests[1].resolve({ data: 0 })
    await newer
    if (staleFails) requests[0].reject(new Error('stale failure'))
    else requests[0].resolve({ data: 5 })
    await older
    assert.equal(page.pendingJoinCount.value, 0)
    assert.equal(page.joinCountLoading.value, false)
  }
})

test('统计失败不伪装成0条，重试成功后恢复真实数量', async () => {
  let fails = true
  const page = setup(async () => {
    if (fails) throw new Error('network unavailable')
    return { data: 2 }
  })
  await page.fetchPendingJoinCount()
  assert.equal(page.pendingJoinCount.value, null)
  assert.equal(page.joinCountLoading.value, false)
  assert.match(page.joinCountLabel.value, /加载失败/)
  fails = false
  await page.fetchPendingJoinCount()
  assert.equal(page.pendingJoinCount.value, 2)
  assert.equal(page.joinCountLabel.value, '入场申请，2条待审核')
})
