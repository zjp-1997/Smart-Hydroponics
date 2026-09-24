const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const listPage = readFileSync('src/views/diseaseControl/ListView.vue', 'utf8')
const formPage = readFileSync('src/views/diseaseControl/AddOrUpdate.vue', 'utf8')

// 列表筛选和编辑弹框都必须支持数据库中的第 4 类生物防治措施。
assert.match(listPage, /label: '生物防治', value: 4/)
assert.match(formPage, /label: '生物防治', value: 4/)
