const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const homePage = readFileSync('pages/index/index.vue', 'utf8')
const timelinePage = readFileSync('pages/secondPage/plot/task_timeline.vue', 'utf8')
const farmListPage = readFileSync('pages/service/farm_list.vue', 'utf8')
const plotListPage = readFileSync('pages/secondPage/plot/plot_list.vue', 'utf8')

// 时间序列页面必须显式隔离样式，首页卡片则保持原有上下结构。
assert.match(timelinePage, /<style scoped>/)
assert.match(homePage, /\.plot-card\s*\{[\s\S]*?display:\s*block;/)
assert.match(homePage, /\.task-card\s*\{[\s\S]*?display:\s*block;/)
// 首页常用服务末尾必须预留底部导航高度，避免最后一行卡片被遮挡。
assert.match(homePage, /class="home-bottom-spacer"/)
assert.match(homePage, /height:\s*calc\(58px \+ 24rpx \+ env\(safe-area-inset-bottom\)\)/)

// 农场管理保持原样；地块列表按设计稿保留左右卡片，并增加状态筛选和字段图标。
assert.match(farmListPage, /plot-count-badge/)
assert.match(farmListPage, /@tap="handleFarmDetail\(farm\)"/)
assert.match(plotListPage, /全部[\s\S]*?种植中[\s\S]*?待采收[\s\S]*?空闲中/)
assert.doesNotMatch(plotListPage, /已采收/)
assert.match(plotListPage, /icon-dingwei1[\s\S]*?地块面积/)
assert.match(plotListPage, /icon-rili[\s\S]*?种植时间/)
assert.match(plotListPage, /icon-a-44tubiao-41[\s\S]*?预采时间/)
assert.match(plotListPage, /icon--ss-yezi[\s\S]*?预估产量/)
// 顶部渐变色和高度属于禁止修改区域，必须保持原参数。
assert.match(plotListPage, /min-height:\s*300rpx/)
assert.match(plotListPage, /linear-gradient\(180deg, rgba\(27, 162, 145, 0\.7\) 0%, rgba\(90, 184, 173, 0\) 100%\)/)
assert.match(plotListPage, /\.plot-list-page \.plot-card\s*\{[\s\S]*?display:\s*flex;/)
assert.match(plotListPage, /@tap\.stop="handleMonitor\(plot\)"/)
assert.match(plotListPage, /icon-shishijiankong monitor-icon/)
