import { get, request, resolveFileUrl, uploadFile } from '@/utils/request.js'

// 后端尚未维护作物图片时使用本地占位图，避免详情卡片布局跳动。
const DEFAULT_CROP_IMAGE = '/static/lecttue.png'

// farm 端只展示四类高频农事任务，前端图标与后端 taskType 保持稳定映射。
export const TASK_TYPE_OPTIONS = [
	{ label: '全部', value: '' },
	{ label: '灌溉', value: 1 },
	{ label: '施肥', value: 2 },
	{ label: '喷药', value: 3 },
	{ label: '采收', value: 4 }
]

export const MY_TASK_STATUS_OPTIONS = [
	{ label: '全部', value: '' },
	{ label: '未开始', value: 1 },
	{ label: '进行中', value: 2 },
	{ label: '已完成', value: 3 },
	{ label: '已逾期', value: 4 }
]

const TYPE_ICON_MAP = {
	1: 'icon-jiaoshuihu-',
	2: 'icon-shifei',
	3: 'icon-iconkaishixiaodu',
	4: 'icon-caishouguanli'
}

const TYPE_NAME_MAP = {
	1: '灌溉',
	2: '施肥',
	3: '喷药',
	4: '采收'
}

const STATUS_NAME_MAP = {
	1: '未开始',
	2: '进行中',
	3: '已完成',
	4: '已逾期',
	5: '已取消'
}

const ACTION_NAME_MAP = {
	0: '创建任务',
	1: '开始执行',
	2: '完成任务',
	3: '进度反馈',
	4: '优化建议'
}

function padNumber(value) {
	return String(value).padStart(2, '0')
}

function formatDate(value) {
	if (!value) {
		return ''
	}
	if (value && /^\d{4}-\d{2}-\d{2}/.test(String(value))) {
		// 后端 LocalDateTime 通常返回 yyyy-MM-ddTHH:mm:ss，这里统一展示到分钟。
		return String(value).replace('T', ' ').slice(0, 16)
	}
	const date = new Date(String(value).replace(/-/g, '/'))
	if (Number.isNaN(date.getTime())) {
		return ''
	}
	// 列表页统一展示截至日期和具体时间，方便用户判断最晚处理时机。
	return `${date.getFullYear()}-${padNumber(date.getMonth() + 1)}-${padNumber(date.getDate())} ${padNumber(date.getHours())}:${padNumber(date.getMinutes())}`
}

function normalizeDateSummary(item) {
	return {
		taskDate: item.taskDate,
		weekday: item.weekday || '-',
		dayText: item.dayText || '-',
		taskCount: Number(item.taskCount || 0)
	}
}

function normalizeTask(item) {
	const taskType = Number(item.taskType || 0)
	const status = Number(item.status || 0)
	return {
		id: item.id,
		plotId: item.plotId,
		plotName: item.plotName || '-',
		cropName: item.cropName || '暂无作物',
		cropImage: item.cropImage ? resolveFileUrl(item.cropImage) : DEFAULT_CROP_IMAGE,
		taskTitle: item.taskTitle || TYPE_NAME_MAP[taskType] || '农事任务',
		taskContent: item.taskContent || '',
		taskType,
		taskTypeName: item.taskTypeName || TYPE_NAME_MAP[taskType] || '其他',
		taskIcon: TYPE_ICON_MAP[taskType] || 'icon-renwu-',
		status,
		statusName: item.statusName || STATUS_NAME_MAP[status] || '未知',
		deadlineTime: item.deadlineTime,
		deadlineText: formatDate(item.deadlineTime),
		createTime: item.createTime,
		createTimeText: formatDate(item.createTime),
		actualStartTime: item.actualStartTime,
		actualStartText: formatDate(item.actualStartTime),
		actualEndTime: item.actualEndTime,
		actualEndText: formatDate(item.actualEndTime),
		executorName: item.executorName || '',
		completeRemark: item.completeRemark || '',
		overdue: item.overdue === true || item.overdue === 1,
		canExecute: item.canExecute === true || item.canExecute === 1 || status === 1 || status === 4,
		canSubmitProgress: item.canSubmitProgress === true || item.canSubmitProgress === 1 || status === 2,
		canComplete: item.canComplete === true || item.canComplete === 1 || status === 2
	}
}

function normalizeAttachmentUrls(value) {
	if (!value) {
		return []
	}
	try {
		const urls = Array.isArray(value) ? value : JSON.parse(value)
		return (Array.isArray(urls) ? urls : []).filter(Boolean).map(resolveFileUrl)
	} catch (error) {
		// 历史脏数据不应影响整条时间线展示，无法解析时按无附件处理。
		return []
	}
}

// 按任务主键读取详情；后端会校验任务是否属于当前登录用户。
export function getFarmTaskDetail(taskId) {
	return get(`/smart_plant/client/farm-tasks/${taskId}`).then((task) => normalizeTask(task || {}))
}

