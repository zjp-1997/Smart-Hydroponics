const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/service/expert_list.vue', 'utf8')
const api = readFileSync('api/expertList.js', 'utf8')

assert.match(api, /consultable: expert\.consultable === true/)
assert.match(page, /'consult-button--disabled': !expert\.consultable/)
assert.match(page, /if \(!expert\.consultable\)[\s\S]*?该专家当前不可咨询[\s\S]*?return[\s\S]*?uni\.navigateTo/)
