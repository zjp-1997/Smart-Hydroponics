import { get, http } from '@/utils/request'

export interface HomeVisitTrendPoint {
  date: string
  label: string
  visitCount: number
}

export interface HomeStats {
  farms: number
  plots: number
  batches: number
  devices: number
  faults: number
}

export interface HomeVisitTrend {
  days: number
  total: number
  points: HomeVisitTrendPoint[]
}

export type HomeOperationMetricKey =
  | 'sensorOnline'
  | 'realtimeMonitoring'
  | 'pumpRunning'
  | 'lightRunning'
  | 'fanRunning'

export interface HomeOperationOverviewItem {
  metricKey: HomeOperationMetricKey
  label: string
  totalCount: number
  onlineCount: number
  activeCount: number
  onlineRate: number
}

export interface HomeOperationOverview {
  generatedAt: string
  items: HomeOperationOverviewItem[]
}

export interface HomeWarehouseValueCategory {
  category: number
  categoryName: string
  totalValue: number
}

/** 顶部栏动态摘要，与 smart_plant HomeHeaderSummaryResponse 字段保持一致。 */
export interface HomeHeaderSummary {
  unreadNoticeCount: number
}

/** 顶部全局搜索结果项，与 smart_plant HomeSearchResultItem 字段保持一致。 */
export interface HomeSearchResultItem {
  type: 'farmTask' | 'iotDevice' | 'plot'
  id: number
  title: string
  description: string
}

export interface HomeSearchResponse {
  keyword: string
  items: HomeSearchResultItem[]
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const recordHomeVisit = () => {
  return http<ApiResult<void>>({ url: '/home-dashboard/visit', method: 'post' })
}

export const getHomeStats = () => {
  return get<ApiResult<HomeStats>>('/home-dashboard/stats')
}

export const getHomeVisitTrend = (days: number) => {
  // 身份由登录令牌确定，不允许前端指定其他用户的访问记录。
  return get<ApiResult<HomeVisitTrend>>('/home-dashboard/visit-trend', { days })
}

export const getHomeOperationOverview = () => {
  return get<ApiResult<HomeOperationOverview>>('/home-dashboard/operation-overview')
}

export const getHomeWarehouseValueAnalysis = () => {
  return get<ApiResult<HomeWarehouseValueCategory[]>>('/home-dashboard/warehouse-value-analysis')
}

/** 查询顶部未读消息数量；管理员共享全局公告数量，其他角色按本人统计。 */
export const getHomeHeaderSummary = () => {
  return get<ApiResult<HomeHeaderSummary>>('/home-dashboard/header-summary')
}

/** 按关键字搜索任务、设备和地块，后端负责应用数据权限。 */
export const searchHomeResources = (keyword: string, limit = 12) => {
  return get<ApiResult<HomeSearchResponse>>('/home-dashboard/search', { keyword, limit })
}
