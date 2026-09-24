const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/service/wareHouse.vue', 'utf8')
const api = readFileSync('api/warehouse.js', 'utf8')
const pagesConfig = readFileSync('pages.json', 'utf8')
const homePage = readFileSync('pages/index/index.vue', 'utf8')

// 仓库页应接入真实接口、保留卡片搜索和分类，并移除参考图顶部的两个操作图标。
assert.match(page, /getWarehouseOverview/)
// 仓库页重新显示时必须刷新接口数据，禁止只在首次加载时保留旧统计。
assert.match(page, /onShow\(\)\s*\{\s*this\.fetchWarehouse\(\)/)
assert.match(page, /class="search-box"/)
assert.match(page, /class="category-scroll"/)
assert.match(page, /:src="item\.image"/)
assert.doesNotMatch(page, /:src="item\.imageUrl"/)
// 仓库页顶部必须与农场管理页复用同一渐变、高度和内容起点。
assert.match(page, /min-height:\s*300rpx/)
assert.match(page, /linear-gradient\(180deg, rgba\(27, 162, 145, 0\.7\) 0%, rgba\(90, 184, 173, 0\) 100%\)/)
assert.match(page, /top:\s*calc\(var\(--status-bar-height\) \+ 136rpx\)/)
assert.doesNotMatch(page, /warehouse-nav[\s\S]*?icon-sousuo/)
assert.doesNotMatch(page, /warehouse-nav[\s\S]*?icon-jia/)

// API、页面注册和首页入口必须同时存在，防止页面实现后无法从应用进入。
assert.match(api, /\/smart_plant\/client\/warehouse\/overview/)
// 上传目录同样包含 warehouse-images，必须使用 startsWith 避免误替换真实设备图片。
assert.match(api, /imageUrl\.startsWith\('\/warehouse-images\/'\)/)
assert.doesNotMatch(api, /imageUrl\.indexOf\('\/warehouse-images\/'\)/)
// 上传图片应直接解析为后端静态资源 URL，避免 H5 将下载临时路径传给 uni-image 后显示空白。
assert.match(api, /return resolveFileUrl\(imageUrl\)/)
assert.doesNotMatch(api, /uni\.downloadFile\(/)
assert.match(pagesConfig, /pages\/service\/wareHouse/)
assert.match(homePage, /path: '\/pages\/service\/wareHouse'/)
