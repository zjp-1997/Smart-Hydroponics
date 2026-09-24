const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const root = path.resolve(__dirname, '..')
const listPage = fs.readFileSync(path.join(root, 'pages/service/monitor_list.vue'), 'utf8')
const detailPage = fs.readFileSync(path.join(root, 'pages/secondPage/monitor/detail.vue'), 'utf8')
const monitorApi = fs.readFileSync(path.join(root, 'api/monitorList.js'), 'utf8')
const pagesJson = fs.readFileSync(path.join(root, 'pages.json'), 'utf8')

test('monitor list keeps its existing classes while loading backend data', () => {
	assert.match(listPage, /class="monitor-card"/)
	assert.match(listPage, /getMonitorList/)
	assert.match(listPage, /openMonitor\(monitor\)/)
	assert.doesNotMatch(listPage, /01号摄像头/)
})

test('monitor detail uses the required iconfont PTZ directions', () => {
	for (const icon of ['icon-shang', 'icon-xiala', 'icon-zuo', 'icon-icon-you']) {
		assert.match(detailPage, new RegExp(`class="iconfont ${icon} ptz-icon"`))
	}
	assert.match(detailPage, /controlMonitorPtz/)
	assert.match(detailPage, /requestFullScreen/)
})

test('monitor routes and client endpoints are connected', () => {
	assert.match(pagesJson, /pages\/secondPage\/monitor\/detail/)
	assert.match(monitorApi, /\/smart_plant\/client\/monitors/)
})
