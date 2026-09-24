import { get, post, resolveFileUrl, uploadFile } from '@/utils/request.js'

function normalizeModel(model) {
	return {
		// id 是发送消息时提交给后端的模型主键，modelName/model 只用于前端展示或兜底判断。
		id: model.id,
		modelName: model.modelName || '',
		model: model.model || ''
	}
}

function normalizeChatResponse(response) {
	return {
		// 后端返回的对话主键可用于后续定位咨询记录，页面展示仍保持现有消息气泡样式。
		id: response.chatId || response.id,
		modelId: response.modelId,
		modelName: response.modelName || '',
		model: response.model || '',
		imageUrl: resolveFileUrl(response.imageUrl),
		fileUrl: resolveFileUrl(response.fileUrl),
		fileName: response.fileName || '',
		userContent: response.userContent || '',
		aiContent: response.aiContent || '',
		createTime: String(response.createTime || '').replace('T', ' ')
	}
}

// 查询后端已启用模型，AI咨询发送时优先使用第一个可用模型。
export function getEnabledModels() {
	return get('/smart_plant/client/models/list').then((data) => {
		const models = Array.isArray(data) ? data : []
		return models.map(normalizeModel).filter((model) => model.id)
	})
}

// 向模型发送用户消息；modelId 可为空，后端会自动选择一个启用模型作为默认模型。
export function sendModelMessage({ modelId, content, imageUrl, fileUrl, fileName } = {}) {
	return post('/smart_plant/client/ai-chat/send', {
		modelId,
		content,
		imageUrl,
		fileUrl,
		fileName
	// DeepSeek 推理与文档分析可超过普通接口 15 秒，单独放宽客户端等待时间。
	}, { timeout: 90000 }).then((data) => normalizeChatResponse(data || {}))
}

/** 按会话记录主键向前翻页，只读取后端按当前登录用户过滤的成功 AI 咨询。 */
export function getModelHistory({ beforeId, pageSize = 50 } = {}) {
	return get('/smart_plant/client/ai-chat/history', { beforeId, pageSize }).then((data) => {
		const history = Array.isArray(data) ? data : []
		return history.map(normalizeChatResponse).filter((item) => item.id)
	})
}

/** AI 咨询附件先传后端，模型调用只接收服务端校验过的本人文件地址。 */
export const uploadModelAttachment = (filePath, kind, displayName) =>
	uploadFile({ url: '/smart_plant/client/ai-chat/attachments', filePath, formData: { kind, displayName } })
