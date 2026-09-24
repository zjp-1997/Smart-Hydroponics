import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 农事消息聚合实体。
 *
 * 消息本体字段来自 notification，taskId/taskTitle/taskType/plotName 来自关联农事任务。
 */
export interface FarmMessage {
  /** 消息列表ID，后端按消息组取组内最小 notification.id。 */
  id: number
  /** 消息组ID，同一条消息发给多个接收人时共享该值。 */
  refId?: number
  /** 关联农事任务ID，对应 notification.task_id。 */
  taskId: number
  /** 关联农事任务标题。 */
  taskTitle?: string
  /** 关联农事任务类型。 */
  taskType?: number
  /** 关联农事任务状态。 */
  taskStatus?: number
  /** 关联地块名称。 */
  plotName?: string
  /** 消息标题，独立于任务标题保存。 */
  title: string
  /** 消息内容，独立于任务内容保存。 */
  content?: string
  /** 消息级别：1普通 2重要 3紧急。 */
  level?: number
  /** 发布人ID。 */
  publisherId?: number
  /** 发布人名称。 */
  publisherName?: string
  /** 接收人数，后端按消息组聚合。 */
  recipientCount?: number
  /** 已读人数，后端按消息组聚合。 */
  readCount?: number
  /** 发布时间。 */
  sendTime?: string
  /** 创建时间。 */
  createTime?: string
  /** 更新时间。 */
  updateTime?: string
}

/** 发布或编辑农事消息时提交的消息本体字段。 */
export interface FarmMessagePayload {
  taskId: number
  title: string
  content: string
  level: number
  recipientIds?: number[]
}

/** 农事消息列表查询参数，按消息标题、消息内容和地块名称筛选。 */
export interface FarmMessageListParams {
  title?: string
  content?: string
  plotName?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

/** smart_plant 统一响应包装。 */
interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询农事消息，后端按 notification 消息组聚合返回。 */
export const listFarmMessages = (params: FarmMessageListParams) => {
  return get<ApiResult<PageInfo<FarmMessage>>>('/farm-msg/list', params as unknown as Record<string, unknown>)
}

/** 查询农事消息详情，供编辑弹窗回填。 */
export const getFarmMessageById = (id: number) => {
  return get<ApiResult<FarmMessage>>(`/farm-msg/${id}`)
}

/** 发布农事消息，只新增 notification 消息明细，不新增农事任务。 */
export const publishFarmMessage = (data: FarmMessagePayload) => {
  return post<ApiResult<FarmMessage>>('/farm-msg/add', data as unknown as Record<string, unknown>)
}

/** 编辑农事消息，只修改消息本体和 taskId 关联，不修改农事任务。 */
export const updateFarmMessage = (id: number, data: FarmMessagePayload) => {
  return http<ApiResult<FarmMessage>>({
    url: '/farm-msg',
    method: 'put',
    params: { id },
    data,
  })
}

/** 删除单条农事消息，后端按消息组逻辑删除接收明细。 */
export const deleteFarmMessage = (id: number) => {
  return http<ApiResult<void>>({ url: `/farm-msg/${id}`, method: 'delete' })
}

/** 批量删除农事消息，入参为列表中的消息ID。 */
export const batchDeleteFarmMessages = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/farm-msg/batch', method: 'delete', data: ids })
}

/** 查询农事消息统计卡片数据。 */
export const getFarmMessageStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/farm-msg/statistics')
}
