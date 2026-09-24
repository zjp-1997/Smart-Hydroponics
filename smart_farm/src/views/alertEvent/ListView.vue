<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  CircleCheckFilled,
  Delete,
  Refresh,
  Search,
  Timer,
  View,
  WarningFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteAlertEvents,
  deleteAlertEvent,
  getAlertEventById,
  getAlertEventStatistics,
  listAlertEvents,
  updateAlertProcessStatus,
  type AlertEvent,
} from '@/api/alertEvent'

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
const selectedRows = ref<AlertEvent[]>([])
const rows = ref<AlertEvent[]>([])
const currentDetail = ref<AlertEvent | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = ref<Record<string, number>>({})

const searchForm = ref<{
  plotName: string
  alertType: '' | number
  alertLevel: '' | number
  processStatus: '' | number
  sourceType: string
  dateRange: string[]
}>({
  plotName: '',
  alertType: '',
  alertLevel: '',
  processStatus: '',
  sourceType: '',
  dateRange: [],
})

const alertTypeMap: Record<number, string> = {
  1: '环境异常',
  2: '设备异常',
  3: '任务逾期',
  4: '摄像头异常',
  5: 'AI识别异常',
  6: '库存预警',
}

const alertLevelMap: Record<number, { text: string; type: 'info' | 'warning' | 'danger' }> = {
  1: { text: '普通', type: 'info' },
  2: { text: '重要', type: 'warning' },
  3: { text: '紧急', type: 'danger' },
}

const defaultAlertLevelMeta: { text: string; type: 'info' | 'warning' | 'danger' } = {
  text: '普通',
  type: 'info',
}

const processStatusMap: Record<number, { text: string; type: 'info' | 'primary' | 'success' | 'warning' }> = {
  1: { text: '未处理', type: 'warning' },
  2: { text: '处理中', type: 'primary' },
  3: { text: '已处理', type: 'success' },
  4: { text: '已忽略', type: 'info' },
}

const defaultProcessStatusMeta: { text: string; type: 'info' | 'primary' | 'success' | 'warning' } = {
  text: '未处理',
  type: 'warning',
}

const statCards = computed<StatCard[]>(() => [
  {
    title: '预警总数',
    value: String(stats.value.totalCount || 0),
    desc: '当前有效预警',
    icon: Bell,
    tone: 'blue',
  },
  {
    title: '未处理',
    value: String(stats.value.pendingCount || 0),
    desc: '等待响应',
    icon: Timer,
    tone: 'orange',
  },
  {
    title: '已处理',
    value: String(stats.value.handledCount || 0),
    desc: '闭环完成',
    icon: CircleCheckFilled,
    tone: 'green',
  },
  {
    title: '紧急预警',
    value: String(stats.value.urgentCount || 0),
    desc: '最高优先响应',
    icon: WarningFilled,
    tone: 'red',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const getLevelMeta = (level?: number) => alertLevelMap[level || 1] ?? defaultAlertLevelMeta
const getProcessMeta = (status?: number) => processStatusMap[status || 1] ?? defaultProcessStatusMeta

const fetchStats = async () => {
  const result = await getAlertEventStatistics()
  stats.value = result.data
}

const fetchRows = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const [startDate, endDate] = searchForm.value.dateRange
    const result = await listAlertEvents({
      plotName: searchForm.value.plotName || undefined,
      alertType: searchForm.value.alertType === '' ? undefined : searchForm.value.alertType,
      alertLevel: searchForm.value.alertLevel === '' ? undefined : searchForm.value.alertLevel,
      processStatus: searchForm.value.processStatus === '' ? undefined : searchForm.value.processStatus,
      sourceType: searchForm.value.sourceType || undefined,
      startDate,
      endDate,
      pageNum,
      pageSize: size,
    })
    rows.value = result.data.list
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
    plotName: '',
    alertType: '',
    alertLevel: '',
    processStatus: '',
    sourceType: '',
    dateRange: [],
  }
  void fetchRows(1)
}

const handleSelectionChange = (selection: AlertEvent[]) => {
  selectedRows.value = selection
}

const handleCurrentPageChange = (page: number) => {
  void fetchRows(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchRows(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
}

const openDetail = async (row: AlertEvent) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const result = await getAlertEventById(row.id)
    currentDetail.value = result.data
  } finally {
    detailLoading.value = false
  }
}

const updateProcess = async (row: AlertEvent, processStatus: number) => {
  const actionName = processStatus === 2 ? '标记处理中' : processStatus === 3 ? '标记已处理' : '忽略预警'
  const prompt = await ElMessageBox.prompt('请输入处理说明', actionName, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: row.handleResult || '',
  }).catch((error: unknown) => error)

  if (prompt === 'cancel' || prompt === 'close') {
    return
  }

  const value = typeof prompt === 'object' && prompt && 'value' in prompt ? String(prompt.value || '') : ''
  await updateAlertProcessStatus(row.id, processStatus, value)
  ElMessage.success(`${actionName}成功`)
  void fetchRows(currentPage.value)
  void fetchStats()
  if (currentDetail.value?.id === row.id) {
    void openDetail(row)
  }
}

