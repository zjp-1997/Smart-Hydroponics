<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCloseFilled,
  CirclePlus,
  Connection,
  Delete,
  EditPen,
  Monitor,
  Refresh,
  Search,
  SwitchButton,
} from '@element-plus/icons-vue'
import {
  batchDeleteIotDevices,
  deleteIotDevice,
  listIotDevices,
  updateIotDeviceStatus,
  type IotDevice,
} from '@/api/iotDevice'
import { listDeviceTypes, type DeviceType } from '@/api/deviceType'
import { getConfigurationSaveFeedback } from '@/utils/hardwareFeedback'
import AddOrUpdate from './AddOrUpdate.vue'

const route = useRoute()

type DeviceStatus = 'on' | 'off'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface DeviceRow {
  id: number
  deviceCode: string
  name: string
  type: string
  typeValue: number | undefined
  typeTone: string
  status: DeviceStatus
  statusTone: string
  plotName: string
  plotCode: string
  onlineStatus: string
  onlineStatusValue: number
  onlineTone: string
  healthStatus: string
  healthStatusValue: number
  healthTone: string
  installTime: string
  onlineDurationSeconds: number
  onlineDurationSyncedAt: number
  onlineDurationText: string
  location: string
  createTime: string
}

interface DeviceStats {
  total: number
  online: number
  running: number
  fault: number
  totalTrend: number
  onlineTrend: number
  runningTrend: number
  faultTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<DeviceRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editDeviceId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])
const devices = ref<DeviceRow[]>([])
const deviceTypeOptions = ref<Array<{ label: string; value: number; tone: string }>>([])
const nowTick = ref(Date.now())
let onlineDurationTimer: number | undefined

const deviceStats = ref<DeviceStats>({
  total: 0,
  online: 0,
  running: 0,
  fault: 0,
  totalTrend: 0,
  onlineTrend: 0,
  runningTrend: 0,
  faultTrend: 0,
})

const searchForm = ref({
  name: '',
  typeId: undefined as number | undefined,
  onlineStatus: undefined as number | undefined,
})

const fallbackTypeToneMap = ['green', 'amber', 'violet', 'blue', 'cyan', 'gray']

const onlineStatusOptions = [
  { label: '在线', value: 1, tone: 'green' },
  { label: '离线', value: 0, tone: 'gray' },
]

