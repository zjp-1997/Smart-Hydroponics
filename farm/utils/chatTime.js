const FIVE_MINUTES = 5 * 60 * 1000
const WEEK = 7 * 24 * 60 * 60 * 1000
const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 后端时间可能是 ISO 或空格分隔格式，统一按设备本地时间解析。 */
function parseTime(value) {
  if (!value) return null
  const date = value instanceof Date ? value : new Date(String(value).replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

function sameDay(a, b) {
  return a.getFullYear() === b.getFullYear()
    && a.getMonth() === b.getMonth()
    && a.getDate() === b.getDate()
}

function clock(date) {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

/** 微信式时间分组：首条、跨天或间隔至少五分钟时才显示居中的时间标签。 */
export function chatTimeLabel(currentValue, previousValue, nowValue = new Date()) {
  const current = parseTime(currentValue)
  if (!current) return ''
  const previous = parseTime(previousValue)
  if (previous && sameDay(current, previous)
    && current.getTime() >= previous.getTime()
    && current.getTime() - previous.getTime() < FIVE_MINUTES) return ''

  const now = parseTime(nowValue) || new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1)
  const time = clock(current)
  if (sameDay(current, now)) return time
  if (sameDay(current, yesterday)) return `昨天 ${time}`
  if (current < today && today.getTime() - current.getTime() < WEEK) {
    return `${WEEKDAYS[current.getDay()]} ${time}`
  }
  const date = `${current.getMonth() + 1}月${current.getDate()}日 ${time}`
  return current.getFullYear() === now.getFullYear() ? date : `${current.getFullYear()}年${date}`
}