// 将后端执行事件转换为移动端时间线模型，页面无需处理空值和时间格式兼容。
function normalizeTaskRecord(item) {
	const actionType = Number(item.actionType || 0)
	const afterStatus = Number(item.afterStatus || 0)
	return {
		id: item.id,
		taskId: item.taskId,
		taskTitle: item.taskTitle || '农事任务',
		actionType,
		actionName: ACTION_NAME_MAP[actionType] || '任务操作',
		beforeStatus: Number(item.beforeStatus || 0),
		afterStatus,
		statusName: STATUS_NAME_MAP[afterStatus] || '',
		actionContent: item.actionContent || '',
		feedbackDetail: item.feedbackDetail || '',
		progressPercent: item.progressPercent == null ? null : Number(item.progressPercent),
		operatorName: item.operatorName || item.operatorNameSnapshot || '执行人员',
		sourceClient: item.sourceClient || '',
		attachmentUrls: normalizeAttachmentUrls(item.attachments),
		// 保留原始发生时间供列表排序，格式化文本只负责界面展示。
		executeTime: item.executeTime || item.createTime || '',
		executeTimeText: formatDate(item.executeTime || item.createTime)
	}
}

// 获取连续 7 天的农事任务数量摘要，供顶部日期滑动栏展示。
export function getTaskDateSummaries(startDate) {
	return get('/smart_plant/client/farm-tasks/dates', {
		startDate
	}).then((list) => {
		const summaries = Array.isArray(list) ? list : []
		return summaries.map(normalizeDateSummary)
	})
}

// 根据日期和任务类型查询农事任务列表；taskType 为空时表示查询全部。
export function getFarmTasks({ taskDate, taskType }) {
	const params = { taskDate }
	if (taskType !== '' && taskType !== undefined && taskType !== null) {
		params.taskType = taskType
	}
	return get('/smart_plant/client/farm-tasks/list', params).then((list) => {
		const tasks = Array.isArray(list) ? list : []
		return tasks.map(normalizeTask)
	})
}

// 查询当前登录用户的全部农事任务；状态筛选及逾期判定统一由服务端完成。
export function getMyFarmTasks(status) {
	const params = {}
	if (status !== '' && status !== undefined && status !== null) {
		params.status = status
	}
	return get('/smart_plant/client/farm-tasks/mine', params).then((list) => {
		return (Array.isArray(list) ? list : []).map(normalizeTask)
	})
}

// 查询当前地块的全部真实农事任务；后端负责用户权限过滤和截止时间排序。
export function getFarmTasksByPlot(plotId) {
	return get(`/smart_plant/client/farm-tasks/plot/${plotId}`).then((list) => {
		const tasks = Array.isArray(list) ? list : []
		return tasks.map(normalizeTask)
	})
}

// 获取首页农事任务状态统计；聚合在后端完成，避免移动端加载全部任务明细。
export function getFarmTaskStatistics() {
	return get('/smart_plant/client/farm-tasks/statistics').then((statistics) => ({
		pendingCount: Number(statistics && statistics.pendingCount || 0),
		runningCount: Number(statistics && statistics.runningCount || 0),
		completedCount: Number(statistics && statistics.completedCount || 0),
		overdueCount: Number(statistics && statistics.overdueCount || 0)
	}))
}

// 点击“执行任务”时调用后端状态流转接口，把未开始任务更新为进行中。
export function executeFarmTask(taskId, requestId) {
	return request({
		url: `/smart_plant/client/farm-tasks/${taskId}/actions/start`,
		method: 'POST',
		data: { requestId }
	}).then((task) => normalizeTask(task || {}))
}

// 查询任务完整执行过程，详情面板按发生时间正序展示。
export function getFarmTaskTimeline(taskId) {
	return get(`/smart_plant/client/farm-tasks/${taskId}/timeline`).then((records) => {
		return (Array.isArray(records) ? records : []).map(normalizeTaskRecord)
	})
}

// 一次获取指定地块下全部任务和全部操作人的完整事件流，避免客户端逐任务请求。
export function getPlotFarmTaskTimeline(plotId) {
	return get(`/smart_plant/client/farm-tasks/plot/${plotId}/timeline`).then((records) => {
		return (Array.isArray(records) ? records : []).map(normalizeTaskRecord)
	})
}

// 保存阶段性进度；后端仅追加不可变事件，不改变任务当前状态。
export function submitFarmTaskProgress(taskId, data) {
	return request({
		url: `/smart_plant/client/farm-tasks/${taskId}/actions/progress`,
		method: 'POST',
		data
	}).then((task) => normalizeTask(task || {}))
}

// 完成任务并写入实际完成时间和完成说明。
export function completeFarmTask(taskId, data) {
	return request({
		url: `/smart_plant/client/farm-tasks/${taskId}/actions/complete`,
		method: 'POST',
		data
	}).then((task) => normalizeTask(task || {}))
}

// 上传任务完成凭证，服务端会同时校验任务归属、状态、图片类型和大小。
export function uploadFarmTaskCompletionImage(taskId, filePath) {
	return uploadFile({
		url: `/smart_plant/client/farm-tasks/${taskId}/completion-image`,
		filePath,
		name: 'image'
	})
}
