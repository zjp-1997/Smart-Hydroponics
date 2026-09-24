<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  Delete,
  DocumentChecked,
  EditPen,
  Medal,
  OfficeBuilding,
  Refresh,
  Search,
  StarFilled,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  auditExpertProfile,
  batchDeleteExpertProfiles,
  batchDeleteExpertReviews,
  deleteExpertProfile,
  deleteExpertReview,
  downloadExpertAsset,
  getExpertProfileStatistics,
  getExpertReviewStatistics,
  listExpertAuditProfiles,
  listExpertProfiles,
  listExpertReviews,
  updateExpertReview,
  updateExpertConsultationStatus,
  type ExpertProfile,
  type ExpertReview,
} from '@/api/expert'
import { getFileUrl } from '@/utils/utils'
import AdminPageFrame from '@/components/admin/AdminPageFrame.vue'
import ListFilterPanel from '@/components/admin/ListFilterPanel.vue'
import ListPagination from '@/components/admin/ListPagination.vue'
import StatusTag from '@/components/admin/StatusTag.vue'
import AddOrUpdate from './AddOrUpdate.vue'
import ExpertAuditDialog from './components/ExpertAuditDialog.vue'
import ExpertReviewDialog from './components/ExpertReviewDialog.vue'

/**
 * 专家管理主页面。
 *
 * 作用说明：
 * - 复用用户管理页面的信息架构：统计卡片、筛选区、表格区、分页和弹框操作。
 * - 通过三个二级入口承载专家信息、专家认证审核、专家评价管理。
 * - smart_plant 后端以 /expert-profile 聚合接口提供所有能力，页面按不同业务场景拆分字段和操作。
 */

type ExpertTab = 'info' | 'audit' | 'evaluation'

type RowStatus = 'enabled' | 'disabled'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface ExpertRow {
  id: number
  userId?: number
  avatarText: string
  avatarUrl?: string
  realName: string
  organization: string
  jobTitle: string
  specialty: string
  introduction: string
  certificateUrl?: string
  certificateName: string
  certificateAuditStatus: number
  rating: number
  consultationCount: number
  auditStatus: number
  auditPassTime: string
  serviceStatus: number
  consultationStatus: number
  status: RowStatus
  remark: string
  userName?: string
  content?: string
  createTime?: string
}

const route = useRoute()

const activeTab = ref<ExpertTab>('info')
const selectedRows = ref<ExpertRow[]>([])
const experts = ref<ExpertRow[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editExpertId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])

const auditDialogVisible = ref(false)
const reviewDialogVisible = ref(false)

const auditForm = reactive({
  id: 0,
  auditStatus: 1,
  realName: '',
  organization: '',
  jobTitle: '',
  specialty: '',
  introduction: '',
  avatarUrl: '',
  certificateName: '',
  certificateUrl: '',
  remark: '',
})

const reviewForm = reactive({
  id: 0,
  realName: '',
  userName: '',
  rating: 5,
  content: '',
})

const expertStats = ref({
  total: 0,
  auditTotal: 0,
  pending: 0,
  approved: 0,
  rejected: 0,
  reviewed: 0,
  reviewTotal: 0,
  reviewUserCount: 0,
  minRating: 0,
  available: 0,
  suspended: 0,
  disabled: 0,
  institutionCount: 0,
  averageRating: 0,
  totalConsultations: 0,
  totalTrend: 0,
  auditTotalTrend: 0,
  pendingTrend: 0,
  approvedTrend: 0,
  rejectedTrend: 0,
  reviewedTrend: 0,
  reviewTotalTrend: 0,
  reviewUserTrend: 0,
  minRatingTrend: 0,
  availableTrend: 0,
  disabledTrend: 0,
  institutionTrend: 0,
  averageRatingTrend: 0,
  totalConsultationsTrend: 0,
})

const searchForm = ref({
  realName: '',
  organization: '',
  specialty: '',
  serviceStatus: '',
  userName: '',
  dateRange: [] as string[],
})

const REVIEW_DATE_RANGE_DEFAULT_TIME = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59),
]

const EXPERT_ASSET_PREFIX = 'smart_farm_expert_asset:'
const expertAssetObjectUrls = new Set<string>()

const pathTabMap: Record<string, ExpertTab> = {
  '/expert/list': 'info',
  '/expert/audit': 'audit',
  '/expert/evaluation': 'evaluation',
}

