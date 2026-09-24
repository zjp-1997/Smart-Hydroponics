import { get, put, resolveFileUrl } from '@/utils/request.js'
import { sortFarmTasksByDate } from '@/utils/taskScroll.js'

// 本地兜底图只在后端暂未维护 cropImage 时使用，保证列表 UI 不会出现空白图片区域。
const DEFAULT_CROP_IMAGE = '/static/lecttue.png'
const LEGACY_IMAGE_PREFIXES = ['smart_farm_crop_image:', 'smart_farm_planting_batch_crop_image:']

function normalizeCropImage(imageUrl) {
	if (!imageUrl || LEGACY_IMAGE_PREFIXES.some((prefix) => String(imageUrl).startsWith(prefix))) {
		return DEFAULT_CROP_IMAGE
	}
	return resolveFileUrl(imageUrl)
}

function normalizePlot(plot) {
	// 空闲状态以服务端结果为准，列表只保留地块名称和面积作为业务信息。
	const idle = plot.plantingStatus === 'IDLE'
	return {
		id: plot.id,
		farmId: plot.farmId,
		farmName: plot.farmName || '-',
		plotName: plot.plotName || '-',
		plotCode: plot.plotCode || '-',
		cropName: idle ? '-' : plot.cropName || '-',
		plantingDays: plot.plantingDays || 0,
		growthStageName: plot.growthStageName || '暂无阶段',
		// 状态由 smart_plant 按当前有效批次和预采日期统一判定，前端只负责展示与筛选。
		plantingStatus: plot.plantingStatus || 'IDLE',
		plantingStatusName: plot.plantingStatusName || '空闲中',
		plotArea: plot.plotArea || '-',
		plantingTime: idle ? '-' : plot.plantingTime || '-',
		harvestTime: idle ? '-' : plot.harvestTime || '-',
		estimatedYield: idle ? '-' : plot.estimatedYield || '-',
		image: normalizeCropImage(plot.cropImage)
	}
}

function formatDateTime(value) {
	if (!value) {
		return '-'
	}
	// 后端 LocalDateTime 可能返回 ISO 字符串，紧凑任务列表按设计稿展示截至日期。
	return String(value).replace('T', ' ').slice(0, 10)
}

function resolveDeviceIcon(device) {
	const typeCode = device.typeCode || ''
	const name = device.name || ''
	if (typeCode === 'GROW_LIGHT' || name.indexOf('灯') > -1) {
		return 'icon-dengpao'
	}
	if (typeCode === 'WATER_PUMP' || name.indexOf('泵') > -1) {
		return 'icon-reshuibeng'
	}
	// 地块详情页设备图标按当前 iconfont.css 统一映射，避免后端设备名称直接影响 UI 表现。
	if (typeCode === 'FAN' || /fan|风机/i.test(name)) {
		return 'icon-fengji'
	}
	if (typeCode === 'SOIL_SENSOR' || /soil|土壤/i.test(name)) {
		return 'icon-tushi_turangshangqingyi'
	}
	if (typeCode === 'ENV_SENSOR' || /environment|环境/i.test(name)) {
		return 'icon-huanjingjiance'
	}
	if (typeCode === 'WATER_QUALITY' || /water quality|水质|检测仪/i.test(name)) {
		return 'icon-shuizhijianceyi'
	}
	return 'icon-shishijiankong'
}

function resolveDeviceStateClass(state) {
	if (state === '在线') {
		return 'online'
	}
	if (state === '故障') {
		return 'error'
	}
	return 'offline'
}

function buildGrowthStages(detail) {
	const defaultStages = ['发芽期', '幼苗期', '莲座期', '成熟期']
	const currentStageName = detail.growthStageName || ''
	const matchedIndex = defaultStages.indexOf(currentStageName)
	const currentIndex = matchedIndex > -1 ? matchedIndex : -1

	// 当前 UI 已固定为阶段进度条，前端只根据后端当前阶段计算 active 状态，不改动页面结构。
	return defaultStages.map((name, index) => ({
		name,
		active: currentIndex > -1 ? index <= currentIndex : false
	}))
}

function normalizePlotDetail(detail) {
	const idle = detail.idle === true
	const monitorItems = Array.isArray(detail.monitorItems) ? detail.monitorItems : []
	const tasks = sortFarmTasksByDate(Array.isArray(detail.farmTasks) ? detail.farmTasks : [])
	const devices = Array.isArray(detail.devices) ? detail.devices : []

	return {
		// plot 字段名称沿用 detail.vue 现有模板，避免改动页面 UI。
		plot: {
			id: detail.id,
			idle,
			// 编辑表单使用原始数值和预计日期，展示文本仍沿用当前页面格式。
			cropId: detail.cropId,
			areaValue: detail.plotAreaValue,
			areaUnit: detail.plotAreaUnit || '亩',
			expectedHarvestAt: detail.expectedHarvestAt || '',
			name: idle ? '-' : detail.cropName || '-',
			farm: detail.plotName || '-',
			area: detail.plotArea || '-',
			rack: idle ? '-' : detail.plantingTime || '-',
			crop: idle ? '-' : detail.harvestTime || '-',
			plantDays: idle ? '-' : (detail.plantingDays ?? 0),
			growthStageName: idle ? '-' : detail.growthStageName || '-',
			harvestable: !idle && detail.harvestable === true,
			image: normalizeCropImage(detail.plotImage)
		},
		envItems: monitorItems.map((item) => ({
			label: item.label || '-',
			value: item.value || '-'
		})),
		stages: idle ? [] : buildGrowthStages(detail),
		// 今天优先，其次为由近及远的历史任务，未来任务最后；序号表示当前展示顺序。
		tasks: (idle ? [] : tasks).map((item, index) => ({
			id: item.id,
			sequence: index + 1,
			title: item.taskTitle || '农事任务',
			status: item.statusName || '未知',
			deadline: formatDateTime(item.deadlineTime)
		})),
		devices: devices.map((item) => ({
			id: item.id,
			name: item.name || '未知设备',
			state: idle ? '离线' : item.state || '离线',
			stateClass: idle ? 'offline' : resolveDeviceStateClass(item.state),
			icon: resolveDeviceIcon(item),
			enabled: !idle && !!item.enabled
		}))
	}
}

// 查询当前登录用户的全部地块。后端会从 token 中识别用户身份，前端不传 userId。
export function getAllPlotList() {
	return get('/smart_plant/client/plots/list').then((list) => {
		const plots = Array.isArray(list) ? list : []
		return plots.map(normalizePlot)
	})
}

// 查询当前登录用户某个农场下的地块列表，后端会校验该农场是否属于当前用户。
export function getPlotListByFarmId(farmId) {
	return get(`/smart_plant/client/farms/${farmId}/plots`).then((list) => {
		const plots = Array.isArray(list) ? list : []
		return plots.map(normalizePlot)
	})
}

// 查询地块详情。后端会校验地块是否属于当前登录用户，前端只传业务主键。
export function getPlotDetail(plotId) {
	return get(`/smart_plant/client/plots/${plotId}`).then((detail) => normalizePlotDetail(detail || {}))
}

// 编辑弹框只展示当前农场主可用的作物；保存由后端再次校验角色和地块归属。
export function getEditableCrops() {
	return get('/smart_plant/client/plots/crops')
}

export function updatePlotBasicInfo(plotId, data) {
	return put(`/smart_plant/client/plots/${plotId}`, data)
}

export function harvestPlot(plotId, data) {
	return put(`/smart_plant/client/plots/${plotId}/harvest`, data)
}
