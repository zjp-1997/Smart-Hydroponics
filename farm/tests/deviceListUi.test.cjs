const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/service/device_list.vue', 'utf8')
const api = readFileSync('api/deviceList.js', 'utf8')

// 顶部导航保持原样，下方按设计稿展示筛选卡、设备分类卡和可操作开关。
assert.match(page, /\.device-hero\s*\{[\s\S]*?min-height:\s*300rpx[\s\S]*?rgba\(27, 162, 145, 0\.7\)/)
assert.match(page, /class="filter-card"[\s\S]*?管理地块[\s\S]*?class="filter-controls"/)
assert.match(page, /openPlotManagement[\s\S]*?pages\/secondPage\/plot\/plot_list/)
assert.match(page, /role="switch"[\s\S]*?:aria-checked="device\.enabled"[\s\S]*?@tap="toggleDevice\(device\)"/)

for (const icon of [
	'icon-buguangdeng',
	'icon-icon-shebeishuliang-shuibengxitong',
	'icon-shexiangtou',
	'icon-a-47-wendu',
	'icon-line-094'
]) {
	assert.match(page, new RegExp(icon))
}

assert.match(api, /环境传感器/)
assert.match(api, /水质传感器/)
