import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'

export interface AiRecognitionRecord {
  id: number
  userId: number
  username?: string
  nickname?: string
  sourceType: number
  cropImageId?: number
  cameraId?: number
  cameraName?: string
  plotId?: number
  plotName?: string
  plotCode?: string
  captureId?: number
  imageUrl: string
  thumbnailUrl?: string
  imageSize?: number
  status: number
  recognitionStartTime?: string
  recognitionEndTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
  resultId?: number
  recognitionType?: number
  recognitionTypeName?: string
  cropId?: number
  cropName?: string
  resultName?: string
  resultSummary?: string
  resultDetail?: string
  diseasePestId?: number
  diseasePestName?: string
  confidence?: number
  severityLevel?: number
  suggestion?: string
  modelName?: string
  modelVersion?: string
  resultStatus?: number
  failReason?: string
  recognizeTime?: string
  resultRemark?: string
  resultCreateTime?: string
  resultCount?: number
}

export interface AiRecognitionRecordListParams {
  username?: string
  sourceType?: number
  status?: number
  resultStatus?: number
  cameraName?: string
  plotName?: string
  recognitionType?: number
  resultName?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

export type AiRecognitionRecordStatisticsParams = Omit<
  AiRecognitionRecordListParams,
  'pageNum' | 'pageSize'
>

export interface AiRecognitionRecordStatistics {
  totalCount?: number
  manualCount?: number
  autoCount?: number
  pendingCount?: number
  runningCount?: number
  completedCount?: number
  failedRecordCount?: number
  successResultCount?: number
  failedResultCount?: number
  firstCreateTime?: string
  latestCreateTime?: string
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const listAiRecognitionRecords = (params: AiRecognitionRecordListParams) => {
  return get<ApiResult<PageInfo<AiRecognitionRecord>>>(
    '/ai-recognition-record/list',
    params as unknown as Record<string, unknown>,
  )
}

export const getAiRecognitionRecordStatistics = (params: AiRecognitionRecordStatisticsParams) => {
  return get<ApiResult<AiRecognitionRecordStatistics>>(
    '/ai-recognition-record/statistics',
    params as unknown as Record<string, unknown>,
  )
}

export const getAiRecognitionRecordById = (id: number) => {
  return get<ApiResult<AiRecognitionRecord>>(`/ai-recognition-record/${id}`)
}

export const deleteAiRecognitionRecord = (id: number) => {
  return http<ApiResult<void>>({ url: `/ai-recognition-record/${id}`, method: 'delete' })
}

export const batchDeleteAiRecognitionRecords = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/ai-recognition-record/batch', method: 'delete', data: ids })
}
