const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/index/index.vue', 'utf8')

for (const tone of ['teal', 'blue', 'amber', 'green', 'red', 'purple', 'cyan', 'orange']) {
	assert.match(page, new RegExp(`tone: '${tone}'`))
	assert.match(page, new RegExp(`\\.service-tone-${tone} \\{`))
}

assert.match(page, /service-icon-wrap" :class="`service-tone-\$\{service\.tone\}`"/)
assert.match(page, /background-color: var\(--service-icon-bg\)/)
assert.match(page, /color: var\(--service-icon-color\)/)
