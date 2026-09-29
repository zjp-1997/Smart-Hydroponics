const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const page = readFileSync(join(__dirname, '..', 'pages', 'login', 'pwd_index.vue'), 'utf8')

assert.match(page, /<form class="form"/)
assert.doesNotMatch(page, /<component\b[^>]*\b:is=/)
