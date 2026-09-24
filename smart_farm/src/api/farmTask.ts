import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'

/** 农事任务实体，对应 smart_plant 后端 FarmTask 返回结构。 */
export interface FarmTask {
  /** 任务ID，对应 farm_task.id。 */
  id: number
  /** 任务归属用户ID，农事消息视角也会作为发布人ID使用。 */
  userId: number
  /** 归属用户账号。 */
  username?: string
  /** 归属用户昵称。 */
  nickname?: string
  /** 农事消息视角使用的发布人名称，优先昵称、兜底账号。 */
  publisherName?: string
  /** 任务所属地块ID。 */
  plotId: number
  /** 地块名称。 */
  plotName?: string
  /** 地块编号，用于辅助区分同名地块。 */
  plotCode?: string
  /** 种植批次ID。 */
  batchId?: number
  /** 种植批次编号。 */
  batchNo?: string
  /** 任务标题，支持条件查询。 */
  taskTitle: string
  /** 任务类型：1浇水 2施肥 3打药 4采收 5巡检 6除草 7补光 8其他。 */
  taskType: number
  /** 任务内容，农事消息页面也会按该字段查询。 */
  taskContent?: string
  /** 优先级：1低 2普通 3高 4紧急。 */
  priority?: number
  /** 任务截至时间。 */
  deadlineTime?: string
  /** 实际开始时间。 */
  actualStartTime?: string
  /** 实际完成时间。 */
  actualEndTime?: string
  /** 任务状态：1未开始 2进行中 3已完成 4已逾期 5已取消。 */
  status?: number
  /** 执行人ID。 */
  executorId?: number
  /** 执行人名称。 */
  executorName?: string
  /** 完成说明。 */
  completeRemark?: string
  /** 后台备注。 */
  remark?: string
  /** 来源类型，例如 AI_SOLUTION。 */
  sourceType?: string
  /** 来源业务ID。 */
  sourceId?: number
  /** AI 处置方案ID。 */
  aiSolutionId?: number
  /** 创建时间。 */
  createTime?: string
  /** 发布时间别名，后端由 create_time 映射。 */
  publishTime?: string
  /** 更新时间。 */
  updateTime?: string
  /** 是否已超过截至时间，该值由后端动态计算。 */
  overdue?: boolean
}

/** 农事任务列表查询参数，同时兼容农事消息页面的标题、内容、地块查询。 */
export interface FarmTaskListParams {
  plotName?: string
  taskTitle?: string
  taskContent?: string
  taskType?: number
  status?: number
  sourceType?: string
  startDate?: string
  endDate?: string
  pageNum: number
  pageSize: number
}

/** 新增或编辑农事任务时提交的表单字段。 */
export interface FarmTaskPayload {
  plotId: number
  batchId?: number
  taskTitle: string
  taskType: number
  taskContent?: string
  priority?: number
  /** 新增或编辑任务时设置的单一截至时间。 */
  deadlineTime?: string
  /** 可预先指派执行人；实际领取任务时后端会记录最终执行人。 */
  executorId?: number
  remark?: string
}

export interface UpdateFarmTaskPayload extends Partial<FarmTaskPayload> {
  id: number
}

/** smart_plant 统一响应包装。 */
interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 查询农事任务详情，供详情抽屉和编辑弹窗回填。 */
export const getFarmTaskById = (id: number) => {
  return get<ApiResult<FarmTask>>(`/farm-task/${id}`)
}

/** 新增农事任务，对接 smart_plant POST /farm-task/add。 */
export const addFarmTask = (data: FarmTaskPayload) => {
  return post<ApiResult<FarmTask>>('/farm-task/add', data as unknown as Record<string, unknown>)
}

/** 编辑农事任务，对接 smart_plant PUT /farm-task。 */
export const updateFarmTask = (data: UpdateFarmTaskPayload) => {
  return http<ApiResult<FarmTask>>({ url: '/farm-task', method: 'put', data })
}

/** 删除单条农事任务。 */
export const deleteFarmTask = (id: number) => {
  return http<ApiResult<void>>({ url: `/farm-task/${id}`, method: 'delete' })
}

/** 批量删除农事任务。 */
export const batchDeleteFarmTasks = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/farm-task/batch', method: 'delete', data: ids })
}

/** 更新农事任务执行状态，详情抽屉中的开始/完成按钮会调用该接口。 */
export const updateFarmTaskStatus = (id: number, status: number, completeRemark?: string) => {
  return http<ApiResult<FarmTask>>({
    url: `/farm-task/${id}/status`,
    method: 'put',
    params: { status, completeRemark },
  })
}

/** 分页查询农事任务，参数名与后端 FarmTaskController 保持一致。 */
export const listFarmTasks = (params: FarmTaskListParams) => {
  return get<ApiResult<PageInfo<FarmTask>>>('/farm-task/list', params as unknown as Record<string, unknown>)
}

/** 查询农事任务统计卡片数据。 */
export const getFarmTaskStatistics = () => {
  return get<ApiResult<Record<string, number>>>('/farm-task/statistics')
}
