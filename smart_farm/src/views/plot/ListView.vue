<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Box,
  Cherry,
  CirclePlus,
  Delete,
  EditPen,
  MapLocation,
  PieChart,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  batchDeletePlots,
  deletePlot,
  getPlotStatistics,
  listPlots,
  updatePlotStatus,
  type Plot,
} from '@/api/plot'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getCurrentUser, isAdminUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'

const route = useRoute()
import AddOrUpdate from './AddOrUpdate.vue'
import HarvestDialog from './HarvestDialog.vue'

type RowStatus = 'enabled' | 'disabled'

interface PlotRow {
  id: number
  userId: number
  ownerName: string
  farmName: string
  plotName: string
  plotCode: string
  type: string
  typeTone: string
  area: string
  coordinate: string
  cropImage: string
  cropName: string
  currentBatchNo: string
  plantingStatus: 'planting' | 'idle'
  currentGrowthStage: string
  currentPlantedAt: string
  currentExpectedHarvestAt: string
  currentExpectedYield: string
  currentGrownDays: string
  status: RowStatus
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
const selectedRows = ref<PlotRow[]>([])
const plots = ref<PlotRow[]>([])
const userOptions = ref<SmartPlantUser[]>([])
const tableLoading = ref(false)
const statsLoading = ref(false)
const addDialogVisible = ref(false)
const editPlotId = ref<number | null>(null)
const harvestDialogVisible = ref(false)
const harvestPlotId = ref<number | null>(null)
const harvestPlotName = ref('')
const harvestYieldUnit = ref('kg')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const currentUser = computed(() => getCurrentUser())
const isAdmin = computed(() => isAdminUser(currentUser.value))

const plotStats = ref({
  plotTotalCount: 0,
  idlePlotCount: 0,
  cropTypeCount: 0,
  plantingArea: 0,
  areaUnit: '亩',
  plotTotalTrend: 0,
  idlePlotTrend: 0,
  cropTypeTrend: 0,
  plantingAreaTrend: 0,
})

const searchForm = reactive({
  plotName: '',
  plotCode: '',
  userId: undefined as number | undefined,
  type: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const typeOptions = [
  { label: '水培种植', value: 1 },
  { label: '大棚种植', value: 2 },
  { label: '室外种植', value: 3 },
]

const typeLabelMap: Record<number, string> = {
  1: '水培种植',
  2: '大棚种植',
  3: '室外种植',
}

const typeToneMap: Record<number, string> = {
  1: 'cyan',
  2: 'green',
  3: 'orange',
}

const CROP_IMAGE_STORAGE_PREFIXES = [
  'smart_farm_crop_image:',
  'smart_farm_planting_batch_crop_image:',
]
const DEFAULT_CROP_IMAGE_URL = '/default-crop.svg'

const statCards = computed<StatCard[]>(() => [
  {
    title: '地块总数',
    value: plotStats.value.plotTotalCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plotStats.value.plotTotalTrend),
    trendType: plotStats.value.plotTotalTrend >= 0 ? 'up' : 'down',
    icon: MapLocation,
    tone: 'blue',
  },
  {
    title: '空闲地块',
    value: plotStats.value.idlePlotCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plotStats.value.idlePlotTrend),
    trendType: plotStats.value.idlePlotTrend >= 0 ? 'up' : 'down',
    icon: Box,
    tone: 'green',
  },
  {
    title: '作物种类',
    value: plotStats.value.cropTypeCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(plotStats.value.cropTypeTrend),
    trendType: plotStats.value.cropTypeTrend >= 0 ? 'up' : 'down',
    icon: Cherry,
    tone: 'orange',
  },
  {
    title: '种植中面积',
    value: `${plotStats.value.plantingArea.toLocaleString()} ${plotStats.value.areaUnit}`,
    desc: '较昨日',
    trend: formatTrend(plotStats.value.plantingAreaTrend),
    trendType: plotStats.value.plantingAreaTrend >= 0 ? 'up' : 'down',
    icon: PieChart,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => {
  return `${value >= 0 ? '↑' : '↓'} ${Math.abs(value).toLocaleString()}`
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

const formatArea = (area?: number, unit?: string) => {
  if (area === undefined || area === null) {
    return '-'
  }

  return `${area}${unit || '亩'}`
}

const formatYield = (amount?: number, unit?: string) => {
  if (amount === undefined || amount === null) {
    return '-'
  }

  return `${amount}${unit || 'kg'}`
}

const formatCoordinate = (longitude?: number, latitude?: number) => {
  if (longitude === undefined || longitude === null || latitude === undefined || latitude === null) {
    return '-'
  }

  return `${longitude}, ${latitude}`
}

const resolveCropImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()
  if (!normalizedValue) {
    return ''
  }

  if (normalizedValue.startsWith('data:image/') || normalizedValue.startsWith('blob:')) {
    return normalizedValue
  }

  if (CROP_IMAGE_STORAGE_PREFIXES.some((prefix) => normalizedValue.startsWith(prefix))) {
    return localStorage.getItem(normalizedValue) || ''
  }

  return getFileUrl(normalizedValue)
}

const getOwnerName = (plot: Plot) => {
  if (plot.nickname && plot.username) {
    return `${plot.nickname}（${plot.username}）`
  }

  return plot.nickname || plot.username || `用户${plot.userId}`
}

const mapPlotToRow = (plot: Plot): PlotRow => {
  const isPlanting = Boolean(plot.currentBatchId)

  return {
    id: plot.id ?? Date.now(),
    userId: plot.userId,
    ownerName: getOwnerName(plot),
    farmName: plot.farmName || '-',
    plotName: plot.plotName || '-',
    plotCode: plot.plotCode || '-',
    type: plot.type === undefined ? '-' : (typeLabelMap[plot.type] ?? '-'),
    typeTone: plot.type === undefined ? 'gray' : (typeToneMap[plot.type] ?? 'gray'),
    area: formatArea(plot.area, plot.areaUnit),
    coordinate: formatCoordinate(plot.longitude, plot.latitude),
    cropImage: isPlanting ? resolveCropImageUrl(plot.currentCropImage) || DEFAULT_CROP_IMAGE_URL : DEFAULT_CROP_IMAGE_URL,
    cropName: isPlanting ? plot.currentCropName || '-' : '-',
    currentBatchNo: isPlanting ? plot.currentBatchNo || '-' : '-',
    plantingStatus: isPlanting ? 'planting' : 'idle',
    currentGrowthStage: isPlanting ? plot.currentGrowthStageName || '-' : '-',
    currentPlantedAt: isPlanting ? plot.currentPlantedAt || '-' : '-',
    currentExpectedHarvestAt: isPlanting ? plot.currentExpectedHarvestAt || '-' : '-',
    currentExpectedYield: isPlanting ? formatYield(plot.currentExpectedYieldAmount, plot.currentYieldUnit) : '-',
    currentGrownDays:
      isPlanting && plot.currentGrownDays !== undefined && plot.currentGrownDays !== null
        ? `${plot.currentGrownDays}天`
        : '-',
    status: plot.status === 0 ? 'disabled' : 'enabled',
  }
}

const getUserOptionLabel = (user: SmartPlantUser) => {
  const name = user.nickname || user.username || `用户${user.id}`
  return user.username && user.nickname ? `${name}（${user.username}）` : name
}

const fetchUserOptions = async () => {
  if (!isAdmin.value) {
    userOptions.value = currentUser.value?.id ? [currentUser.value as SmartPlantUser] : []
    return
  }

  const result = await listSmartPlantUsers({ pageNum: 1, pageSize: 1000, status: 1 })

  userOptions.value = result.data.list
}

const fetchPlotStats = async () => {
  statsLoading.value = true

  try {
    const result = await getPlotStatistics()
    plotStats.value = {
      ...result.data,
      plotTotalTrend: result.data.plotTotalTrend || 0,
      idlePlotTrend: result.data.idlePlotTrend || 0,
      cropTypeTrend: result.data.cropTypeTrend || 0,
      plantingAreaTrend: result.data.plantingAreaTrend || 0,
    }
  } finally {
    statsLoading.value = false
  }
}

const fetchPlots = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listPlots({
      plotName: searchForm.plotName || undefined,
      plotCode: searchForm.plotCode || undefined,
      userId: isAdmin.value ? searchForm.userId : undefined,
      type: searchForm.type,
      status: getNumberValue(searchForm.status),
      pageNum,
      pageSize: size,
    })

    plots.value = result.data.list.map((item) => mapPlotToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.plotName = ''
  searchForm.plotCode = ''
  searchForm.userId = undefined
  searchForm.type = undefined
  searchForm.status = ''
  void fetchPlots(1)
}

const handleSelectionChange = (rows: PlotRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editPlotId.value = null
  addDialogVisible.value = true
}

const handleEditPlot = (row: PlotRow) => {
  editPlotId.value = row.id
  addDialogVisible.value = true
}

const handleHarvestPlot = (row: PlotRow) => {
  if (row.plantingStatus !== 'planting') {
    ElMessage.warning('该地块当前没有种植中的批次')
    return
  }
  harvestPlotId.value = row.id
  harvestPlotName.value = row.plotName
  harvestYieldUnit.value = 'kg'
  harvestDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchPlots(editPlotId.value ? currentPage.value : 1)
  void fetchPlotStats()
  void fetchUserOptions()
}

const handleCurrentPageChange = (page: number) => {
  void fetchPlots(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchPlots(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return plots.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: PlotRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updatePlotStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用地块成功' : '禁用地块成功')
    void fetchPlots(currentPage.value)
    void fetchPlotStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeletePlot = async (row: PlotRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除地块「${row.plotName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deletePlot(row.id)
    ElMessage.success('删除地块成功')
    void fetchPlots(getNextPageAfterDelete(1))
    void fetchPlotStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的地块')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条地块吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeletePlots(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除地块成功')
    void fetchPlots(getNextPageAfterDelete(selectedRows.value.length))
    void fetchPlotStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

// 接收首页搜索和“添加地块”快捷入口，复用本页现有功能。
const applyHomeRouteIntent = () => {
  const keyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  if (keyword) searchForm.plotName = keyword
  if (route.query.action === 'create') openAddDialog()
}

watch(() => route.fullPath, () => {
  applyHomeRouteIntent()
  if (route.query.keyword) void fetchPlots(1)
})

onMounted(() => {
  applyHomeRouteIntent()
  void fetchUserOptions()
  void fetchPlots()
  void fetchPlotStats()
})
</script>

<template>
  <div class="plot-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="plot-shell admin-shell">
      <Header :breadcrumbs="['首页', '地块管理', '地块信息管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="plot-content admin-content">
        <section v-loading="statsLoading" class="stats-grid">
          <article v-for="card in statCards" :key="card.title" class="stat-card">
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
            <el-form-item label="地块名称">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <el-form-item v-if="isAdmin" label="所属用户">
              <el-select v-model="searchForm.userId" placeholder="全部用户" filterable clearable>
                <el-option v-for="item in userOptions" :key="item.id" :label="getUserOptionLabel(item)"
                  :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchPlots(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增地块</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table v-loading="tableLoading" :data="plots" row-key="id" class="plot-table" height="100%"
              @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="plotName" label="地块名称" min-width="150" fixed="left" align="center"
                header-align="center" show-overflow-tooltip />
              <!-- <el-table-column prop="plotCode" label="地块编号" min-width="130" align="center" header-align="center" /> -->
              <el-table-column prop="farmName" label="所属农场" min-width="150" align="center" header-align="center"
                show-overflow-tooltip />
              <el-table-column v-if="isAdmin" prop="ownerName" label="所属用户" min-width="170" align="center" header-align="center"
                show-overflow-tooltip />
              <el-table-column label="地块类型" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="`type-${row.typeTone}`" effect="light" round>{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="area" label="地块面积" min-width="110" align="center" header-align="center" />
              <!-- <el-table-column prop="coordinate" label="经纬度" min-width="180" align="center" header-align="center" show-overflow-tooltip /> -->
              <el-table-column label="作物图片" width="112" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.cropImage"
                    class="crop-image"
                    :src="row.cropImage"
                    :preview-src-list="[row.cropImage]"
                    fit="contain"
                    preview-teleported
                  />
                  <span v-else class="empty-crop-image">暂无图片</span>
                </template>
              </el-table-column>
              <el-table-column prop="cropName" label="作物名称" min-width="140" align="center" header-align="center"
                show-overflow-tooltip />
              <el-table-column prop="currentBatchNo" label="种植批次编号" min-width="140" align="center"
                header-align="center" />
              <el-table-column label="种植状态" min-width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :type="row.plantingStatus === 'planting' ? 'success' : 'info'" effect="light" round>
                    {{ row.plantingStatus === 'planting' ? '种植中' : '空闲' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="currentGrowthStage" label="生长阶段" min-width="130" align="center"
                header-align="center" />
              <el-table-column prop="currentPlantedAt" label="种植日期" min-width="120" align="center"
                header-align="center" />
              <el-table-column prop="currentGrownDays" label="已生长" min-width="100" align="center"
                header-align="center" />
              <el-table-column prop="currentExpectedHarvestAt" label="预计采摘时间" min-width="140" align="center"
                header-align="center" />
              <el-table-column prop="currentExpectedYield" label="预计产量" min-width="110" align="center"
                header-align="center" />
              <el-table-column label="启用状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch :model-value="row.status === 'enabled'" :loading="isStatusUpdating(row.id)"
                      @change="(value: boolean | string | number) => handleToggleStatus(row, value)" />
                    <span>{{ row.status === 'enabled' ? '启用' : '禁用' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="310" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditPlot(row)">
                      编辑
                    </el-button>
                    <el-button link type="success" :disabled="row.plantingStatus !== 'planting'"
                      @click="handleHarvestPlot(row)">
                      采收
                    </el-button>
                    <el-button link :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)" @click="handleToggleStatus(row)">
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeletePlot(row)">
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

    <AddOrUpdate v-model="addDialogVisible" :id="editPlotId" @success="handleSaveSuccess" />
    <HarvestDialog v-model="harvestDialogVisible" :plot-id="harvestPlotId" :plot-name="harvestPlotName"
      :yield-unit="harvestYieldUnit" @success="handleSaveSuccess" />
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

.plot-table {
  width: 100%;
}

.plot-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.plot-table :deep(.el-table__row) {
  height: 64px;
}

.plot-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.crop-image {
  width: 44px;
  height: 44px;
  display: block;
  margin: 0 auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
}

.empty-crop-image {
  color: #94a3b8;
  font-size: 12px;
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

.type-cyan {
  color: #0891b2;
  background: #ecfeff;
  border-color: #cffafe;
}

.type-green {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.type-orange {
  color: #d97706;
  background: #fffbeb;
  border-color: #fef3c7;
}

.type-gray {
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
  .plot-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .plot-content {
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
