const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const root = join(__dirname, '..')
const app = readFileSync(join(root, 'App.vue'), 'utf8')
const pages = readFileSync(join(root, 'pages.json'), 'utf8')

assert.match(app, /button::after\s*\{[\s\S]*display:\s*none;[\s\S]*border:\s*0;/)
assert.match(app, /button,\s*\n\s*input,\s*\n\s*textarea\s*\{[\s\S]*font-family:\s*inherit;/)
assert.match(app, /view,[\s\S]*scroll-view,[\s\S]*box-sizing:\s*border-box;/)
assert.match(pages, /"backgroundColorContent":\s*"#F7F7F7"/)
