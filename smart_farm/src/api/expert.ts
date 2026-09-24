import { get, http, post } from '@/utils/request'
import type { PageInfo } from '@/api/user'
import type { AxiosResponse } from 'axios'

/**
 * 专家信息接口文件。
 *
 * 作用说明：
 * - 统一封装 smart_plant 后端提供的 /expert-profile 专家信息管理接口。
 * - 页面层只关心“查询、详情、新增、编辑、删除、认证审核、评价”等业务动作，
 *   不直接拼接后端 URL，便于后续接口路径或字段调整。
 */

export interface ExpertProfilePayload {
  userId?: number
  institutionId?: number
  username?: string
  realName: string
  organization?: string
  jobTitle?: string
  specialty?: string
  introduction?: string
  certificateUrl?: string
  certificateName?: string
  certificateAuditStatus?: number
  avatar?: string
  rating?: number
  consultationCount?: number
  auditStatus?: number
  auditPassTime?: string
  serviceStatus?: number
  consultationStatus?: number
  status?: number
  remark?: string
}

export interface UpdateExpertProfilePayload extends Partial<ExpertProfilePayload> {
  id: number
}

export interface ExpertProfile extends Partial<ExpertProfilePayload> {
  id?: number
  createTime?: string
  updateTime?: string
}

export interface ExpertAssetUploadResult {
  url: string
  size: number
}

export interface ExpertReview {
  id?: number
  expertId?: number
  userId?: number
  expertName?: string
  userName?: string
  rating?: number
  content?: string
  createTime?: string
  updateTime?: string
}

export interface UpdateExpertReviewPayload {
  id: number
  rating: number
  content: string
}

export interface ExpertProfileListParams {
  realName?: string
  organization?: string
  specialty?: string
  auditStatus?: number
  serviceStatus?: number
  status?: number
  pageNum: number
  pageSize: number
}

export interface ExpertReviewListParams {
  expertId?: number
  expertName?: string
  userId?: number
  userName?: string
  startTime?: string
  endTime?: string
  pageNum: number
  pageSize: number
}

export interface ExpertProfileStatistics {
  totalCount?: number
  auditTotalCount?: number
  pendingCount?: number
  approvedCount?: number
  rejectedCount?: number
  reviewedCount?: number
  availableCount?: number
  suspendedCount?: number
  disabledCount?: number
  institutionCount?: number
  averageRating?: number
  totalConsultations?: number
  totalTrend?: number
  auditTotalTrend?: number
  pendingTrend?: number
  approvedTrend?: number
  rejectedTrend?: number
  reviewedTrend?: number
  availableTrend?: number
  disabledTrend?: number
  institutionTrend?: number
  averageRatingTrend?: number
  totalConsultationsTrend?: number
}

export interface ExpertReviewStatistics {
  reviewTotalCount?: number
  reviewUserCount?: number
  averageRating?: number
  minRating?: number
  reviewTotalTrend?: number
  reviewUserTrend?: number
  averageRatingTrend?: number
  minRatingTrend?: number
}

interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页查询专家资料列表，对应 smart_plant: GET /expert-profile/list。 */
export const listExpertProfiles = (params: ExpertProfileListParams) => {
  return get<ApiResult<PageInfo<ExpertProfile>>>(
    '/expert-profile/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 分页查询专家认证审核列表，对应 smart_plant: GET /expert-profile/audit/list。 */
export const listExpertAuditProfiles = (params: ExpertProfileListParams) => {
  return get<ApiResult<PageInfo<ExpertProfile>>>(
    '/expert-profile/audit/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 查询专家管理统计，对应 smart_plant: GET /expert-profile/statistics。 */
export const getExpertProfileStatistics = () => {
  return get<ApiResult<ExpertProfileStatistics>>('/expert-profile/statistics')
}

/** 分页查询专家评价列表，对应 smart_plant: GET /expert-review/list。 */
export const listExpertReviews = (params: ExpertReviewListParams) => {
  return get<ApiResult<PageInfo<ExpertReview>>>(
    '/expert-review/list',
    params as unknown as Record<string, unknown>,
  )
}

/** 查询专家评价统计，对应 smart_plant: GET /expert-review/statistics。 */
export const getExpertReviewStatistics = () => {
  return get<ApiResult<ExpertReviewStatistics>>('/expert-review/statistics')
}

/** 编辑专家评价，对应 smart_plant: PUT /expert-review。 */
export const updateExpertReview = (data: UpdateExpertReviewPayload) => {
  return http<ApiResult<ExpertReview>>({ url: '/expert-review', method: 'put', data })
}

/** 删除单条专家评价，对应 smart_plant: DELETE /expert-review/{id}。 */
export const deleteExpertReview = (id: number) => {
  return http<ApiResult<void>>({ url: `/expert-review/${id}`, method: 'delete' })
}

/** 批量删除专家评价，对应 smart_plant: DELETE /expert-review/batch。 */
export const batchDeleteExpertReviews = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/expert-review/batch', method: 'delete', data: ids })
}

/** 根据专家资料 ID 查询详情，对应 smart_plant: GET /expert-profile/{id}。 */
export const getExpertProfileById = (id: number) => {
  return get<ApiResult<ExpertProfile>>(`/expert-profile/${id}`)
}

/** 新增专家资料，对应 smart_plant: POST /expert-profile/add。 */
export const addExpertProfile = (data: ExpertProfilePayload) => {
  return post<ApiResult<ExpertProfile>>(
    '/expert-profile/add',
    data as unknown as Record<string, unknown>,
  )
}

/** 编辑专家资料，对应 smart_plant: PUT /expert-profile。 */
export const updateExpertProfile = (data: UpdateExpertProfilePayload) => {
  return http<ApiResult<ExpertProfile>>({ url: '/expert-profile', method: 'put', data })
}

/** 上传专家头像或证书图片，对应 smart_plant: POST /expert-profile/upload-asset。 */
export const uploadExpertAsset = (file: File, assetType: 'avatar' | 'certificate' = 'avatar') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('assetType', assetType)

  return http<ApiResult<ExpertAssetUploadResult>>({
    url: '/expert-profile/upload-asset',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 使用管理端 JWT 下载受保护的专家头像或资质证书，供页面生成本地预览地址。 */
export const downloadExpertAsset = (url: string) => {
  return http<AxiosResponse<Blob>>({ url, method: 'get', responseType: 'blob' })
}

/** 删除单条专家资料，对应 smart_plant: DELETE /expert-profile/{id}。 */
export const deleteExpertProfile = (id: number) => {
  return http<ApiResult<void>>({ url: `/expert-profile/${id}`, method: 'delete' })
}

/** 批量删除专家资料，对应 smart_plant: DELETE /expert-profile/batch。 */
export const batchDeleteExpertProfiles = (ids: number[]) => {
  return http<ApiResult<number>>({ url: '/expert-profile/batch', method: 'delete', data: ids })
}

/** 修改专家账号启用/禁用状态，对应 smart_plant: PUT /expert-profile/{id}/status。 */
export const updateExpertProfileStatus = (id: number, status: number) => {
  return http<ApiResult<void>>({
    url: `/expert-profile/${id}/status`,
    method: 'put',
    params: { status },
  })
}

/** 修改专家咨询状态，对应 smart_plant: PUT /expert-profile/{id}/consultation-status。 */
export const updateExpertConsultationStatus = (id: number, consultationStatus: number) => {
  return http<ApiResult<void>>({
    url: `/expert-profile/${id}/consultation-status`,
    method: 'put',
    params: { consultationStatus },
  })
}

/** 专家认证审核，本质上是更新 expert_profile.audit_status 和备注字段。 */
export const auditExpertProfile = (id: number, auditStatus: number, remark?: string) => {
  return updateExpertProfile({ id, auditStatus, remark })
}