const handleDelete = async (row: AlertEvent) => {
  try {
    await ElMessageBox.confirm(`确定要删除预警“${row.alertTitle}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAlertEvent(row.id)
    ElMessage.success('删除成功')
    void fetchRows(getNextPageAfterDelete(1))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的预警事件')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要批量删除已选中的 ${selectedRows.value.length} 条预警事件吗？`, '批量删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await batchDeleteAlertEvents(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除成功')
    void fetchRows(getNextPageAfterDelete(selectedRows.value.length))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchRows()
  void fetchStats()
})
</script>

<template>
  <div class="alert-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="alert-shell admin-shell">
      <Header :breadcrumbs="['首页', '预警管理', '预警事件管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="alert-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="82px">
            <el-form-item label="地块">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="searchForm.alertType" placeholder="全部类型" clearable>
                <el-option v-for="(label, value) in alertTypeMap" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>
            <el-form-item label="等级">
              <el-select v-model="searchForm.alertLevel" placeholder="全部等级" clearable>
                <el-option v-for="(meta, value) in alertLevelMap" :key="value" :label="meta.text" :value="Number(value)" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
              />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchRows(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
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
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" />
              <el-table-column prop="alertTitle" label="预警标题" min-width="220" fixed="left" show-overflow-tooltip />
              <el-table-column label="类型" width="120" align="center">
                <template #default="{ row }">{{ alertTypeMap[row.alertType] || '-' }}</template>
              </el-table-column>
              <el-table-column label="等级" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getLevelMeta(row.alertLevel).type">{{ getLevelMeta(row.alertLevel).text }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="getProcessMeta(row.processStatus).type">{{ getProcessMeta(row.processStatus).text }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="plotName" label="地块" min-width="130" align="center" />
              <el-table-column prop="deviceName" label="设备" min-width="140" align="center" show-overflow-tooltip />
              <el-table-column prop="sourceType" label="来源" min-width="150" align="center" show-overflow-tooltip />
              <el-table-column prop="triggerTime" label="触发时间" min-width="170" align="center" />
              <el-table-column label="操作" width="320" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
                  <el-button link type="primary" @click="updateProcess(row, 2)">处理中</el-button>
                  <el-button link type="success" @click="updateProcess(row, 3)">已处理</el-button>
                  <el-button link type="warning" @click="updateProcess(row, 4)">忽略</el-button>
                  <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-row">
            <span>共 {{ total.toLocaleString() }} 条</span>
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
        </section>
      </main>
    </div>

    <el-drawer v-model="detailVisible" title="预警事件详情" direction="rtl" size="640px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="currentDetail">
          <h3>{{ currentDetail.alertTitle }}</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="预警类型">{{ alertTypeMap[currentDetail.alertType] || '-' }}</el-descriptions-item>
            <el-descriptions-item label="预警等级">
              <el-tag :type="getLevelMeta(currentDetail.alertLevel).type">{{ getLevelMeta(currentDetail.alertLevel).text }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理状态">
              <el-tag :type="getProcessMeta(currentDetail.processStatus).type">{{ getProcessMeta(currentDetail.processStatus).text }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="地块">{{ currentDetail.plotName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备">{{ currentDetail.deviceName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ currentDetail.sourceType || '-' }} / {{ currentDetail.sourceId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="触发时间">{{ currentDetail.triggerTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处理结果">{{ currentDetail.handleResult || '-' }}</el-descriptions-item>
            <el-descriptions-item label="内容">{{ currentDetail.alertContent || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card,
.filter-card,
.table-card {
  background: #ffffff;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
}

.stat-card {
  min-height: 112px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px 26px;
  border: 1px solid #f1f5f9;
}

.stat-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  font-size: 30px;
}

.stat-card p,
.stat-card span {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin: 9px 0 7px;
  color: #111827;
  font-size: 26px;
  line-height: 1;
}

.tone-blue { background: #2563eb; }
.tone-orange { background: #f59e0b; }
.tone-green { background: #10b981; }
.tone-red { background: #ef4444; }

.filter-card {
  margin-top: 10px;
  padding: 14px 18px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(220px, 1fr));
  gap: 14px 22px;
  align-items: center;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-select__wrapper) {
  height: 40px;
  border-radius: 2px;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
}

.table-card {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 10px;
  padding: 14px 18px;
}

.table-scroll {
  flex: 1 1 auto;
  min-height: 0;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  color: #334155;
  font-size: 14px;
}

.detail-body {
  min-height: 360px;
}

.detail-body h3 {
  margin: 0 0 18px;
  color: #111827;
  font-size: 18px;
}

@media (max-width: 1280px) {
  .stats-grid,
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .alert-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
