import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

// farm 未声明 ESM 包类型；通过 data URL 直接加载源码以运行原生 Node 断言。
const source = readFileSync(new URL('../utils/chatTime.js', import.meta.url), 'utf8')
const { chatTimeLabel } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)

// 固定当前日期验证微信式间隔和跨日期标签，避免测试依赖运行时钟。
const now = new Date(2026, 8, 20, 14, 30)
const today = new Date(2026, 8, 20, 14, 20)
assert.equal(chatTimeLabel(today, null, now), '14:20')
assert.equal(chatTimeLabel(new Date(2026, 8, 20, 14, 24), today, now), '')
assert.equal(chatTimeLabel(new Date(2026, 8, 20, 14, 25), today, now), '14:25')
assert.equal(chatTimeLabel(new Date(2026, 8, 20, 0, 2), new Date(2026, 8, 19, 23, 59), now), '00:02')
assert.equal(chatTimeLabel(new Date(2026, 8, 19, 23, 58), null, now), '昨天 23:58')
assert.equal(chatTimeLabel(new Date(2026, 8, 17, 11, 0), null, now), '星期四 11:00')
assert.equal(chatTimeLabel(new Date(2026, 8, 1, 8, 15), null, now), '9月1日 08:15')
assert.equal(chatTimeLabel(new Date(2025, 11, 31, 8, 15), null, now), '2025年12月31日 08:15')
assert.equal(chatTimeLabel('invalid', null, now), '')
