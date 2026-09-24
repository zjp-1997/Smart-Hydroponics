import { get, request } from '@/utils/request.js'

// 移动端使用个人消息入口，用户ID由登录态确定，不接受前端指定接收人。
const base = '/smart_plant/client/maintenance-msg'

function normalizeMessage(value) {
	const message = value && typeof value === 'object' ? value : {}

	return {
		...message,
		id: message.id == null ? message.messageId : message.id,
		title: message.title || '设备维护通知',
		content: message.content || '',
		plotName: message.plotName || '未关联地块',
		publisherName: message.publisherName || '系统',
		sendTime: message.sendTime || message.createTime || '',
		readCount: Number(message.readCount || 0)
	}
}

function normalizePage(value) {
	const page = value && typeof value === 'object' ? value : {}
	const source = Array.isArray(page.list)
		? page.list
		: (Array.isArray(page.records) ? page.records : [])
	const list = source.filter((item) => item && typeof item === 'object').map(normalizeMessage)

	return {
		...page,
		list,
		total: Number.isFinite(Number(page.total)) ? Number(page.total) : list.length
	}
}

export const listMaintenanceMessages = (pageNum = 1) =>
	get(`${base}/list`, { pageNum, pageSize: 20 }).then(normalizePage)
export const getMaintenanceStatistics = () => get(`${base}/statistics`)
// 消息ID与管理端一致，阅读同步到同一条notification接收明细。
export const markMaintenanceRead = (id) => request({ url: `${base}/${id}/read`, method: 'PUT' })
