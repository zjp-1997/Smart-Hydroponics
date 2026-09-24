import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../utils/taskScroll.js', import.meta.url), 'utf8')
const { nextTaskScrollTop, sortFarmTasksByDate, sortTasksByDeadline, sortRecordsByTime } = await import(
	`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`
)

const ordered = sortFarmTasksByDate([
	{ id: 1, deadlineTime: '2026-09-10T08:00:00' },
	{ id: 2, deadlineTime: '2026-09-08T08:00:00' },
	{ id: 3, deadlineTime: '2026-09-09T08:00:00' },
	{ id: 4, deadlineTime: '2026-09-07T08:00:00' }
], '2026-09-09')

assert.deepEqual(ordered.map((task) => task.id), [3, 2, 4, 1])

const timelineTasks = [
	{ id: 1, deadlineTime: '2026-09-06T08:00:00' },
	{ id: 2, deadlineTime: '2026-09-07T08:00:00' },
	{ id: 3, deadlineTime: '2026-09-08T08:00:00' },
	{ id: 4, deadlineTime: '2026-09-09T08:00:00' },
	{ id: 5, deadlineTime: '2026-09-12T08:00:00' }
]
// 默认倒序展示最新截止日期；点击切换后按正序展示。
assert.deepEqual(sortTasksByDeadline(timelineTasks).map((task) => task.id), [5, 4, 3, 2, 1])
assert.deepEqual(sortTasksByDeadline(timelineTasks, true).map((task) => task.id), [1, 2, 3, 4, 5])

const operationRecords = [
	{ id: 1, executeTime: '2026-09-04T09:00:00' },
	{ id: 2, executeTime: '2026-09-08T09:00:00' },
	{ id: 3, executeTime: '2026-09-12T09:00:00' }
]
assert.deepEqual(sortRecordsByTime(operationRecords, true).map((record) => record.id), [1, 2, 3])
assert.deepEqual(sortRecordsByTime(operationRecords).map((record) => record.id), [3, 2, 1])
assert.equal(nextTaskScrollTop(0, 5, 44, 176), 44)
assert.equal(nextTaskScrollTop(44, 5, 44, 176), 0)
