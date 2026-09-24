import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/**
 * 系统公告实体。
 *
 * 后端底层复用 notification 表，前端只按公告聚合视图使用这些字段。
 */
export interface SystemAnnouncement {
  id: number
  title: string
  content?: string
  level?: number
  publisherId?: number
  publisherName?: string
  recipientCount?: number
  readCount?: number
  sendTime?: string
  createTime?: string
  updateTime?: string
}

/** 发布或编辑系统公告时提交的业务字段。 */
export interface SystemAnnouncementPayload {
  title: string
  content: string
  level: number
}

/** 系统公告列表查询参数。 */
export interface SystemAnnouncementListParams {
  title?: string
  content?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询系统公告，支持标题、内容和发布时间范围查询。 */
export const listSystemAnnouncements = (params: SystemAnnouncementListParams) => {
  return get<ApiResult<PageInfo<SystemAnnouncement>>>('/sys-msg/list', params as unknown as Record<string, unknown>)
}

/** 查询系统公告详情，供编辑弹窗回填。 */
export const getSystemAnnouncementById = (id: number) => {
  return get<ApiResult<SystemAnnouncement>>(`/sys-msg/${id}`)
}

/** 发布系统公告，后端会自动生成所有启用用户的接收明细。 */
export const publishSystemAnnouncement = (data: SystemAnnouncementPayload) => {
  return post<ApiResult<SystemAnnouncement>>('/sys-msg/add', data as unknown as Record<string, unknown>)
}

/** 编辑系统公告，后端会同步更新同一公告组下所有接收人的明细。 */
export const updateSystemAnnouncement = (id: number, data: SystemAnnouncementPayload) => {
  return http<ApiResult<SystemAnnouncement>>({
    url: '/sys-msg',
    method: 'put',
    params: { id },
    data,
  })
}

/** 删除单条系统公告，后端执行公告组逻辑删除。 */
export const deleteSystemAnnouncement = (id: number) => {
  return http<ApiResult<void>>({ url: `/sys-msg/${id}`, method: 'delete' })
}

/** 批量删除系统公告，入参为表格中的公告ID。 */
export const batchDeleteSystemAnnouncements = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/sys-msg/batch', method: 'delete', data: ids })
}

/** 查询系统公告统计卡片数据。 */
export const getSystemAnnouncementStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/sys-msg/statistics')
}
