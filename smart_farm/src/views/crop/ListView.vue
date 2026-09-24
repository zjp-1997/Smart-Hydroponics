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
import {
  batchDeleteCrops,
  deleteCrop,
  listCrops,
  updateCropStatus,
  type Crop,
} from '@/api/crop'
import { listCropTypes, type CropType } from '@/api/cropType'
import { getCurrentUserId, hasPermission, isAdminUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface CropRow {
  id: number
  userId?: number
  imageText: string
  imageUrl?: string
  typeId: string
  typeName: string
  cropName: string
  cropCode: string
  variety: string
  growthDays: string
  suitableTemperature: string
  suitableHumidity: string
  suitablePh: string
  description: string
  status: RowStatus
  remark: string
  createTime: string
  updateTime: string
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
const selectedRows = ref<CropRow[]>([])
const crops = ref<CropRow[]>([])
const typeOptions = ref<CropType[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editCropId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const canAddCrop = () => hasPermission('crop:add')
const canUpdateCrop = () => hasPermission('crop:update')
const canDeleteCrop = () => hasPermission('crop:delete')
const canWriteCropRow = (row: CropRow) => isAdminUser() || row.userId === getCurrentUserId()
const canSelectCropRow = (row: CropRow) => canDeleteCrop() && canWriteCropRow(row)

const CROP_IMAGE_STORAGE_PREFIX = 'smart_farm_crop_image:'

const cropStats = ref({
  total: 0,
  typed: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  typedTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = reactive({
  cropName: '',
  typeId: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '作物总数',
    value: cropStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropStats.value.totalTrend),
    trendType: cropStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Grape,
    tone: 'blue',
  },
  {
    title: '已分类作物',
    value: cropStats.value.typed.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropStats.value.typedTrend),
    trendType: cropStats.value.typedTrend >= 0 ? 'up' : 'down',
    icon: Timer,
    tone: 'green',
  },
  {
    title: '启用作物',
    value: cropStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropStats.value.enabledTrend),
    trendType: cropStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '禁用作物',
    value: cropStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropStats.value.disabledTrend),
    trendType: cropStats.value.disabledTrend >= 0 ? 'up' : 'down',
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

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(CROP_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue
  }

  return getStoredImageUrl(normalizedValue)
}

const getCropImageText = (crop: Crop) => {
  return crop.cropName?.trim().slice(0, 1) || '作'
}

const mapCropToRow = (crop: Crop): CropRow => ({
  id: crop.id ?? Date.now(),
  userId: crop.userId,
  imageText: getCropImageText(crop),
  imageUrl: resolveImageUrl(crop.imageUrl),
  typeId: crop.typeId === undefined || crop.typeId === null ? '-' : String(crop.typeId),
  typeName: crop.typeName || '未分类',
  cropName: crop.cropName || '-',
  cropCode: crop.cropCode || '-',
  variety: crop.variety || '-',
  growthDays: crop.growthDays === undefined || crop.growthDays === null ? '-' : String(crop.growthDays),
  suitableTemperature: crop.suitableTemperature || '-',
  suitableHumidity: crop.suitableHumidity || '-',
  suitablePh: crop.suitablePh || '-',
  description: crop.description || '-',
  status: crop.status === 0 ? 'disabled' : 'enabled',
  remark: crop.remark || '-',
  createTime: crop.createTime || '-',
  updateTime: crop.updateTime || '-',
})

const fetchTypeOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000 })

  typeOptions.value = result.data.list.filter((item) => item.status !== 0)
}

const fetchCropStats = async () => {
  const result = await listCrops({ pageNum: 1, pageSize: 1000 })

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allCrops = result.data.list
  const todayCrops = allCrops.filter((item) => isSameDate(item.createTime, today))
  const yesterdayCrops = allCrops.filter((item) => isSameDate(item.createTime, yesterday))

  cropStats.value = {
    total: result.data.total,
    typed: allCrops.filter((item) => Boolean(item.typeId)).length,
    enabled: allCrops.filter((item) => item.status !== 0).length,
    disabled: allCrops.filter((item) => item.status === 0).length,
    totalTrend: todayCrops.length - yesterdayCrops.length,
    typedTrend:
      todayCrops.filter((item) => Boolean(item.typeId)).length -
      yesterdayCrops.filter((item) => Boolean(item.typeId)).length,
    enabledTrend:
      todayCrops.filter((item) => item.status !== 0).length -
      yesterdayCrops.filter((item) => item.status !== 0).length,
    disabledTrend:
      todayCrops.filter((item) => item.status === 0).length -
      yesterdayCrops.filter((item) => item.status === 0).length,
  }
}

const fetchCrops = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCrops({
      cropName: searchForm.cropName || undefined,
      typeId: searchForm.typeId,
      status: getNumberValue(searchForm.status),
      pageNum,
      pageSize: size,
    })

    crops.value = result.data.list.map((item) => mapCropToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.cropName = ''
  searchForm.typeId = undefined
  searchForm.status = ''
  void fetchCrops(1)
}

const handleSelectionChange = (rows: CropRow[]) => {
  selectedRows.value = rows.filter((row) => canSelectCropRow(row))
}

