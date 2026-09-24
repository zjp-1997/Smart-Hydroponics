<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CirclePlus,
  DataLine,
  Delete,
  EditPen,
  MapLocation,
  OfficeBuilding,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  batchDeleteFarms,
  deleteFarm,
  getFarmStatistics,
  listFarms,
  updateFarmStatus,
  type Farm,
} from '@/api/farm'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getCurrentUser, isAdminUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

type RowStatus = 'enabled' | 'disabled'

interface FarmRow {
  id: number
  userId: number
  ownerName: string
  farmName: string
  farmCode: string
  imageUrl: string
  contact: string
  address: string
  totalArea: string
  coordinate: string
  plotCount: number
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
const selectedRows = ref<FarmRow[]>([])
const farms = ref<FarmRow[]>([])
const userOptions = ref<SmartPlantUser[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editFarmId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const currentUser = computed(() => getCurrentUser())
const isAdmin = computed(() => isAdminUser(currentUser.value))

const farmStats = ref({
  total: 0,
  plots: 0,
  totalArea: 0,
  enabled: 0,
  totalTrend: 0,
  plotTrend: 0,
  totalAreaTrend: 0,
  enabledTrend: 0,
})

const searchForm = reactive({
  farmName: '',
  farmCode: '',
  userId: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '农场总数',
    value: farmStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(farmStats.value.totalTrend),
    trendType: farmStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: OfficeBuilding,
    tone: 'blue',
  },
  {
    title: '地块总数',
    value: farmStats.value.plots.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(farmStats.value.plotTrend),
    trendType: farmStats.value.plotTrend >= 0 ? 'up' : 'down',
    icon: MapLocation,
    tone: 'green',
  },
  {
    title: '农场总面积',
    value: formatAreaTotal(farmStats.value.totalArea),
    desc: '较昨日',
    trend: formatTrend(farmStats.value.totalAreaTrend),
    trendType: farmStats.value.totalAreaTrend >= 0 ? 'up' : 'down',
    icon: DataLine,
    tone: 'purple',
  },
  {
    title: '启用农场',
    value: farmStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(farmStats.value.enabledTrend),
    trendType: farmStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => {
  const displayValue = Number.isInteger(value) ? Math.abs(value).toLocaleString() : Math.abs(value).toFixed(2)

  return `${value >= 0 ? '↑' : '↓'} ${displayValue}`
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

const formatAreaTotal = (area: number) => {
  return `${area.toLocaleString(undefined, { maximumFractionDigits: 2 })}亩`
}

const getStatisticNumber = (value: unknown) => {
  const numberValue = Number(value)

  return Number.isFinite(numberValue) ? numberValue : 0
}

const formatCoordinate = (longitude?: number, latitude?: number) => {
  if (longitude === undefined || longitude === null || latitude === undefined || latitude === null) {
    return '-'
  }

  return `${longitude}, ${latitude}`
}

const getCoordinate = (farm: Farm) => {
  return farm.coordinate || formatCoordinate(farm.longitude, farm.latitude)
}

// 后端允许保存完整地址或 /uploads 相对地址，列表展示前统一转换成浏览器可访问地址。
const resolveFarmImageUrl = (imgUrl?: string) => {
  return imgUrl ? getFileUrl(imgUrl) : ''
}

const getOwnerName = (farm: Farm) => {
  if (farm.nickname && farm.username) {
    return `${farm.nickname}（${farm.username}）`
  }

  return farm.nickname || farm.username || `用户${farm.userId}`
}

const getContact = (farm: Farm) => {
  const parts = [farm.contactPhone].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

const mapFarmToRow = (farm: Farm): FarmRow => ({
  id: farm.id ?? Date.now(),
  userId: farm.userId,
  ownerName: getOwnerName(farm),
  farmName: farm.farmName || '-',
  farmCode: farm.farmCode || '-',
  imageUrl: resolveFarmImageUrl(farm.imgUrl),
  contact: getContact(farm),
  address: formatValue(farm.address),
  totalArea: formatArea(farm.totalArea, farm.areaUnit),
  coordinate: getCoordinate(farm),
  plotCount: farm.plotCount ?? 0,
  status: farm.status === 0 ? 'disabled' : 'enabled',
  remark: farm.remark || '-',
  createTime: farm.createTime || '-',
})

const getUserOptionLabel = (user: SmartPlantUser) => {
  const name = user.nickname || user.username || `用户${user.id}`
  return user.username && user.nickname ? `${name}（${user.username}）` : name
}

const fetchUserOptions = async () => {
  if (!isAdmin.value) {
    userOptions.value = currentUser.value?.id ? [currentUser.value as SmartPlantUser] : []
    return
  }

  const result = await listSmartPlantUsers({ pageNum: 1, pageSize: 1000, roleCode: 'farm_owner', status: 1 })

  userOptions.value = result.data.list
}

const fetchFarmStats = async () => {
  const result = await getFarmStatistics()
  const stats = result.data || {}

  farmStats.value = {
    total: getStatisticNumber(stats.farmTotalCount),
    plots: getStatisticNumber(stats.plotTotalCount),
    totalArea: getStatisticNumber(stats.farmTotalArea),
    enabled: getStatisticNumber(stats.enabledFarmCount),
    totalTrend: getStatisticNumber(stats.farmTotalTrend),
    plotTrend: getStatisticNumber(stats.plotTotalTrend),
    totalAreaTrend: getStatisticNumber(stats.farmTotalAreaTrend),
    enabledTrend: getStatisticNumber(stats.enabledFarmTrend),
  }
}

const fetchFarms = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listFarms({
      farmName: searchForm.farmName || undefined,
      farmCode: searchForm.farmCode || undefined,
      userId: isAdmin.value ? searchForm.userId : undefined,
      status: getNumberValue(searchForm.status),
      pageNum,
      pageSize: size,
    })

    farms.value = result.data.list.map((item) => mapFarmToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.farmName = ''
  searchForm.farmCode = ''
  searchForm.userId = undefined
  searchForm.status = ''
  void fetchFarms(1)
}

const handleSelectionChange = (rows: FarmRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editFarmId.value = null
  addDialogVisible.value = true
}

const handleEditFarm = (row: FarmRow) => {
  editFarmId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchFarms(editFarmId.value ? currentPage.value : 1)
  void fetchFarmStats()
  void fetchUserOptions()
}

const handleCurrentPageChange = (page: number) => {
  void fetchFarms(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchFarms(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return farms.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleStatus = async (row: FarmRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateFarmStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用农场成功' : '禁用农场成功')
    void fetchFarms(currentPage.value)
    void fetchFarmStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDeleteFarm = async (row: FarmRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除农场「${row.farmName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteFarm(row.id)
    ElMessage.success('删除农场成功')
    void fetchFarms(getNextPageAfterDelete(1))
    void fetchFarmStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的农场')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 个农场吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteFarms(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除农场成功')
    void fetchFarms(getNextPageAfterDelete(selectedRows.value.length))
    void fetchFarmStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchUserOptions()
  void fetchFarms()
  void fetchFarmStats()
})
</script>

<template>
  <div class="farm-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="farm-shell admin-shell">
      <Header :breadcrumbs="['首页', '地块管理', '农场管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="farm-content admin-content">
        <section class="stats-grid">
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
            <el-form-item label="农场名称">
              <el-input v-model="searchForm.farmName" placeholder="请输入农场名称" clearable />
            </el-form-item>
            <el-form-item v-if="isAdmin" label="所属用户">
              <el-select v-model="searchForm.userId" placeholder="全部农场主" filterable clearable>
                <el-option
                  v-for="item in userOptions"
                  :key="item.id"
                  :label="getUserOptionLabel(item)"
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
              <el-button type="primary" :icon="Search" @click="fetchFarms(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增农场</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="farms"
              row-key="id"
              class="farm-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="农场图片" width="118" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="farm-image"
                    :src="row.imageUrl"
                    :preview-src-list="[row.imageUrl]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else class="empty-image">暂无图片</span>
                </template>
              </el-table-column>
              <el-table-column prop="farmName" label="农场名称" min-width="150" align="center" header-align="center" />
              <!-- <el-table-column prop="farmCode" label="农场编号" min-width="130" align="center" header-align="center" /> -->
              <el-table-column
                v-if="isAdmin"
                prop="ownerName"
                label="所属用户"
                min-width="170"
                align="center"
                header-align="center"
                show-overflow-tooltip
              />
              <el-table-column
                prop="contact"
                label="联系电话"
                min-width="160"
                align="center"
                header-align="center"
                show-overflow-tooltip
              />
              <el-table-column
                prop="address"
                label="地址"
                min-width="200"
                align="center"
                header-align="center"
                show-overflow-tooltip
              />
              <el-table-column prop="totalArea" label="农场面积" min-width="110" align="center" header-align="center" />
              <el-table-column
                prop="coordinate"
                label="经纬度"
                min-width="180"
                align="center"
                header-align="center"
                show-overflow-tooltip
              />
              <el-table-column prop="plotCount" label="地块数" min-width="90" align="center" header-align="center" />
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
              <el-table-column
                prop="remark"
                label="备注"
                min-width="180"
                align="center"
                header-align="center"
                show-overflow-tooltip
              />
              <el-table-column label="操作" width="250" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditFarm(row)">
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
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteFarm(row)">
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

    <AddOrUpdate v-model="addDialogVisible" :id="editFarmId" @success="handleSaveSuccess" />
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

.tone-purple {
  background: linear-gradient(135deg, #8b5cf6, #6d5dfc);
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

.farm-table {
  width: 100%;
}

.farm-image {
  width: 64px;
  height: 48px;
  border-radius: 6px;
  background: #f1f5f9;
}

.empty-image {
  color: #94a3b8;
  font-size: 12px;
}

.farm-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.farm-table :deep(.el-table__row) {
  height: 64px;
}

.farm-table :deep(.el-table__cell) {
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
  .farm-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .farm-content {
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
