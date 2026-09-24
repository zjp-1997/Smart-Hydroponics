import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 与 smart_plant ErrorLog 字段一一对应的错误日志类型。 */
export interface ErrorLog {
  id: number
  traceId: string
  level: string
  exceptionType: string
  errorMessage?: string
  requestMethod?: string
  requestUri?: string
  operatorId?: number
  operatorName?: string
  ip?: string
  userAgent?: string
  stackTrace?: string
  handleStatus: 0 | 1 | 2
  handleRemark?: string
  handlerId?: number
  handlerName?: string
  handleTime?: string
  version: number
  createTime: string
  updateTime?: string
}

export interface ErrorLogListParams {
  traceId?: string
  operatorName?: string
  requestUri?: string
  handleStatus?: number
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export interface ErrorLogHandlePayload {
  handleStatus: 0 | 1 | 2
  handleRemark?: string
  version: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 列表接口不返回大字段异常堆栈，保证分页查询响应稳定。 */
export const listErrorLogs = (params: ErrorLogListParams) =>
  get<ApiResult<PageInfo<ErrorLog>>>('/error-log/list', params as unknown as Record<string, unknown>)

/** 用户点击详情时再按需加载完整异常现场。 */
export const getErrorLogDetail = (id: number) =>
  get<ApiResult<ErrorLog>>(`/error-log/${id}`)

/** 修改处理状态和备注，version 用于后端乐观锁校验。 */
export const updateErrorLogHandle = (id: number, data: ErrorLogHandlePayload) =>
  http<ApiResult<ErrorLog>>({ url: `/error-log/${id}/handle`, method: 'put', data })
