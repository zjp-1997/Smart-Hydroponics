import { get, getBaseUrl, post, resolveFileUrl, uploadFile } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

const LEGACY_EXPERT_ASSET_PREFIX = 'smart_farm_expert_asset:'

function normalizeExpertChatResponse(response) {
	return {
		// sessionId 用于后续连续发送消息时复用同一个咨询会话。
		sessionId: response.sessionId,
		messageId: response.messageId,
		expertId: response.expertId,
		expertName: response.expertName || '',
		content: response.content || '',
		mediaUrl: resolveFileUrl(response.mediaUrl),
		createTime: String(response.createTime || '').replace('T', ' ')
	}
}

// 用户向专家发送咨询消息；expertId 为空时后端会自动选择第一位可咨询专家。
export function sendExpertChatMessage({ expertId, sessionId, content, messageType = 1, mediaUrl } = {}) {
	return post('/smart_plant/client/expert-chat/send', {
		expertId,
		sessionId,
		content,
		messageType,
		mediaUrl
	}).then((data) => normalizeExpertChatResponse(data || {}))
}

/** 先上传本人选中的聊天附件，再将返回的 mediaUrl 放入消息发送请求。 */
export const uploadExpertChatAttachment = (filePath, kind, displayName) =>
  uploadFile({ url: '/smart_plant/client/expert-chat/attachments', filePath, formData: { kind, displayName } })

function formatChatTime(time) {
	if (!time) {
		return ''
	}
	const normalized = String(time).replace('T', ' ')
	const date = new Date(normalized.replace(/-/g, '/'))
	if (Number.isNaN(date.getTime())) {
		return normalized
	}
	const now = new Date()
	const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
	const targetDay = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
	const dayDiff = Math.floor((today - targetDay) / 86400000)
	const hour = String(date.getHours()).padStart(2, '0')
	const minute = String(date.getMinutes()).padStart(2, '0')

	if (dayDiff === 0) {
		return `${hour}:${minute}`
	}
	if (dayDiff === 1) {
		return '昨天'
	}
	if (dayDiff > 1 && dayDiff < 7) {
		return `${dayDiff}天前`
	}
	return `${date.getMonth() + 1}月${date.getDate()}日`
}

function normalizeUnreadCount(value) {
	const count = Number(value || 0)
	// 后端未读数可能来自数据库聚合，前端统一兜底为非负整数，避免页面展示 NaN 或负数。
	return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

function normalizeExpertAvatar(avatar) {
	// 旧版本管理端可能保存 localStorage key，移动端无法读取，统一回退默认头像。
	if (!avatar || String(avatar).startsWith(LEGACY_EXPERT_ASSET_PREFIX)) {
		return '/static/avatar.png'
	}
	return resolveFileUrl(avatar) || '/static/avatar.png'
}

function normalizeSession(session) {
	const lastContent = session.lastMessageContent || '暂无聊天内容'
	return {
		// news.vue 保持原有 id/title/content/time/unread/avatar 字段，页面 UI 不需要调整。
		id: session.sessionId,
		sessionId: session.sessionId,
		expertId: session.expertId,
		title: session.expertName || '未知专家',
		content: lastContent,
		time: formatChatTime(session.chatTime),
		unread: normalizeUnreadCount(session.unreadCount),
		avatar: normalizeExpertAvatar(session.expertAvatar)
	}
}

// 获取当前用户与专家的聊天会话列表；后端已在列表接口中合并返回 unreadCount，避免额外请求未读数量接口。
export function getExpertChatSessions() {
	// request.js 已由本模块和其他接口静态引用，直接复用同一 GET 方法即可。
	return get('/smart_plant/client/expert-chat/sessions').then((data) => {
		const sessions = Array.isArray(data) ? data : []
		return sessions.map(normalizeSession).filter((session) => session.id && session.expertId)
	})
}

function formatFullChatTime(time) {
	if (!time) {
		return ''
	}
	const normalized = String(time).replace('T', ' ')
	const date = new Date(normalized.replace(/-/g, '/'))
	if (Number.isNaN(date.getTime())) {
		return normalized
	}
	return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function normalizeChatMessage(message) {
	const mediaTextMap = {
		2: '[图片]',
		3: '[语音]',
		4: '[文件]'
	}
	return {
		// 聊天页沿用原有 id/role/content 字段，保证页面结构和样式不需要改动。
		id: message.messageId,
		role: message.role === 'user' ? 'user' : 'expert',
		content: message.content || mediaTextMap[message.messageType] || '',
		messageType: message.messageType,
		mediaUrl: resolveFileUrl(message.mediaUrl),
		createTime: String(message.createTime || '').replace('T', ' ')
	}
}

function normalizeChatDetail(detail) {
	const messages = Array.isArray(detail.messages) ? detail.messages : []
	return {
		// sessionId 为空表示当前专家暂无历史会话，首次发送消息时由后端创建。
		sessionId: detail.sessionId || '',
		expertId: detail.expertId || '',
		expertName: detail.expertName || '',
		expertAvatar: normalizeExpertAvatar(detail.expertAvatar),
		chatTime: formatFullChatTime(detail.chatTime),
		// 历史媒体消息即使没有文字内容也必须保留，按 mediaUrl 渲染图片或文件。
		messages: messages.map(normalizeChatMessage).filter((message) => message.id && (message.content || message.mediaUrl))
	}
}

// 查询当前用户与指定专家的聊天详情，统一专家列表入口和消息列表入口的历史消息来源。
export function getExpertChatDetail({ expertId, sessionId } = {}) {
	const params = {}
	if (expertId) {
		params.expertId = expertId
	}
	if (sessionId) {
		params.sessionId = sessionId
	}
	// 与会话列表共用静态导入的 GET 方法，保持原有 Promise 返回值与错误处理。
	return get('/smart_plant/client/expert-chat/detail', params).then((data) => normalizeChatDetail(data || {}))
}

// 构建专家咨询 WebSocket 地址；token 通过 query 传递，兼容 uni-app 多端 WebSocket Header 差异。
export function buildExpertChatSocketUrl() {
	const token = getToken()
	if (!token) {
		return ''
	}
	const baseUrl = getBaseUrl()
	const wsBaseUrl = baseUrl.replace(/^https:/i, 'wss:').replace(/^http:/i, 'ws:')
	return `${wsBaseUrl}/smart_plant/ws/consult?token=${encodeURIComponent(token)}`
}

// 将 WebSocket 推送消息转换成聊天页原有消息结构，便于页面直接合并展示。
export function normalizeSocketChatMessage(message) {
	return normalizeChatMessage(message || {})
}
