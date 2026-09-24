<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Camera,
  CircleCheckFilled,
  DataAnalysis,
  Delete,
  Refresh,
  Search,
  Upload,
  View,
} from '@element-plus/icons-vue'
import {
  batchDeleteAiRecognitionRecords,
  deleteAiRecognitionRecord,
  getAiRecognitionRecordById,
  getAiRecognitionRecordStatistics,
  listAiRecognitionRecords,
  type AiRecognitionRecord,
  type AiRecognitionRecordStatistics,
} from '@/api/aiRecognitionRecord'
import {
  generateAiSolution,
  generateFarmTaskFromSolution,
  getLatestAiSolutionByRecordId,
  type AiSolution,
} from '@/api/aiSolution'
import { getFarmTaskById, type FarmTask } from '@/api/farmTask'
import { getFileUrl } from '@/utils/utils'
import {
  addFarmTaskRecord,
  listFarmTaskRecords,
  type FarmTaskRecord,
} from '@/api/farmTaskRecord'

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

interface RecordRow {
  id: number
  imageUrl: string
  imageText: string
  sourceType: number
  sourceText: string
  sourceTone: string
  camera: string
  plot: string
  resultName: string
  recognitionTypeName: string
  suggestion: string
  recognitionTime: string
  status: number
  statusText: string
  statusTone: string
}

const sidebarVisible = ref(false)
const selectedRows = ref<RecordRow[]>([])
const rows = ref<RecordRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const solutionLoading = ref(false)
const taskLoading = ref(false)
const feedbackSubmitting = ref(false)
const currentDetail = ref<AiRecognitionRecord | null>(null)
const currentSolution = ref<AiSolution | null>(null)
const currentFarmTask = ref<FarmTask | null>(null)
const taskRecords = ref<FarmTaskRecord[]>([])
const stats = ref<AiRecognitionRecordStatistics>({})
const feedbackForm = ref({
  resultStatus: 1,
  feedbackScore: 5,
  feedbackDetail: '',
  optimizeSuggestion: '',
})

const searchForm = ref<{
  username: string
  sourceType: '' | number
  status: '' | number
  cameraName: string
  plotName: string
  dateRange: string[]
}>({
  username: '',
  sourceType: '',
  status: '',
  cameraName: '',
  plotName: '',
  dateRange: [],
})

const sourceTypeMap: Record<number, { text: string; tone: string }> = {
  1: { text: '手动识别', tone: 'source-manual' },
  2: { text: '自动识别', tone: 'source-auto' },
}

const recordStatusMap: Record<number, { text: string; tone: string }> = {
  1: { text: '待识别', tone: 'status-pending' },
  2: { text: '识别中', tone: 'status-running' },
  3: { text: '已完成', tone: 'status-success' },
  4: { text: '识别失败', tone: 'status-failed' },
}

const resultStatusMap: Record<number, string> = {
  1: '成功',
  2: '失败',
  3: '处理中',
}

const priorityMap: Record<number, string> = {
  1: '低',
  2: '普通',
  3: '高',
  4: '紧急',
}

const taskStatusMap: Record<number, string> = {
  1: '未开始',
  2: '进行中',
  3: '已完成',
  4: '已逾期',
  5: '已取消',
}

const taskRecordActionMap: Record<number, string> = {
  1: '开始执行',
  2: '完成任务',
  3: '反馈',
  4: '优化',
}

const severityMap: Record<number, string> = {
  1: '轻微',
  2: '中等',
  3: '严重',
}