const tabLabels: Record<ExpertTab, string> = {
  info: '专家信息管理',
  audit: '专家认证审核',
  evaluation: '专家评价管理',
}

const auditStatusMap: Record<number, { label: string; type: 'info' | 'warning' | 'success' | 'danger' }> = {
  0: { label: '未提交', type: 'info' },
  1: { label: '待审核', type: 'warning' },
  2: { label: '审核通过', type: 'success' },
  3: { label: '审核不通过', type: 'danger' },
}

const serviceStatusMap: Record<number, { label: string; type: 'success' | 'info' }> = {
  1: { label: '可咨询', type: 'success' },
  0: { label: '不可咨询', type: 'info' },
}

const showOrganizationFilter = computed(() => activeTab.value !== 'evaluation')

const showServiceFilter = computed(() => activeTab.value === 'info')

const showSpecialtyFilter = computed(() => activeTab.value === 'audit')

const showReviewFilters = computed(() => activeTab.value === 'evaluation')

const showInfoManageActions = computed(() => activeTab.value === 'info')

const showEvaluationManageActions = computed(() => activeTab.value === 'evaluation')

const isAuditPending = computed(() => auditForm.auditStatus === 1)

const auditDialogTitle = computed(() => (isAuditPending.value ? '专家认证审核' : '专家认证详情'))

const getAuditActionText = (auditStatus: number) => {
  if (auditStatus === 2) {
    return '通过'
  }

  if (auditStatus === 3) {
    return '不通过'
  }

  return '审核'
}

const getAuditActionType = (auditStatus: number) => {
  if (auditStatus === 2) {
    return 'success'
  }

  if (auditStatus === 3) {
    return 'danger'
  }

  return 'primary'
}

const createStatCard = (
  title: string,
  value: string,
  trendValue: number,
  icon: Component,
  tone: string,
): StatCard => ({
  title,
  value,
  desc: '较昨日',
  trend: formatTrend(trendValue),
  trendType: trendValue >= 0 ? 'up' : 'down',
  icon,
  tone,
})

