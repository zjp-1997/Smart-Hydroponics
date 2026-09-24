import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../utils/deviceFilters.js', import.meta.url), 'utf8')
const { filterDeviceGroups } = await import(
	`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
)

const groups = [{
	title: '水泵',
	devices: [
		{ id: 1, cropName: '循环水泵', plotId: 2, plotName: '水培_02号' },
		{ id: 2, cropName: '灌溉水泵', plotId: 3, plotName: '土培_03号' }
	]
}]

assert.deepEqual(filterDeviceGroups(groups, '循环', 2)[0].devices.map((item) => item.id), [1])
assert.deepEqual(filterDeviceGroups(groups, '土培')[0].devices.map((item) => item.id), [2])
assert.equal(groups[0].devices.length, 2)
