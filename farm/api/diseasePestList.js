import { get, resolveFileUrl, uploadFile } from '@/utils/request.js'

/** 将后端病虫害数据转换成页面原有卡片所需字段。 */
function normalizeDiseasePest(item) {
	return {
		id: item.id,
		// coverImage 与 smart_farm 病虫害信息管理字段一致，相对路径在这里补齐服务端地址。
		image: resolveFileUrl(item.coverImage),
		name: item.name || '',
		symptom: item.symptom || ''
	}
}

/** 将详情接口数据规范化，页面只使用稳定的展示字段。 */
function normalizeDiseasePestDetail(item = {}) {
	// imageUrls 由后端 JSON 字段直接返回数组，并在 API 层统一补齐服务端地址。
	const detailImages = Array.isArray(item.imageUrls)
		? item.imageUrls.filter(Boolean).map(resolveFileUrl)
		: []

	return {
		id: item.id,
		// 图片相对路径统一在 API 层补齐，页面无需感知服务端部署地址。
		image: resolveFileUrl(item.coverImage),
		name: item.name || '',
		cropTypeName: item.cropTypeName || '通用作物',
		affectedCrops: item.affectedCrops || item.cropTypeName || '通用作物',
		type: Number(item.type),
		symptom: item.symptom || '暂无症状描述',
		cause: item.cause || '暂无发生原因说明',
		suitableStage: item.suitableStage || '暂无明确阶段',
		occurrencePeriod: item.occurrencePeriod || '暂无明确发生时期',
		livingHabits: item.livingHabits || '',
		suitableEnvironment: item.suitableEnvironment || '',
		transmissionRoute: item.transmissionRoute || '',
		imageUrls: detailImages,
		remark: item.remark || '',
		// controls 始终转换为数组，避免页面处理 null 分支。
		controls: Array.isArray(item.controls) ? item.controls.map((control) => ({
			id: control.id,
			controlType: Number(control.controlType),
			controlCategory: Number(control.controlCategory),
			method: control.method || '暂无具体措施',
			drugName: control.drugName || '',
			usageMethod: control.usageMethod || '',
			dosageSpec: control.dosageSpec || '',
			safetyIntervalDays: control.safetyIntervalDays == null ? null : Number(control.safetyIntervalDays),
			precautions: control.precautions || '',
			suitableStage: control.suitableStage || '',
			remark: control.remark || ''
		})) : []
	}
}

/**
 * 复用 smart_farm 病虫害信息管理使用的 smart_plant 分页接口；name 由后端执行名称模糊匹配。
 */
export function getDiseasePests(name = '') {
	return get('/smart_plant/disease-pest/list', {
		name: String(name).trim(),
		// farm 知识库列表只展示后台已启用的条目。
		status: 1,
		pageNum: 1,
		// farm 当前页面没有分页 UI，一次读取病虫害信息管理中的全部数据。
		pageSize: 10000
	}).then((data) => {
		// smart_plant 使用 PageInfo 返回分页结果，真实数据位于 list 字段。
		const items = data && Array.isArray(data.list) ? data.list : []
		return items.map(normalizeDiseasePest).filter((item) => item.id && item.name)
	})
}

/** 根据病虫害主键读取用户端详情，响应中包含启用的防治措施。 */
export function getDiseasePestDetail(id) {
	return get(`/smart_plant/client/disease-pests/${encodeURIComponent(id)}`)
		.then(normalizeDiseasePestDetail)
}

/** 查询启用的作物类型，供病虫害上报弹框选择。 */
export function getDiseaseCropTypes() {
	return get('/smart_plant/crop-type/list', {
		status: 1,
		pageNum: 1,
		pageSize: 1000
	}).then((data) => {
		const items = data && Array.isArray(data.list) ? data.list : []
		return items
			.map((item) => ({ id: item.id, name: item.typeName || '' }))
			.filter((item) => item.id && item.name)
	})
}

/** 上传病虫害图片及表单字段，smart_plant 会统一完成图片和知识库数据入库。 */
export function uploadDiseasePest({ imagePath, ...formData }) {
	return uploadFile({
		url: '/smart_plant/client/disease-pests',
		filePath: imagePath,
		name: 'image',
		formData
	}).then(normalizeDiseasePest)
}
