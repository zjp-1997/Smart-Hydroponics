<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  DataAnalysis,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  Tools,
  View,
} from '@element-plus/icons-vue'
import {
  batchDeleteFarmTasks,
  deleteFarmTask,
  getFarmTaskById,
  getFarmTaskStatistics,
  listFarmTasks,
  updateFarmTaskStatus,
  type FarmTask,
} from '@/api/farmTask'
import { listFarmTaskRecords, type FarmTaskRecord } from '@/api/farmTaskRecord'

const route = useRoute()
import AddOrUpdate from './AddOrUpdate.vue'

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const tableLoading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
// 状态动作独立显示加载状态，防止连续点击产生并发请求。
const statusActionLoading = ref(false)
const formVisible = ref(false)
const editTaskId = ref<number | null>(null)
const selectedRows = ref<FarmTask[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref<FarmTask[]>([])
const currentDetail = ref<FarmTask | null>(null)
const taskRecords = ref<FarmTaskRecord[]>([])
const stats = ref<Record<string, number>>({})

// 农事任务管理查询表单，覆盖任务标题、地块、类型、状态、来源和截至时间范围。
const searchForm = ref<{
  taskTitle: string
  plotName: string
  taskType: '' | number
  status: '' | number
  sourceType: string
  dateRange: string[]
}>({
  taskTitle: '',
  plotName: '',
  taskType: '',
  status: '',
  sourceType: '',
  dateRange: [],
})


const taskStatusMap: Record<number, { text: string; tone: string }> = {
  1: { text: '未开始', tone: 'status-pending' },
  2: { text: '进行中', tone: 'status-running' },
  3: { text: '已完成', tone: 'status-success' },
  4: { text: '已逾期', tone: 'status-failed' },
  5: { text: '已取消', tone: 'status-muted' },
}

const taskTypeMap: Record<number, string> = {
  1: '浇水',
  2: '施肥',
  3: '打药',
  4: '采收',
  5: '巡检',
  6: '除草',
  7: '补光',
  8: '其他',
}

const priorityMap: Record<number, string> = {
  1: '低',
  2: '普通',
  3: '高',
  4: '紧急',
}

const actionMap: Record<number, string> = {
  1: '开始执行',
  2: '完成任务',
  3: '反馈',
  4: '优化',
}

// 顶部统计卡片展示任务管理需要关注的总量、AI来源、执行中和已完成数量。
const statCards = computed<StatCard[]>(() => [
  { title: '任务总数', value: formatCount(stats.value.totalCount), desc: '全部农事任务', icon: Tools, tone: 'blue' },
  { title: 'AI生成', value: formatCount(stats.value.aiGeneratedCount), desc: '来源于识别方案', icon: DataAnalysis, tone: 'green' },
  { title: '进行中数', value: formatCount(stats.value.runningCount), desc: '正在执行处理', icon: Refresh, tone: 'orange' },
  { title: '已完成数', value: formatCount(stats.value.completedCount), desc: '已形成执行闭环', icon: CircleCheckFilled, tone: 'gray' },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: number) => Number(value || 0).toLocaleString()

const getStatusMeta = (status?: number) => status == null
  ? { text: '-', tone: 'status-muted' }
  : taskStatusMap[status] || { text: '-', tone: 'status-muted' }

// 构建分页查询参数时过滤空字符串，避免后端收到无意义的 LIKE 条件。
const getQueryParams = () => {
  const [startDate, endDate] = searchForm.value.dateRange
  return {
    taskTitle: searchForm.value.taskTitle || undefined,
    plotName: searchForm.value.plotName || undefined,
    taskType: searchForm.value.taskType === '' ? undefined : searchForm.value.taskType,
    status: searchForm.value.status === '' ? undefined : searchForm.value.status,
    sourceType: searchForm.value.sourceType || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
  }
}

// 读取任务统计数据，接口返回字段与统计卡片 computed 一一对应。
const fetchStats = async () => {
  const result = await getFarmTaskStatistics()
  stats.value = result.data || {}
}

// 分页读取农事任务列表，并重置表格选择状态。
const fetchTasks = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const result = await listFarmTasks({ ...getQueryParams(), pageNum, pageSize: size })
    rows.value = result.data.list || []
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
    taskTitle: '',
    plotName: '',
    taskType: '',
    status: '',
    sourceType: '',
    dateRange: [],
  }
  void fetchTasks(1)
}

const openAdd = () => {
  editTaskId.value = null
  formVisible.value = true
}

const openEdit = (row: FarmTask) => {
  editTaskId.value = row.id
  formVisible.value = true
}

const handleFormSuccess = () => {
  void fetchTasks(editTaskId.value ? currentPage.value : 1)
  void fetchStats()
}

const handleSelectionChange = (value: FarmTask[]) => {
  selectedRows.value = value
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
}

