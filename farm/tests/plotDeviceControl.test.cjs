const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/secondPage/plot/detail.vue', 'utf8')
const api = readFileSync('api/plotList.js', 'utf8')

assert.match(api, /client\/devices\/\$\{encodeURIComponent\(deviceId\)\}\/control-status/)
assert.match(api, /controlStatus:\s*enabled \? 1 : 0/)
assert.match(page, /await updatePlotDeviceControlStatus\(device\.id, targetEnabled\)/)
assert.match(page, /device\.enabled = targetEnabled/)
assert.match(page, /if \(this\.plot\.idle \|\| device\.updating\) return/)
