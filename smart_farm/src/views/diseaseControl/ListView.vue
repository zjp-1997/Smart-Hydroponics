<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  FirstAidKit,
  Refresh,
  Search,
  Tools,
} from '@element-plus/icons-vue'
import {
  batchDeleteDiseaseControls,
  deleteDiseaseControl,
  listDiseaseControls,
  updateDiseaseControlStatus,
  type DiseaseControl,
} from '@/api/diseaseControl'
import AddOrUpdate from './AddOrUpdate.vue'

type ControlStatus = 'enabled' | 'disabled'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface DiseaseControlRow {
  id: number
  diseaseName: string
  controlType: string
  controlTone: string
  controlCategory: string
  categoryTone: string
  method: string
  drugName: string
  usageMethod: string
  dosageSpec: string
  safetyInterval: string
  precautions: string
  suitableStage: string
  sortOrder: number
  status: ControlStatus
  remark: string
  createTime: string
}

interface DiseaseControlStats {
  total: number
  prevention: number
  enabled: number
  disabled: number
  totalTrend: number
  preventionTrend: number
  enabledTrend: number
  disabledTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<DiseaseControlRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editControlId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])
const controls = ref<DiseaseControlRow[]>([])

const controlStats = ref<DiseaseControlStats>({
  total: 0,
  prevention: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  preventionTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = ref({
  diseaseName: '',
  controlType: undefined as number | undefined,
  controlCategory: undefined as number | undefined,
  status: '',
})

const controlTypeOptions = [
  { label: '预防', value: 1, tone: 'success' },
  { label: '治疗', value: 2, tone: 'warning' },
]

const controlCategoryOptions = [
  { label: '农业防治', value: 1, tone: 'success' },
  { label: '物理防治', value: 2, tone: 'warning' },
  { label: '化学防治', value: 3, tone: 'danger' },
  { label: '生物防治', value: 4, tone: 'primary' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '措施总数',
    value: controlStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(controlStats.value.totalTrend),
    trendType: controlStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Tools,
    tone: 'blue',
  },
  {
    title: '预防措施',
    value: controlStats.value.prevention.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(controlStats.value.preventionTrend),
    trendType: controlStats.value.preventionTrend >= 0 ? 'up' : 'down',
    icon: FirstAidKit,
    tone: 'green',
  },
  {
    title: '启用措施',
    value: controlStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(controlStats.value.enabledTrend),
    trendType: controlStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CirclePlus,
    tone: 'orange',
  },
  {
    title: '禁用措施',
    value: controlStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(controlStats.value.disabledTrend),
    trendType: controlStats.value.disabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const formatTrend = (value: number) => `${value >= 0 ? '↑' : '↓'} ${Math.abs(value)}`

const isSameDate = (value: string | undefined, target: Date) => {
  if (!value) {
    return false
  }

  const date = new Date(value)

  return (
    date.getFullYear() === target.getFullYear() &&
    date.getMonth() === target.getMonth() &&
    date.getDate() === target.getDate()
  )
}

const closeSidebar = () => {
  sidebarVisible.value = false
}

const getControlTypeMeta = (controlType?: number) => {
  return controlTypeOptions.find((item) => item.value === controlType)
}

/** 将数据库防治手段枚举转换成管理端标签。 */
const getControlCategoryMeta = (controlCategory?: number) => {
  return controlCategoryOptions.find((item) => item.value === controlCategory)
}

/**
 * 将后端 DiseaseControl 实体转换为表格行数据。
 * 防治类型标签、状态文本和空值兜底在这里统一处理。
 */
const mapDiseaseControlToRow = (control: DiseaseControl): DiseaseControlRow => {
  const controlTypeMeta = getControlTypeMeta(control.controlType)
  const controlCategoryMeta = getControlCategoryMeta(control.controlCategory)

  return {
    id: control.id ?? Date.now(),
    diseaseName: control.diseaseName || '-',
    controlType: controlTypeMeta?.label || '-',
    controlTone: controlTypeMeta?.tone || 'info',
    controlCategory: controlCategoryMeta?.label || '未分类',
    categoryTone: controlCategoryMeta?.tone || 'info',
    method: control.method || '-',
    drugName: control.drugName || '-',
    usageMethod: control.usageMethod || '-',
    dosageSpec: control.dosageSpec || '-',
    safetyInterval: control.safetyIntervalDays == null ? '-' : `${control.safetyIntervalDays} 天`,
    precautions: control.precautions || '-',
    suitableStage: control.suitableStage || '-',
    sortOrder: control.sortOrder ?? 0,
    status: control.status === 0 ? 'disabled' : 'enabled',
    remark: control.remark || '-',
    createTime: control.createTime || '-',
  }
}

const getSearchStatus = () => {
  if (searchForm.value.status === 'enabled') {
    return 1
  }

  if (searchForm.value.status === 'disabled') {
    return 0
  }

  return undefined
}

const fetchControlStats = async () => {
  const [allResult, preventionResult, enabledResult, disabledResult] = await Promise.all([
    listDiseaseControls({ pageNum: 1, pageSize: 10000 }),
    listDiseaseControls({ pageNum: 1, pageSize: 1, controlType: 1 }),
    listDiseaseControls({ pageNum: 1, pageSize: 1, status: 1 }),
    listDiseaseControls({ pageNum: 1, pageSize: 1, status: 0 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allControls = allResult.data.list
  const todayControls = allControls.filter((item) => isSameDate(item.createTime, today))
  const yesterdayControls = allControls.filter((item) => isSameDate(item.createTime, yesterday))
  const todayPreventions = todayControls.filter((item) => item.controlType === 1)
  const yesterdayPreventions = yesterdayControls.filter((item) => item.controlType === 1)
  const todayEnabled = todayControls.filter((item) => item.status !== 0)
  const yesterdayEnabled = yesterdayControls.filter((item) => item.status !== 0)
  const todayDisabled = todayControls.filter((item) => item.status === 0)
  const yesterdayDisabled = yesterdayControls.filter((item) => item.status === 0)

  controlStats.value = {
    total: allResult.data.total,
    prevention: preventionResult.data.total,
    enabled: enabledResult.data.total,
    disabled: disabledResult.data.total,
    totalTrend: todayControls.length - yesterdayControls.length,
    preventionTrend: todayPreventions.length - yesterdayPreventions.length,
    enabledTrend: todayEnabled.length - yesterdayEnabled.length,
    disabledTrend: todayDisabled.length - yesterdayDisabled.length,
  }
}

/** 拉取防治措施分页数据，供查询、分页和弹框提交成功后刷新。 */
const fetchControls = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listDiseaseControls({
      diseaseName: searchForm.value.diseaseName || undefined,
      controlType: searchForm.value.controlType,
      controlCategory: searchForm.value.controlCategory,
      status: getSearchStatus(),
      pageNum,
      pageSize: size,
    })

    controls.value = result.data.list.map((item) => mapDiseaseControlToRow(item))
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
    diseaseName: '',
    controlType: undefined,
    controlCategory: undefined,
    status: '',
  }
  void fetchControls(1)
}

const handleSelectionChange = (rows: DiseaseControlRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editControlId.value = null
  dialogVisible.value = true
}

const handleEditControl = (row: DiseaseControlRow) => {
  editControlId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  ElMessage.success(editControlId.value ? '编辑防治措施成功' : '新增防治措施成功')
  void fetchControls(editControlId.value ? currentPage.value : 1)
  void fetchControlStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchControls(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchControls(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return controls.value.length <= deletedCount && currentPage.value > 1
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

const handleToggleStatus = async (row: DiseaseControlRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateDiseaseControlStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用防治措施成功' : '禁用防治措施成功')
    void fetchControls(currentPage.value)
    void fetchControlStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的防治措施')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条防治措施吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteDiseaseControls(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchControls(getNextPageAfterDelete(selectedRows.value.length))
    void fetchControlStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteControl = async (row: DiseaseControlRow) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.diseaseName}」的这条防治措施吗？`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await deleteDiseaseControl(row.id)
    ElMessage.success('删除成功')
    void fetchControls(getNextPageAfterDelete(1))
    void fetchControlStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

onMounted(() => {
  void fetchControls()
  void fetchControlStats()
})
</script>

<template>
  <div class="control-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="control-shell admin-shell">
      <Header :breadcrumbs="['首页', '病虫害知识库', '防治措施管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="control-content admin-content">
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

        <section class="filter-card">
          <el-form class="filter-form" :model="searchForm" label-width="82px">
            <el-form-item label="病虫害名称">
              <el-input v-model="searchForm.diseaseName" placeholder="请输入病虫害名称" clearable />
            </el-form-item>
            <el-form-item label="防治类型">
              <el-select v-model="searchForm.controlType" placeholder="全部类型" clearable>
                <el-option
                  v-for="item in controlTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="防治手段">
              <el-select v-model="searchForm.controlCategory" placeholder="全部手段" clearable>
                <el-option
                  v-for="item in controlCategoryOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchControls(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增措施</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="controls"
              row-key="id"
              class="control-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="diseaseName" label="病虫害名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="防治类型" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :type="row.controlTone" effect="light" round>{{ row.controlType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="防治手段" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :type="row.categoryTone" effect="plain" round>{{ row.controlCategory }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="method" label="措施内容" min-width="260" show-overflow-tooltip header-align="center" />
              <el-table-column prop="drugName" label="药剂名称" min-width="130" align="center" header-align="center" />
              <el-table-column prop="usageMethod" label="使用方法" min-width="220" show-overflow-tooltip header-align="center" />
              <el-table-column prop="dosageSpec" label="用量规格" min-width="170" show-overflow-tooltip header-align="center" />
              <el-table-column prop="safetyInterval" label="安全间隔期" min-width="110" align="center" header-align="center" />
              <el-table-column prop="precautions" label="注意事项" min-width="220" show-overflow-tooltip header-align="center" />
              <el-table-column prop="suitableStage" label="适用阶段" min-width="130" align="center" header-align="center" />
              <el-table-column prop="sortOrder" label="排序" width="82" align="center" header-align="center" />
              <el-table-column label="状态" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch
                      :model-value="row.status === 'enabled'"
                      :loading="isStatusUpdating(row.id)"
                      @change="(value: boolean | string | number) => handleToggleStatus(row, value)"
                    />
                    <span>{{ row.status === 'enabled' ? '启用' : '禁用' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="230" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditControl(row)">编辑</el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteControl(row)">删除</el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editControlId" @success="handleDialogSuccess" />
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
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 116px;
  padding: 22px 28px;
  border: 1px solid #f1f5f9;
}

.stat-icon {
  width: 60px;
  height: 60px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  font-size: 31px;
  flex: 0 0 auto;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.stat-card p {
  margin: 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.2;
}

.stat-card strong {
  display: block;
  margin: 9px 0 8px;
  color: #111827;
  font-size: 26px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: 0;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.stat-trend small,
.stat-trend b {
  font-size: 12px;
  line-height: 1;
}

.trend-up {
  color: #e34c67 !important;
}

.trend-down {
  color: #10b981 !important;
}

.tone-blue {
  background: linear-gradient(135deg, #2f80ed, #2563eb);
}

.tone-green {
  background: linear-gradient(135deg, #41c79a, #10b981);
}

.tone-orange {
  background: linear-gradient(135deg, #ffbe35, #f59e0b);
}

.tone-gray {
  background: linear-gradient(135deg, #9aa3b2, #64748b);
}

.filter-card {
  flex: 0 0 auto;
  margin-top: 10px;
  padding: 14px 18px;
}

.filter-form {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(200px, 1fr));
  align-items: center;
  gap: 14px 22px;
  padding-right: 250px;
}

.filter-form :deep(.el-form-item) {
  align-items: center;
  margin-bottom: 0;
}

.filter-form :deep(.el-form-item__label) {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  line-height: 40px;
}

.filter-form :deep(.el-form-item__content) {
  min-height: 40px;
  display: flex;
  align-items: center;
}

.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-select__wrapper) {
  height: 40px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.filter-actions,
.manage-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
}

.manage-actions {
  position: absolute;
  right: 0;
  top: 0;
}

.filter-actions :deep(.el-button),
.manage-actions :deep(.el-button) {
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
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

.control-table {
  width: 100%;
}

.control-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.control-table :deep(.el-table__row) {
  height: 64px;
}

.control-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.status-cell,
.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-cell {
  gap: 8px;
}

.table-actions {
  gap: 2px;
  white-space: nowrap;
}

.pagination-row {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  color: #334155;
  font-size: 14px;
}

.pagination-box {
  display: flex;
  justify-content: flex-end;
  min-width: 0;
  flex: 1;
}

.pagination-box :deep(.el-pagination) {
  justify-content: flex-end;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
    padding-right: 0;
  }

  .manage-actions {
    position: static;
    width: fit-content;
  }
}

@media (max-width: 960px) {
  .control-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .control-content {
    padding: 12px;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }

  .manage-actions,
  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
