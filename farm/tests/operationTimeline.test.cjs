const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/secondPage/plot/operation_timeline.vue', 'utf8')
const api = readFileSync('api/taskList.js', 'utf8')

// 页面必须通过计算列表切换时间顺序，且按钮文案与下一次排序动作一致。
assert.match(page, /v-for="\(record, index\) in sortedRecords"/)
assert.match(page, /按时间顺序排列/)
assert.match(page, /按时间倒序排列/)
assert.match(page, /toggleTimeSort/)
assert.match(api, /executeTime:\s*item\.executeTime \|\| item\.createTime/)

// 点击凭证图片时，uni.previewImage 应接收该记录的全部有效附件。
assert.match(page, /@tap="previewRecordImages\(record\)"/)
assert.match(page, /uni\.previewImage\(\{ current: urls\[0\], urls \}\)/)
