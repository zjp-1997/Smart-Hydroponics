const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.join(__dirname, '..')
const pagesConfig = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')
const iconNames = ['home', 'message', 'map', 'profile']

for (const name of iconNames) {
	for (const suffix of ['', '-active']) {
		const relativePath = `static/tabbar/${name}${suffix}.png`
		assert.ok(pagesConfig.includes(`"${suffix ? 'selectedIconPath' : 'iconPath'}": "${relativePath}"`))
		const image = fs.readFileSync(path.join(root, relativePath))
		assert.deepEqual([...image.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10])
	}
}
