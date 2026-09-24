<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  Collection,
  Delete,
  EditPen,
  FolderOpened,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  batchDeleteCropTypes,
  deleteCropType,
  listCropTypes,
  updateCropTypeStatus,
  type CropType,
} from '@/api/cropType'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface CropTypeRow {
  id: number
  parentId?: number | null
  parentName: string
  typeName: string
  description: string
  status: RowStatus
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
const selectedRows = ref<CropTypeRow[]>([])
const cropTypes = ref<CropTypeRow[]>([])
const parentOptions = ref<CropType[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editCropTypeId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])

const cropTypeStats = ref({
  total: 0,
  root: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  rootTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = reactive({
  typeName: '',
  parentId: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '类型总数',
    value: cropTypeStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropTypeStats.value.totalTrend),
    trendType: cropTypeStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Collection,
    tone: 'blue',
  },
  {
    title: '一级分类',
    value: cropTypeStats.value.root.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropTypeStats.value.rootTrend),
    trendType: cropTypeStats.value.rootTrend >= 0 ? 'up' : 'down',
    icon: FolderOpened,
    tone: 'green',
  },
  {
    title: '启用类型',
    value: cropTypeStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropTypeStats.value.enabledTrend),
    trendType: cropTypeStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '禁用类型',
    value: cropTypeStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cropTypeStats.value.disabledTrend),
    trendType: cropTypeStats.value.disabledTrend >= 0 ? 'up' : 'down',
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

const mapCropTypeToRow = (cropType: CropType): CropTypeRow => ({
  id: cropType.id ?? Date.now(),
  parentId: cropType.parentId ?? null,
  parentName: cropType.parentName || '一级分类',
  typeName: cropType.typeName || '-',
  description: cropType.description || '-',
  status: cropType.status === 0 ? 'disabled' : 'enabled',
  createTime: cropType.createTime || '-',
})

const fetchParentOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000 })

  parentOptions.value = result.data.list
}

const fetchCropTypeStats = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000 })
  const allTypes = result.data.list
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  const todayTypes = allTypes.filter((item) => isSameDate(item.createTime, today))
  const yesterdayTypes = allTypes.filter((item) => isSameDate(item.createTime, yesterday))

  cropTypeStats.value = {
    total: result.data.total,
    root: allTypes.filter((item) => !item.parentId).length,
    enabled: allTypes.filter((item) => item.status !== 0).length,
    disabled: allTypes.filter((item) => item.status === 0).length,
    totalTrend: todayTypes.length - yesterdayTypes.length,
    rootTrend:
      todayTypes.filter((item) => !item.parentId).length -
      yesterdayTypes.filter((item) => !item.parentId).length,
    enabledTrend:
      todayTypes.filter((item) => item.status !== 0).length -
      yesterdayTypes.filter((item) => item.status !== 0).length,
    disabledTrend:
      todayTypes.filter((item) => item.status === 0).length -
      yesterdayTypes.filter((item) => item.status === 0).length,
  }

  parentOptions.value = allTypes
}

const fetchCropTypes = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCropTypes({
      typeName: searchForm.typeName || undefined,
      parentId: searchForm.parentId,
      status: getNumberValue(searchForm.status),
      pageNum,
      pageSize: size,
    })

    cropTypes.value = result.data.list.map((item) => mapCropTypeToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.typeName = ''
  searchForm.parentId = undefined
  searchForm.status = ''
  void fetchCropTypes(1)
}

const handleSelectionChange = (rows: CropTypeRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editCropTypeId.value = null
  addDialogVisible.value = true
}

const handleEditCropType = (row: CropTypeRow) => {
  editCropTypeId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchCropTypes(editCropTypeId.value ? currentPage.value : 1)
  void fetchCropTypeStats()
  void fetchParentOptions()
}

const handleCurrentPageChange = (page: number) => {
  void fetchCropTypes(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchCropTypes(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return cropTypes.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: CropTypeRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateCropTypeStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用作物类型成功' : '禁用作物类型成功')
    void fetchCropTypes(currentPage.value)
    void fetchCropTypeStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteCropType = async (row: CropTypeRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除作物类型「${row.typeName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCropType(row.id)
    ElMessage.success('删除作物类型成功')
    void fetchCropTypes(getNextPageAfterDelete(1))
    void fetchCropTypeStats()
    void fetchParentOptions()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的作物类型')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条作物类型吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteCropTypes(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除作物类型成功')
    void fetchCropTypes(getNextPageAfterDelete(selectedRows.value.length))
    void fetchCropTypeStats()
    void fetchParentOptions()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchCropTypes()
  void fetchCropTypeStats()
})
</script>

<template>
  <div class="crop-type-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="crop-type-shell admin-shell">
      <Header :breadcrumbs="['首页', '作物管理', '作物类型管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="crop-type-content admin-content">
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
            <el-form-item label="分类名称">
              <el-input v-model="searchForm.typeName" placeholder="请输入分类名称" clearable />
            </el-form-item>
            <el-form-item label="父分类">
              <el-select v-model="searchForm.parentId" placeholder="全部父分类" clearable>
                <el-option
                  v-for="item in parentOptions"
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
              <el-button type="primary" :icon="Search" @click="fetchCropTypes(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增类型</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="cropTypes"
              row-key="id"
              class="crop-type-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="typeName" label="分类名称" min-width="160" align="center" header-align="center" />
              <el-table-column prop="parentName" label="父分类" min-width="150" align="center" header-align="center" />
              <el-table-column prop="description" label="描述" min-width="240" align="center" header-align="center" show-overflow-tooltip />
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
              <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="操作" width="260" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditCropType(row)">
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
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteCropType(row)">
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

    <AddOrUpdate v-model="addDialogVisible" :id="editCropTypeId" @success="handleSaveSuccess" />
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

.crop-type-table {
  width: 100%;
}

.crop-type-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.crop-type-table :deep(.el-table__row) {
  height: 64px;
}

.crop-type-table :deep(.el-table__cell) {
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
  .crop-type-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .crop-type-content {
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
