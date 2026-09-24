const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/secondPage/plot/plot_list.vue', 'utf8')
const api = readFileSync('api/plotList.js', 'utf8')

// 筛选顺序、展示文案和图标必须与地块列表设计要求保持一致。
assert.match(page, /全部[\s\S]*?种植中[\s\S]*?待采收[\s\S]*?空闲中/)
assert.doesNotMatch(page, /已采收/)
assert.match(page, /icon-dingwei1[\s\S]*?地块面积/)
assert.match(page, /icon-rili[\s\S]*?种植时间/)
assert.match(page, /icon-a-44tubiao-41[\s\S]*?预采时间/)
assert.match(page, /icon--ss-yezi[\s\S]*?预估产量/)
assert.match(page, /icon-shishijiankong monitor-icon/)

// 选中态与种植状态徽标复用 farm 现有主色，不使用高饱和独立绿色。
assert.match(page, /\.status-chip\.active[\s\S]*?background-color:\s*#6FC4BA/)
assert.match(page, /\.status-badge[\s\S]*?color:\s*#267F74;[\s\S]*?background-color:\s*#E7F5F2/)
assert.doesNotMatch(page, /#0dbd91|#0aa879/)

// 状态直接消费 smart_plant 返回字段，前端不得根据日期另建业务口径。
assert.match(api, /plantingStatus:\s*plot\.plantingStatus/)
assert.match(api, /plantingStatusName:\s*plot\.plantingStatusName/)

// 顶部渐变颜色和高度属于禁止修改区域。
assert.match(page, /min-height:\s*300rpx/)
assert.match(page, /linear-gradient\(180deg, rgba\(27, 162, 145, 0\.7\) 0%, rgba\(90, 184, 173, 0\) 100%\)/)
