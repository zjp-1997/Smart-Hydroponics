import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 与 smart_plant MaintenanceMessage 完全对应的维护消息快照，id 为消息本体ID。 */
export interface MaintenanceMessage {
  id: number
  faultId?: number
  deviceId?: number
  deviceName?: string
  version: number
  publisherId?: number
  plotId: number
  plotName: string
  title: string
  content: string
  level: number
  publisherName: string
  sendTime: string
  recipientCount: number
  readCount: number
}
/** 查询参数及统一响应包装。 */
interface Query { title?: string; content?: string; plotName?: string; pageNum: number; pageSize: number }
interface ApiResult<T> { code: number; message: string; data: T }
/** 分页读取自动生成的维护消息，不与农事消息接口混用。 */
export const listMaintenanceMessages = (params: Query) => get<ApiResult<PageInfo<MaintenanceMessage>>>('/maintenance-msg/list', { ...params })
/** 统计保持农事消息四卡片口径。 */
export const getMaintenanceMessageStatistics = () => get<ApiResult<Record<string, number>>>('/maintenance-msg/statistics')
/** 服务端校验消息接收人范围后返回详情。 */
export const getMaintenanceMessage = (id: number) => get<ApiResult<MaintenanceMessage>>(`/maintenance-msg/${id}`)
/** 仅标记登录接收人已读。 */
export const markMaintenanceMessageRead = (id: number) => http<ApiResult<void>>({ url: `/maintenance-msg/${id}/read`, method: 'put' })

/** 发布与编辑只提交可修改字段，正文按用户输入保存，接收人由服务端确定。 */
export interface MaintenanceMessagePayload { deviceId?: number; title: string; content: string; level: number; version?: number }
/** 下拉候选只返回当前用户有权发布通知的设备。 */
export interface MaintenanceDeviceOption { id: number; name: string; plotName: string }
export const getMaintenanceDevices = (keyword?: string) => get<ApiResult<MaintenanceDeviceOption[]>>('/maintenance-msg/devices', { keyword })
/** 人工发布消息，保留自动故障通知链路。 */
export const publishMaintenanceMessage = (data: MaintenanceMessagePayload) => http<ApiResult<MaintenanceMessage>>({ url: '/maintenance-msg/add', method: 'post', data })
/** 编辑时携带版本号，避免覆盖其他用户已保存的修改。 */
export const updateMaintenanceMessage = (id: number, data: MaintenanceMessagePayload) => http<ApiResult<MaintenanceMessage>>({ url: `/maintenance-msg/${id}`, method: 'put', data })
/** 删除整条消息及其接收明细，服务端采用逻辑删除。 */
export const deleteMaintenanceMessage = (id: number) => http<ApiResult<number>>({ url: `/maintenance-msg/${id}`, method: 'delete' })
/** 批量删除作为一个事务执行，不允许跨用户越权删除。 */
export const batchDeleteMaintenanceMessages = (ids: number[]) => http<ApiResult<number>>({ url: '/maintenance-msg/batch', method: 'delete', data: ids })
