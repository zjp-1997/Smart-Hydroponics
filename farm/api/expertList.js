import { get, resolveFileUrl } from '@/utils/request.js'

// 默认专家标签，兼容后台历史数据中擅长方向为空的情况。
const DEFAULT_TAGS = ['资深专家', '农学专家']

// 专家列表接口地址。项目 context-path 为 /smart_plant，请求层会补齐后端域名。
const EXPERT_LIST_URL = '/smart_plant/client/experts/list'
const LEGACY_EXPERT_ASSET_PREFIX = 'smart_farm_expert_asset:'

function formatScore(rating) {
	const numericRating = Number(rating)
	if (Number.isNaN(numericRating)) {
		return '0.0'
	}
	// 页面设计中推荐分保留一位小数，这里统一格式化，避免模板里混入展示逻辑。
	return numericRating.toFixed(1)
}

function normalizeTags(specialty, jobTitle) {
	const specialtyTags = String(specialty || '')
		.split(/[、,，\s]+/)
		.map((tag) => tag.trim())
		.filter(Boolean)

	const tags = [...specialtyTags, jobTitle].filter(Boolean)
	const uniqueTags = Array.from(new Set(tags))
	return uniqueTags.length ? uniqueTags.slice(0, 2) : DEFAULT_TAGS
}

function normalizeAvatarUrl(avatar) {
	// 旧版本管理端曾保存 localStorage key，farm 端无法读取这种本地缓存，直接回退默认头像。
	if (!avatar || String(avatar).startsWith(LEGACY_EXPERT_ASSET_PREFIX)) {
		return '/static/avatar.png'
	}
	return resolveFileUrl(avatar) || '/static/avatar.png'
}

function normalizeExpert(expert) {
	const unit = expert.unit || '未知机构'
	const jobTitle = expert.jobTitle || '农学专家'

	return {
		id: expert.id,
		name: expert.name || '未知专家',
		avatar: normalizeAvatarUrl(expert.avatar),
		// 当前 expert_list.vue 只渲染 unit 字段，为不改 UI，这里合并机构和职称。
		unit: `${unit}${jobTitle}`,
		tags: normalizeTags(expert.specialty, jobTitle),
		desc: expert.introduction || '暂无专家简介',
		score: formatScore(expert.rating),
		consultable: expert.consultable === true
	}
}

// 查询用户端全部专家列表，咨询状态由 consultable 字段控制。
export function getExpertList(keyword = '') {
	const params = keyword ? { keyword } : {}
	return get(EXPERT_LIST_URL, params).then((list) => {
		const experts = Array.isArray(list) ? list : []
		return experts.map(normalizeExpert)
	})
}
