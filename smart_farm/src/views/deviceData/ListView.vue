<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  DataAnalysis,
  Delete,
  Document,
  Monitor,
  Refresh,
  Search,
  View,
} from '@element-plus/icons-vue'
import {
  batchDeleteCollectedDeviceData,
  deleteCollectedDeviceData,
  getCollectedDeviceDataById,
  getCollectedDeviceDataStatistics,
  listCollectedDeviceData,
  type CollectedDeviceData,
  type CollectedDeviceDataStatistics,
  type DeviceDataMode,
  type DeviceDataMetricValue,
} from '@/api/collectedDeviceData'
import { listPlots, type Plot } from '@/api/plot'

interface MetricColumn {
  prop: string
  label: string
  unit?: string
  minWidth?: number
}

interface DetailItem {
  label: string
  value: string
  wide?: boolean
}

interface PageConfig {
  mode: DeviceDataMode
  title: string
  breadcrumb: string
  deleteName: string
  primaryMetricKey: string
  primaryMetricLabel: string
  primaryMetricUnit?: string
  metricColumns: MetricColumn[]
}

interface TableRow {
  id: number
  source: CollectedDeviceData
  deviceCode: string
  deviceName: string
  typeName: string
  plotName: string
  statusText: string
  statusTone: string
  abnormalDetail: string
  cumulativeRuntime: string
  collectTime: string
  createTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

const pageConfigs: Record<DeviceDataMode, PageConfig> = {
  environment: {
    mode: 'environment',
    title: '环境数据管理',
    breadcrumb: '环境数据管理',
    deleteName: '环境数据',
    primaryMetricKey: 'avgAirTemperature',
    primaryMetricLabel: '平均温度',
    primaryMetricUnit: '℃',
    metricColumns: [
      { prop: 'airTemperature', label: '空气温度', unit: '℃' },
      { prop: 'airHumidity', label: '空气湿度', unit: '%' },
      { prop: 'windSpeed', label: '风速', unit: 'm/s' },
      { prop: 'airPressure', label: '气压', unit: 'hPa' },
      { prop: 'co2Concentration', label: 'CO2浓度', unit: 'ppm' },
      { prop: 'pm25', label: 'PM2.5', unit: 'μg/m³' },
    ],
  },
  waterQuality: {
    mode: 'waterQuality',
    title: '水质数据管理',
    breadcrumb: '水质数据管理',
    deleteName: '水质数据',
    primaryMetricKey: 'avgPh',
    primaryMetricLabel: '平均PH',
    metricColumns: [
      { prop: 'waterTemperature', label: '水温', unit: '℃' },
      { prop: 'ph', label: 'PH值' },
      { prop: 'ecValue', label: 'EC值', unit: 'mS/cm' },
      { prop: 'dissolvedOxygen', label: '溶解氧', unit: 'mg/L' },
    ],
  },
  light: {
    mode: 'light',
    title: '补光灯数据管理',
    breadcrumb: '补光灯数据管理',
    deleteName: '补光灯数据',
    primaryMetricKey: 'avgLightIntensity',
    primaryMetricLabel: '平均光照',
    primaryMetricUnit: 'lux',
    metricColumns: [{ prop: 'lightIntensity', label: '光照强度', unit: 'lux', minWidth: 150 }],
  },
  pump: {
    mode: 'pump',
    title: '水泵数据管理',
    breadcrumb: '水泵数据管理',
    deleteName: '水泵数据',
    primaryMetricKey: 'avgWaterFlow',
    primaryMetricLabel: '平均水流量',
    primaryMetricUnit: 'm³/h',
    metricColumns: [
      { prop: 'waterFlow', label: '水流量', unit: 'm³/h' },
      { prop: 'waterPressure', label: '水压', unit: 'MPa' },
    ],
  },
}

const route = useRoute()
const sidebarVisible = ref(false)
const selectedRows = ref<TableRow[]>([])
const rows = ref<TableRow[]>([])
const plotOptions = ref<Plot[]>([])
const tableLoading = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statistics = ref<CollectedDeviceDataStatistics>({})
const detailData = ref<CollectedDeviceData | null>(null)

const searchForm = reactive({
  plotId: undefined as number | undefined,
  deviceCode: '',
  dataStatus: undefined as number | undefined,
  collectRange: [] as string[],
})

const currentMode = computed<DeviceDataMode>(() => {
  const mode = route.meta.dataMode
  return (typeof mode === 'string' && mode in pageConfigs ? mode : 'environment') as DeviceDataMode
})

const pageConfig = computed(() => pageConfigs[currentMode.value])

const statCards = computed<StatCard[]>(() => [
  {
    title: '数据总数',
    value: formatCount(statistics.value.totalCount),
    desc: '当前筛选范围',
    icon: DataAnalysis,
    tone: 'blue',
  },
  {
    title: '正常数据',
    value: formatCount(statistics.value.normalCount),
    desc: '状态正常记录',
    icon: CircleCheckFilled,
    tone: 'green',
  },
  {
    title: '异常数据',
    value: formatCount(statistics.value.abnormalCount),
    desc: '需要关注记录',
    icon: CircleCloseFilled,
    tone: 'orange',
  },
  {
    title: pageConfig.value.primaryMetricLabel,
    value: formatMetric(
      statistics.value[pageConfig.value.primaryMetricKey],
      pageConfig.value.primaryMetricUnit,
    ),
    desc: '平均采集指标',
    icon: Monitor,
    tone: 'gray',
  },
])

const detailItems = computed<DetailItem[]>(() => {
  if (!detailData.value) {
    return []
  }

  const item = detailData.value
  const commonItems: DetailItem[] = [
    { label: '设备编码', value: item.deviceCode || '-' },
    { label: '设备名称', value: item.deviceName || '-' },
    { label: '设备类型', value: item.typeName || '-' },
    { label: '地块名称', value: item.plotName || '-' },
    { label: '数据状态', value: getStatusMeta(item.dataStatus).text },
    { label: '累计运行', value: formatMetric(item.cumulativeRuntime, '小时') },
    { label: '采集时间', value: item.collectTime || '-' },
    { label: '创建时间', value: item.createTime || '-' },
  ]

  const metricItems = pageConfig.value.metricColumns.map((column) => ({
    label: column.label,
    value: formatMetric(getMetricValue(item, column.prop), column.unit),
  }))

  return [
    ...commonItems,
    ...metricItems,
    { label: '异常详情', value: item.abnormalDetail || '-', wide: true },
    { label: '备注', value: item.remark || '-', wide: true },
  ]
})

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: DeviceDataMetricValue) => Number(value ?? 0).toLocaleString()

