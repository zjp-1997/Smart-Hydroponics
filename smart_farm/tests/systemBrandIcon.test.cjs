const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')

test('左侧品牌区使用已保存的网站图标并保留默认回退', () => {
  const menu = readFileSync('src/components/LeftMenu.vue', 'utf8')

  assert.match(menu, /systemSetting\.faviconUrl \|\| systemSetting\.logoUrl/)
  assert.match(menu, /<img v-if="brandIconUrl"/)
  assert.match(menu, /<el-icon v-else>/)
  assert.match(menu, /\.brand-icon\s*\{[\s\S]*?background:\s*transparent;/)
})

test('顶部品牌Logo不附加背景颜色', () => {
  const header = readFileSync('src/components/Header.vue', 'utf8')

  assert.match(header, /\.header-logo\s*\{[\s\S]*?background:\s*transparent;/)
})
