const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '..')
const pagesJson = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')
const pagePaths = [...pagesJson.matchAll(/"path"\s*:\s*"(pages\/[^\"]+)"/g)].map((match) => match[1])

const pagesWithoutTopNavigation = new Set([
	'pages/login/code_index',
	'pages/login/pwd_index',
	'pages/register/index',
	'pages/index/index',
	'pages/index/mine',
	'pages/expert/mine',
	'pages/technician/mine'
])

test('every enabled pull refresh uses the farm management feedback UI', () => {
	const globalStyle = pagesJson.slice(pagesJson.indexOf('"globalStyle"'))
	assert.match(globalStyle, /"enablePullDownRefresh"\s*:\s*false/)
	assert.doesNotMatch(pagesJson.slice(0, pagesJson.indexOf('"globalStyle"')), /"enablePullDownRefresh"\s*:\s*true/)

	let refreshPageCount = 0
	for (const pagePath of pagePaths) {
		const source = fs.readFileSync(path.join(root, `${pagePath}.vue`), 'utf8')
		if (!source.includes('refresher-enabled')) continue
		refreshPageCount++
		assert.match(source, /<scroll-view\b[^>]*\bscroll-y\b[^>]*\brefresher-enabled\b[^>]*>/s, pagePath)
		assert.match(source, /@refresherrefresh="\$handlePullDownRefresh"/, pagePath)
		assert.match(source, /refresher-default-style="none"/, pagePath)
		assert.match(source, /refresher-background="#f7f7f7"/, pagePath)
		assert.match(source, /:refresher-threshold="56"/, pagePath)
		assert.match(source, /<farm-pull-refresh slot="refresher" :refreshing="pullRefreshing"/, pagePath)
	}
	assert.equal(refreshPageCount, 43)
})

test('the shared refresh component owns the approved feedback appearance', () => {
	const component = fs.readFileSync(path.join(root, 'componment/FarmPullRefresh.vue'), 'utf8')
	assert.match(pagesJson, /"\^farm-pull-refresh\$"\s*:\s*"@\/componment\/FarmPullRefresh\.vue"/)
	assert.match(component, /class="pull-refresh-feedback farm-pull-refresh"/)
	assert.match(component, /class="pull-refresh-feedback__spinner"/)
	assert.match(component, /正在刷新\$\{label\}…/)
	assert.match(component, /\.farm-pull-refresh\s*\{[\s\S]*height:\s*112rpx;[\s\S]*linear-gradient\(180deg/)
	assert.match(component, /\.pull-refresh-feedback__spinner\s*\{[\s\S]*border-top-color:\s*#1ba291;/)
})

test('all existing top navigation bars share the message page design contract', () => {
	const app = fs.readFileSync(path.join(root, 'App.vue'), 'utf8')
	for (const pagePath of pagePaths) {
		if (pagesWithoutTopNavigation.has(pagePath)) continue
		const source = fs.readFileSync(path.join(root, `${pagePath}.vue`), 'utf8')
		assert.match(source, /app-nav-surface/, pagePath)
		assert.match(source, /app-nav-row/, pagePath)
		assert.match(source, /app-nav-title/, pagePath)
	}
	assert.match(app, /\.app-nav-surface\s*\{[\s\S]*background:\s*transparent\s*!important;/)
	assert.match(app, /\.app-nav-surface::before\s*\{[\s\S]*height:\s*calc\(var\(--status-bar-height\) \+ 88rpx\);[\s\S]*linear-gradient\(110deg, #60cbba 0%, #57c5b4 50%, #4ebfad 100%\)/)
	assert.match(app, /\.app-nav-row\s*\{[\s\S]*height:\s*58rpx\s*!important;/)
	assert.match(app, /\.app-nav-title\s*\{[\s\S]*font-size:\s*16px\s*!important;/)
})

test('farm management keeps the approved card hierarchy and shared UI primitives', () => {
	const source = fs.readFileSync(path.join(root, 'pages/service/farm_list.vue'), 'utf8')
	assert.match(source, /<farm-pull-refresh[^>]*label="农场数据"/)
	assert.match(source, /class="page-hero app-nav-surface"/)
	assert.match(source, /class="navbar app-nav-row"/)
	assert.match(source, /class="nav-title app-nav-title"/)
	assert.match(source, /:show-scrollbar="false"/)
	assert.match(source, /class="farm-content-inner"/)
	assert.match(source, /class="farm-section-title">\{\{ isOrdinaryUser \? '农场信息' : '我的农场' \}\}/)
	assert.match(source, /class="farm-total">共 \{\{ farms\.length \}\} 个农场/)
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
