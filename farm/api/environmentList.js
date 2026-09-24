import { get } from '@/utils/request.js'

function isEmpty(value) {
	return value === null || value === undefined || value === ''
}

function formatValue(value, unit = '') {
	if (isEmpty(value)) {
		return '--'
	}
	return `${value}${unit}`
}

function normalizeEnvironmentList(data) {
	const source = data || {}

	return {
		// 气象监测展示空气环境类指标，字段统一由后端环境监测接口返回。
		weatherItems: [
			{ label: '温度', value: formatValue(source.airTemperature, '°C'), icon: 'icon-wendu1' },
			{ label: '湿度', value: formatValue(source.airHumidity, '%'), icon: 'icon-shidu1' },
			{ label: 'PM2.5', value: formatValue(source.pm25, 'μg/m³'), icon: 'icon-app_icons--' },
			{ label: '风速', value: formatValue(source.windSpeed, 'm/s'), icon: 'icon-fengsu' },
			{ label: '气压', value: formatValue(source.airPressure, 'hPa'), icon: 'icon-qiya' },
			{ label: '二氧化碳', value: formatValue(source.co2Concentration, 'ppm'), icon: 'icon-eryanghuatannongdu' },
			{ label: '光照', value: formatValue(source.lightIntensity, 'lux'), icon: 'icon-guangzhaoqiangdu' }
		],
		// 水质监测承载水温、PH、EC、溶解氧以及水泵采集的水流量、水压。
		waterQualityItems: [
			{ label: '水温', value: formatValue(source.waterTemperature, '°C'), icon: 'icon-shuiwen' },
			{ label: 'PH值', value: formatValue(source.ph), icon: 'icon-PHzhi' },
			{ label: 'EC值', value: formatValue(source.ecValue, 'mS/cm'), icon: 'icon-a-aseECzhi' },
			{ label: '溶解氧', value: formatValue(source.dissolvedOxygen, 'mg/L'), icon: 'icon-rongjieyang' },
			{ label: '水流量', value: formatValue(source.waterFlow, 'm³/h'), icon: 'icon-journalism' },
			{ label: '水压', value: formatValue(source.waterPressure, 'MPa'), icon: 'icon-linhuaqingnongdu' }
		]
	}
}

// 查询当前登录用户的最新环境监测数据；用户身份由 token 决定，前端不传 userId。
export function getEnvironmentList() {
	return get('/smart_plant/client/environment/list').then((data) => normalizeEnvironmentList(data))
}

// 根据地块 ID 查询当前登录用户该地块下的监测数据；后端会校验地块归属，前端只负责传业务主键。
export function getEnvironmentListByPlotId(plotId) {
	return get(`/smart_plant/client/plots/${plotId}/environment`).then((data) => normalizeEnvironmentList(data))
}