const statCards = computed<StatCard[]>(() => {
  const stats = expertStats.value

  if (activeTab.value === 'audit') {
    return [
      createStatCard('审核总数', stats.auditTotal.toLocaleString(), stats.auditTotalTrend, UserFilled, 'blue'),
      createStatCard('待认证专家', stats.pending.toLocaleString(), stats.pendingTrend, DocumentChecked, 'orange'),
      createStatCard('认证通过', stats.approved.toLocaleString(), stats.approvedTrend, CircleCheckFilled, 'green'),
      createStatCard('已审核数', stats.reviewed.toLocaleString(), stats.reviewedTrend, CircleCloseFilled, 'gray'),
    ]
  }

  if (activeTab.value === 'evaluation') {
    return [
      createStatCard('评价总条数', stats.reviewTotal.toLocaleString(), stats.reviewTotalTrend, DocumentChecked, 'blue'),
      createStatCard('评价用户总数', stats.reviewUserCount.toLocaleString(), stats.reviewUserTrend, UserFilled, 'green'),
      createStatCard('平均评分', stats.averageRating.toFixed(1), stats.averageRatingTrend, StarFilled, 'purple'),
      createStatCard('最低评分', stats.minRating.toFixed(1), stats.minRatingTrend, CircleCloseFilled, 'gray'),
    ]
  }

  return [
    createStatCard('专家总数', stats.total.toLocaleString(), stats.totalTrend, UserFilled, 'blue'),
    createStatCard('可咨询专家', stats.available.toLocaleString(), stats.availableTrend, Medal, 'green'),
    createStatCard('禁用专家', stats.disabled.toLocaleString(), stats.disabledTrend, CircleCloseFilled, 'gray'),
    createStatCard('机构总数', stats.institutionCount.toLocaleString(), stats.institutionTrend, OfficeBuilding, 'purple'),
  ]
})

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      normalizedValue !== '/default-avatar.svg' &&
      !normalizedValue.toLowerCase().includes('default-avatar') &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredAssetUrl = (value?: string) => {
  if (!value?.startsWith(EXPERT_ASSET_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveAssetUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue
  }

  return getStoredAssetUrl(normalizedValue)
}

/**
 * 头像和证书位于受保护的上传目录，原生 img 请求不会携带管理端 JWT。
 * 通过统一请求层下载 Blob 后再创建本地地址，兼容鉴权和 token 自动刷新。
 */
const createExpertAssetPreview = async (value?: string) => {
  const resolvedUrl = resolveAssetUrl(value)

  if (!resolvedUrl || /^(blob:|data:image\/)/.test(resolvedUrl)) {
    return resolvedUrl
  }

  try {
    const response = await downloadExpertAsset(resolvedUrl)
    const objectUrl = URL.createObjectURL(response.data)
    expertAssetObjectUrls.add(objectUrl)
    return objectUrl
  } catch {
    return ''
  }
}

const releaseExpertAssetPreviews = (urls = expertAssetObjectUrls) => {
  urls.forEach((url) => URL.revokeObjectURL(url))
  urls.clear()
}

const mapExpertsWithAssetPreviews = async (items: ExpertProfile[], includeCertificate: boolean) => {
  const previousObjectUrls = new Set(expertAssetObjectUrls)
  expertAssetObjectUrls.clear()

  const rows = await Promise.all(
    items.map(async (item) => {
      const row = mapExpertToRow(item)
      row.avatarUrl = await createExpertAssetPreview(item.avatar)

      if (includeCertificate) {
        row.certificateUrl = await createExpertAssetPreview(item.certificateUrl)
      }

      return row
    }),
  )

  releaseExpertAssetPreviews(previousObjectUrls)
  return rows
}

const getAvatarText = (expert: ExpertProfile) => {
  const firstText = expert.realName?.trim().slice(0, 1) || expert.username?.trim().slice(0, 1)

  return firstText || '专'
}

/**
 * 将后端 ExpertProfile 实体转换为表格展示行。
 * 页面展示字段和接口字段分开映射，后续调整列布局时不会影响接口层。
 */
const mapExpertToRow = (expert: ExpertProfile): ExpertRow => {
  return {
    id: expert.id ?? Date.now(),
    userId: expert.userId,
    avatarText: getAvatarText(expert),
    avatarUrl: resolveAssetUrl(expert.avatar),
    realName: expert.realName || '-',
    organization: expert.organization || '-',
    jobTitle: expert.jobTitle || '-',
    specialty: expert.specialty || '-',
    introduction: expert.introduction || '-',
    certificateUrl: resolveAssetUrl(expert.certificateUrl),
    certificateName: expert.certificateName || '-',
    certificateAuditStatus: expert.certificateAuditStatus ?? 1,
    rating: Number(expert.rating ?? 0),
    consultationCount: expert.consultationCount ?? 0,
    auditStatus: expert.auditStatus ?? 1,
    auditPassTime: formatDateTime(expert.auditPassTime),
    serviceStatus: expert.serviceStatus ?? 1,
    consultationStatus: expert.consultationStatus ?? expert.serviceStatus ?? 1,
    status: expert.status === 0 ? 'disabled' : 'enabled',
    remark: expert.remark || '-',
    createTime: formatDateTime(expert.createTime),
  }
}

const mapReviewToRow = (review: ExpertReview): ExpertRow => {
  const expertName = review.expertName || '-'

  return {
    id: review.id ?? Date.now(),
    userId: review.userId,
    avatarText: expertName.slice(0, 1) || '评',
    realName: expertName,
    organization: '-',
    jobTitle: '-',
    specialty: '-',
    introduction: '-',
    certificateName: '-',
    certificateAuditStatus: 2,
    rating: Number(review.rating ?? 0),
    consultationCount: 0,
    auditStatus: 2,
    auditPassTime: '-',
    serviceStatus: 1,
    consultationStatus: 1,
    status: 'enabled',
    remark: '-',
    userName: review.userName || '-',
    content: review.content || '-',
    createTime: formatDateTime(review.createTime),
  }
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }

  return value.slice(0, 19)
}

const formatTrend = (value: number) => {
  const displayValue = Number.isInteger(value) ? Math.abs(value).toLocaleString() : Math.abs(value).toFixed(1)

  return `${value >= 0 ? '↑' : '↓'} ${displayValue}`
}

const getNumberValue = (value: string) => {
  return value === '' ? undefined : Number(value)
}

const getStatisticNumber = (value: unknown) => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

