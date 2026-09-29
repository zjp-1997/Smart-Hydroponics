const assert = require('node:assert/strict')
const fs = require('node:fs')

const page = fs.readFileSync('pages/index/news.vue', 'utf8')

assert.match(page, /\.page-hero\s*\{[\s\S]*border-radius:\s*0;[\s\S]*linear-gradient\(110deg, #60cbba 0%, #57c5b4 50%, #4ebfad 100%\);/)
assert.match(page, /class="message-card system-card"/)
assert.match(page, /v-if="memberMessages\.length" class="message-card conversation-card"/)
assert.match(page, /v-if="messages\.length" class="message-card conversation-card"/)
assert.match(page, /\.message-card\s*\{[\s\S]*border-radius:\s*24rpx;[\s\S]*box-shadow:/)
assert.match(page, /\.message-item\s*\{[\s\S]*min-height:\s*140rpx;/)
assert.match(page, /hover-class="message-item-pressed"/)
