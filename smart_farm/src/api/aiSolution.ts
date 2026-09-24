import { get, http } from '@/utils/request'
import type { PageInfo } from '@/api/user'
import type { FarmTask } from '@/api/farmTask'

export interface AiSolution {
  id: number
  recordId: number
  resultId: number
  userId: number
  username?: string
  nickname?: string
  plotId?: number
  plotName?: string
  plotCode?: string
  cameraId?: number
  cameraName?: string
  solutionTitle: string
  solutionSummary?: string
  solutionDetail?: string
  priorityLevel?: number
  generateTask?: number
  taskGenerated?: number
  farmTaskId?: number
  farmTaskTitle?: string
  farmTaskStatus?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface AiSolutionListParams {
  plotName?: string
  cameraName?: string
  solutionTitle?: string
  priorityLevel?: number
  taskGenerated?: number
  status?: number
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export const generateAiSolution = (recordId: number, generateTask = false) => {
  return http<ApiResult<AiSolution>>({
    url: `/ai-solution/generate/${recordId}`,
    method: 'post',
    params: { generateTask },
  })
}

export const generateFarmTaskFromSolution = (solutionId: number, data?: Partial<FarmTask>) => {
  return http<ApiResult<FarmTask>>({
    url: `/ai-solution/${solutionId}/generate-task`,
    method: 'post',
    data,
  })
}

export const getAiSolutionById = (id: number) => {
  return get<ApiResult<AiSolution>>(`/ai-solution/${id}`)
}

export const getLatestAiSolutionByRecordId = (recordId: number) => {
  return get<ApiResult<AiSolution | null>>(`/ai-solution/record/${recordId}`)
}

export const listAiSolutions = (params: AiSolutionListParams) => {
  return get<ApiResult<PageInfo<AiSolution>>>('/ai-solution/list', params as unknown as Record<string, unknown>)
}

export const getAiSolutionStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/ai-solution/statistics')
}
