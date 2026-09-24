const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const listPage = readFileSync('src/views/diseasePest/ListView.vue', 'utf8')
const formPage = readFileSync('src/views/diseasePest/AddOrUpdate.vue', 'utf8')
const api = readFileSync('src/api/diseasePest.ts', 'utf8')

// 病虫害主数据不再展示、编辑或提交动态危害等级。
assert.doesNotMatch(listPage, /severityLevel|severityTone|危害等级/)
assert.doesNotMatch(formPage, /severityLevel|severityOptions|危害等级/)
assert.doesNotMatch(api, /severityLevel/)
