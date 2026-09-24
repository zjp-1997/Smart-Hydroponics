<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCloseFilled,
  CirclePlus,
  Connection,
  Delete,
  EditPen,
  Refresh,
  Search,
  VideoCamera,
} from '@element-plus/icons-vue'
import {
  batchDeleteCameraDevices,
  deleteCameraDevice,
  listCameraDevices,
  updateCameraDeviceOnlineStatus,
  type CameraDevice,
} from '@/api/cameraDevice'
import AddOrUpdate from './AddOrUpdate.vue'

type CameraStatus = 'online' | 'offline'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface CameraRow {
  id: number
  deviceId: number
  name: string
  streamProtocol: string
  streamUrl: string
  snapshotUrl: string
  resolution: string
  status: CameraStatus
  plotName: string
  plotCode: string
  direction: string
  createTime: string
}

interface CameraStats {
  total: number
  online: number
  offline: number
  http: number
  totalTrend: number
  onlineTrend: number
  offlineTrend: number
  httpTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<CameraRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editCameraId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])
const cameras = ref<CameraRow[]>([])

const cameraStats = ref<CameraStats>({
  total: 0,
  online: 0,
  offline: 0,
  http: 0,
  totalTrend: 0,
  onlineTrend: 0,
  offlineTrend: 0,
  httpTrend: 0,
})

const searchForm = ref({
  name: '',
  plotName: '',
  streamProtocol: '',
  onlineStatus: undefined as number | undefined,
})

const onlineStatusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
]

