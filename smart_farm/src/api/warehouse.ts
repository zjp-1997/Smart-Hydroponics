import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface WarehouseItem {
  id?: number
  userId?: number
  /** 所属农场主，与创建账号 userId、入库人分别记录。 */
  farmOwnerId?: number
  farmOwnerName?: string
  username?: string
  nickname?: string
  inboundOperatorId?: number
  inboundOperatorName?: string
  latestOutboundOperatorName?: string
  latestOutboundRecipient?: string
  itemName: string
  itemCode?: string
  imageUrl?: string
  category: number
  specification?: string
  unit: string
  stockQty?: number
  initialUnitPrice?: number
  warningQty?: number
  manufacturer?: string
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

// 新增编码由后端生成，调用方不能在创建载荷中指定 itemCode。
export type WarehouseItemCreatePayload = Omit<WarehouseItem, 'itemCode'> & {
  initialUnitPrice: number
}

export interface WarehouseRecord {
  id?: number
  requestId?: string
  itemId: number
  itemName?: string
  itemCode?: string
  userId?: number
  operatorId?: number
  operatorName?: string
  recipient?: string
  recordType: number
  quantity: number
  beforeQty?: number
  afterQty?: number
  relatedPlotId?: number
  relatedTaskId?: number
  relatedFaultId?: number
  relatedFaultCode?: string
  relatedFaultName?: string
  relatedFaultStatus?: number
  relatedDeviceId?: number
  relatedDeviceName?: string
  sourceType?: number
  supplier?: string
  price?: number
  totalAmount?: number
  recordTime?: string
  status?: number
  remark?: string
}

export type WarehouseRecordCreatePayload = WarehouseRecord & { requestId: string }

export interface WarehouseStats {
  totalItems: number
  stockQuantity: number
  inboundQuantity: number
  outboundQuantity: number
}

export interface WarehouseOperatorOption {
  id: number
  username: string
  nickname?: string
}

export interface WarehouseFaultOption {
  id: number
  deviceId: number
  deviceName?: string
  faultCode: string
  faultName: string
  status?: number
  handleUsername?: string
  handleNickname?: string
}

export interface WarehouseItemListParams {
  itemName?: string
  itemCode?: string
  category?: number
  status?: number
  pageNum: number
  pageSize: number
}

export interface WarehouseRecordListParams {
  itemId?: number
  itemName?: string
  recordType?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listWarehouseItems = (params: WarehouseItemListParams) =>
  get<ApiResult<PageInfo<WarehouseItem>>>('/warehouse/item/list', params as unknown as Record<string, unknown>)

export const getWarehouseItemById = (id: number) =>
  get<ApiResult<WarehouseItem>>(`/warehouse/item/${id}`)

export const addWarehouseItem = (data: WarehouseItemCreatePayload) =>
  post<ApiResult<WarehouseItem>>('/warehouse/item/add', data as unknown as Record<string, unknown>)

export const updateWarehouseItem = (data: Partial<WarehouseItem> & { id: number }) =>
  http<ApiResult<WarehouseItem>>({ url: '/warehouse/item', method: 'put', data })

export const deleteWarehouseItem = (id: number) =>
  http<ApiResult<void>>({ url: `/warehouse/item/${id}`, method: 'delete' })

export const batchDeleteWarehouseItems = (ids: number[]) =>
  http<ApiResult<number>>({ url: '/warehouse/item/batch', method: 'delete', data: ids })

export const updateWarehouseItemStatus = (id: number, status: number) =>
  http<ApiResult<void>>({ url: `/warehouse/item/${id}/status`, method: 'put', params: { status } })

export const addWarehouseRecord = (data: WarehouseRecordCreatePayload) =>
  post<ApiResult<WarehouseRecord>>('/warehouse/record/add', data as unknown as Record<string, unknown>)

export const listWarehouseRecords = (params: WarehouseRecordListParams) =>
  get<ApiResult<PageInfo<WarehouseRecord>>>('/warehouse/record/list', params as unknown as Record<string, unknown>)

export const getWarehouseStats = () => get<ApiResult<WarehouseStats>>('/warehouse/stats')

export const listWarehouseOperatorOptions = () =>
  get<ApiResult<WarehouseOperatorOption[]>>('/warehouse/operator/options')

/** 新增和编辑物资时仅提供启用的农场主。 */
export const listWarehouseFarmOwners = () =>
  get<ApiResult<WarehouseOperatorOption[]>>('/warehouse/farm-owner/options')

export const listWarehouseFaultOptions = (itemId: number) =>
  get<ApiResult<WarehouseFaultOption[]>>(`/warehouse/item/${itemId}/fault/options`)
