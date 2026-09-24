<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  Cpu,
  Delete,
  EditPen,
  Refresh,
  Search,
  SetUp,
} from '@element-plus/icons-vue'
import {
  batchDeleteDeviceTypes,
  deleteDeviceType,
  listDeviceTypes,
  updateDeviceTypeStatus,
  type DeviceType,
} from '@/api/deviceType'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface DeviceTypeRow {
  id: number
  typeCode: string
  typeName: string
  category: string
  categoryTone: string
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
const selectedRows = ref<DeviceTypeRow[]>([])
const deviceTypes = ref<DeviceTypeRow[]>([])
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editDeviceTypeId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])

const deviceTypeStats = ref({
  total: 0,
  sensor: 0,
  actuator: 0,
  enabled: 0,
})

const searchForm = reactive({
  typeCode: '',
  typeName: '',
  category: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const categoryOptions = [
  { label: '传感器', value: 1, tone: 'green' },
  { label: '执行器', value: 2, tone: 'blue' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '类型总数',
    value: deviceTypeStats.value.total.toLocaleString(),
    desc: '全部设备类型',
    icon: Cpu,
    tone: 'blue',
  },
  {
    title: '传感器类型',
    value: deviceTypeStats.value.sensor.toLocaleString(),
    desc: '采集环境与设备数据',
    icon: SetUp,
    tone: 'green',
  },
  {
    title: '执行器类型',
    value: deviceTypeStats.value.actuator.toLocaleString(),
    desc: '控制水泵等执行设备',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '启用类型',
    value: deviceTypeStats.value.enabled.toLocaleString(),
    desc: '新增设备可选择',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const getStatusValue = () => {
  if (searchForm.status === 'enabled') {
    return 1
  }

  if (searchForm.status === 'disabled') {
    return 0
  }

  return undefined
}

const getCategoryMeta = (category?: number) => {
  return categoryOptions.find((item) => item.value === category)
}

const mapDeviceTypeToRow = (deviceType: DeviceType): DeviceTypeRow => {
  const categoryMeta = getCategoryMeta(deviceType.category)

  return {
    id: deviceType.id ?? Date.now(),
    typeCode: deviceType.typeCode || '-',
    typeName: deviceType.typeName || '-',
    category: categoryMeta?.label || '-',
    categoryTone: categoryMeta?.tone || 'gray',
    description: deviceType.description || '-',
    status: deviceType.status === 0 ? 'disabled' : 'enabled',
    createTime: deviceType.createTime || '-',
    updateTime: deviceType.updateTime || '-',
  }
}

const fetchDeviceTypeStats = async () => {
  const result = await listDeviceTypes({ pageNum: 1, pageSize: 10000 })
  const allTypes = result.data.list

  deviceTypeStats.value = {
    total: result.data.total,
    sensor: allTypes.filter((item) => item.category === 1).length,
    actuator: allTypes.filter((item) => item.category === 2).length,
    enabled: allTypes.filter((item) => item.status !== 0).length,
  }
}

/** 拉取分页列表，并把后端设备类型实体转换成表格行展示结构。 */
const fetchDeviceTypes = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listDeviceTypes({
      typeCode: searchForm.typeCode || undefined,
      typeName: searchForm.typeName || undefined,
      category: searchForm.category,
      status: getStatusValue(),
      pageNum,
      pageSize: size,
    })

    deviceTypes.value = result.data.list.map((item) => mapDeviceTypeToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.typeCode = ''
  searchForm.typeName = ''
  searchForm.category = undefined
  searchForm.status = ''
  void fetchDeviceTypes(1)
}

const handleSelectionChange = (rows: DeviceTypeRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editDeviceTypeId.value = null
  dialogVisible.value = true
}

const handleEditDeviceType = (row: DeviceTypeRow) => {
  editDeviceTypeId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void fetchDeviceTypes(editDeviceTypeId.value ? currentPage.value : 1)
  void fetchDeviceTypeStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchDeviceTypes(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchDeviceTypes(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return deviceTypes.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: DeviceTypeRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateDeviceTypeStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用设备类型成功' : '禁用设备类型成功')
    void fetchDeviceTypes(currentPage.value)
    void fetchDeviceTypeStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteDeviceType = async (row: DeviceTypeRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除设备类型“${row.typeName}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteDeviceType(row.id)
    ElMessage.success('删除设备类型成功')
    void fetchDeviceTypes(getNextPageAfterDelete(1))
    void fetchDeviceTypeStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的设备类型')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条设备类型吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteDeviceTypes(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除设备类型成功')
    void fetchDeviceTypes(getNextPageAfterDelete(selectedRows.value.length))
    void fetchDeviceTypeStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchDeviceTypes()
  void fetchDeviceTypeStats()
})
</script>

<template>
  <div class="device-type-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="device-type-shell admin-shell">
      <Header :breadcrumbs="['首页', '设备管理', '设备类型管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="device-type-content admin-content">
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
              <el-button type="primary" :icon="Search" @click="fetchDeviceTypes(1)">查询</el-button>
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
              :data="deviceTypes"
              row-key="id"
              class="device-type-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="typeCode" label="类型编码" min-width="150" align="center" header-align="center" />
              <el-table-column prop="typeName" label="类型名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="设备分类" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="type-chip" :class="`chip-${row.categoryTone}`" effect="plain" round>
                    {{ row.category }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="类型说明" min-width="240" show-overflow-tooltip header-align="center" />
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
              <el-table-column label="操作" width="260" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditDeviceType(row)">
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
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteDeviceType(row)">
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

    <AddOrUpdate v-model="dialogVisible" :id="editDeviceTypeId" @success="handleDialogSuccess" />
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
  grid-template-columns: repeat(4, minmax(168px, 1fr));
  align-items: center;
  gap: 14px 18px;
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

.device-type-table {
  width: 100%;
}

.device-type-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.device-type-table :deep(.el-table__row) {
  height: 64px;
}

.device-type-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.type-chip {
  border-width: 1px;
  border-style: solid;
  font-size: 12px;
  font-weight: 600;
}

.chip-blue {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.chip-green {
  color: #059669;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.chip-gray {
  color: #475569;
  background: #f8fafc;
  border-color: #cbd5e1;
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
  .device-type-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .device-type-content {
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
