import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface FarmTaskRecord {
  id: number
  taskId: number
  taskTitle?: string
  userId?: number
  plotId?: number
  plotName?: string
  operatorId?: number
  /** 操作时保存的人员名称快照。 */
  operatorNameSnapshot?: string
  operatorName?: string
  actionType: number
  actionContent?: string
  resultStatus?: number
  /** 阶段性进度，范围为0到100。 */
  progressPercent?: number
  feedbackScore?: number
  feedbackDetail?: string
  optimizeSuggestion?: string
  /** 附件地址JSON，为后续查看执行凭证预留。 */
  attachments?: string
  /** 客户端幂等请求号。 */
  requestId?: string
  /** 事件来源：FARM_APP、SMART_FARM 或 SYSTEM。 */
  sourceClient?: string
  beforeStatus?: number
  afterStatus?: number
  executeTime?: string
  createTime?: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const addFarmTaskRecord = (data: Partial<FarmTaskRecord>) => {
  return http<ApiResult<FarmTaskRecord>>({ url: '/farm-task-record/add', method: 'post', data })
}

export const getFarmTaskRecordById = (id: number) => {
  return get<ApiResult<FarmTaskRecord>>(`/farm-task-record/${id}`)
}

export const listFarmTaskRecords = (params: { taskId?: number; pageNum: number; pageSize: number }) => {
  return get<ApiResult<PageInfo<FarmTaskRecord>>>(
    '/farm-task-record/list',
    params as unknown as Record<string, unknown>,
  )
}