const protocolOptions = [
  { label: 'RTSP', value: 'RTSP' },
  { label: 'GB28181', value: 'GB28181' },
  { label: 'HTTP', value: 'HTTP' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '监控总数',
    value: cameraStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cameraStats.value.totalTrend),
    trendType: cameraStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: VideoCamera,
    tone: 'blue',
  },
  {
    title: '在线监控',
    value: cameraStats.value.online.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cameraStats.value.onlineTrend),
    trendType: cameraStats.value.onlineTrend >= 0 ? 'up' : 'down',
    icon: Connection,
    tone: 'green',
  },
  {
    title: '离线监控',
    value: cameraStats.value.offline.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cameraStats.value.offlineTrend),
    trendType: cameraStats.value.offlineTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'orange',
  },
  {
    title: 'HTTP流',
    value: cameraStats.value.http.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(cameraStats.value.httpTrend),
    trendType: cameraStats.value.httpTrend >= 0 ? 'up' : 'down',
    icon: VideoCamera,
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

const mapCameraToRow = (camera: CameraDevice): CameraRow => ({
  id: camera.id ?? Date.now(),
  deviceId: camera.deviceId,
  name: camera.name || '-',
  streamProtocol: camera.streamProtocol || '-',
  streamUrl: camera.streamUrl || '-',
  snapshotUrl: camera.snapshotUrl || '-',
  resolution: camera.resolution || '-',
  status: camera.onlineStatus === 1 ? 'online' : 'offline',
  plotName: camera.plotName || '-',
  plotCode: camera.plotCode || '',
  direction: camera.direction || '-',
  createTime: camera.createTime || '-',
})

const fetchCameraStats = async () => {
  const [allResult, onlineResult, offlineResult, httpResult] = await Promise.all([
    listCameraDevices({ pageNum: 1, pageSize: 10000 }),
    listCameraDevices({ pageNum: 1, pageSize: 1, onlineStatus: 1 }),
    listCameraDevices({ pageNum: 1, pageSize: 1, onlineStatus: 0 }),
    listCameraDevices({ pageNum: 1, pageSize: 1, streamProtocol: 'HTTP' }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allCameras = allResult.data.list
  const todayCameras = allCameras.filter((item) => isSameDate(item.createTime, today))
  const yesterdayCameras = allCameras.filter((item) => isSameDate(item.createTime, yesterday))
  const todayOnline = todayCameras.filter((item) => item.onlineStatus === 1)
  const yesterdayOnline = yesterdayCameras.filter((item) => item.onlineStatus === 1)
  const todayOffline = todayCameras.filter((item) => item.onlineStatus !== 1)
  const yesterdayOffline = yesterdayCameras.filter((item) => item.onlineStatus !== 1)
  const todayHttp = todayCameras.filter((item) => item.streamProtocol === 'HTTP')
  const yesterdayHttp = yesterdayCameras.filter((item) => item.streamProtocol === 'HTTP')

  cameraStats.value = {
    total: allResult.data.total,
    online: onlineResult.data.total,
    offline: offlineResult.data.total,
    http: httpResult.data.total,
    totalTrend: todayCameras.length - yesterdayCameras.length,
    onlineTrend: todayOnline.length - yesterdayOnline.length,
    offlineTrend: todayOffline.length - yesterdayOffline.length,
    httpTrend: todayHttp.length - yesterdayHttp.length,
  }
}

const fetchCameras = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCameraDevices({
      name: searchForm.value.name || undefined,
      plotName: searchForm.value.plotName || undefined,
      streamProtocol: searchForm.value.streamProtocol || undefined,
      onlineStatus: searchForm.value.onlineStatus,
      pageNum,
      pageSize: size,
    })

    cameras.value = result.data.list.map(mapCameraToRow)
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
    plotName: '',
    streamProtocol: '',
    onlineStatus: undefined,
  }
  void fetchCameras(1)
}

const handleSelectionChange = (rows: CameraRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editCameraId.value = null
  dialogVisible.value = true
}

const handleEditCamera = (row: CameraRow) => {
  editCameraId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void fetchCameras(editCameraId.value ? currentPage.value : 1)
  void fetchCameraStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchCameras(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchCameras(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return cameras.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const updateStatusLoading = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleOnlineStatus = async (row: CameraRow, enabled?: boolean | string | number) => {
  const nextOnline = typeof enabled === 'boolean' ? enabled : row.status !== 'online'
  const nextStatus = nextOnline ? 1 : 0

  updateStatusLoading(row.id, true)

  try {
    await updateCameraDeviceOnlineStatus(row.id, nextStatus)
    // 该操作只维护后台在线标记，不将其描述为摄像头硬件回执。
    ElMessage.success(nextOnline ? '在线状态标记已保存' : '离线状态标记已保存')
    void fetchCameras(currentPage.value)
    void fetchCameraStats()
  } finally {
    updateStatusLoading(row.id, false)
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的监控设备')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 个监控设备吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteCameraDevices(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchCameras(getNextPageAfterDelete(selectedRows.value.length))
    void fetchCameraStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteCamera = async (row: CameraRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除监控设备「${row.name}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCameraDevice(row.id)
    ElMessage.success('删除成功')
    void fetchCameras(getNextPageAfterDelete(1))
    void fetchCameraStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const openStreamUrl = (row: CameraRow) => {
  if (!row.streamUrl || row.streamUrl === '-') {
    ElMessage.warning('当前监控设备未配置视频流地址')
    return
  }

  window.open(row.streamUrl, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  void fetchCameras()
  void fetchCameraStats()
})
</script>

<template>
  <div class="camera-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="camera-shell admin-shell">
      <Header :breadcrumbs="['首页', '监控管理', '监控设备管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="camera-content admin-content">
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
            <el-form-item label="设备名称">
              <el-input v-model="searchForm.name" placeholder="请输入设备名称" clearable />
            </el-form-item>
            <el-form-item label="所属地块">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <!-- <el-form-item label="视频协议">
              <el-select v-model="searchForm.streamProtocol" placeholder="全部协议" clearable>
                <el-option v-for="item in protocolOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item> -->
            <el-form-item label="在线状态">
              <el-select v-model="searchForm.onlineStatus" placeholder="全部状态" clearable>
                <el-option v-for="item in onlineStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchCameras(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增监控</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="cameras"
              row-key="id"
              class="camera-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="deviceId" label="来源编号" min-width="110" align="center" header-align="center" />
              <el-table-column prop="name" label="设备名称" min-width="160" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch
                      :model-value="row.status === 'online'"
                      :loading="isStatusUpdating(row.id)"
                      @change="(value: boolean | string | number) => handleToggleOnlineStatus(row, value)"
                    />
                    <span>{{ row.status === 'online' ? '在线' : '离线' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="协议" min-width="100" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="chip-green" effect="plain" round>{{ row.streamProtocol }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="resolution" label="分辨率" min-width="120" align="center" header-align="center" />
              <el-table-column label="所属地块" min-width="180" align="center" header-align="center" show-overflow-tooltip>
                <template #default="{ row }">
                  <span>{{ row.plotCode ? `${row.plotName}（${row.plotCode}）` : row.plotName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="direction" label="监控方向" min-width="130" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="操作" width="300" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditCamera(row)">编辑</el-button>
                    <el-button link type="primary" :icon="VideoCamera" @click="openStreamUrl(row)">预览</el-button>
                    <el-button
                      link
                      :type="row.status === 'online' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleOnlineStatus(row)"
                    >
                      {{ row.status === 'online' ? '离线' : '在线' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteCamera(row)">删除</el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editCameraId" @success="handleDialogSuccess" />
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
  grid-template-columns: repeat(5, minmax(160px, 1fr));
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

.camera-table {
  width: 100%;
}

.camera-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.camera-table :deep(.el-table__row) {
  height: 64px;
}

.camera-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
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

.chip-blue,
.chip-green,
.chip-orange {
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

.chip-orange {
  color: #d97706;
  background: #fffbeb;
  border-color: #fef3c7;
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
  .camera-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .camera-content {
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