const openAddDialog = () => {
  editCropId.value = null
  addDialogVisible.value = true
}

const handleEditCrop = (row: CropRow) => {
  if (!canUpdateCrop() || !canWriteCropRow(row)) {
    ElMessage.warning('当前账号只能查看该作物信息')
    return
  }

  editCropId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchCrops(editCropId.value ? currentPage.value : 1)
  void fetchCropStats()
  void fetchTypeOptions()
}

const handleCurrentPageChange = (page: number) => {
  void fetchCrops(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchCrops(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return crops.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: CropRow, enabled?: boolean | string | number) => {
  if (!canUpdateCrop() || !canWriteCropRow(row)) {
    ElMessage.warning('当前账号只能查看该作物信息')
    return
  }

  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateCropStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用作物成功' : '禁用作物成功')
    void fetchCrops(currentPage.value)
    void fetchCropStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteCrop = async (row: CropRow) => {
  if (!canDeleteCrop() || !canWriteCropRow(row)) {
    ElMessage.warning('当前账号只能查看该作物信息')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除作物「${row.cropName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCrop(row.id)
    ElMessage.success('删除作物成功')
    void fetchCrops(getNextPageAfterDelete(1))
    void fetchCropStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的作物')
    return
  }

  const deletableRows = selectedRows.value.filter((row) => canSelectCropRow(row))

  if (deletableRows.length !== selectedRows.value.length) {
    ElMessage.warning('已选作物中包含当前账号不可删除的数据')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${deletableRows.length} 条作物信息吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteCrops(deletableRows.map((row) => row.id))
    ElMessage.success('批量删除作物成功')
    void fetchCrops(getNextPageAfterDelete(deletableRows.length))
    void fetchCropStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchTypeOptions()
  void fetchCrops()
  void fetchCropStats()
})
</script>

<template>
  <div class="crop-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="crop-shell admin-shell">
      <Header :breadcrumbs="['首页', '作物管理', '作物信息管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="crop-content admin-content">
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
            <el-form-item label="作物名称">
              <el-input v-model="searchForm.cropName" placeholder="请输入作物名称" clearable />
            </el-form-item>
            <el-form-item label="作物类型">
              <el-select v-model="searchForm.typeId" placeholder="全部类型" clearable>
                <el-option
                  v-for="item in typeOptions"
                  :key="item.id"
                  :label="item.typeName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchCrops(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button
                type="danger"
                plain
                :icon="Delete"
                @click="handleBatchDelete"
              >
                批量删除
              </el-button>
              <el-button v-if="canAddCrop()" type="primary" :icon="CirclePlus" @click="openAddDialog">新增作物</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="crops"
              row-key="id"
              class="crop-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column
                type="selection"
                width="58"
                fixed="left"
                align="center"
                header-align="center"
                :selectable="canSelectCropRow"
              />
              <el-table-column prop="typeName" label="作物类型" min-width="130" align="center" header-align="center" />
              <el-table-column prop="cropName" label="作物名称" min-width="130" align="center" header-align="center" />
              <!-- <el-table-column prop="cropCode" label="作物编码" min-width="110" align="center" header-align="center" /> -->
              <el-table-column prop="variety" label="品种名称" min-width="120" align="center" header-align="center" />
              <el-table-column prop="growthDays" label="推荐生长周期(天)" min-width="150" align="center" header-align="center" />
              <el-table-column prop="suitableTemperature" label="适宜温度范围" min-width="140" align="center" header-align="center" />
              <el-table-column prop="suitableHumidity" label="适宜湿度范围" min-width="140" align="center" header-align="center" />
              <el-table-column prop="suitablePh" label="适宜PH范围" min-width="130" align="center" header-align="center" />
              <el-table-column label="作物图片" width="104" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="crop-thumb"
                    :src="row.imageUrl"
                    fit="contain"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                  />
                  <span v-else class="default-crop-image">{{ row.imageText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="作物说明" min-width="220" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch
                      :model-value="row.status === 'enabled'"
                      :loading="isStatusUpdating(row.id)"
                      :disabled="!canUpdateCrop() || !canWriteCropRow(row)"
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
                    <el-button v-if="canUpdateCrop() && canWriteCropRow(row)" link type="primary" :icon="EditPen" @click="handleEditCrop(row)">
                      编辑
                    </el-button>
                    <el-button
                      v-if="canUpdateCrop() && canWriteCropRow(row)"
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button v-if="canDeleteCrop() && canWriteCropRow(row)" link type="danger" :icon="Delete" @click="handleDeleteCrop(row)">
                      删除
                    </el-button>
                    <span v-if="(!canUpdateCrop() && !canDeleteCrop()) || !canWriteCropRow(row)">-</span>
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

    <AddOrUpdate v-model="addDialogVisible" :id="editCropId" @success="handleSaveSuccess" />
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

.crop-table {
  width: 100%;
}

.crop-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.crop-table :deep(.el-table__row) {
  height: 64px;
}

.crop-table :deep(.el-table__cell) {
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

.default-crop-image {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #ffffff;
  background: #409eff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.22);
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
  .crop-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .crop-content {
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
