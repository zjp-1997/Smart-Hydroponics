import { get } from '@/utils/request.js'

function normalizeWeather(weather) {
	return {
		location: weather.location || '当前位置',
		weather: weather.weather || '',
		temperature: weather.temperature || '--',
		humidity: weather.humidity || '--',
		pm25: weather.pm25 || '--',
		reportTime: weather.reportTime || ''
	}
}

// 根据设备定位经纬度查询首页天气条数据，第三方 Key 不在前端保存。
export function getCurrentWeather({ longitude, latitude, coordinateSystem = 'wgs84' }) {
	return get('/smart_plant/client/weather/current', {
		longitude,
		latitude,
		coordinateSystem
	}).then((weather) => normalizeWeather(weather || {}))
}
