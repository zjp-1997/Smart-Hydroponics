function dateText(task) {
	return String(task && task.deadlineTime || '').replace(' ', 'T')
}

function localToday() {
	const date = new Date()
	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')
	return `${date.getFullYear()}-${month}-${day}`
}

export function sortFarmTasksByDate(tasks, today = localToday()) {
	const rank = (value) => !value ? 3 : value.slice(0, 10) === today ? 0 : value < today ? 1 : 2
	return [...tasks].sort((left, right) => {
		const leftDate = dateText(left)
		const rightDate = dateText(right)
		const rankDiff = rank(leftDate) - rank(rightDate)
		if (rankDiff) return rankDiff
		return rank(leftDate) === 1
			? rightDate.localeCompare(leftDate)
			: leftDate.localeCompare(rightDate)
	})
}

// 地块农事时间序列只按截止时间排序；无截止时间的数据固定放在列表末尾。
export function sortTasksByDeadline(tasks, ascending = false) {
	return [...tasks].sort((left, right) => {
		const leftDate = dateText(left)
		const rightDate = dateText(right)
		if (!leftDate || !rightDate) return leftDate ? -1 : rightDate ? 1 : 0
		return ascending
			? leftDate.localeCompare(rightDate)
			: rightDate.localeCompare(leftDate)
	})
}

// 操作记录按真实发生时间排序，缺少时间的历史记录固定放到列表末尾。
export function sortRecordsByTime(records, ascending = false) {
	const executeTime = (record) => String(record && (record.executeTime || record.executeTimeText) || '').replace(' ', 'T')
	return [...records].sort((left, right) => {
		const leftTime = executeTime(left)
		const rightTime = executeTime(right)
		if (!leftTime || !rightTime) return leftTime ? -1 : rightTime ? 1 : 0
		return ascending
			? leftTime.localeCompare(rightTime)
			: rightTime.localeCompare(leftTime)
	})
}

export function nextTaskScrollTop(current, taskCount, rowHeight, viewportHeight) {
	const maxScrollTop = Math.max(taskCount * rowHeight - viewportHeight, 0)
	return Number(current || 0) >= maxScrollTop - 1
		? 0
		: Math.min(Number(current || 0) + rowHeight, maxScrollTop)
}
