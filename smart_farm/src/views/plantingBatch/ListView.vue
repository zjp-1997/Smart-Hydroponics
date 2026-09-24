<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MapLocation, CirclePlus, Delete, EditPen, Refresh, Search, Grape, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { listCrops, type Crop } from '@/api/crop'
import type { Component } from 'vue'
import {
  batchDeletePlantingBatches,
  deletePlantingBatch,
  listPlantingBatches,
  updatePlantingBatchStatus,
  type PlantingBatch,
} from '@/api/plantingBatch'
import { listPlots, type Plot } from '@/api/plot'
import { getCurrentUser, isAdminUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'planting' | 'harvested' | 'failed' | 'cancelled'

interface BatchRow {
  id: number
  batchNo: string
  plotName: string
  cropImage: string
  cropName: string
  ownerName: string
  plantingArea: string
  plantedAt: string
  expectedHarvestAt: string
  actualHarvestAt: string
  harvester: string
  growthStage: string
  growthStageName: string
  expectedYieldAmount: string
  grownDays: string
  status: RowStatus
  yieldAmount: string
  remark: string
  createTime: string
}

interface PlantingBatchCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const selectedRows = ref<BatchRow[]>([])
const batches = ref<BatchRow[]>([])
const plotOptions = ref<Plot[]>([])
const cropOptions = ref<Crop[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editBatchId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const BATCH_CROP_IMAGE_STORAGE_PREFIX = 'smart_farm_planting_batch_crop_image:'
const currentUser = computed(() => getCurrentUser())
const isAdmin = computed(() => isAdminUser(currentUser.value))

const plantingBatchStats = ref({
  total: 0,
  planted: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  plantedTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = reactive({
  batchNo: '',
  plotId: undefined as number | undefined,
  cropId: undefined as number | undefined,
  status: undefined as number | undefined,
})

const statusOptions = [
  { label: '种植中', value: 1 },
  { label: '已采收', value: 2 },
  { label: '已失败', value: 3 },
  { label: '已取消', value: 4 },
]

const statusLabelMap: Record<number, string> = {
  1: '种植中',
  2: '已采收',
  3: '已失败',
  4: '已取消',
}

const statusTypeMap: Record<number, RowStatus> = {
  1: 'planting',
  2: 'harvested',
  3: 'failed',
  4: 'cancelled',
}

const plantingBatchCards = computed<PlantingBatchCard[]>(() => [
  {
    title: '批次总数',
    value: plantingBatchStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plantingBatchStats.value.totalTrend),
    trendType: plantingBatchStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: MapLocation,
    tone: 'blue',
  },
  {
    title: '已完成批次',
    value: plantingBatchStats.value.planted.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plantingBatchStats.value.plantedTrend),
    trendType: plantingBatchStats.value.plantedTrend >= 0 ? 'up' : 'down',
    icon: Grape,
    tone: 'green',
  },
  {
    title: '种植面积',
    value: plantingBatchStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plantingBatchStats.value.enabledTrend),
    trendType: plantingBatchStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '实际产量',
    value: plantingBatchStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plantingBatchStats.value.disabledTrend),
    trendType: plantingBatchStats.value.disabledTrend >= 0 ? 'up' : 'down',
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

const formatValue = (value?: number | string) => {
  return value === undefined || value === null || value === '' ? '-' : String(value)
}

const formatAmount = (amount?: number, unit?: string) => {
  if (amount === undefined || amount === null) {
    return '-'
  }

  return `${amount}${unit || ''}`
}

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
    (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(BATCH_CROP_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue || ''
  }

  return getStoredImageUrl(normalizedValue)
}

const getOwnerName = (batch: PlantingBatch) => {
  if (batch.nickname && batch.username) {
    return `${batch.nickname}（${batch.username}）`
  }

  return batch.nickname || batch.username || `用户${batch.userId}`
}

const mapBatchToRow = (batch: PlantingBatch): BatchRow => ({
  id: batch.id ?? Date.now(),
  batchNo: batch.batchNo || '-',
  plotName: batch.plotName || '-',
  cropImage: resolveImageUrl(batch.cropImage),
  cropName: batch.cropName || '-',
  ownerName: getOwnerName(batch),
  plantingArea: formatAmount(batch.plantingArea, batch.areaUnit || '亩'),
  plantedAt: batch.plantedAt || '-',
  expectedHarvestAt: batch.expectedHarvestAt || '-',
  actualHarvestAt: batch.actualHarvestAt || '-',
  harvester: batch.harvester || '-',
  growthStage: batch.growthStageName || '-',
  growthStageName: batch.growthStageName || '-',
  expectedYieldAmount: formatAmount(batch.expectedYieldAmount, batch.yieldUnit || 'kg'),
  grownDays: batch.grownDays === undefined || batch.grownDays === null ? '-' : `${batch.grownDays}天`,
  status: batch.status === undefined ? 'planting' : (statusTypeMap[batch.status] ?? 'planting'),
  yieldAmount: formatAmount(batch.yieldAmount, batch.yieldUnit || 'kg'),
  remark: batch.remark || '-',
  createTime: batch.createTime || '-',
})

const getStatusNumber = (status: RowStatus) => {
  return Number(Object.entries(statusTypeMap).find(([, value]) => value === status)?.[0] || 1)
}

const fetchOptions = async () => {
  const [plotResult, cropResult] = await Promise.all([
    listPlots({ pageNum: 1, pageSize: 1000, status: 1 }),
    listCrops({ pageNum: 1, pageSize: 1000, status: 1 }),
  ])

  plotOptions.value = plotResult.data.list
  cropOptions.value = cropResult.data.list
}

const fetchBatches = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listPlantingBatches({
      batchNo: searchForm.batchNo || undefined,
      plotId: searchForm.plotId,
      cropId: searchForm.cropId,
      status: searchForm.status,
      pageNum,
      pageSize: size,
    })

    batches.value = result.data.list.map((item) => mapBatchToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.batchNo = ''
  searchForm.plotId = undefined
  searchForm.cropId = undefined
  searchForm.status = undefined
  void fetchBatches(1)
}

const handleSelectionChange = (rows: BatchRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editBatchId.value = null
  addDialogVisible.value = true
}

const handleEditBatch = (row: BatchRow) => {
  editBatchId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchBatches(editBatchId.value ? currentPage.value : 1)
}

const handleCurrentPageChange = (page: number) => {
  void fetchBatches(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchBatches(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return batches.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleSetStatus = async (row: BatchRow, status: number) => {
  setStatusUpdating(row.id, true)

  try {
    await updatePlantingBatchStatus(row.id, status)
    ElMessage.success('修改种植批次状态成功')
    void fetchBatches(currentPage.value)
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteBatch = async (row: BatchRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除种植批次「${row.batchNo}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deletePlantingBatch(row.id)
    ElMessage.success('删除种植批次成功')
    void fetchBatches(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的种植批次')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条种植批次吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeletePlantingBatches(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除种植批次成功')
    void fetchBatches(getNextPageAfterDelete(selectedRows.value.length))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchOptions()
  void fetchBatches()
})
</script>

<template>
  <div class="batch-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="batch-shell admin-shell">
      <Header :breadcrumbs="['首页', '地块管理', '种植批次管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="batch-content admin-content">
        <section class="stats-grid">
          <article v-for="card in plantingBatchCards" :key="card.title" class="stat-card">
            <div class="stat-icon" :class="`tone-${card.tone}`">
              <el-icon>
                <component :is="card.icon" />
              </el-icon>
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
            <el-form-item label="批次编号">
              <el-input v-model="searchForm.batchNo" placeholder="请输入批次编号" clearable />
            </el-form-item>
            <el-form-item label="地块">
              <el-select v-model="searchForm.plotId" placeholder="全部地块" filterable clearable>
                <el-option v-for="item in plotOptions" :key="item.id" :label="item.plotName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="作物">
              <el-select v-model="searchForm.cropId" placeholder="全部作物" filterable clearable>
                <el-option v-for="item in cropOptions" :key="item.id" :label="item.cropName" :value="item.id" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchBatches(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增批次</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table v-loading="tableLoading" :data="batches" row-key="id" class="batch-table" height="100%"
              @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="batchNo" label="批次编号" min-width="130" align="center" header-align="center" />
              <el-table-column prop="plotName" label="地块名称" min-width="130" align="center" header-align="center" />
              <el-table-column label="作物图片" width="104" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image v-if="row.cropImage" class="crop-thumb" :src="row.cropImage" fit="contain"
                    :preview-src-list="[row.cropImage]" preview-teleported />
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="cropName" label="作物名称" min-width="130" align="center" header-align="center" />
              <el-table-column v-if="isAdmin" prop="ownerName" label="所属用户" min-width="170" align="center" header-align="center"
                show-overflow-tooltip />
              <el-table-column prop="plantingArea" label="种植面积" min-width="110" align="center" header-align="center" />
              <el-table-column prop="plantedAt" label="种植日期" min-width="120" align="center" header-align="center" />
              <el-table-column prop="expectedHarvestAt" label="预计采收日期" min-width="130" align="center"
                header-align="center" />
              <el-table-column prop="actualHarvestAt" label="实际采收日期" min-width="130" align="center"
                header-align="center" />
              <el-table-column prop="harvester" label="采收人" min-width="110" align="center" header-align="center" />
              <el-table-column prop="growthStage" label="生长阶段" min-width="110" align="center" header-align="center" />
              <!-- <el-table-column prop="growthStageName" label="生长期配置" min-width="130" align="center"
                header-align="center" /> -->
              <el-table-column prop="expectedYieldAmount" label="预计产量" min-width="110" align="center"
                header-align="center" />
              <el-table-column prop="grownDays" label="已生长天数" min-width="110" align="center" header-align="center" />
              <el-table-column label="状态" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="`status-${row.status}`" effect="light" round>
                    {{ statusLabelMap[getStatusNumber(row.status)] }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="yieldAmount" label="实际产量" min-width="110" align="center" header-align="center" />
              <el-table-column label="操作" width="330" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditBatch(row)">
                      编辑
                    </el-button>
                    <el-dropdown trigger="click" @command="(status: number) => handleSetStatus(row, status)">
                      <el-button link type="primary" :loading="isStatusUpdating(row.id)">状态</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="item in statusOptions" :key="item.value" :command="item.value">
                            {{ item.label }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteBatch(row)">
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
              <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" background
                layout="sizes, prev, pager, next, jumper" :total="total" :page-sizes="[10, 20, 50, 100]"
                @current-change="handleCurrentPageChange" @size-change="handlePageSizeChange" />
            </div>
          </div>
        </section>
      </main>
    </div>

    <AddOrUpdate v-model="addDialogVisible" :id="editBatchId" @success="handleSaveSuccess" />
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

.batch-table {
  width: 100%;
}

.batch-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.batch-table :deep(.el-table__row) {
  height: 64px;
}

.batch-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.crop-thumb {
  width: 42px;
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.status-planting {
  color: #2563eb;
  background: #eff6ff;
  border-color: #dbeafe;
}

.status-harvested {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.status-failed {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fee2e2;
}

.status-cancelled {
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
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
  .batch-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .batch-content {
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
