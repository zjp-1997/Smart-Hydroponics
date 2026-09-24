import { get, resolveFileUrl } from '@/utils/request.js'

// 仓库分类与 smart_plant 数据字典保持一致，页面只消费统一的显示名称和色彩标识。
export const WAREHOUSE_CATEGORIES = [
	{ value: null, label: '全部' },
	{ value: 1, label: '种子' },
	{ value: 2, label: '肥料' },
	{ value: 3, label: '农药' },
	{ value: 4, label: '工具' },
	{ value: 5, label: '设备耗材' },
	{ value: 6, label: '其他' }
]

const CATEGORY_LABELS = Object.fromEntries(
	WAREHOUSE_CATEGORIES.filter((item) => item.value !== null).map((item) => [item.value, item.label])
)

// 后台默认分类图位于 Web 管理端公共目录，移动端使用同名本地资源保证离线可展示。
function resolveWarehouseImage(imageUrl, category) {
	// 仅替换后台默认分类图；/uploads/warehouse-images/... 是 smart_farm 上传的真实图片，必须保留。
	if (!imageUrl || imageUrl.startsWith('/warehouse-images/')) {
		const localNames = { 1: 'seed', 2: 'fertilizer', 3: 'pesticide', 4: 'tool', 5: 'consumable', 6: 'other' }
		return `/static/warehouse-images/${localNames[category] || 'other'}.svg`
	}

	// 真实上传图片直接使用 smart_plant 的静态资源地址，避免临时下载路径无法被 uni-image 解析。
	return resolveFileUrl(imageUrl)
}

// 数量保留必要的小数位，避免接口 decimal 值在卡片中显示无意义的尾随零。
function formatQuantity(value) {
	const number = Number(value || 0)
	return Number.isFinite(number) ? number.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '0'
}

// 将后端领域对象转换为仓库卡片需要的轻量展示模型，避免页面耦合接口字段细节。
function normalizeWarehouseItem(item) {
	const stockQty = Number(item.stockQty || 0)
	const warningQty = Number(item.warningQty || 0)
	return {
		id: item.id,
		// 接收后端明确的农场主归属；当前仓库卡片 UI 无需改动。
		farmOwnerId: item.farmOwnerId ?? null,
		farmOwnerName: item.farmOwnerName || '',
		name: item.itemName || '未命名物资',
		code: item.itemCode || '',
		specification: item.specification || '暂无规格',
		category: Number(item.category || 6),
		categoryName: CATEGORY_LABELS[item.category] || '其他',
		stockQty,
		stockText: formatQuantity(stockQty),
		unit: item.unit || '件',
		image: resolveWarehouseImage(item.imageUrl, Number(item.category || 6)),
		isWarning: warningQty > 0 && stockQty <= warningQty,
		inboundDate: String(item.createTime || '').slice(0, 10) || '-'
	}
}

// 由 Bearer Token 确定身份：农场主及绑定的普通用户都只接收所属农场物资。
export function getWarehouseOverview({ keyword, category, pageNum = 1, pageSize = 20 } = {}) {
	return get('/smart_plant/client/warehouse/overview', {
		...(keyword ? { keyword } : {}), ...(category == null ? {} : { category }), pageNum, pageSize
	}).then((data) => {
		const source = data || {}
		return {
			totalStock: formatQuantity(source.stockQuantity),
			itemTypes: Number(source.totalItems || 0),
			todayInbound: formatQuantity(source.todayInboundQuantity),
			items: (Array.isArray(source.items) ? source.items : []).map(normalizeWarehouseItem),
			hasNextPage: Boolean(source.hasNextPage)
		}
	})
}

/** 读取当前用户可见的仓库流水，记录类型 1 为入库、2 为出库。 */
export function getWarehouseRecords({ recordType, pageNum = 1, pageSize = 10 } = {}) {
	return get('/smart_plant/client/warehouse/records', {
		// “全部”不传类型参数，避免某些真机网络栈把 null 序列化为字符串。
		...(recordType == null ? {} : { recordType }), pageNum, pageSize
	}).then((data) => {
		const page = data || {}
		return {
			// 后端分页对象的 list 是当前页数据，hasNextPage 避免前端猜测总页数。
			items: (Array.isArray(page.list) ? page.list : []).map((record) => ({
				id: record.id,
				itemName: record.itemName || '未命名物资',
				itemCode: record.itemCode || '',
				itemUnit: record.itemUnit || '件',
				recordType: Number(record.recordType),
				quantity: formatQuantity(record.quantity),
				afterQty: formatQuantity(record.afterQty),
				operator: record.operatorName || record.nickname || record.username || '系统记录',
				recipient: record.recipient || '',
				recordTime: String(record.recordTime || record.createTime || '').replace('T', ' ').slice(0, 16),
				remark: record.remark || ''
			})),
			hasNextPage: Boolean(page.hasNextPage)
		}
	})
}
