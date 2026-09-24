import { get, put } from '@/utils/request.js'

const DEVICE_GROUP_MAP = [
	{ title: '补光灯', key: 'growLightList' },
	{ title: '水泵', key: 'pumpList' },
	{ title: '摄像头', key: 'cameraList' },
	{ title: '环境传感器', key: 'environmentList' },
	{ title: '水质传感器', key: 'waterQualityList' }
]

function normalizeDevice(device) {
	return {
		id: device.id,
		// 当前 device_list.vue 的第一列字段名是 cropName，为了不改 UI，这里映射为设备名称。
		cropName: device.deviceName || '未知设备',
		plotId: device.plotId,
		plotName: device.plotName || '-',
		status: device.status || '离线',
		enabled: !!device.enabled,
		source: device.source || 'iot',
		updating: false
	}
}

function normalizeDeviceGroups(data) {
	// 后端按固定业务类别返回列表，前端在这里转换成页面 v-for 需要的分组数组。
	return DEVICE_GROUP_MAP.map((group) => {
		const devices = Array.isArray(data && data[group.key]) ? data[group.key] : []
		return {
			title: group.title,
			devices: devices.map(normalizeDevice)
		}
	})
}

// 查询当前登录用户的设备分组列表。用户身份由 token 确定，前端不传 userId。
export function getDeviceList() {
	return get('/smart_plant/client/devices/list').then((data) => normalizeDeviceGroups(data || {}))
}

// 切换设备在线状态；source 用于区分普通设备表和独立摄像头表中可能重复的主键。
export function updateDeviceOnlineStatus(device, enabled) {
	const source = encodeURIComponent(device.source || 'iot')
	return put(`/smart_plant/client/devices/${source}/${device.id}/online-status`, {
		onlineStatus: enabled ? 1 : 0
	}).then(normalizeDevice)
}
