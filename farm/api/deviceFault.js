import { get, post, resolveFileUrl, uploadFile } from '@/utils/request.js'

// 故障处理页的状态筛选项与后端状态值保持一致。
export const DEVICE_FAULT_STATUS_OPTIONS = [
	{ label: '全部', value: '' },
	{ label: '待处理', value: 0 },
	{ label: '处理中', value: 1 },
	{ label: '已处理', value: 2 }
]

const STATUS_NAME_MAP = { 0: '待处理', 1: '处理中', 2: '已处理', 3: '已关闭' }
const SEVERITY_NAME_MAP = { 1: '一般', 2: '重要', 3: '严重' }

// 统一时间显示格式，兼容后端返回的 ISO 日期字符串。
function formatDateTime(value) {
	if (!value) return '-'
	return String(value).replace('T', ' ').slice(0, 16)
}

// 将接口字段补充为页面可直接渲染的展示模型。
function normalizeFault(fault) {
	return {
		...fault,
		faultName: fault.faultName || '设备故障',
		deviceName: fault.deviceName || '未知设备',
		plotName: fault.plotName || '未关联地块',
		handleUserName: fault.handleUserName || '待指派',
		statusName: STATUS_NAME_MAP[fault.status] || '未知状态',
		severityName: SEVERITY_NAME_MAP[fault.severity] || '一般',
		startTimeText: formatDateTime(fault.startTime),
		handleTimeText: formatDateTime(fault.handleTime),
		endTimeText: formatDateTime(fault.endTime)
	}
}

// 查询当前用户有权查看的故障；身份和数据范围由登录 token 决定。
export function getDeviceFaults(status = '') {
	const params = status === '' ? {} : { status }
	return get('/smart_plant/client/device-faults/list', params)
		.then((data) => (Array.isArray(data) ? data : []).map(normalizeFault))
}

// 技术人员工作台读取本人全部故障，后端按处理进度及发生时间完成排序。
export function getTechnicianFaults() {
	return get('/smart_plant/client/device-faults/technician/tasks')
		.then((data) => (Array.isArray(data) ? data : []).map(normalizeFault))
}

// 我的处理记录只读取本人已完成的故障，详情仍复用维护时间轴接口。
export function getTechnicianCompletedFaults() {
	return get('/smart_plant/client/device-faults/technician/completed')
		.then((data) => (Array.isArray(data) ? data : []).map(normalizeFault))
}

// 维护详情与时间轴由同一接口返回；图片地址在 API 层转换为 uni-image 可用的地址。
export function getDeviceFaultRecord(id) {
	return get(`/smart_plant/client/device-faults/${id}/record`).then((data) => ({
		fault: normalizeFault(data.fault || {}),
		records: (Array.isArray(data.records) ? data.records : []).map((record) => ({
			...record,
			executeTimeText: formatDateTime(record.executeTime),
			imageUrl: record.imageUrl ? resolveFileUrl(record.imageUrl) : ''
		}))
	}))
}

// 点击“处理故障”后接单，后端将状态从待处理更新为处理中。
export function acceptDeviceFault(id) {
	return post(`/smart_plant/client/device-faults/${id}/actions/accept`)
}

// 上传必填的现场图片，服务端负责校验故障归属、类型和大小。
export function uploadDeviceFaultCompletionImage(id, filePath) {
	return uploadFile({ url: `/smart_plant/client/device-faults/${id}/completion-image`, filePath, name: 'image' })
}

// 携带图片地址和选填说明完成故障，状态由服务端更新为已处理。
export function completeDeviceFault(id, handleResult, completionImageUrl) {
	return post(`/smart_plant/client/device-faults/${id}/actions/complete`, { handleResult, completionImageUrl })
}
