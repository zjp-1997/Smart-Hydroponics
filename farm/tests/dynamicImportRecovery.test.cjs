const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const main = readFileSync(join(__dirname, '..', 'main.js'), 'utf8')

assert.match(main, /Failed to fetch dynamically imported module/)
assert.match(main, /Date\.now\(\) - lastReload < 10000/)
assert.match(main, /window\.location\.reload\(\)/)
