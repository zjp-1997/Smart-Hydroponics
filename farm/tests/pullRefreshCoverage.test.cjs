const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '..')
const pagesJson = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')
const pagePaths = [...pagesJson.matchAll(/"path"\s*:\s*"(pages\/[^"]+)"/g)].map((match) => match[1])

test('every registered page has pull refresh and internal scrollers expose refresh feedback', () => {
	assert.match(pagesJson, /"enablePullDownRefresh"\s*:\s*true/)
	assert.ok(pagePaths.length > 0)

	for (const pagePath of pagePaths) {
		const source = fs.readFileSync(path.join(root, `${pagePath}.vue`), 'utf8')
		if (/<scroll-view\b[^>]*\bscroll-y\b[^>]*>/s.test(source)) {
			assert.match(source, /<scroll-view\b[^>]*\brefresher-enabled\b[^>]*>/s, pagePath)
			assert.match(source, /@refresherrefresh="\$handlePullDownRefresh"/, pagePath)
		} else {
			const start = pagesJson.indexOf(`"path": "${pagePath}"`)
			const next = pagesJson.indexOf('"path":', start + 1)
			const pageConfig = pagesJson.slice(start, next < 0 ? pagesJson.length : next)
			assert.doesNotMatch(pageConfig, /"disableScroll"\s*:\s*true/, pagePath)
		}
	}
})

test('Vue 2 and Vue 3 both register the shared refresh lifecycle', () => {
	const main = fs.readFileSync(path.join(root, 'main.js'), 'utf8')
	assert.match(main, /Vue\.mixin\(pullRefreshMixin\)/)
	assert.match(main, /app\.mixin\(pullRefreshMixin\)/)
})