const healthStatusOptions = [
  { label: '正常', value: 0, tone: 'green' },
  { label: '故障', value: 1, tone: 'red' },
  { label: '维护中', value: 2, tone: 'amber' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '设备总数',
    value: deviceStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(deviceStats.value.totalTrend),
    trendType: deviceStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Monitor,
    tone: 'blue',
  },
  {
    title: '在线设备',
    value: deviceStats.value.online.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(deviceStats.value.onlineTrend),
    trendType: deviceStats.value.onlineTrend >= 0 ? 'up' : 'down',
    icon: Connection,
    tone: 'green',
  },
  {
    title: '开启配置',
    value: deviceStats.value.running.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(deviceStats.value.runningTrend),
    trendType: deviceStats.value.runningTrend >= 0 ? 'up' : 'down',
    icon: SwitchButton,
    tone: 'orange',
  },
  {
    title: '故障设备',
    value: deviceStats.value.fault.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(deviceStats.value.faultTrend),
    trendType: deviceStats.value.faultTrend >= 0 ? 'up' : 'down',
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

const getTypeMeta = (typeId?: number) => {
  return deviceTypeOptions.value.find((item) => item.value === typeId)
}

const getHealthMeta = (healthStatus?: number) => {
  return healthStatusOptions.find((item) => item.value === healthStatus)
}

const getOnlineMeta = (onlineStatus?: number) => {
  return onlineStatusOptions.find((item) => item.value === onlineStatus)
}

const formatOnlineDuration = (seconds?: number) => {
  const totalMinutes = Math.max(0, Math.floor((seconds || 0) / 60))
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60

  if (days > 0) {
    return `${days}天${hours}小时`
  }

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  }

  if (minutes > 0) {
    return `${minutes}分钟`
  }

  return '0分钟'
}

const resolveOnlineDurationSeconds = (row: DeviceRow) => {
  if (row.onlineStatusValue !== 1 || row.healthStatusValue !== 0) {
    return row.onlineDurationSeconds
  }

  const elapsedSeconds = Math.floor((nowTick.value - row.onlineDurationSyncedAt) / 1000)
  return Math.max(0, row.onlineDurationSeconds + elapsedSeconds)
}

const tableDevices = computed(() =>
  devices.value.map((row) => ({
    ...row,
    onlineDurationText: formatOnlineDuration(resolveOnlineDurationSeconds(row)),
  })),
)

/**
 * 将后端 IotDevice 实体转换为表格行数据。
 * 标签文案、颜色、空值兜底统一集中在这里，表格模板只负责展示。
 */
const mapIotDeviceToRow = (device: IotDevice): DeviceRow => {
  const typeMeta = getTypeMeta(device.typeId)
  const healthMeta = getHealthMeta(device.healthStatus)
  const onlineMeta = getOnlineMeta(device.onlineStatus)
  const isRunning = device.controlStatus === 1
  const plotName = device.plotName || device.location || '-'

  return {
    id: device.id ?? Date.now(),
    deviceCode: device.deviceCode || '-',
    name: device.name || '-',
    type: device.typeName || typeMeta?.label || '-',
    typeValue: device.typeId,
    typeTone: typeMeta?.tone || 'gray',
    status: isRunning ? 'on' : 'off',
    statusTone: isRunning ? 'green' : 'gray',
    plotName,
    plotCode: device.plotCode || '',
    onlineStatus: onlineMeta?.label || '离线',
    onlineStatusValue: device.onlineStatus ?? 0,
    onlineTone: onlineMeta?.tone || 'gray',
    healthStatus: healthMeta?.label || '-',
    healthStatusValue: device.healthStatus ?? 0,
    healthTone: healthMeta?.tone || 'gray',
    installTime: device.installTime || device.createTime || '-',
    onlineDurationSeconds: Math.max(0, Math.floor(device.onlineDuration || 0)),
    onlineDurationSyncedAt: Date.now(),
    onlineDurationText: formatOnlineDuration(device.onlineDuration),
    location: plotName,
    createTime: device.createTime || '-',
  }
}

const fetchDeviceStats = async () => {
  const [allResult, onlineResult, runningResult, faultResult] = await Promise.all([
    listIotDevices({ pageNum: 1, pageSize: 10000 }),
    listIotDevices({ pageNum: 1, pageSize: 1, onlineStatus: 1 }),
    listIotDevices({ pageNum: 1, pageSize: 1, controlStatus: 1 }),
    listIotDevices({ pageNum: 1, pageSize: 1, healthStatus: 1 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allDevices = allResult.data.list
  const todayDevices = allDevices.filter((item) => isSameDate(item.createTime, today))
  const yesterdayDevices = allDevices.filter((item) => isSameDate(item.createTime, yesterday))
  const todayOnline = todayDevices.filter((item) => item.onlineStatus === 1)
  const yesterdayOnline = yesterdayDevices.filter((item) => item.onlineStatus === 1)
  const todayRunning = todayDevices.filter((item) => item.controlStatus === 1)
  const yesterdayRunning = yesterdayDevices.filter((item) => item.controlStatus === 1)
  const todayFault = todayDevices.filter((item) => item.healthStatus === 1)
  const yesterdayFault = yesterdayDevices.filter((item) => item.healthStatus === 1)

  deviceStats.value = {
    total: allResult.data.total,
    online: onlineResult.data.total,
    running: runningResult.data.total,
    fault: faultResult.data.total,
    totalTrend: todayDevices.length - yesterdayDevices.length,
    onlineTrend: todayOnline.length - yesterdayOnline.length,
    runningTrend: todayRunning.length - yesterdayRunning.length,
    faultTrend: todayFault.length - yesterdayFault.length,
  }
}

const mapDeviceTypeToOption = (item: DeviceType, index: number) => ({
  label: item.typeName || item.typeCode || `设备类型${item.id}`,
  value: item.id || 0,
  tone: fallbackTypeToneMap[index % fallbackTypeToneMap.length] || 'gray',
})

const fetchDeviceTypeOptions = async () => {
  const result = await listDeviceTypes({ pageNum: 1, pageSize: 1000, status: 1 })
  deviceTypeOptions.value = result.data.list
    .filter((item) => item.id !== undefined && item.id !== null)
    .map(mapDeviceTypeToOption)
}

/** 拉取设备分页数据，供查询、分页、弹框提交和状态操作后刷新。 */
const fetchDevices = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listIotDevices({
      name: searchForm.value.name || undefined,
      typeId: searchForm.value.typeId,
      onlineStatus: searchForm.value.onlineStatus,
      pageNum,
      pageSize: size,
    })

    devices.value = result.data.list.map((item) => mapIotDeviceToRow(item))
    nowTick.value = Date.now()
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
    typeId: undefined,
    onlineStatus: undefined,
  }
  void fetchDevices(1)
}

const handleSelectionChange = (rows: DeviceRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editDeviceId.value = null
  dialogVisible.value = true
}

const handleEditDevice = (row: DeviceRow) => {
  editDeviceId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void fetchDevices(editDeviceId.value ? currentPage.value : 1)
  void fetchDeviceStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchDevices(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchDevices(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return devices.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const updateLoadingIds = (ids: number[], id: number, loading: boolean) => {
  return loading ? [...ids, id] : ids.filter((item) => item !== id)
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const handleToggleStatus = async (row: DeviceRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'on'
  const nextStatus = nextEnabled ? 1 : 0

  statusLoadingIds.value = updateLoadingIds(statusLoadingIds.value, row.id, true)

  try {
    await updateIotDeviceStatus(row.id, nextStatus)
    // 当前接口只保存期望控制状态，没有设备回执，因此明确提示仍需下发。
    const feedback = getConfigurationSaveFeedback(0)
    ElMessage({
      type: feedback.type,
      message: `${nextEnabled ? '期望开启' : '期望关闭'}${feedback.text}`,
    })
    void fetchDevices(currentPage.value)
    void fetchDeviceStats()
  } finally {
    statusLoadingIds.value = updateLoadingIds(statusLoadingIds.value, row.id, false)
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的设备')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 台设备吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteIotDevices(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchDevices(getNextPageAfterDelete(selectedRows.value.length))
    void fetchDeviceStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteDevice = async (row: DeviceRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除设备「${row.name}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteIotDevice(row.id)
    ElMessage.success('删除成功')
    void fetchDevices(getNextPageAfterDelete(1))
    void fetchDeviceStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

// 首页搜索命中设备后，将结果标题带入本页现有名称筛选条件。
const applyHomeRouteIntent = () => {
  const keyword = typeof route.query.keyword === 'string' ? route.query.keyword.trim() : ''
  if (keyword) searchForm.value.name = keyword
}

watch(() => route.fullPath, () => {
  applyHomeRouteIntent()
  if (route.query.keyword) void fetchDevices(1)
})

onMounted(() => {
  applyHomeRouteIntent()
  void fetchDeviceTypeOptions()
  void fetchDevices()
  void fetchDeviceStats()
  onlineDurationTimer = window.setInterval(() => {
    nowTick.value = Date.now()
  }, 60 * 1000)
})

onBeforeUnmount(() => {
  if (onlineDurationTimer !== undefined) {
    window.clearInterval(onlineDurationTimer)
  }
})
</script>

<template>
  <div class="device-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="device-shell admin-shell">
      <Header :breadcrumbs="['首页', '设备管理', '设备信息管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="device-content admin-content">
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
            <el-form-item label="设备类型">
              <el-select v-model="searchForm.typeId" placeholder="全部类型" clearable>
                <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="在线状态">
              <el-select v-model="searchForm.onlineStatus" placeholder="全部状态" clearable>
                <el-option v-for="item in onlineStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchDevices(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增设备</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="tableDevices"
              row-key="id"
              class="device-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="name" label="设备名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="设备类型" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="device-chip" :class="`chip-${row.typeTone}`" effect="plain" round>{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="期望状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <span class="status-chip" :class="`chip-${row.statusTone}`">
                      {{ row.status === 'on' ? '期望开启' : '期望关闭' }}
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="在线状态" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="device-chip" :class="`chip-${row.onlineTone}`" effect="plain" round>
                    {{ row.onlineStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="onlineDurationText" label="在线时长" min-width="130" align="center" header-align="center" />
              <el-table-column prop="installTime" label="安装时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="健康状态" min-width="140" align="center" header-align="center">
                 <template #default="{ row }">
                  <el-tag class="device-chip" :class="`chip-${row.healthTone}`" effect="plain" round>
                    {{ row.healthStatus }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="location" label="安装地块" min-width="180" show-overflow-tooltip header-align="center" />
             <el-table-column label="操作" width="310" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditDevice(row)">编辑</el-button>
                    <el-button
                      link
                      :type="row.status === 'on' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'on' ? '设为关闭' : '设为开启' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteDevice(row)">删除</el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editDeviceId" @success="handleDialogSuccess" />
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

.device-table {
  width: 100%;
}

.device-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.device-table :deep(.el-table__row) {
  height: 64px;
}

.device-table :deep(.el-table__cell) {
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

.device-chip,
.status-chip {
  border-width: 1px;
  border-style: solid;
  font-size: 12px;
  font-weight: 600;
}

.status-chip {
  min-width: 42px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-radius: 999px;
  line-height: 1;
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

.chip-amber {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.chip-violet {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ddd6fe;
}

.chip-cyan {
  color: #0891b2;
  background: #ecfeff;
  border-color: #a5f3fc;
}

.chip-red {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fecaca;
}

.chip-gray {
  color: #475569;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.table-actions {
  gap: 2px;
  white-space: nowrap;
}

.mini-select {
  width: 104px;
}

.mini-select :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 6px;
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
  .device-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .device-content {
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
