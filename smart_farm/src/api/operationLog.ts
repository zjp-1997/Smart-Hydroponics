import { get } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface OperationLog {
  id: number
  operatorId?: number
  operatorName?: string
  operation?: string
  ip?: string
  operateTime?: string
}

export interface OperationLogListParams {
  operatorName?: string
  operation?: string
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

export const listOperationLogs = (params: OperationLogListParams) =>
  get<ApiResult<PageInfo<OperationLog>>>('/operation-log/list', params as unknown as Record<string, unknown>)
