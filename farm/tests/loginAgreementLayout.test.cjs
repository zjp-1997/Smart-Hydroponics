const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const root = join(__dirname, '..', 'pages', 'login')

for (const file of ['pwd_index.vue', 'code_index.vue']) {
	const page = readFileSync(join(root, file), 'utf8')
	assert.match(page, /:class="\{ 'agreement-agreed': agreed \}"/)
	assert.match(page, /\.agreement \{[\s\S]*position: fixed;[\s\S]*env\(safe-area-inset-bottom\)/)
	assert.match(page, /\.agreement\.agreement-agreed \.agreement-prefix,[\s\S]*color: #1ba291;/)
}
