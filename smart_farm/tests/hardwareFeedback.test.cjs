const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const vm = require('node:vm')
const ts = require('typescript')

// 执行真实状态映射模块，验证 UI 不会把待下发或失败误报为成功。
const source = readFileSync('src/utils/hardwareFeedback.ts', 'utf8')
const exportsObject = {}
vm.runInNewContext(ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText, { exports: exportsObject })

test('硬件反馈区分待下发、下发中、设备确认和执行失败', () => {
  assert.deepEqual(
    JSON.parse(JSON.stringify(exportsObject.getHardwareApplyFeedback(0))),
    { text: '指令待下发', type: 'warning' },
  )
  assert.equal(exportsObject.getHardwareApplyFeedback(1).text, '指令下发中')
  assert.equal(exportsObject.getHardwareApplyFeedback(2).text, '设备已确认')
  assert.equal(exportsObject.getHardwareApplyFeedback(3, '设备离线').text, '执行失败：设备离线')
  assert.equal(exportsObject.getConfigurationSaveFeedback(0).text, '配置已保存，指令待下发')
})