const formatMetric = (value?: DeviceDataMetricValue, unit?: string) => {
  if (value === undefined || value === null || value === '') {
    return '-'
  }

  return unit ? `${value} ${unit}` : String(value)
}

const getMetricValue = (item: CollectedDeviceData, prop: string) => {
  return (item as Record<string, DeviceDataMetricValue | undefined>)[prop]
}

const getStatusMeta = (status?: number) => {
  if (status === 2) {
    return { text: '异常', tone: 'red' }
  }

  return { text: '正常', tone: 'green' }
}

const buildBaseParams = () => ({
  plotId: searchForm.plotId,
  deviceCode: searchForm.deviceCode || undefined,
  dataStatus: searchForm.dataStatus,
  startTime: searchForm.collectRange?.[0],
  endTime: searchForm.collectRange?.[1],
})

const mapToRow = (item: CollectedDeviceData): TableRow => {
  const statusMeta = getStatusMeta(item.dataStatus)

  return {
    id: item.id ?? Date.now(),
    source: item,
    deviceCode: item.deviceCode || '-',
    deviceName: item.deviceName || '-',
    typeName: item.typeName || '-',
    plotName: item.plotName || '-',
    statusText: statusMeta.text,
    statusTone: statusMeta.tone,
    abnormalDetail: item.abnormalDetail || '-',
    cumulativeRuntime: formatMetric(item.cumulativeRuntime, '小时'),
    collectTime: item.collectTime || '-',
    createTime: item.createTime || '-',
  }
}

const fetchPlotOptions = async () => {
  const result = await listPlots({ pageNum: 1, pageSize: 1000 })
  plotOptions.value = result.data.list
}

const fetchStatistics = async () => {
  const result = await getCollectedDeviceDataStatistics(currentMode.value, buildBaseParams())
  statistics.value = result.data || {}
}