const fetchExpertStats = async () => {
  if (activeTab.value === 'evaluation') {
    const result = await getExpertReviewStatistics()
    const stats = result.data || {}

    expertStats.value = {
      ...expertStats.value,
      reviewTotal: getStatisticNumber(stats.reviewTotalCount),
      reviewUserCount: getStatisticNumber(stats.reviewUserCount),
      averageRating: getStatisticNumber(stats.averageRating),
      minRating: getStatisticNumber(stats.minRating),
      reviewTotalTrend: getStatisticNumber(stats.reviewTotalTrend),
      reviewUserTrend: getStatisticNumber(stats.reviewUserTrend),
      averageRatingTrend: getStatisticNumber(stats.averageRatingTrend),
      minRatingTrend: getStatisticNumber(stats.minRatingTrend),
    }
    return
  }

  const profileResult = await getExpertProfileStatistics()
  const stats = profileResult.data || {}

  expertStats.value = {
    total: getStatisticNumber(stats.totalCount),
    auditTotal: getStatisticNumber(stats.auditTotalCount),
    pending: getStatisticNumber(stats.pendingCount),
    approved: getStatisticNumber(stats.approvedCount),
    rejected: getStatisticNumber(stats.rejectedCount),
    reviewed: getStatisticNumber(stats.reviewedCount),
    reviewTotal: expertStats.value.reviewTotal,
    reviewUserCount: expertStats.value.reviewUserCount,
    minRating: expertStats.value.minRating,
    available: getStatisticNumber(stats.availableCount),
    suspended: getStatisticNumber(stats.suspendedCount),
    disabled: getStatisticNumber(stats.disabledCount),
    institutionCount: getStatisticNumber(stats.institutionCount),
    averageRating: getStatisticNumber(stats.averageRating),
    totalConsultations: getStatisticNumber(stats.totalConsultations),
    totalTrend: getStatisticNumber(stats.totalTrend),
    auditTotalTrend: getStatisticNumber(stats.auditTotalTrend),
    pendingTrend: getStatisticNumber(stats.pendingTrend),
    approvedTrend: getStatisticNumber(stats.approvedTrend),
    rejectedTrend: getStatisticNumber(stats.rejectedTrend),
    reviewedTrend: getStatisticNumber(stats.reviewedTrend),
    reviewTotalTrend: expertStats.value.reviewTotalTrend,
    reviewUserTrend: expertStats.value.reviewUserTrend,
    minRatingTrend: expertStats.value.minRatingTrend,
    availableTrend: getStatisticNumber(stats.availableTrend),
    disabledTrend: getStatisticNumber(stats.disabledTrend),
    institutionTrend: getStatisticNumber(stats.institutionTrend),
    averageRatingTrend: getStatisticNumber(stats.averageRatingTrend),
    totalConsultationsTrend: getStatisticNumber(stats.totalConsultationsTrend),
  }
}

/**
 * 不同专家功能使用不同的默认数据范围。
 * - 专家信息管理：展示全部专家，方便做基础维护。
 * - 专家认证审核：只聚焦待审核资料，减少审核人员干扰。
 * - 专家评价管理：默认聚焦审核通过专家，评价数据只沉淀到有效专家上。
 */
const getTabFixedParams = () => {
  if (activeTab.value === 'evaluation') {
    return { auditStatus: 2 }
  }

  return {}
}

const fetchExperts = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    if (activeTab.value === 'evaluation') {
      const [startTime, endTime] = searchForm.value.dateRange
      const result = await listExpertReviews({
        expertName: searchForm.value.realName || undefined,
        userName: searchForm.value.userName || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined,
        pageNum,
        pageSize: size,
      })

      releaseExpertAssetPreviews()
      experts.value = result.data.list.map((item) => mapReviewToRow(item))
      total.value = result.data.total
      currentPage.value = result.data.pageNum
      pageSize.value = result.data.pageSize
      selectedRows.value = []
      return
    }

    if (activeTab.value === 'audit') {
      const result = await listExpertAuditProfiles({
        realName: searchForm.value.realName || undefined,
        organization: searchForm.value.organization || undefined,
        specialty: searchForm.value.specialty || undefined,
        pageNum,
        pageSize: size,
      })

      experts.value = await mapExpertsWithAssetPreviews(result.data.list, true)
      total.value = result.data.total
      currentPage.value = result.data.pageNum
      pageSize.value = result.data.pageSize
      selectedRows.value = []
      return
    }

    const result = await listExpertProfiles({
      ...getTabFixedParams(),
      realName: searchForm.value.realName || undefined,
      organization: showOrganizationFilter.value ? searchForm.value.organization || undefined : undefined,
      specialty: showSpecialtyFilter.value ? searchForm.value.specialty || undefined : undefined,
      serviceStatus: showServiceFilter.value
        ? getNumberValue(searchForm.value.serviceStatus)
        : undefined,
      pageNum,
      pageSize: size,
    })

    experts.value = await mapExpertsWithAssetPreviews(result.data.list, false)
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.value = {
    realName: '',
    organization: '',
    specialty: '',
    serviceStatus: '',
    userName: '',
    dateRange: [],
  }
  void fetchExperts(1)
}

