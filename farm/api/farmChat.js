import { get, getBaseUrl, post, resolveFileUrl, uploadFile } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

const DEFAULT_AVATAR = '/static/avatar.png'

// 将后端时间转换为消息列表使用的简短时间，异常格式直接原样展示。
function formatListTime(value) {
	if (!value) return ''
	const normalized = String(value).replace('T', ' ')
	const date = new Date(normalized.replace(/-/g, '/'))
	if (Number.isNaN(date.getTime())) return normalized
	const now = new Date()
	const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
	const target = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
	const days = Math.floor((today - target) / 86400000)
	if (days === 0) return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
	if (days === 1) return '昨天'
	if (days > 1 && days < 7) return `${days}天前`
	return `${date.getMonth() + 1}月${date.getDate()}日`
}

function normalizeAvatar(value) {
	return resolveFileUrl(value) || DEFAULT_AVATAR
}

// 会话接口也会返回尚未产生消息的绑定联系人，便于三类账号直接发起沟通。
function normalizeSession(item = {}) {
	const unread = Number(item.unreadCount || 0)
	return {
		sessionId: item.sessionId || '',
		peerUserId: item.peerUserId,
		name: item.peerName || '未命名用户',
		avatar: normalizeAvatar(item.peerAvatar),
		roleCode: String(item.peerRoleCode || '').toLowerCase(),
		content: item.lastMessageContent || '点击开始沟通',
		time: formatListTime(item.chatTime),
		unread: Number.isFinite(unread) && unread > 0 ? Math.floor(unread) : 0,
	}
}

function normalizeMessage(item = {}) {
	const mediaText = { 2: '[图片]', 3: '[语音]', 4: '[文件]' }
	return {
		id: item.messageId,
		role: item.role === 'self' ? 'self' : 'peer',
		messageType: Number(item.messageType || 1),
		content: item.content || mediaText[item.messageType] || '',
		mediaUrl: resolveFileUrl(item.mediaUrl),
		createTime: String(item.createTime || '').replace('T', ' '),
	}
}

/** 查询当前账号按服务端绑定关系允许联系的人员。 */
export function getFarmChatSessions() {
	return get('/smart_plant/client/farm-chat/sessions').then(data =>
		(Array.isArray(data) ? data : []).map(normalizeSession).filter(item => item.peerUserId))
}

/** 使用已有会话或联系人读取详情；进入详情后后端同步清空当前账号未读数。 */
export function getFarmChatDetail({ sessionId, peerUserId, beforeId, afterId, pageSize = 30 } = {}) {
	const params = {}
	if (sessionId) params.sessionId = sessionId
	if (peerUserId) params.peerUserId = peerUserId
	if (beforeId) params.beforeId = beforeId
	if (afterId) params.afterId = afterId
	params.pageSize = pageSize
	return get('/smart_plant/client/farm-chat/detail', params).then((data = {}) => ({
		sessionId: data.sessionId || '',
		peerUserId: data.peerUserId || '',
		peerName: data.peerName || '聊天',
		peerAvatar: normalizeAvatar(data.peerAvatar),
		peerRoleCode: String(data.peerRoleCode || '').toLowerCase(),
		messages: (Array.isArray(data.messages) ? data.messages : []).map(normalizeMessage),
		hasMoreBefore: Boolean(data.hasMoreBefore),
		hasMoreAfter: Boolean(data.hasMoreAfter),
	}))
}

/** 发送人由后端登录态确定，前端只提交会话或联系人以及消息正文。 */
export function sendFarmChatMessage(payload = {}) {
	return post('/smart_plant/client/farm-chat/send', payload)
}

/** 复用统一上传器携带 Bearer Token，返回的相对地址随后写入聊天消息。 */
export const uploadFarmChatAttachment = (filePath, kind, displayName) =>
	uploadFile({ url: '/smart_plant/client/farm-chat/attachments', filePath, formData: { kind, displayName } })

/** 通用聊天连接与专家咨询连接分开命名，但服务端共享同一鉴权和在线连接池。 */
export function buildFarmChatSocketUrl() {
	const token = getToken()
	if (!token) return ''
	return `${getBaseUrl().replace(/^https:/i, 'wss:').replace(/^http:/i, 'ws:')}/smart_plant/ws/chat?token=${encodeURIComponent(token)}`
}

// WebSocket 事件与历史消息归一化成同一结构，页面按 messageId 去重。
export function normalizeFarmChatSocketMessage(message) {
	return normalizeMessage(message || {})
}
