const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/index/index.vue', 'utf8')
const api = readFileSync('api/homeWeather.js', 'utf8')

assert.match(page, /onShow\(\)[\s\S]*?this\.fetchHomeWeather\(\)/)
assert.match(page, /\['冻雨'\][\s\S]*?icon-yujiaxue/)
assert.match(page, /matchedRule \? matchedRule\.art\(\) : singleLayer\('icon-yintian'/)
assert.doesNotMatch(page, /matchedRule \? matchedRule\.art\(\) : singleLayer\('icon-qing'/)
assert.match(api, /location: weather\.location \|\| '当前位置'/)
