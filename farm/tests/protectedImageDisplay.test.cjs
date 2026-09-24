const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.join(__dirname, '..')
const component = fs.readFileSync(path.join(root, 'componment/ProtectedImage.vue'), 'utf8')
const pagesConfig = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')
const farmList = fs.readFileSync(path.join(root, 'pages/service/farm_list.vue'), 'utf8')

function vueFiles(directory) {
	return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
		const target = path.join(directory, entry.name)
		return entry.isDirectory() ? vueFiles(target) : entry.name.endsWith('.vue') ? [target] : []
	})
}

assert.match(component, /downloadProtectedFile\(src\)/)
assert.match(component, /#ifdef APP-PLUS/)
assert.match(component, /:fade-show="false"/)
assert.match(pagesConfig, /"\^protected-image\$":\s*"@\/componment\/ProtectedImage\.vue"/)

for (const file of vueFiles(path.join(root, 'pages'))) {
	const source = fs.readFileSync(file, 'utf8')
	assert.doesNotMatch(source, /<image\b[^>]*:src=/s, `${path.relative(root, file)} 仍绕过了真机鉴权图片组件`)
}

assert.doesNotMatch(farmList, /最后更新|lastUpdated|refresh-button|list-meta/)