const statCards = computed<StatCard[]>(() => [
  {
    title: '识别总数',
    value: formatCount(stats.value.totalCount),
    desc: '全部AI识别记录',
    icon: DataAnalysis,
    tone: 'blue',
  },
  {
    title: '手动识别',
    value: formatCount(stats.value.manualCount),
    desc: '用户上传触发',
    icon: Upload,
    tone: 'green',
  },
  {
    title: '自动识别',
    value: formatCount(stats.value.autoCount),
    desc: '摄像头自动触发',
    icon: Camera,
    tone: 'orange',
  },
  {
    title: '已完成',
    value: formatCount(stats.value.completedCount),
    desc: '识别流程完成',
    icon: CircleCheckFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: number) => Number(value || 0).toLocaleString()

const formatImageSize = (size?: number) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const getImageText = (record: AiRecognitionRecord) => {
  return record.id ? String(record.id).slice(-1) : '识'
}

const getUserLabel = (record: AiRecognitionRecord) => {
  if (record.nickname && record.username) return `${record.nickname}（${record.username}）`
  return record.nickname || record.username || `用户${record.userId}`
}

const getSourceMeta = (sourceType?: number) => sourceTypeMap[sourceType || 0] || { text: '-', tone: 'source-unknown' }

const getStatusMeta = (status?: number) => status == null
  ? { text: '-', tone: 'status-pending' }
  : recordStatusMap[status] || { text: '-', tone: 'status-pending' }

const mapRecordToRow = (record: AiRecognitionRecord): RecordRow => {
  const source = getSourceMeta(record.sourceType)
  const status = getStatusMeta(record.status)

  return {
    id: record.id,
    imageUrl: getFileUrl(record.thumbnailUrl || record.imageUrl || ''),
    imageText: getImageText(record),
    sourceType: record.sourceType,
    sourceText: source.text,
    sourceTone: source.tone,
    camera: record.cameraName || (record.cameraId ? `摄像头${record.cameraId}` : '-'),
    plot: record.plotName || (record.plotId ? `地块${record.plotId}` : '-'),
    resultName: record.resultName || '-',
    recognitionTypeName: record.recognitionTypeName || '-',
    suggestion: record.suggestion || '-',
    recognitionTime:
      record.recognizeTime ||
      record.recognitionEndTime ||
      record.recognitionStartTime ||
      record.createTime ||
      '-',
    status: record.status,
    statusText: status.text,
    statusTone: status.tone,
  }
}

const getQueryParams = () => {
  const [startDate, endDate] = searchForm.value.dateRange
  return {
    username: searchForm.value.username || undefined,
    sourceType: searchForm.value.sourceType === '' ? undefined : searchForm.value.sourceType,
    status: searchForm.value.status === '' ? undefined : searchForm.value.status,
    cameraName: searchForm.value.cameraName || undefined,
    plotName: searchForm.value.plotName || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
  }
}

const fetchStats = async () => {
  const result = await getAiRecognitionRecordStatistics(getQueryParams())
  stats.value = result.data || {}
}

const fetchRecords = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const result = await listAiRecognitionRecords({
      ...getQueryParams(),
      pageNum,
      pageSize: size,
    })

    rows.value = result.data.list.map(mapRecordToRow)
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
    username: '',
    sourceType: '',
    status: '',
    cameraName: '',
    plotName: '',
    dateRange: [],
  }
  void fetchRecords(1)
  void fetchStats()
}

const handleSearch = () => {
  void fetchRecords(1)
  void fetchStats()
}

const handleSelectionChange = (value: RecordRow[]) => {
  selectedRows.value = value
}

