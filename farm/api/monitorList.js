import { get, post, resolveFileUrl } from '@/utils/request.js'

/** 保留 HTTP/RTSP 等完整媒体地址，相对上传路径则补齐 smart_plant 服务地址。 */
function resolveMonitorUrl(url) {
	if (!url) {
		return ''
	}
	return /^[a-z][a-z\d+.-]*:\/\//i.test(url) ? url : resolveFileUrl(url)
}

/** 把后端监控对象转换为现有监控列表 UI 使用的字段，页面结构无需调整。 */
function normalizeMonitor(monitor) {
	const online = Number(monitor && monitor.onlineStatus) === 1
	const coverUrl = resolveMonitorUrl(monitor && (monitor.coverUrl || monitor.snapshotUrl))
	return {
		id: monitor && monitor.id,
		deviceId: monitor && monitor.deviceId,
		plotId: monitor && monitor.plotId,
		plotName: (monitor && monitor.plotName) || '',
		cropName: (monitor && monitor.cropName) || '暂无作物',
		cameraName: (monitor && monitor.cameraName) || '未命名摄像头',
		status: online ? '在线' : '离线',
		onlineStatus: online ? 1 : 0,
		streamProtocol: (monitor && monitor.streamProtocol) || '',
		streamUrl: resolveMonitorUrl(monitor && monitor.streamUrl),
		snapshotUrl: resolveMonitorUrl(monitor && monitor.snapshotUrl),
		cover: coverUrl || '/static/m1.png',
		resolution: (monitor && monitor.resolution) || '',
		direction: (monitor && monitor.direction) || ''
	}
}

/** 查询当前登录用户的监控列表，plotId 可用于地块入口过滤。 */
export function getMonitorList({ plotId, keyword } = {}) {
	return get('/smart_plant/client/monitors', {
		plotId: plotId || undefined,
		keyword: keyword || undefined
	}).then((data) => (Array.isArray(data) ? data.map(normalizeMonitor) : []))
}

/** 查询实时监控详情和播放地址。 */
export function getMonitorDetail(id) {
	return get(`/smart_plant/client/monitors/${id}`).then(normalizeMonitor)
}

/** 向后端下发云台方向指令。 */
export function controlMonitorPtz(id, direction) {
	return post(`/smart_plant/client/monitors/${id}/ptz`, { direction })
}

