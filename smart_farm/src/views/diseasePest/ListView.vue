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
  WarningFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteDiseasePests,
  deleteDiseasePest,
  listDiseasePests,
  updateDiseasePestStatus,
  type DiseasePest,
} from '@/api/diseasePest'
import { listCropTypes, type CropType } from '@/api/cropType'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

type DiseaseStatus = 'enabled' | 'disabled'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface DiseasePestRow {
  id: number
  cropTypeName: string
  affectedCrops: string
  name: string
  type: string
  typeTone: string
  symptom: string
  suitableStage: string
  occurrencePeriod: string
  coverImage: string
  coverImageUrl?: string
  detailImageCount: number
  sortOrder: number
  status: DiseaseStatus
  remark: string
  createTime: string
}

interface DiseasePestStats {
  total: number
  disease: number
  enabled: number
  disabled: number
  totalTrend: number
  diseaseTrend: number
  enabledTrend: number
  disabledTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<DiseasePestRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editDiseaseId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])
const diseases = ref<DiseasePestRow[]>([])
const cropTypeOptions = ref<CropType[]>([])
const DISEASE_COVER_STORAGE_PREFIX = 'smart_farm_disease_cover:'

const diseaseStats = ref<DiseasePestStats>({
  total: 0,
  disease: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  diseaseTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = ref({
  name: '',
  cropTypeId: undefined as number | undefined,
  type: undefined as number | undefined,
  status: '',
})

const typeOptions = [
  { label: '病害', value: 1, tone: 'danger' },
  { label: '虫害', value: 2, tone: 'warning' },
  { label: '生理性病害', value: 3, tone: 'info' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '病虫害总数',
    value: diseaseStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(diseaseStats.value.totalTrend),
    trendType: diseaseStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: WarningFilled,
    tone: 'blue',
  },
  {
    title: '病害数量',
    value: diseaseStats.value.disease.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(diseaseStats.value.diseaseTrend),
    trendType: diseaseStats.value.diseaseTrend >= 0 ? 'up' : 'down',
    icon: FirstAidKit,
    tone: 'green',
  },
  {
    title: '启用信息',
    value: diseaseStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(diseaseStats.value.enabledTrend),
    trendType: diseaseStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CirclePlus,
    tone: 'orange',
  },
  {
    title: '禁用信息',
    value: diseaseStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(diseaseStats.value.disabledTrend),
    trendType: diseaseStats.value.disabledTrend >= 0 ? 'up' : 'down',
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

const isImageUrl = (value?: string) => {
  return Boolean(
    value &&
      (/^(https?:|blob:|data:image\/)/.test(value) || value.startsWith('/')),
  )
}

const getStoredCoverUrl = (value?: string) => {
  if (!value?.startsWith(DISEASE_COVER_STORAGE_PREFIX)) {
    return undefined
  }

  return localStorage.getItem(value) || undefined
}

const resolveCoverUrl = (value?: string) => {
  if (isImageUrl(value)) {
    return value?.startsWith('/') ? getFileUrl(value) : value
  }

  return getStoredCoverUrl(value)
}

const getTypeMeta = (type?: number) => {
  return typeOptions.find((item) => item.value === type)
}

/**
 * 将后端 DiseasePest 实体转换为表格行数据。
 * 页面展示字段、标签颜色和图片预览都集中在这里处理，表格模板保持简单。
 */
const mapDiseasePestToRow = (diseasePest: DiseasePest): DiseasePestRow => {
  const typeMeta = getTypeMeta(diseasePest.type)
  const coverImage = diseasePest.coverImage || ''

  return {
    id: diseasePest.id ?? Date.now(),
    cropTypeName: diseasePest.cropTypeName || '通用',
    affectedCrops: diseasePest.affectedCrops || '-',
    name: diseasePest.name || '-',
    type: typeMeta?.label || '-',
    typeTone: typeMeta?.tone || 'info',
    symptom: diseasePest.symptom || '-',
    suitableStage: diseasePest.suitableStage || '-',
    occurrencePeriod: diseasePest.occurrencePeriod || '-',
    coverImage,
    coverImageUrl: resolveCoverUrl(coverImage),
    detailImageCount: Array.isArray(diseasePest.imageUrls) ? diseasePest.imageUrls.length : 0,
    sortOrder: diseasePest.sortOrder ?? 0,
    status: diseasePest.status === 0 ? 'disabled' : 'enabled',
    remark: diseasePest.remark || '-',
    createTime: diseasePest.createTime || '-',
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

const fetchOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000, status: 1 })
  cropTypeOptions.value = result.data.list
}

const fetchDiseaseStats = async () => {
  const [allResult, diseaseResult, enabledResult, disabledResult] = await Promise.all([
    listDiseasePests({ pageNum: 1, pageSize: 10000 }),
    listDiseasePests({ pageNum: 1, pageSize: 1, type: 1 }),
    listDiseasePests({ pageNum: 1, pageSize: 1, status: 1 }),
    listDiseasePests({ pageNum: 1, pageSize: 1, status: 0 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allDiseases = allResult.data.list
  const todayDiseases = allDiseases.filter((item) => isSameDate(item.createTime, today))
  const yesterdayDiseases = allDiseases.filter((item) => isSameDate(item.createTime, yesterday))
  const todayTypeDiseases = todayDiseases.filter((item) => item.type === 1)
  const yesterdayTypeDiseases = yesterdayDiseases.filter((item) => item.type === 1)
  const todayEnabled = todayDiseases.filter((item) => item.status !== 0)
  const yesterdayEnabled = yesterdayDiseases.filter((item) => item.status !== 0)
  const todayDisabled = todayDiseases.filter((item) => item.status === 0)
  const yesterdayDisabled = yesterdayDiseases.filter((item) => item.status === 0)

  diseaseStats.value = {
    total: allResult.data.total,
    disease: diseaseResult.data.total,
    enabled: enabledResult.data.total,
    disabled: disabledResult.data.total,
    totalTrend: todayDiseases.length - yesterdayDiseases.length,
    diseaseTrend: todayTypeDiseases.length - yesterdayTypeDiseases.length,
    enabledTrend: todayEnabled.length - yesterdayEnabled.length,
    disabledTrend: todayDisabled.length - yesterdayDisabled.length,
  }
}

/** 拉取病虫害基本信息分页数据，供查询、分页和新增编辑成功后刷新。 */
const fetchDiseases = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listDiseasePests({
      name: searchForm.value.name || undefined,
      cropTypeId: searchForm.value.cropTypeId,
      type: searchForm.value.type,
      status: getSearchStatus(),
      pageNum,
      pageSize: size,
    })

    diseases.value = result.data.list.map((item) => mapDiseasePestToRow(item))
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
    name: '',
    cropTypeId: undefined,
    type: undefined,
    status: '',
  }
  void fetchDiseases(1)
}

const handleSelectionChange = (rows: DiseasePestRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editDiseaseId.value = null
  dialogVisible.value = true
}

const handleEditDisease = (row: DiseasePestRow) => {
  editDiseaseId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void fetchDiseases(editDiseaseId.value ? currentPage.value : 1)
  void fetchDiseaseStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchDiseases(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchDiseases(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return diseases.value.length <= deletedCount && currentPage.value > 1
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

const handleToggleStatus = async (row: DiseasePestRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateDiseasePestStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用病虫害信息成功' : '禁用病虫害信息成功')
    void fetchDiseases(currentPage.value)
    void fetchDiseaseStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的病虫害信息')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条病虫害信息吗？关联防治措施也会被删除。`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteDiseasePests(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchDiseases(getNextPageAfterDelete(selectedRows.value.length))
    void fetchDiseaseStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteDisease = async (row: DiseasePestRow) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除病虫害「${row.name}」吗？关联防治措施也会被删除。`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await deleteDiseasePest(row.id)
    ElMessage.success('删除成功')
    void fetchDiseases(getNextPageAfterDelete(1))
    void fetchDiseaseStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

onMounted(() => {
  void fetchOptions()
  void fetchDiseases()
  void fetchDiseaseStats()
})
</script>

<template>
  <div class="disease-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="disease-shell admin-shell">
      <Header :breadcrumbs="['首页', '病虫害知识库', '病虫害信息管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="disease-content admin-content">
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
              <el-input v-model="searchForm.name" placeholder="请输入病虫害名称" clearable />
            </el-form-item>
            <el-form-item label="作物类型">
              <el-select v-model="searchForm.cropTypeId" placeholder="全部作物类型" filterable clearable>
                <el-option v-for="item in cropTypeOptions" :key="item.id" :label="item.typeName" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="病虫害类型">
              <el-select v-model="searchForm.type" placeholder="全部类型" clearable>
                <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchDiseases(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增病虫害</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="diseases"
              row-key="id"
              class="disease-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="图片" width="100" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.coverImageUrl"
                    class="cover-thumb"
                    :src="row.coverImageUrl"
                    fit="contain"
                    :preview-src-list="[row.coverImageUrl]"
                    preview-teleported
                  />
                  <span v-else class="cover-empty">无</span>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="病虫害名称" min-width="150" align="center" header-align="center" />
              <el-table-column prop="cropTypeName" label="作物类型" min-width="130" align="center" header-align="center" />
              <el-table-column prop="affectedCrops" label="危害作物" min-width="180" show-overflow-tooltip header-align="center" />
              <el-table-column label="类型" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :type="row.typeTone" effect="light" round>{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="suitableStage" label="易发阶段" min-width="130" align="center" header-align="center" />
              <el-table-column prop="occurrencePeriod" label="发生时期" min-width="130" align="center" header-align="center" />
              <el-table-column label="详情图片" width="100" align="center" header-align="center">
                <template #default="{ row }">{{ row.detailImageCount }} 张</template>
              </el-table-column>
              <el-table-column prop="sortOrder" label="排序" width="82" align="center" header-align="center" />
              <el-table-column prop="symptom" label="症状描述" min-width="220" show-overflow-tooltip header-align="center" />
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
                    <el-button link type="primary" :icon="EditPen" @click="handleEditDisease(row)">编辑</el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteDisease(row)">删除</el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editDiseaseId" @success="handleDialogSuccess" />
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

.disease-table {
  width: 100%;
}

.disease-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.disease-table :deep(.el-table__row) {
  height: 64px;
}

.disease-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.cover-thumb {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.cover-empty {
  color: #94a3b8;
  font-size: 12px;
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
  .disease-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .disease-content {
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
