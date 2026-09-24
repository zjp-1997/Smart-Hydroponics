<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  DataAnalysis,
  Delete,
  EditPen,
  Refresh,
  Search,
  Tickets,
} from '@element-plus/icons-vue'
import {
  batchDeleteAiRecognitionTypes,
  deleteAiRecognitionType,
  getAiRecognitionTypeStatistics,
  listAiRecognitionTypes,
  updateAiRecognitionTypeStatus,
  type AiRecognitionType,
  type AiRecognitionTypeStatistics,
} from '@/api/aiRecognitionType'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface AiRecognitionTypeRow {
  id: number
  typeCode: string
  typeName: string
  description: string
  status: RowStatus
  createTime: string
  updateTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const selectedRows = ref<AiRecognitionTypeRow[]>([])
const rows = ref<AiRecognitionTypeRow[]>([])
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editTypeId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const stats = ref<AiRecognitionTypeStatistics>({})

const searchForm = reactive({
  typeCode: '',
  typeName: '',
  status: '' as '' | 'enabled' | 'disabled',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '类型总数',
    value: formatCount(stats.value.totalCount),
    desc: '全部AI识别类型',
    icon: DataAnalysis,
    tone: 'blue',
  },
  {
    title: '启用类型',
    value: formatCount(stats.value.enabledCount),
    desc: '当前可用类型',
    icon: CircleCheckFilled,
    tone: 'green',
  },
  {
    title: '禁用类型',
    value: formatCount(stats.value.disabledCount),
    desc: '暂不可用类型',
    icon: CircleCloseFilled,
    tone: 'orange',
  },
  {
    title: '识别结果引用',
    value: formatCount(stats.value.resultCount),
    desc: '已关联结果数据',
    icon: Tickets,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: number | string) => Number(value ?? 0).toLocaleString()

const getStatusValue = () => {
  if (searchForm.status === 'enabled') {
    return 1
  }

  if (searchForm.status === 'disabled') {
    return 0
  }

  return undefined
}

const getQueryParams = () => ({
  typeCode: searchForm.typeCode || undefined,
  typeName: searchForm.typeName || undefined,
  status: getStatusValue(),
})

const mapToRow = (item: AiRecognitionType): AiRecognitionTypeRow => ({
  id: item.id ?? Date.now(),
  typeCode: item.typeCode || '-',
  typeName: item.typeName || '-',
  description: item.description || '-',
  status: item.status === 0 ? 'disabled' : 'enabled',
  createTime: item.createTime || '-',
  updateTime: item.updateTime || '-',
})

const fetchStats = async () => {
  const result = await getAiRecognitionTypeStatistics(getQueryParams())
  stats.value = result.data || {}
}

/** 拉取 AI 识别类型列表，供查询、分页、弹框保存和状态操作后刷新使用。 */
const fetchRows = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listAiRecognitionTypes({
      ...getQueryParams(),
      pageNum,
      pageSize: size,
    })

    rows.value = result.data.list.map((item) => mapToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshPageData = async (pageNum = currentPage.value) => {
  await Promise.all([fetchRows(pageNum), fetchStats()])
}

const resetSearch = () => {
  searchForm.typeCode = ''
  searchForm.typeName = ''
  searchForm.status = ''
  void refreshPageData(1)
}

const handleSelectionChange = (value: AiRecognitionTypeRow[]) => {
  selectedRows.value = value
}

const openAddDialog = () => {
  editTypeId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: AiRecognitionTypeRow) => {
  editTypeId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void refreshPageData(editTypeId.value ? currentPage.value : 1)
}

const handleCurrentPageChange = (page: number) => {
  void fetchRows(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchRows(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: AiRecognitionTypeRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateAiRecognitionTypeStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用AI识别类型成功' : '禁用AI识别类型成功')
    void refreshPageData(currentPage.value)
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDelete = async (row: AiRecognitionTypeRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除AI识别类型“${row.typeName}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteAiRecognitionType(row.id)
    ElMessage.success('删除AI识别类型成功')
    void refreshPageData(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的AI识别类型')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条AI识别类型吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteAiRecognitionTypes(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除AI识别类型成功')
    void refreshPageData(getNextPageAfterDelete(selectedRows.value.length))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void refreshPageData()
})
</script>

<template>
  <div class="ai-type-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="ai-type-shell admin-shell">
      <Header :breadcrumbs="['首页', 'AI识别管理', 'AI识别类型管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="ai-type-content admin-content">
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
            <el-form-item label="类型编码">
              <el-input v-model="searchForm.typeCode" placeholder="请输入类型编码" clearable />
            </el-form-item>
            <el-form-item label="类型名称">
              <el-input v-model="searchForm.typeName" placeholder="请输入类型名称" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="refreshPageData(1)">查询</el-button>
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
              :data="rows"
              row-key="id"
              class="ai-type-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="typeCode" label="类型编码" min-width="170" align="center" header-align="center" />
              <el-table-column prop="typeName" label="类型名称" min-width="160" align="center" header-align="center" />
              <el-table-column prop="description" label="类型说明" min-width="280" show-overflow-tooltip header-align="center" />
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
                    <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">
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
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
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

    <AddOrUpdate v-model="dialogVisible" :id="editTypeId" @success="handleDialogSuccess" />
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

.stat-card span {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
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

.ai-type-table {
  width: 100%;
}

.ai-type-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.ai-type-table :deep(.el-table__row) {
  height: 64px;
}

.ai-type-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
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
  .ai-type-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .ai-type-content {
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

  .manage-actions,
  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
