const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const listPage = readFileSync('pages/service/disease_control.vue', 'utf8')
const detailPage = readFileSync('pages/secondPage/disease/detail.vue', 'utf8')
const api = readFileSync('api/diseasePestList.js', 'utf8')
const pagesConfig = readFileSync('pages.json', 'utf8')
const diseaseListPage = readFileSync('pages/service/disease_control.vue', 'utf8')

// 列表卡片必须使用数据库主键跳转，不在路由中携带可变的详情数据。
assert.match(listPage, /handleDisease\(item\)[\s\S]*?pages\/secondPage\/disease\/detail\?id=\$\{encodeURIComponent\(item\.id\)\}/)

// 详情页必须注册在 pages.json，并使用 farm 统一的自定义导航栏。
assert.match(pagesConfig, /"path":\s*"pages\/secondPage\/disease\/detail"[\s\S]*?"navigationStyle":\s*"custom"/)

// API 必须调用 farm 专用的 RESTful 详情接口，并复用统一图片地址解析。
assert.match(api, /getDiseasePestDetail\(id\)/)
assert.match(api, /client\/disease-pests\/\$\{encodeURIComponent\(id\)\}/)
assert.match(api, /image:\s*resolveFileUrl\(item\.coverImage\)/)
assert.match(api, /imageUrls:\s*detailImages/)
assert.match(api, /controlCategory:\s*Number\(control\.controlCategory\)/)

// 页面覆盖新主数据字段、JSON 图片、防治分类、推荐药剂以及加载和失败重试状态。
assert.match(detailPage, /getDiseasePestDetail\(this\.diseaseId\)/)
assert.match(detailPage, /症状识别/)
assert.match(detailPage, /发生规律/)
assert.match(detailPage, /防治方法/)
assert.match(detailPage, /推荐药剂/)
assert.match(detailPage, /v-for="\(image, index\) in galleryImages"/)
assert.match(detailPage, /v-for="group in controlGroups"/)
assert.match(detailPage, /category: 4, title: '生物防治'/)
assert.match(detailPage, /detail \? detail\.name : '病虫害详情'/)
assert.match(detailPage, /v-if="loading"/)
assert.match(detailPage, /v-else-if="errorMessage"/)
assert.match(detailPage, /class="retry-button"/)

// 参考图的核心层级必须存在：顶部双操作、重叠摘要、独立防治卡和药剂安全提示。
assert.match(detailPage, /static\/disease-detail\/heart\.svg/)
assert.match(detailPage, /open-type="share"/)
assert.match(detailPage, /class="summary-card"/)
assert.match(detailPage, /class="control-card"/)
assert.match(detailPage, /class="medicine-notice"/)
assert.doesNotMatch(detailPage, /<section-card title="防治方法">/)

// 顶部图标操作保留 44pt 触控区，正文与系统手势区域之间保留安全距离。
assert.match(detailPage, /\.nav-action\s*\{[\s\S]*?width:\s*88rpx;[\s\S]*?height:\s*88rpx;/)

// 详情页顶部导航必须复用病虫害防治页的同一条 300rpx 渐变背景。
const navGradient = /linear-gradient\(180deg, rgba\(27, 162, 145, 0\.7\) 0%, rgba\(90, 184, 173, 0\) 100%\)/
assert.match(diseaseListPage, navGradient)
assert.match(detailPage, navGradient)
assert.match(detailPage, /background-size:\s*100% 300rpx/)

// 主内容底部必须预留系统手势安全区，避免末尾防治方案被遮挡。
assert.match(detailPage, /safe-area-inset-bottom/)
