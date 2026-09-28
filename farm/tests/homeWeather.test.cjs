const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/index/index.vue', 'utf8')
const api = readFileSync('api/homeWeather.js', 'utf8')

const greetingSource = page.match(/function getTimeGreeting\([\s\S]*?\n\}/)?.[0]
assert.ok(greetingSource)
const getTimeGreeting = new Function(`${greetingSource}; return getTimeGreeting`)()

assert.equal(getTimeGreeting(0), '凌晨好')
assert.equal(getTimeGreeting(6), '早上好')
assert.equal(getTimeGreeting(12), '中午好')
assert.equal(getTimeGreeting(14), '下午好')
assert.equal(getTimeGreeting(18), '晚上好')
assert.match(page, /<view class="hello">\{\{ greeting \}\}<\/view>/)
assert.match(page, /onShow\(\)[\s\S]*?this\.greeting = getTimeGreeting\(\)/)
assert.match(page, /onShow\(\)[\s\S]*?this\.fetchHomeWeather\(\)/)
assert.match(page, /\['冻雨'\][\s\S]*?icon-yujiaxue/)
assert.match(page, /matchedRule \? matchedRule\.art\(\) : singleLayer\('icon-yintian'/)
assert.doesNotMatch(page, /matchedRule \? matchedRule\.art\(\) : singleLayer\('icon-qing'/)
assert.match(api, /location: weather\.location \|\| '当前位置'/)