const handleCurrentPageChange = (page: number) => {
  void fetchRecords(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchRecords(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const openDetail = async (row: RecordRow) => {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  currentSolution.value = null
  currentFarmTask.value = null
  taskRecords.value = []
  try {
    const result = await getAiRecognitionRecordById(row.id)
    currentDetail.value = result.data
    await loadProcessData(row.id)
  } finally {
    detailLoading.value = false
  }
}

const loadProcessData = async (recordId: number) => {
  currentSolution.value = null
  currentFarmTask.value = null
  taskRecords.value = []

  const solutionResult = await getLatestAiSolutionByRecordId(recordId)
  currentSolution.value = solutionResult.data || null

  if (currentSolution.value?.farmTaskId) {
    const taskResult = await getFarmTaskById(currentSolution.value.farmTaskId)
    currentFarmTask.value = taskResult.data || null
    await loadTaskRecords(currentSolution.value.farmTaskId)
  }
}

const loadTaskRecords = async (taskId: number) => {
  const result = await listFarmTaskRecords({ taskId, pageNum: 1, pageSize: 20 })
  taskRecords.value = result.data.list || []
}

const handleGenerateSolution = async (generateTask = false) => {
  if (!currentDetail.value) return
  solutionLoading.value = true
  try {
    const result = await generateAiSolution(currentDetail.value.id, generateTask)
    currentSolution.value = result.data
    if (currentSolution.value?.farmTaskId) {
      const taskResult = await getFarmTaskById(currentSolution.value.farmTaskId)
      currentFarmTask.value = taskResult.data
      await loadTaskRecords(currentSolution.value.farmTaskId)
    }
    ElMessage.success(generateTask ? '已生成处置方案和农事任务' : '已生成处置方案')
  } finally {
    solutionLoading.value = false
  }
}

const handleGenerateFarmTask = async () => {
  if (!currentDetail.value) return
  if (!currentDetail.value.plotId) {
    ElMessage.warning('手动识别记录没有地块信息，不能自动生成农事任务')
    return
  }

  taskLoading.value = true
  try {
    if (!currentSolution.value) {
      const solutionResult = await generateAiSolution(currentDetail.value.id, false)
      currentSolution.value = solutionResult.data
    }
    if (!currentSolution.value) return

    const taskResult = await generateFarmTaskFromSolution(currentSolution.value.id)
    currentFarmTask.value = taskResult.data
    const refreshedSolution = await getLatestAiSolutionByRecordId(currentDetail.value.id)
    currentSolution.value = refreshedSolution.data || currentSolution.value
    await loadTaskRecords(taskResult.data.id)
    ElMessage.success('已生成农事任务')
  } finally {
    taskLoading.value = false
  }
}

const submitTaskFeedback = async (actionType = 3) => {
  if (!currentFarmTask.value) {
    ElMessage.warning('请先生成农事任务')
    return
  }
  feedbackSubmitting.value = true
  try {
    await addFarmTaskRecord({
      taskId: currentFarmTask.value.id,
      actionType,
      resultStatus: feedbackForm.value.resultStatus,
      feedbackScore: feedbackForm.value.feedbackScore,
      feedbackDetail: feedbackForm.value.feedbackDetail,
      optimizeSuggestion: feedbackForm.value.optimizeSuggestion,
    })
    feedbackForm.value.feedbackDetail = ''
    feedbackForm.value.optimizeSuggestion = ''
    const taskResult = await getFarmTaskById(currentFarmTask.value.id)
    currentFarmTask.value = taskResult.data
    await loadTaskRecords(currentFarmTask.value.id)
    ElMessage.success(actionType === 2 ? '任务已完成并记录反馈' : '反馈已记录')
  } finally {
    feedbackSubmitting.value = false
  }
}

const refreshAfterDelete = (deletedCount: number) => {
  void fetchRecords(getNextPageAfterDelete(deletedCount))
  void fetchStats()
}

const handleDelete = async (row: RecordRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除识别记录 #${row.id} 吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteAiRecognitionRecord(row.id)
    ElMessage.success('删除AI识别记录成功')
    refreshAfterDelete(1)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的AI识别记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条AI识别记录吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteAiRecognitionRecords(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除AI识别记录成功')
    refreshAfterDelete(selectedRows.value.length)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchRecords()
  void fetchStats()
})
</script>

<template>
  <div class="ai-record-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="ai-record-shell admin-shell">
      <Header :breadcrumbs="['首页', 'AI识别管理', 'AI识别记录管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="ai-record-content admin-content">
        <section class="stats-grid">
          <article v-for="card in statCards" :key="card.title" class="stat-card">
            <div class="stat-icon" :class="`tone-${card.tone}`">
              <el-icon><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <p>{{ card.title }}</p>
              <strong>{{ card.value }}</strong>
              <span class="stat-desc">{{ card.desc }}</span>
            </div>
          </article>
        </section>

        <section class="filter-card">
          <el-form class="filter-form" :model="searchForm" label-width="76px">
            <el-form-item label="地块">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <el-form-item label="摄像头">
              <el-input v-model="searchForm.cameraName" placeholder="请输入摄像头名称" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="待识别" :value="1" />
                <el-option label="识别中" :value="2" />
                <el-option label="已完成" :value="3" />
                <el-option label="识别失败" :value="4" />
              </el-select>
            </el-form-item>
            
            
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="rows"
              row-key="id"
              class="ai-record-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="图片" width="96" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="table-image"
                    :src="row.imageUrl"
                    fit="cover"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                  />
                  <span v-else class="default-image">{{ row.imageText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="plot" label="地块" min-width="150" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="camera" label="摄像头" min-width="160" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="recognitionTypeName" label="识别类型" min-width="140" align="center" header-align="center" />
              <el-table-column label="识别方式" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="row.sourceTone" effect="light" round>{{ row.sourceText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="resultName" label="识别结果" min-width="150" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="suggestion" label="建议" min-width="260" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="recognitionTime" label="识别时间" min-width="180" align="center" header-align="center" />
              <el-table-column label="记录状态" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="row.statusTone" effect="light" round>{{ row.statusText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="160" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-row">
            <span>共 {{ total.toLocaleString() }} 条</span>
            <div class="pagination-box">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                background
                layout="sizes, prev, pager, next, jumper"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                @current-change="handleCurrentPageChange"
                @size-change="handlePageSizeChange"
              />
            </div>
          </div>
        </section>
      </main>
    </div>

    <el-drawer
      v-model="detailVisible"
      title="AI识别记录详情"
      direction="rtl"
      size="760px"
      class="ai-record-drawer"
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="currentDetail">
          <div class="detail-head">
            <el-image
              v-if="currentDetail.imageUrl"
              class="detail-image"
              :src="currentDetail.imageUrl"
              fit="cover"
              :preview-src-list="[currentDetail.imageUrl]"
              preview-teleported
            />
            <div class="detail-title">
              <strong>{{ currentDetail.resultName || '暂无识别结果' }}</strong>
              <span>{{ currentDetail.resultSummary || '暂无摘要' }}</span>
              <div class="detail-tags">
                <el-tag :class="getSourceMeta(currentDetail.sourceType).tone" effect="light" round>
                  {{ getSourceMeta(currentDetail.sourceType).text }}
                </el-tag>
                <el-tag :class="getStatusMeta(currentDetail.status).tone" effect="light" round>
                  {{ getStatusMeta(currentDetail.status).text }}
                </el-tag>
              </div>
            </div>
          </div>

          <section class="detail-section">
            <h3>识别信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="用户">{{ getUserLabel(currentDetail) }}</el-descriptions-item>
              <el-descriptions-item label="识别方式">{{ getSourceMeta(currentDetail.sourceType).text }}</el-descriptions-item>
              <el-descriptions-item label="地块">{{ currentDetail.plotName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="摄像头">{{ currentDetail.cameraName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="图片大小">{{ formatImageSize(currentDetail.imageSize) }}</el-descriptions-item>
              <el-descriptions-item label="记录状态">{{ getStatusMeta(currentDetail.status).text }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ currentDetail.recognitionStartTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ currentDetail.recognizeTime || currentDetail.recognitionEndTime || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h3>识别结果</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="识别类型">{{ currentDetail.recognitionTypeName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="结果数量">{{ currentDetail.resultCount ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="作物">{{ currentDetail.cropName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="病虫害">{{ currentDetail.diseasePestName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="严重程度">
                {{ currentDetail.severityLevel ? severityMap[currentDetail.severityLevel] || '-' : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="置信度">
                {{ currentDetail.confidence == null ? '-' : `${Number(currentDetail.confidence).toFixed(2)}%` }}
              </el-descriptions-item>
              <el-descriptions-item label="结果状态">
                {{ currentDetail.resultStatus ? resultStatusMap[currentDetail.resultStatus] || '-' : '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="模型">
                {{ currentDetail.modelName || '-' }}{{ currentDetail.modelVersion ? ` / ${currentDetail.modelVersion}` : '' }}
              </el-descriptions-item>
              <el-descriptions-item label="结果详情" :span="2">{{ currentDetail.resultDetail || '-' }}</el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="detail-section">
            <h3>处理建议</h3>
            <div class="detail-text">{{ currentDetail.suggestion || '-' }}</div>
            <div v-if="currentDetail.failReason" class="detail-warning">{{ currentDetail.failReason }}</div>
            <div class="detail-note">{{ currentDetail.remark || currentDetail.resultRemark || '-' }}</div>
          </section>
          <section class="detail-section process-section">
            <div class="section-title-row">
              <h3>AI识别流程</h3>
              <div class="process-actions">
                <el-button
                  type="primary"
                  plain
                  :icon="DataAnalysis"
                  :loading="solutionLoading"
                  :disabled="currentDetail.resultStatus !== 1"
                  @click="handleGenerateSolution(false)"
                >
                  生成方案
                </el-button>
                <el-button
                  type="success"
                  plain
                  :icon="CircleCheckFilled"
                  :loading="taskLoading || solutionLoading"
                  :disabled="currentDetail.resultStatus !== 1 || !currentDetail.plotId"
                  @click="handleGenerateFarmTask"
                >
                  生成任务
                </el-button>
              </div>
            </div>

            <el-empty v-if="!currentSolution" description="暂无处置方案" :image-size="92" />
            <template v-else>
              <div class="solution-panel">
                <div class="solution-title">
                  <strong>{{ currentSolution.solutionTitle }}</strong>
                  <el-tag type="warning" effect="light" round>
                    {{ priorityMap[currentSolution.priorityLevel || 2] || '-' }}
                  </el-tag>
                </div>
                <p>{{ currentSolution.solutionSummary || '-' }}</p>
                <pre>{{ currentSolution.solutionDetail || '-' }}</pre>
              </div>

              <div class="task-panel">
                <div class="task-head">
                  <strong>农事任务</strong>
                  <el-tag v-if="currentFarmTask" type="success" effect="light" round>
                    {{ taskStatusMap[currentFarmTask.status || 1] || '-' }}
                  </el-tag>
                </div>
                <el-empty v-if="!currentFarmTask" description="尚未生成农事任务" :image-size="80" />
                <template v-else>
                  <el-descriptions :column="2" border>
                    <el-descriptions-item label="任务标题">{{ currentFarmTask.taskTitle }}</el-descriptions-item>
                    <el-descriptions-item label="所属地块">{{ currentFarmTask.plotName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="截至时间">{{ currentFarmTask.deadlineTime || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="执行人">{{ currentFarmTask.executorName || currentFarmTask.executorId || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="来源">AI识别方案</el-descriptions-item>
                    <el-descriptions-item label="任务内容" :span="2">{{ currentFarmTask.taskContent || '-' }}</el-descriptions-item>
                  </el-descriptions>

                  <div class="feedback-box">
                    <div class="feedback-grid">
                      <el-select v-model="feedbackForm.resultStatus" placeholder="执行效果">
                        <el-option label="有效" :value="1" />
                        <el-option label="部分有效" :value="2" />
                        <el-option label="无效" :value="3" />
                      </el-select>
                      <el-rate v-model="feedbackForm.feedbackScore" />
                    </div>
                    <el-input
                      v-model="feedbackForm.feedbackDetail"
                      type="textarea"
                      :rows="3"
                      placeholder="填写执行反馈"
                    />
                    <el-input
                      v-model="feedbackForm.optimizeSuggestion"
                      type="textarea"
                      :rows="2"
                      placeholder="填写后续优化建议"
                    />
                    <div class="feedback-actions">
                      <el-button type="primary" plain :loading="feedbackSubmitting" @click="submitTaskFeedback(3)">
                        保存反馈
                      </el-button>
                      <el-button
                        type="success"
                        :loading="feedbackSubmitting"
                        :disabled="currentFarmTask.status === 3"
                        @click="submitTaskFeedback(2)"
                      >
                        完成任务
                      </el-button>
                    </div>
                  </div>

                  <el-timeline v-if="taskRecords.length" class="record-timeline">
                    <el-timeline-item
                      v-for="record in taskRecords"
                      :key="record.id"
                      :timestamp="record.executeTime || record.createTime"
                    >
                      <strong>{{ taskRecordActionMap[record.actionType] || '记录' }}</strong>
                      <p>{{ record.feedbackDetail || record.actionContent || '-' }}</p>
                      <span v-if="record.optimizeSuggestion">优化：{{ record.optimizeSuggestion }}</span>
                    </el-timeline-item>
                  </el-timeline>
                </template>
              </div>
            </template>
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped src="../../common/styles/pages/ai-recognition-record-list.css"></style>
