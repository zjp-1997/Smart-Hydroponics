const assert = require('node:assert/strict')
const fs = require('node:fs')

const page = fs.readFileSync('pages/index/news.vue', 'utf8')

assert.match(page, /class="message-card system-card"/)
assert.match(page, /v-if="memberMessages\.length" class="message-card conversation-card"/)
assert.match(page, /v-if="messages\.length" class="message-card conversation-card"/)
assert.match(page, /\.message-card\s*\{[\s\S]*border-radius:\s*24rpx;[\s\S]*box-shadow:/)
assert.match(page, /\.message-item\s*\{[\s\S]*min-height:\s*140rpx;/)
assert.match(page, /hover-class="message-item-pressed"/)
