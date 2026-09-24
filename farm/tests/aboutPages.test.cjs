const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

// 静态检查三个关于类页面、指定图标和路由入口，防止后续修改退回为空页面。
const projectRoot = path.resolve(__dirname, '..')
const read = (file) => fs.readFileSync(path.join(projectRoot, file), 'utf8')
const aboutPage = read('pages/secondPage/about/about_me.vue')
const helpPage = read('pages/secondPage/about/help.vue')
const technicalPage = read('pages/secondPage/about/technical.vue')
const pagesConfig = read('pages.json')
const minePage = read('pages/index/mine.vue')

for (const icon of ['icon-shuangyezi', 'icon-bazi1', 'icon-guanyuwomen1']) {
	assert.match(aboutPage, new RegExp(icon))
}
for (const icon of ['icon-changjianwentixiangguanwenti2', 'icon-fankui-tianchong']) {
	assert.match(helpPage, new RegExp(icon))
}
for (const icon of ['icon-dianhua', 'icon-fasongyoujian', 'icon-weixin', 'icon-shijian']) {
	assert.match(technicalPage, new RegExp(icon))
}
for (const route of ['about/about_me', 'about/help', 'about/technical']) {
	assert.match(pagesConfig, new RegExp(route))
	assert.match(minePage, new RegExp(route))
}

// 关于我们与技术支持页应直接铺满视口，不再使用会显示滚动条的 scroll-view。
assert.doesNotMatch(aboutPage, /<scroll-view/)
assert.doesNotMatch(technicalPage, /<scroll-view/)
assert.doesNotMatch(technicalPage, /TechnicalPage|\.\/technical \.vue/)