const handleSelectionChange = (rows: ExpertRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editExpertId.value = null
  addDialogVisible.value = true
}

const openEditDialog = (row: ExpertRow) => {
  editExpertId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchExperts(editExpertId.value ? currentPage.value : 1)
  void fetchExpertStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchExperts(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchExperts(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return experts.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => {
  return statusLoadingIds.value.includes(id)
}

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleConsultationStatus = async (row: ExpertRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.consultationStatus !== 1
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateExpertConsultationStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '专家已设为可咨询' : '专家已设为不可咨询')
    void fetchExperts(currentPage.value)
    void fetchExpertStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDelete = async (row: ExpertRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除专家「${row.realName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteExpertProfile(row.id)
    ElMessage.success('删除专家资料成功')
    void fetchExperts(getNextPageAfterDelete(1))
    void fetchExpertStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleReviewDelete = async (row: ExpertRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除专家「${row.realName}」的这条评价吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteExpertReview(row.id)
    ElMessage.success('删除专家评价成功')
    void fetchExperts(getNextPageAfterDelete(1))
    void fetchExpertStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning(activeTab.value === 'evaluation' ? '请先选择需要删除的评价' : '请先选择需要删除的专家资料')
    return
  }

  if (activeTab.value === 'evaluation') {
    try {
      await ElMessageBox.confirm(
        `确定要批量删除已选中的 ${selectedRows.value.length} 条评价吗？`,
        '批量删除确认',
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
        },
      )

      await batchDeleteExpertReviews(selectedRows.value.map((row) => row.id))
      ElMessage.success('批量删除专家评价成功')
      void fetchExperts(getNextPageAfterDelete(selectedRows.value.length))
      void fetchExpertStats()
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        ElMessage.info('已取消批量删除')
      }
    }
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条专家资料吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteExpertProfiles(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除专家资料成功')
    void fetchExperts(getNextPageAfterDelete(selectedRows.value.length))
    void fetchExpertStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const openReviewDialog = (row: ExpertRow) => {
  reviewForm.id = row.id
  reviewForm.realName = row.realName
  reviewForm.userName = row.userName || '-'
  reviewForm.rating = row.rating
  reviewForm.content = row.content === '-' ? '' : row.content || ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewForm.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }

  await updateExpertReview({
    id: reviewForm.id,
    rating: reviewForm.rating,
    content: reviewForm.content.trim(),
  })
  ElMessage.success('编辑专家评价成功')
  reviewDialogVisible.value = false
  void fetchExperts(currentPage.value)
  void fetchExpertStats()
}

const openAuditDialog = (row: ExpertRow) => {
  auditForm.id = row.id
  auditForm.auditStatus = row.auditStatus
  auditForm.realName = row.realName
  auditForm.organization = row.organization === '-' ? '' : row.organization
  auditForm.jobTitle = row.jobTitle === '-' ? '' : row.jobTitle
  auditForm.specialty = row.specialty === '-' ? '' : row.specialty
  auditForm.introduction = row.introduction === '-' ? '' : row.introduction
  auditForm.avatarUrl = row.avatarUrl || ''
  auditForm.certificateName = row.certificateName === '-' ? '' : row.certificateName
  auditForm.certificateUrl = row.certificateUrl || ''
  auditForm.remark = row.remark === '-' ? '' : row.remark
  auditDialogVisible.value = true
}

const submitAudit = async (auditStatus: number) => {
  if (!isAuditPending.value) {
    return
  }

  await auditExpertProfile(auditForm.id, auditStatus, auditForm.remark)
  ElMessage.success(auditStatus === 2 ? '专家认证审核通过' : '专家认证申请已驳回')
  auditDialogVisible.value = false
  void fetchExperts(currentPage.value)
  void fetchExpertStats()
}

watch(
  () => route.path,
  (path) => {
    activeTab.value = pathTabMap[path] || 'info'
    selectedRows.value = []

    if (experts.value.length) {
      void fetchExperts(1)
    }

    void fetchExpertStats()
  },
  { immediate: true },
)

onMounted(() => {
  void fetchExperts()
})

onBeforeUnmount(() => {
  releaseExpertAssetPreviews()
})
</script>

<template>
  <AdminPageFrame
    :breadcrumbs="['首页', '专家管理', tabLabels[activeTab]]"
    page-class="expert-page"
    shell-class="expert-shell"
    content-class="expert-content"
  >
        <section class="stats-grid">
          <article v-for="card in statCards" :key="card.title" class="stat-card">
            <div class="stat-icon" :class="`tone-${card.tone}`">
              <el-icon><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <p>{{ card.title }}</p>
              <strong>{{ card.value }}</strong>
              <span class="stat-trend">
                <small>{{ card.desc }}</small>
                <b :class="card.trendType === 'up' ? 'trend-up' : 'trend-down'">{{ card.trend }}</b>
              </span>
            </div>
          </article>
        </section>

        <ListFilterPanel
            :class="{ 'without-manage': !showInfoManageActions }"
            :model="searchForm"
            :show-manage="showInfoManageActions"
          >
            <el-form-item label="专家姓名">
              <el-input v-model="searchForm.realName" placeholder="请输入专家姓名" clearable />
            </el-form-item>
            <el-form-item v-if="showOrganizationFilter" label="所属机构">
              <el-input v-model="searchForm.organization" placeholder="请输入所属机构" clearable />
            </el-form-item>
            <el-form-item v-if="showSpecialtyFilter" label="擅长方向">
              <el-input v-model="searchForm.specialty" placeholder="请输入擅长方向" clearable />
            </el-form-item>
            <el-form-item v-if="showServiceFilter" label="咨询状态">
              <el-select v-model="searchForm.serviceStatus" placeholder="全部状态" clearable>
                <el-option label="可咨询" value="1" />
                <el-option label="不可咨询" value="0" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="showReviewFilters" label="评价用户">
              <el-input v-model="searchForm.userName" placeholder="请输入评价用户" clearable />
            </el-form-item>
            <el-form-item v-if="showReviewFilters" label="评价时间">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                range-separator="至"
                :default-time="REVIEW_DATE_RANGE_DEFAULT_TIME"
                clearable
              />
            </el-form-item>
            <template #actions>
              <el-button type="primary" :icon="Search" @click="fetchExperts(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
              <el-button
                v-if="showEvaluationManageActions"
                type="danger"
                plain
                :icon="Delete"
                @click="handleBatchDelete"
              >
                批量删除
              </el-button>
            </template>
            <template #manage>
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增专家</el-button>
            </template>
        </ListFilterPanel>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="experts"
              row-key="id"
              class="expert-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column v-if="activeTab === 'info' || activeTab === 'evaluation'" type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column v-if="activeTab !== 'evaluation'" label="头像" width="96" align="center" header-align="center">
                <template #default="{ row }">
                  <el-avatar
                    v-if="row.avatarUrl"
                    class="table-avatar"
                    :size="34"
                    :src="row.avatarUrl"
                    :alt="`${row.realName}的头像`"
                    fit="contain"
                  />
                  <span v-else class="default-avatar">{{ row.avatarText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="realName" label="专家姓名" min-width="130" align="center" header-align="center" />
              <el-table-column v-if="activeTab !== 'evaluation'" prop="organization" label="所属机构" min-width="160" align="center" header-align="center" />
              <el-table-column v-if="activeTab !== 'evaluation'" prop="jobTitle" label="职称" min-width="120" align="center" header-align="center" />
              <el-table-column v-if="activeTab !== 'evaluation'" prop="specialty" label="擅长方向" min-width="180" align="center" header-align="center" show-overflow-tooltip />

              <el-table-column v-if="activeTab === 'audit'" prop="certificateName" label="证书名称" min-width="150" align="center" header-align="center" show-overflow-tooltip />

              <el-table-column v-if="activeTab === 'audit'" label="认证材料" width="116" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.certificateUrl"
                    class="table-cert"
                    :src="row.certificateUrl"
                    :alt="`${row.realName}的资质证书`"
                    fit="contain"
                    :preview-src-list="[row.certificateUrl]"
                    preview-teleported
                  />
                  <span v-else>-</span>
                </template>
              </el-table-column>

              <el-table-column v-if="activeTab === 'audit'" prop="introduction" label="专家简介" min-width="190" align="center" header-align="center" show-overflow-tooltip />

              <el-table-column v-if="activeTab !== 'evaluation'" label="审核状态" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <StatusTag
                    :type="auditStatusMap[row.auditStatus]?.type || 'info'"
                    :label="auditStatusMap[row.auditStatus]?.label || '-'"
                  />
                </template>
              </el-table-column>

              <el-table-column v-if="activeTab === 'info'" label="咨询状态" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <StatusTag
                    :type="serviceStatusMap[row.consultationStatus]?.type || 'info'"
                    :label="serviceStatusMap[row.consultationStatus]?.label || '-'"
                  />
                </template>
              </el-table-column>

              <el-table-column v-if="activeTab === 'info'" prop="createTime" label="创建时间" min-width="170" align="center" header-align="center" />

              <el-table-column v-if="activeTab === 'audit'" prop="auditPassTime" label="通过时间" min-width="170" align="center" header-align="center" />

              <el-table-column v-if="activeTab === 'evaluation'" label="评分" min-width="160" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="rating-cell">
                    <el-rate :model-value="row.rating" disabled allow-half />
                    <b>{{ row.rating.toFixed(1) }}</b>
                  </div>
                </template>
              </el-table-column>
              <el-table-column v-if="activeTab === 'evaluation'" prop="userName" label="评价用户" min-width="130" align="center" header-align="center" />
              <el-table-column v-if="activeTab === 'evaluation'" prop="content" label="评价" min-width="260" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column v-if="activeTab === 'evaluation'" prop="createTime" label="评价时间" min-width="170" align="center" header-align="center" />

              

              <el-table-column v-if="activeTab === 'audit'" prop="remark" label="审核意见" min-width="150" align="center" header-align="center" show-overflow-tooltip />
              

              <el-table-column label="操作" width="300" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <template v-if="activeTab === 'info'">
                      <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
                      <el-button
                        link
                        :type="row.consultationStatus === 1 ? 'warning' : 'success'"
                        :loading="isStatusUpdating(row.id)"
                        @click="handleToggleConsultationStatus(row)"
                      >
                        {{ row.consultationStatus === 1 ? '禁用' : '启用' }}
                      </el-button>
                      <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                    </template>
                    <template v-else-if="activeTab === 'audit'">
                      <el-button
                        link
                        :type="getAuditActionType(row.auditStatus)"
                        :icon="DocumentChecked"
                        @click="openAuditDialog(row)"
                      >
                        {{ getAuditActionText(row.auditStatus) }}
                      </el-button>
                    </template>
                    <template v-else>
                      <el-button link type="primary" :icon="EditPen" @click="openReviewDialog(row)">编辑</el-button>
                      <el-button link type="danger" :icon="Delete" @click="handleReviewDelete(row)">删除</el-button>
                    </template>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <ListPagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            @current-change="handleCurrentPageChange"
            @size-change="handlePageSizeChange"
          />
        </section>

    <template #overlay>
      <AddOrUpdate v-model="addDialogVisible" :id="editExpertId" @success="handleSaveSuccess" />

      <ExpertAuditDialog
        v-model="auditDialogVisible"
        :title="auditDialogTitle"
        :form="auditForm"
        :pending="isAuditPending"
        :status-type="auditStatusMap[auditForm.auditStatus]?.type || 'info'"
        :status-label="auditStatusMap[auditForm.auditStatus]?.label || '-'"
        @decision="submitAudit"
      />
      <ExpertReviewDialog v-model="reviewDialogVisible" :form="reviewForm" @save="submitReview" />
    </template>
  </AdminPageFrame>
</template>

<style scoped src="../../common/styles/pages/expert-list.css"></style>
