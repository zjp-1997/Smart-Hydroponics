import { load as loadAmapJsApi } from '@amap/amap-jsapi-loader'

declare global {
  interface Window {
    /** 高德 JS API 2.0 要求在加载 SDK 前声明的安全配置。 */
    _AMapSecurityConfig?: { securityJsCode: string }
  }
}

export interface Coordinate {
  /** WGS-84 经度，作为系统业务数据的统一存储坐标。 */
  longitude: number
  /** WGS-84 纬度，作为系统业务数据的统一存储坐标。 */
  latitude: number
}

export interface LocatedAddress extends Coordinate {
  /** 高德定位或逆地理编码返回的格式化地址。 */
  address: string
}

let amapPromise: Promise<typeof AMap> | null = null

/**
 * 全局只加载一次高德 Web JS API，并统一装载定位、逆地理编码插件。
 */
export const loadAmap = () => {
  if (amapPromise) return amapPromise

  const key = import.meta.env.VITE_AMAP_KEY?.trim()
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE?.trim()
  if (!key || !securityJsCode) {
    return Promise.reject(new Error('高德地图 Key 或安全密钥未配置'))
  }

  // 安全配置必须先于 JS API 脚本加载，否则高德服务插件会鉴权失败。
  window._AMapSecurityConfig = { securityJsCode }
  amapPromise = loadAmapJsApi({
    key,
    version: '2.0',
    plugins: ['AMap.Geolocation', 'AMap.Geocoder'],
  }) as Promise<typeof AMap>
  return amapPromise
}

/** 判断坐标是否位于中国大陆坐标偏移范围之外。 */
const isOutsideChina = (longitude: number, latitude: number) => (
  longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
)

/** 计算 WGS-84 转 GCJ-02 所需的纬度偏移量。 */
const transformLatitude = (longitude: number, latitude: number) => {
  let result = -100 + 2 * longitude + 3 * latitude + 0.2 * latitude * latitude
    + 0.1 * longitude * latitude + 0.2 * Math.sqrt(Math.abs(longitude))
  result += (20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2 / 3
  result += (20 * Math.sin(latitude * Math.PI) + 40 * Math.sin(latitude / 3 * Math.PI)) * 2 / 3
  result += (160 * Math.sin(latitude / 12 * Math.PI) + 320 * Math.sin(latitude * Math.PI / 30)) * 2 / 3
  return result
}

/** 计算 WGS-84 转 GCJ-02 所需的经度偏移量。 */
const transformLongitude = (longitude: number, latitude: number) => {
  let result = 300 + longitude + 2 * latitude + 0.1 * longitude * longitude
    + 0.1 * longitude * latitude + 0.1 * Math.sqrt(Math.abs(longitude))
  result += (20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2 / 3
  result += (20 * Math.sin(longitude * Math.PI) + 40 * Math.sin(longitude / 3 * Math.PI)) * 2 / 3
  result += (150 * Math.sin(longitude / 12 * Math.PI) + 300 * Math.sin(longitude / 30 * Math.PI)) * 2 / 3
  return result
}

/** 将系统保存的 WGS-84 坐标转换为高德地图使用的 GCJ-02 坐标。 */
export const toAmapCoordinate = (longitude: number, latitude: number): Coordinate => {
  if (isOutsideChina(longitude, latitude)) return { longitude, latitude }

  const earthRadius = 6378245
  const eccentricity = 0.006693421622965943
  let latitudeOffset = transformLatitude(longitude - 105, latitude - 35)
  let longitudeOffset = transformLongitude(longitude - 105, latitude - 35)
  const radianLatitude = latitude / 180 * Math.PI
  let magic = Math.sin(radianLatitude)
  magic = 1 - eccentricity * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  latitudeOffset = (latitudeOffset * 180) / ((earthRadius * (1 - eccentricity)) / (magic * sqrtMagic) * Math.PI)
  longitudeOffset = (longitudeOffset * 180) / (earthRadius / sqrtMagic * Math.cos(radianLatitude) * Math.PI)
  return { longitude: longitude + longitudeOffset, latitude: latitude + latitudeOffset }
}

/** 将高德定位返回的 GCJ-02 坐标近似还原为系统统一存储的 WGS-84 坐标。 */
export const toWgs84Coordinate = (longitude: number, latitude: number): Coordinate => {
  if (isOutsideChina(longitude, latitude)) return { longitude, latitude }
  const offsetCoordinate = toAmapCoordinate(longitude, latitude)
  return {
    longitude: longitude * 2 - offsetCoordinate.longitude,
    latitude: latitude * 2 - offsetCoordinate.latitude,
  }
}

/** 根据系统中的 WGS-84 坐标调用高德逆地理编码并返回详细地址。 */
export const reverseGeocodeWithAmap = async (longitude: number, latitude: number) => {
  const AMapApi = await loadAmap()
  const coordinate = toAmapCoordinate(longitude, latitude)
  const geocoder = new AMapApi.Geocoder({ extensions: 'all' })

  return new Promise<string>((resolve, reject) => {
    geocoder.getAddress([coordinate.longitude, coordinate.latitude], (status, result) => {
      if (status === 'complete' && typeof result !== 'string' && result.regeocode?.formattedAddress) {
        resolve(result.regeocode.formattedAddress)
        return
      }
      reject(new Error(typeof result === 'string' ? result : '高德逆地理编码失败'))
    })
  })
}

/** 使用高德定位插件获取当前位置，并将坐标统一还原为 WGS-84 后交给业务表单。 */
export const locateWithAmap = async (): Promise<LocatedAddress> => {
  const AMapApi = await loadAmap()
  const geolocation = new AMapApi.Geolocation({
    enableHighAccuracy: true,
    timeout: 10000,
    maximumAge: 30000,
    convert: true,
    noIpLocate: 3,
    GeoLocationFirst: true,
    extensions: 'all',
  })

  return new Promise<LocatedAddress>((resolve, reject) => {
    geolocation.getCurrentPosition((status, result) => {
      if (status !== 'complete' || !('position' in result)) {
        reject(new Error(result.message || result.info || '高德定位失败'))
        return
      }

      const longitude = result.position.getLng()
      const latitude = result.position.getLat()
      const coordinate = result.isConverted
        ? toWgs84Coordinate(longitude, latitude)
        : { longitude, latitude }
      resolve({
        ...coordinate,
        address: result.formattedAddress || '',
      })
    })
  })
}