const handleDelete = async (row: FarmTask) => {
  try {
    await ElMessageBox.confirm(`确定删除农事任务“${row.taskTitle}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteFarmTask(row.id)
    ElMessage.success('删除农事任务成功')
    void fetchTasks(getNextPageAfterDelete(1))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的农事任务')
    return
  }
  try {
    await ElMessageBox.confirm(`确定批量删除已选中的 ${selectedRows.value.length} 条农事任务吗？`, '批量删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await batchDeleteFarmTasks(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除农事任务成功')
    void fetchTasks(getNextPageAfterDelete(selectedRows.value.length))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const openDetail = async (row: FarmTask) => {
  detailVisible.value = true
  detailLoading.value = true
  currentDetail.value = null
  taskRecords.value = []
  try {
    const result = await getFarmTaskById(row.id)
    currentDetail.value = result.data
    const records = await listFarmTaskRecords({ taskId: row.id, pageNum: 1, pageSize: 20 })
    // 管理端详情同样按时间正序展示，保证阅读顺序为开始、进行中、已完成。
    taskRecords.value = [...(records.data.list || [])].reverse()
  } finally {
    detailLoading.value = false
  }
}

const changeStatus = async (status: number) => {
  if (!currentDetail.value || statusActionLoading.value) return
  statusActionLoading.value = true
  try {
    const result = await updateFarmTaskStatus(currentDetail.value.id, status)
    currentDetail.value = result.data
    // 状态动作成功后立即重载时间线，让管理人员看到本次开始或完成记录。
    const records = await listFarmTaskRecords({ taskId: currentDetail.value.id, pageNum: 1, pageSize: 50 })
    taskRecords.value = [...(records.data.list || [])].reverse()
    ElMessage.success(status === 2 ? '任务已开始执行' : '任务已完成')
    void fetchTasks()
    void fetchStats()
  } finally {
    statusActionLoading.value = false
  }
}

// 接收首页搜索和快捷入口意图，复用本页现有查询与新增表单。
const applyHomeRouteIntent = () => {
  const keyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  if (keyword) searchForm.value.taskTitle = keyword
  if (route.query.action === 'create') openAdd()
}

watch(() => route.fullPath, () => {
  applyHomeRouteIntent()
  if (route.query.keyword) void fetchTasks(1)
})

onMounted(() => {
  applyHomeRouteIntent()
  void fetchTasks()
  void fetchStats()
})
</script>

<template>
  <div class="farm-task-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />
    <div class="farm-task-shell admin-shell">
      <Header :breadcrumbs="['首页', '农事任务管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="farm-task-content admin-content">
        <section class="stats-grid">
          <article v-for="card in statCards" :key="card.title" class="stat-card">
            <div class="stat-icon" :class="`tone-${card.tone}`">
              <el-icon><component :is="card.icon" /></el-icon>
            </div>
            <div>
              <p>{{ card.title }}</p>
              <strong>{{ card.value }}</strong>
              <span>{{ card.desc }}</span>
            </div>
          </article>
        </section>

        <section class="filter-card">
          <el-form class="filter-form" :model="searchForm" label-width="76px">
            <el-form-item label="任务">
              <el-input v-model="searchForm.taskTitle" placeholder="请输入任务标题" clearable />
            </el-form-item>
            <el-form-item label="地块">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="searchForm.taskType" placeholder="全部类型" clearable>
                <el-option v-for="(label, value) in taskTypeMap" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchTasks(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="Plus" @click="openAdd">新增任务</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <el-table
            v-loading="tableLoading"
            :data="rows"
            row-key="id"
            height="100%"
            class="task-table"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="56" fixed="left" align="center" />
            <el-table-column prop="taskTitle" label="任务标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="plotName" label="地块" min-width="130" align="center" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">{{ taskTypeMap[row.taskType] || '-' }}</template>
            </el-table-column>
            <el-table-column label="优先级" width="100" align="center">
              <template #default="{ row }">{{ priorityMap[row.priority || 2] || '-' }}</template>
            </el-table-column>
            <el-table-column prop="deadlineTime" label="截至时间" min-width="170" align="center" />
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :class="getStatusMeta(row.status).tone" effect="light" round>
                  {{ getStatusMeta(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="来源" width="120" align="center">
              <template #default="{ row }">{{ row.sourceType === 'AI_SOLUTION' ? 'AI识别' : '手动创建' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="230" fixed="right" align="center">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
                  <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <span>共 {{ total.toLocaleString() }} 条</span>
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              background
              layout="sizes, prev, pager, next, jumper"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              @current-change="fetchTasks"
              @size-change="(size: number) => fetchTasks(1, size)"
            />
          </div>
        </section>
      </main>
    </div>

    <AddOrUpdate v-model="formVisible" :id="editTaskId" @success="handleFormSuccess" />

    <el-drawer v-model="detailVisible" title="农事任务详情" direction="rtl" size="720px" class="farm-task-drawer">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="currentDetail">
          <div class="detail-head">
            <div>
              <strong>{{ currentDetail.taskTitle }}</strong>
              <span>{{ currentDetail.plotName || '-' }} / {{ taskTypeMap[currentDetail.taskType] || '-' }}</span>
            </div>
            <el-tag :class="getStatusMeta(currentDetail.status).tone" effect="light" round>
              {{ getStatusMeta(currentDetail.status).text }}
            </el-tag>
          </div>

          <div class="drawer-actions">
            <el-button
              type="warning"
              plain
              :loading="statusActionLoading"
              :disabled="![1, 4].includes(currentDetail.status || 0)"
              @click="changeStatus(2)"
            >开始执行</el-button>
            <el-button
              type="success"
              plain
              :loading="statusActionLoading"
              :disabled="currentDetail.status !== 2"
              @click="changeStatus(3)"
            >完成农事</el-button>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="截至时间">{{ currentDetail.deadlineTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="是否逾期">{{ currentDetail.overdue ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="实际开始">{{ currentDetail.actualStartTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="实际完成">{{ currentDetail.actualEndTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="执行人">{{ currentDetail.executorName || currentDetail.executorId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ currentDetail.batchNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="任务内容" :span="2">{{ currentDetail.taskContent || '-' }}</el-descriptions-item>
            <el-descriptions-item label="完成备注" :span="2">{{ currentDetail.completeRemark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <section class="record-section">
            <h3>执行反馈</h3>
            <el-empty v-if="!taskRecords.length" description="暂无反馈记录" :image-size="90" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="record in taskRecords"
                :key="record.id"
                :timestamp="record.executeTime || record.createTime"
              >
                <strong>{{ actionMap[record.actionType] || '记录' }}</strong>
                <span v-if="record.operatorName">操作人：{{ record.operatorName }}</span>
                <span v-if="record.progressPercent != null">进度：{{ record.progressPercent }}%</span>
                <p>{{ record.feedbackDetail || record.actionContent || '-' }}</p>
                <span v-if="record.optimizeSuggestion">优化：{{ record.optimizeSuggestion }}</span>
              </el-timeline-item>
            </el-timeline>
          </section>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  flex: 0 0 auto;
  gap: 18px;
}

.stat-card,
.filter-card,
.table-card {
  background: #ffffff;
  border: 1px solid #eef2f7;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 108px;
  padding: 20px 24px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  font-size: 28px;
}

.tone-blue { background: #3b82f6; }
.tone-green { background: #10b981; }
.tone-orange { background: #f59e0b; }
.tone-gray { background: #64748b; }

.stat-card p {
  margin: 0;
  color: #475569;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.stat-card span {
  display: block;
  margin-top: 8px;
  color: #94a3b8;
  font-size: 12px;
}

.filter-card {
  flex: 0 0 auto;
  margin-top: 14px;
  padding: 16px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr)) auto auto;
  align-items: center;
  gap: 12px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-actions,
.manage-actions,
.table-actions,
.drawer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.table-actions {
  justify-content: center;
  gap: 2px;
}

.table-card {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 14px;
  padding: 12px;
}

.task-table {
  flex: 1 1 auto;
}

.task-table :deep(.el-table__header th) {
  color: #334155;
  background: #f8fafc;
  font-weight: 800;
}

.pagination-row {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 14px;
  color: #334155;
  font-size: 14px;
}

.status-pending,
.status-muted {
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.status-running {
  color: #d97706;
  background: #fffbeb;
  border-color: #fef3c7;
}

.status-success {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.status-failed {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fee2e2;
}

.detail-body {
  min-height: 200px;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  margin-bottom: 14px;
  border-bottom: 1px solid #eef2f7;
}

.detail-head strong,
.detail-head span {
  display: block;
}

.detail-head strong {
  color: #111827;
  font-size: 19px;
}

.detail-head span {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
}

.drawer-actions {
  justify-content: flex-end;
  margin-bottom: 14px;
}

.record-section {
  margin-top: 18px;
}

.record-section h3 {
  margin: 0 0 12px;
  color: #111827;
  font-size: 15px;
}

.record-section p {
  margin: 4px 0;
  color: #475569;
  line-height: 1.6;
}

.record-section span {
  color: #64748b;
  font-size: 12px;
}

:global(.farm-task-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 22px;
  border-bottom: 1px solid #eef2f7;
}

:global(.farm-task-drawer .el-drawer__body) {
  padding: 18px 22px 22px;
  overflow-y: auto;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-form {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }
}

@media (max-width: 768px) {
  .farm-task-shell {
    margin-left: 0;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
