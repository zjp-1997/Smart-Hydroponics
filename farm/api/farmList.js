import { get, resolveFileUrl } from '@/utils/request.js'

// 用户端农场列表接口。后端会根据 token 中的用户 ID 做数据隔离，前端不传 userId。
export function getFarmList() {
	return get('/smart_plant/client/farms/list').then((list) => {
		const farms = Array.isArray(list) ? list : []
		return farms.map((farm) => ({
			id: farm.id,
			farmName: farm.farmName || '未命名农场',
			plotCount: farm.plotCount || 0,
			plantArea: farm.plantArea || '-',
			address: farm.address || '-',
			// 后端可能返回完整 URL 或 /uploads 相对地址，这里统一转换为 image 可访问地址。
			image: resolveFileUrl(farm.imgUrl)
		}))
	})
}
