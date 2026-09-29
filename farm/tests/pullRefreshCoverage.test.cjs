const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '..')
const pagesJson = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')
const pagePaths = [...pagesJson.matchAll(/"path"\s*:\s*"(pages\/[^"]+)"/g)].map((match) => match[1])

function pageConfig(pagePath) {
	const start = pagesJson.indexOf(`"path": "${pagePath}"`)
	const next = pagesJson.indexOf('"path":', start + 1)
	return pagesJson.slice(start, next < 0 ? pagesJson.indexOf('"globalStyle"', start) : next)
}

test('every page uses exactly one pull refresh mechanism', () => {
	const globalStyle = pagesJson.slice(pagesJson.indexOf('"globalStyle"'))
	assert.match(globalStyle, /"enablePullDownRefresh"\s*:\s*false/)
	assert.ok(pagePaths.length > 0)

	for (const pagePath of pagePaths) {
		const source = fs.readFileSync(path.join(root, `${pagePath}.vue`), 'utf8')
		if (/<scroll-view\b[^>]*\bscroll-y\b[^>]*>/s.test(source)) {
			assert.match(source, /<scroll-view\b[^>]*\brefresher-enabled\b[^>]*>/s, pagePath)
			assert.match(source, /@refresherrefresh="\$handlePullDownRefresh"/, pagePath)
			assert.match(source, /refresher-background="#F3F8F6"/, pagePath)
			assert.doesNotMatch(pageConfig(pagePath), /"enablePullDownRefresh"\s*:\s*true/, pagePath)
		} else {
			assert.match(pageConfig(pagePath), /"enablePullDownRefresh"\s*:\s*true/, pagePath)
		}
	}
})

test('refresh props and H5 feedback use safe types and the farm theme', () => {
	const vueSources = pagePaths.map((pagePath) => fs.readFileSync(path.join(root, `${pagePath}.vue`), 'utf8')).join('\n')
	const app = fs.readFileSync(path.join(root, 'App.vue'), 'utf8')
	assert.doesNotMatch(vueSources, /(?<!:)refresher-threshold="\d+"/)
	assert.match(app, /\.uni-scroll-view-refresh__spinner\s*\{\s*color:\s*#1ba291;/)
})

test('Vue 2 and Vue 3 both register the shared refresh lifecycle', () => {
	const main = fs.readFileSync(path.join(root, 'main.js'), 'utf8')
	const mixin = fs.readFileSync(path.join(root, 'utils/pullRefresh.js'), 'utf8')
	assert.match(main, /Vue\.mixin\(pullRefreshMixin\)/)
	assert.match(main, /app\.mixin\(pullRefreshMixin\)/)
	assert.match(mixin, /onPullDownRefresh\(\)\s*\{\s*return this\.\$handlePullDownRefresh\(\)/)
	assert.match(mixin, /onPageRefresh/)
	assert.match(mixin, /onShow/)
	assert.match(mixin, /onLoad/)
})
