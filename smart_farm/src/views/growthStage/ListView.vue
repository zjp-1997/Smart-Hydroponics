<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  Grape,
  Refresh,
  Search,
  Timer,
} from '@element-plus/icons-vue'
import { listCrops, type Crop } from '@/api/crop'
import {
  batchDeleteGrowthStages,
  deleteGrowthStage,
  listGrowthStages,
  updateGrowthStageStatus,
  type GrowthStage,
} from '@/api/growthStage'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface GrowthStageRow {
  id: number
  cropId: number
  cropName: string
  stageName: string
  stageCode: string
  stageOrder: string
  dayRange: string
  duration: string
  lightHours: string
  tempRange: string
  humidityRange: string
  phRange: string
  ecRange: string
  waterIntervalDays: string
  fertilizerIntervalDays: string
  managementAdvice: string
  status: RowStatus
  remark: string
  createTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const selectedRows = ref<GrowthStageRow[]>([])
const growthStages = ref<GrowthStageRow[]>([])
const cropOptions = ref<Crop[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editGrowthStageId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])

const growthStageStats = ref({
  total: 0,
  cropCount: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  cropCountTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = reactive({
  cropId: undefined as number | undefined,
  stageName: '',
  status: '' as '' | 'enabled' | 'disabled',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '阶段总数',
    value: growthStageStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(growthStageStats.value.totalTrend),
    trendType: growthStageStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Timer,
    tone: 'blue',
  },
  {
    title: '关联作物',
    value: growthStageStats.value.cropCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(growthStageStats.value.cropCountTrend),
    trendType: growthStageStats.value.cropCountTrend >= 0 ? 'up' : 'down',
    icon: Grape,
    tone: 'green',
  },
  {
    title: '启用阶段',
    value: growthStageStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(growthStageStats.value.enabledTrend),
    trendType: growthStageStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '禁用阶段',
    value: growthStageStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(growthStageStats.value.disabledTrend),
    trendType: growthStageStats.value.disabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => {
  return `${value >= 0 ? '↑' : '↓'} ${Math.abs(value).toLocaleString()}`
}

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

const getNumberValue = (value: '' | 'enabled' | 'disabled') => {
  if (value === 'enabled') {
    return 1
  }

  if (value === 'disabled') {
    return 0
  }

  return undefined
}

const formatValue = (value?: number | string) => {
  return value === undefined || value === null || value === '' ? '-' : String(value)
}

const formatRange = (min?: number, max?: number, unit = '') => {
  if (min === undefined && max === undefined) {
    return '-'
  }

  if (min !== undefined && max !== undefined) {
    return `${min}-${max}${unit}`
  }

  return `${formatValue(min ?? max)}${unit}`
}

const mapGrowthStageToRow = (growthStage: GrowthStage): GrowthStageRow => ({
  id: growthStage.id ?? Date.now(),
  cropId: growthStage.cropId,
  cropName: growthStage.cropName || '-',
  stageName: growthStage.stageName || '-',
  stageCode: growthStage.stageCode || '-',
  stageOrder: formatValue(growthStage.stageOrder),
  dayRange: formatRange(growthStage.startDay, growthStage.endDay, '天'),
  duration: growthStage.duration === undefined || growthStage.duration === null ? '-' : `${growthStage.duration}天`,
  lightHours: growthStage.lightHours === undefined || growthStage.lightHours === null ? '-' : `${growthStage.lightHours}h/天`,
  tempRange: formatRange(growthStage.tempMin, growthStage.tempMax, '℃'),
  humidityRange: formatRange(growthStage.humidityMin, growthStage.humidityMax, '%'),
  phRange: formatRange(growthStage.phMin, growthStage.phMax),
  ecRange: formatRange(growthStage.ecMin, growthStage.ecMax),
  waterIntervalDays:
    growthStage.waterIntervalDays === undefined || growthStage.waterIntervalDays === null
      ? '-'
      : `${growthStage.waterIntervalDays}天`,
  fertilizerIntervalDays:
    growthStage.fertilizerIntervalDays === undefined || growthStage.fertilizerIntervalDays === null
      ? '-'
      : `${growthStage.fertilizerIntervalDays}天`,
  managementAdvice: growthStage.managementAdvice || '-',
  status: growthStage.status === 0 ? 'disabled' : 'enabled',
  remark: growthStage.remark || '-',
  createTime: growthStage.createTime || '-',
})

const fetchCropOptions = async () => {
  const result = await listCrops({ pageNum: 1, pageSize: 1000 })

  cropOptions.value = result.data.list.filter((item) => item.status !== 0)
}

const fetchGrowthStageStats = async () => {
  const result = await listGrowthStages({ pageNum: 1, pageSize: 1000 })
  const allStages = result.data.list
  const cropIds = new Set(allStages.map((item) => item.cropId).filter(Boolean))
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  const todayStages = allStages.filter((item) => isSameDate(item.createTime, today))
  const yesterdayStages = allStages.filter((item) => isSameDate(item.createTime, yesterday))
  const todayCropIds = new Set(todayStages.map((item) => item.cropId).filter(Boolean))
  const yesterdayCropIds = new Set(yesterdayStages.map((item) => item.cropId).filter(Boolean))

  growthStageStats.value = {
    total: result.data.total,
    cropCount: cropIds.size,
    enabled: allStages.filter((item) => item.status !== 0).length,
    disabled: allStages.filter((item) => item.status === 0).length,
    totalTrend: todayStages.length - yesterdayStages.length,
    cropCountTrend: todayCropIds.size - yesterdayCropIds.size,
    enabledTrend:
      todayStages.filter((item) => item.status !== 0).length -
      yesterdayStages.filter((item) => item.status !== 0).length,
    disabledTrend:
      todayStages.filter((item) => item.status === 0).length -
      yesterdayStages.filter((item) => item.status === 0).length,
  }
}

const fetchGrowthStages = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listGrowthStages({
      cropId: searchForm.cropId,
      stageName: searchForm.stageName || undefined,
      status: getNumberValue(searchForm.status),
      pageNum,
      pageSize: size,
    })

    growthStages.value = result.data.list.map((item) => mapGrowthStageToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.cropId = undefined
  searchForm.stageName = ''
  searchForm.status = ''
  void fetchGrowthStages(1)
}

const handleSelectionChange = (rows: GrowthStageRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editGrowthStageId.value = null
  addDialogVisible.value = true
}

const handleEditGrowthStage = (row: GrowthStageRow) => {
  editGrowthStageId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchGrowthStages(editGrowthStageId.value ? currentPage.value : 1)
  void fetchGrowthStageStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchGrowthStages(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchGrowthStages(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return growthStages.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: GrowthStageRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateGrowthStageStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用作物生长期成功' : '禁用作物生长期成功')
    void fetchGrowthStages(currentPage.value)
    void fetchGrowthStageStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteGrowthStage = async (row: GrowthStageRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除「${row.cropName} - ${row.stageName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteGrowthStage(row.id)
    ElMessage.success('删除作物生长期成功')
    void fetchGrowthStages(getNextPageAfterDelete(1))
    void fetchGrowthStageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的作物生长期')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条作物生长期吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteGrowthStages(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除作物生长期成功')
    void fetchGrowthStages(getNextPageAfterDelete(selectedRows.value.length))
    void fetchGrowthStageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchCropOptions()
  void fetchGrowthStages()
  void fetchGrowthStageStats()
})
</script>

<template>
  <div class="growth-stage-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="growth-stage-shell admin-shell">
      <Header :breadcrumbs="['首页', '作物管理', '作物生长期管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="growth-stage-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="76px">
            <el-form-item label="作物">
              <el-select v-model="searchForm.cropId" placeholder="全部作物" filterable clearable>
                <el-option
                  v-for="item in cropOptions"
                  :key="item.id"
                  :label="item.cropName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="阶段名称">
              <el-input v-model="searchForm.stageName" placeholder="请输入阶段名称" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchGrowthStages(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增阶段</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="growthStages"
              row-key="id"
              class="growth-stage-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="cropName" label="作物名称" min-width="130" align="center" header-align="center" />
              <el-table-column prop="stageName" label="阶段名称" min-width="130" align="center" header-align="center" />
              <!-- <el-table-column prop="stageCode" label="阶段编码" min-width="120" align="center" header-align="center" /> -->
              <el-table-column prop="stageOrder" label="阶段顺序" min-width="100" align="center" header-align="center" />
              <el-table-column prop="dayRange" label="起止天数" min-width="120" align="center" header-align="center" />
              <el-table-column prop="duration" label="持续天数" min-width="110" align="center" header-align="center" />
              <el-table-column prop="lightHours" label="光照时长" min-width="120" align="center" header-align="center" />
              <el-table-column prop="tempRange" label="温度范围" min-width="120" align="center" header-align="center" />
              <el-table-column prop="humidityRange" label="湿度范围" min-width="120" align="center" header-align="center" />
              <el-table-column prop="phRange" label="PH范围" min-width="110" align="center" header-align="center" />
              <el-table-column prop="ecRange" label="EC范围" min-width="110" align="center" header-align="center" />
              <el-table-column prop="waterIntervalDays" label="浇水间隔" min-width="110" align="center" header-align="center" />
              <el-table-column prop="fertilizerIntervalDays" label="施肥间隔" min-width="110" align="center" header-align="center" />
              <el-table-column prop="managementAdvice" label="管理建议" min-width="220" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="状态" min-width="140" align="center" header-align="center">
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
              <el-table-column prop="remark" label="备注" min-width="180" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="操作" width="260" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditGrowthStage(row)">
                      编辑
                    </el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteGrowthStage(row)">
                      删除
                    </el-button>
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

    <AddOrUpdate v-model="addDialogVisible" :id="editGrowthStageId" @success="handleSaveSuccess" />
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

.stat-trend b {
  font-weight: 800;
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
  grid-template-columns: repeat(4, minmax(190px, 1fr));
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

.growth-stage-table {
  width: 100%;
}

.growth-stage-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.growth-stage-table :deep(.el-table__row) {
  height: 64px;
}

.growth-stage-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.status-cell,
.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
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
  .growth-stage-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .growth-stage-content {
    padding: 12px;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }

  .stat-card {
    min-height: 96px;
    padding: 18px;
  }
}
</style>