/** 拉取当前路由对应的数据列表，四个管理页共用同一套分页和筛选逻辑。 */
const fetchRows = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCollectedDeviceData(currentMode.value, {
      ...buildBaseParams(),
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
  await Promise.all([fetchRows(pageNum), fetchStatistics()])
}

const resetSearch = () => {
  searchForm.plotId = undefined
  searchForm.deviceCode = ''
  searchForm.dataStatus = undefined
  searchForm.collectRange = []
  void refreshPageData(1)
}

const handleSelectionChange = (value: TableRow[]) => {
  selectedRows.value = value
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

const openDetail = async (row: TableRow) => {
  detailVisible.value = true
  detailLoading.value = true

  try {
    const result = await getCollectedDeviceDataById(currentMode.value, row.id)
    detailData.value = result.data
  } finally {
    detailLoading.value = false
  }
}

const handleDelete = async (row: TableRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除这条${pageConfig.value.deleteName}吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCollectedDeviceData(currentMode.value, row.id)
    ElMessage.success('删除成功')
    void refreshPageData(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning(`请先选择需要删除的${pageConfig.value.deleteName}`)
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条${pageConfig.value.deleteName}吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteCollectedDeviceData(
      currentMode.value,
      selectedRows.value.map((row) => row.id),
    )
    ElMessage.success('批量删除成功')
    void refreshPageData(getNextPageAfterDelete(selectedRows.value.length))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

watch(
  () => currentMode.value,
  () => {
    resetSearch()
  },
)

onMounted(() => {
  void fetchPlotOptions()
  void refreshPageData()
})
</script>

<template>
  <div class="device-data-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="device-data-shell admin-shell">
      <Header :breadcrumbs="['首页', '环境监测管理', pageConfig.breadcrumb]" @toggle-sidebar="sidebarVisible = true" />

      <main class="device-data-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="78px">
            <el-form-item label="地块名称">
              <el-select v-model="searchForm.plotId" placeholder="全部地块" clearable filterable>
                <el-option
                  v-for="item in plotOptions"
                  :key="item.id"
                  :label="item.plotName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="数据状态">
              <el-select v-model="searchForm.dataStatus" placeholder="全部状态" clearable>
                <el-option label="正常" :value="1" />
                <el-option label="异常" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="采集时间" class="range-item">
              <el-date-picker
                v-model="searchForm.collectRange"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                range-separator="至"
                clearable
              />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="refreshPageData(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="rows"
              row-key="id"
              class="device-data-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" header-align="center" />
              <el-table-column prop="plotName" label="地块名称" min-width="150" align="center" header-align="center" />
              <el-table-column
                v-for="column in pageConfig.metricColumns"
                :key="String(column.prop)"
                :label="column.label"
                :min-width="column.minWidth || 120"
                align="center"
                header-align="center"
              >
                <template #default="{ row }">
                  {{ formatMetric(getMetricValue(row.source, column.prop), column.unit) }}
                </template>
              </el-table-column>
              <el-table-column label="数据状态" min-width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="status-chip" :class="`chip-${row.statusTone}`" effect="plain" round>
                    {{ row.statusText }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="abnormalDetail" label="异常详情" min-width="220" show-overflow-tooltip header-align="center" />
              <el-table-column prop="cumulativeRuntime" label="累计运行" min-width="130" align="center" header-align="center" />
              <el-table-column prop="collectTime" label="采集时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="操作" width="180" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="detailVisible"
      :title="`${pageConfig.title}详情`"
      width="760px"
      class="device-data-dialog"
      align-center
      destroy-on-close
    >
      <div v-loading="detailLoading" class="detail-grid">
        <article v-for="item in detailItems" :key="item.label" class="detail-item" :class="{ wide: item.wide }">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
      <template #footer>
        <el-button type="primary" :icon="Document" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
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
  grid-template-columns: minmax(180px, 0.85fr) minmax(180px, 0.85fr) minmax(150px, 0.7fr) minmax(320px, 1.4fr) minmax(140px, auto);
  align-items: center;
  gap: 14px 18px;
  padding-right: 128px;
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
.filter-form :deep(.el-select__wrapper),
.filter-form :deep(.el-date-editor) {
  height: 40px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.range-item :deep(.el-date-editor) {
  width: 100%;
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

.device-data-table {
  width: 100%;
}

.device-data-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.device-data-table :deep(.el-table__row) {
  height: 64px;
}

.device-data-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.status-chip {
  border-width: 1px;
  border-style: solid;
  font-size: 12px;
  font-weight: 600;
}

.chip-green {
  color: #059669;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.chip-red {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fecaca;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
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

:global(.device-data-dialog) {
  border-radius: 10px;
  overflow: hidden;
}

:global(.device-data-dialog .el-dialog__header) {
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.device-data-dialog .el-dialog__body) {
  padding: 22px 24px 8px;
  background: #fbfdff;
}

:global(.device-data-dialog .el-dialog__footer) {
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-item {
  min-height: 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.detail-item.wide {
  grid-column: 1 / -1;
}

.detail-item span {
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.detail-item strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  word-break: break-word;
}

@media (max-width: 1440px) {
  .filter-form {
    grid-template-columns: repeat(3, minmax(180px, 1fr));
    padding-right: 0;
  }

  .range-item {
    grid-column: span 2;
  }

  .manage-actions {
    position: static;
    width: fit-content;
  }
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}

@media (max-width: 960px) {
  .device-data-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .device-data-content {
    padding: 12px;
  }

  .stats-grid,
  .filter-form,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .range-item {
    grid-column: auto;
  }

  .manage-actions,
  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
