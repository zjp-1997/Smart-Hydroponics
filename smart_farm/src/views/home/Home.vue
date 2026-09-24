<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  CirclePlus,
  Cpu,
  DataLine,
  Grape,
  Histogram,
  MapLocation,
  Monitor,
  Notification,
  OfficeBuilding,
  Setting,
  Tools,
  WarningFilled,
} from '@element-plus/icons-vue'
import { Chart, type ChartConfiguration, type Plugin, registerables } from 'chart.js'
import { ElMessage } from 'element-plus'
import { listFarmTasks, type FarmTask } from '@/api/farmTask'
import {
  getHomeOperationOverview,
  getHomeStats,
  getHomeVisitTrend,
  getHomeWarehouseValueAnalysis,
  recordHomeVisit,
  type HomeOperationMetricKey,
  type HomeOperationOverviewItem,
  type HomeVisitTrendPoint,
  type HomeWarehouseValueCategory,
} from '@/api/homeDashboard'
import {
  getSystemAnnouncementById,
  listSystemAnnouncements,
  type SystemAnnouncement,
} from '@/api/sysMsg'

Chart.register(...registerables)

type TrendDirection = 'up' | 'down'
type TaskStatus = 'pending' | 'running' | 'done' | 'overdue' | 'cancelled'

interface MetricCard {
  label: string
  value: string
  unit?: string
  trend: string
  trendDirection: TrendDirection
  icon: Component
  tone: string
}

interface TaskRow {
  id: string
  name: string
  owner: string
  plot: string
  status: string
  statusType: TaskStatus
  time: string
}

interface OperationItem {
  label: string
  value: string
  desc: string
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const visitChartRef = ref<HTMLCanvasElement | null>(null)
const warehouseChartRef = ref<HTMLCanvasElement | null>(null)
const router = useRouter()
const activeVisitTab = ref('近7天')
const homeStatsLoading = ref(false)
const visitTrendLoading = ref(false)
const operationOverviewLoading = ref(false)
const warehouseAnalysisLoading = ref(false)
const announcementLoading = ref(false)
const announcementDetailLoading = ref(false)
const announcementDetailVisible = ref(false)
const farmTaskLoading = ref(false)
const visitTrendPoints = ref<HomeVisitTrendPoint[]>([])
const warehouseValueCategories = ref<HomeWarehouseValueCategory[]>([])
const announcements = ref<SystemAnnouncement[]>([])
const farmTasks = ref<FarmTask[]>([])
const currentAnnouncement = ref<SystemAnnouncement | null>(null)
const homeStats = ref({
  farms: 0,
  plots: 0,
  batches: 0,
  devices: 0,
  faults: 0,
})
const operationOverview = ref<Record<HomeOperationMetricKey, HomeOperationOverviewItem>>(createDefaultOperationOverview())

let visitChart: Chart | undefined
let warehouseChart: Chart | undefined

const formatCount = (value: number) => value.toLocaleString()
const formatDeviceCount = (value: number) => `${formatCount(value)}台`
const formatOnlineRate = (value: number) => `${Number(value || 0).toFixed(1)}%`
const formatCurrency = (value: number) => new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  maximumFractionDigits: 2,
}).format(value || 0)
const formatCompactCurrency = (value: number) => new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  notation: 'compact',
  maximumFractionDigits: 1,
}).format(value || 0)
const warehouseTotalValue = computed(() =>
  warehouseValueCategories.value.reduce((total, item) => total + Number(item.totalValue || 0), 0),
)
const warehouseChartColors = (count: number) => Array.from(
  { length: count },
  (_, index) => `hsl(${Math.round((index * 360) / Math.max(count, 1))} 68% 52%)`,
)

const metricCards = computed<MetricCard[]>(() => [
  { label: '农场总数', value: formatCount(homeStats.value.farms), trend: '实时', trendDirection: 'up', icon: OfficeBuilding, tone: 'primary' },
  { label: '地块数量', value: formatCount(homeStats.value.plots), trend: '实时', trendDirection: 'up', icon: MapLocation, tone: 'emerald' },
  { label: '种植批次', value: formatCount(homeStats.value.batches), trend: '实时', trendDirection: 'up', icon: Grape, tone: 'amber' },
  { label: '设备数量', value: formatCount(homeStats.value.devices), trend: '实时', trendDirection: 'up', icon: Cpu, tone: 'violet' },
  { label: '故障记录', value: formatCount(homeStats.value.faults), trend: '实时', trendDirection: homeStats.value.faults > 0 ? 'down' : 'up', icon: WarningFilled, tone: 'sky' },
])

// 快捷入口复用现有页面和全局弹框，不复制业务表单；数据统计继续保留静态提示。
const quickActions = [
  { label: '添加任务', icon: CirclePlus, tone: 'primary', route: '/farm-task/list', action: 'create' },
  { label: '发布通知', icon: Notification, tone: 'emerald', route: '/sys_msg/list', action: 'create' },
  { label: '添加地块', icon: MapLocation, tone: 'amber', route: '/plot/info', action: 'create' },
  { label: '设备管理', icon: Tools, tone: 'violet', route: '/device/list' },
  { label: '数据统计', icon: Histogram, tone: 'rose' },
  { label: '系统设置', icon: Setting, tone: 'slate', action: 'system-settings' },
]

const handleQuickAction = (action: (typeof quickActions)[number]) => {
  if (action.action === 'system-settings') {
    // Header 持有系统设置弹框，通过全局事件打开同一个实例，避免首页重复挂载组件。
    window.dispatchEvent(new CustomEvent('open-system-settings'))
    return
  }
  if (action.route) {
    // 新增类入口携带 action=create，由目标列表页打开其已有新增表单。
    void router.push({
      path: action.route,
      query: { ...(action.action ? { action: action.action } : {}), source: 'home-shortcut' },
    })
    return
  }
  ElMessage.info(`${action.label}功能暂未开放`)
}

function createDefaultOperationItem(metricKey: HomeOperationMetricKey, label: string): HomeOperationOverviewItem {
  return {
    metricKey,
    label,
    totalCount: 0,
    onlineCount: 0,
    activeCount: 0,
    onlineRate: 0,
  }
}

function createDefaultOperationOverview(): Record<HomeOperationMetricKey, HomeOperationOverviewItem> {
  return {
    sensorOnline: createDefaultOperationItem('sensorOnline', '传感器在线'),
    realtimeMonitoring: createDefaultOperationItem('realtimeMonitoring', '实时监测'),
    pumpRunning: createDefaultOperationItem('pumpRunning', '水泵运行'),
    lightRunning: createDefaultOperationItem('lightRunning', '补光灯照明'),
    fanRunning: createDefaultOperationItem('fanRunning', '风机运行'),
  }
}

const getOperationDesc = (item: HomeOperationOverviewItem) => {
  return `在线率 ${formatOnlineRate(item.onlineRate)}，共 ${formatCount(item.totalCount)}台`
}

const operationItems = computed<OperationItem[]>(() => {
  const overview = operationOverview.value

  return [
    {
      label: overview.sensorOnline.label,
      value: formatDeviceCount(overview.sensorOnline.onlineCount),
      desc: getOperationDesc(overview.sensorOnline),
      icon: Cpu,
      tone: 'primary',
    },
    {
      label: overview.realtimeMonitoring.label,
      value: formatDeviceCount(overview.realtimeMonitoring.activeCount),
      desc: getOperationDesc(overview.realtimeMonitoring),
      icon: DataLine,
      tone: 'emerald',
    },
    {
      label: overview.pumpRunning.label,
      value: formatDeviceCount(overview.pumpRunning.activeCount),
      desc: getOperationDesc(overview.pumpRunning),
      icon: Monitor,
      tone: 'amber',
    },
    {
      label: overview.lightRunning.label,
      value: formatDeviceCount(overview.lightRunning.activeCount),
      desc: getOperationDesc(overview.lightRunning),
      icon: OfficeBuilding,
      tone: 'violet',
    },
    {
      label: overview.fanRunning.label,
      value: formatDeviceCount(overview.fanRunning.activeCount),
      desc: getOperationDesc(overview.fanRunning),
      icon: WarningFilled,
      tone: 'sky',
    },
  ]
})

const visitTabs = [
  { label: '近7天', days: 7 },
  { label: '近30天', days: 30 },
  { label: '近90天', days: 90 },
]

const closeSidebar = () => {
  sidebarVisible.value = false
}

const levelMap: Record<number, { text: string; type: 'info' | 'warning' | 'danger'; tone: string }> = {
  1: { text: '普通', type: 'info', tone: 'primary' },
  2: { text: '重要', type: 'warning', tone: 'amber' },
  3: { text: '紧急', type: 'danger', tone: 'rose' },
}

const defaultLevelMeta: { text: string; type: 'info' | 'warning' | 'danger'; tone: string } = {
  text: '普通',
  type: 'info',
  tone: 'primary',
}

const getLevelMeta = (level?: number) => levelMap[level || 1] ?? defaultLevelMeta

// 农事任务状态映射，首页只做展示，不在工作台直接修改任务状态。
const taskStatusMap: Record<number, { text: string; type: TaskStatus }> = {
  1: { text: '未开始', type: 'pending' },
  2: { text: '进行中', type: 'running' },
  3: { text: '已完成', type: 'done' },
  4: { text: '已逾期', type: 'overdue' },
  5: { text: '已取消', type: 'cancelled' },
}

const defaultTaskStatus: { text: string; type: TaskStatus } = {
  text: '未知',
  type: 'pending',
}

const getTaskStatusMeta = (status?: number) => taskStatusMap[status || 1] ?? defaultTaskStatus

const formatTaskTime = (task: FarmTask) => {
  // 首页统一展示任务截至时间，缺失时才退回发布时间。
  return task.deadlineTime || task.publishTime || task.createTime || '-'
}

// 将后端 FarmTask 转成首页表格需要的轻量展示模型。
const taskRows = computed<TaskRow[]>(() =>
  farmTasks.value.map((task) => {
    const statusMeta = getTaskStatusMeta(task.status)
    return {
      id: `TASK${String(task.id).padStart(6, '0')}`,
      name: task.taskTitle || '-',
      owner: task.executorName || task.publisherName || task.nickname || task.username || '-',
      plot: task.plotName || task.plotCode || '-',
      status: statusMeta.text,
      statusType: statusMeta.type,
      time: formatTaskTime(task),
    }
  }),
)

const formatAnnouncementDate = (value?: string) => {
  if (!value) {
    return '-'
  }
  const date = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const goSystemAnnouncements = () => {
  void router.push('/sys_msg/list')
}

const goFarmTasks = () => {
  void router.push('/farm-task/list')
}

const fetchHomeStats = async () => {
  homeStatsLoading.value = true
  try {
    const result = await getHomeStats()
    homeStats.value = result.data
  } finally {
    homeStatsLoading.value = false
  }
}

const fetchOperationOverview = async () => {
  operationOverviewLoading.value = true
  try {
    const result = await getHomeOperationOverview()
    const nextOverview = createDefaultOperationOverview()

    for (const item of result.data.items || []) {
      if (Object.prototype.hasOwnProperty.call(nextOverview, item.metricKey)) {
        nextOverview[item.metricKey] = {
          ...nextOverview[item.metricKey],
          ...item,
          totalCount: Number(item.totalCount || 0),
          onlineCount: Number(item.onlineCount || 0),
          activeCount: Number(item.activeCount || 0),
          onlineRate: Number(item.onlineRate || 0),
        }
      }
    }

    operationOverview.value = nextOverview
  } finally {
    operationOverviewLoading.value = false
  }
}

const updateWarehouseChart = () => {
  if (!warehouseChart) return
  warehouseChart.data.labels = warehouseValueCategories.value.map((item) => item.categoryName)
  const [dataset] = warehouseChart.data.datasets
  if (dataset) {
    dataset.data = warehouseValueCategories.value.map((item) => Number(item.totalValue || 0))
    dataset.backgroundColor = warehouseChartColors(warehouseValueCategories.value.length)
  }
  warehouseChart.update()
}

const fetchWarehouseValueAnalysis = async () => {
  warehouseAnalysisLoading.value = true
  try {
    const result = await getHomeWarehouseValueAnalysis()
    warehouseValueCategories.value = result.data || []
    updateWarehouseChart()
  } finally {
    warehouseAnalysisLoading.value = false
  }
}

const fetchAnnouncements = async () => {
  announcementLoading.value = true
  try {
    const result = await listSystemAnnouncements({ pageNum: 1, pageSize: 5 })
    announcements.value = result.data.list || []
  } finally {
    announcementLoading.value = false
  }
}

const fetchFarmTasks = async () => {
  farmTaskLoading.value = true
  try {
    const result = await listFarmTasks({ pageNum: 1, pageSize: 5 })
    farmTasks.value = result.data.list || []
  } finally {
    farmTaskLoading.value = false
  }
}

const openAnnouncementDetail = async (announcement: SystemAnnouncement) => {
  announcementDetailVisible.value = true
  announcementDetailLoading.value = true
  currentAnnouncement.value = null
  try {
    const result = await getSystemAnnouncementById(announcement.id)
    currentAnnouncement.value = result.data
  } finally {
    announcementDetailLoading.value = false
  }
}

const getActiveVisitDays = () => {
  return visitTabs.find((tab) => tab.label === activeVisitTab.value)?.days || 7
}

const getVisitChartMax = (values: number[]) => {
  const maxValue = Math.max(...values, 0)
  if (maxValue <= 0) {
    return 10
  }
  return Math.ceil((maxValue * 1.2) / 10) * 10
}

const updateVisitChart = () => {
  if (!visitChart) {
    return
  }

  const labels = visitTrendPoints.value.map((item) => item.label)
  const values = visitTrendPoints.value.map((item) => item.visitCount || 0)
  visitChart.data.labels = labels
  const [visitDataset] = visitChart.data.datasets
  if (visitDataset) {
    visitDataset.data = values
  }
  visitChart.options.scales = {
    ...visitChart.options.scales,
    y: {
      ...visitChart.options.scales?.y,
      beginAtZero: true,
      max: getVisitChartMax(values),
    },
  }
  visitChart.update()
}

const fetchVisitTrend = async () => {
  visitTrendLoading.value = true
  try {
    const result = await getHomeVisitTrend(getActiveVisitDays())
    visitTrendPoints.value = result.data.points || []
    updateVisitChart()
  } finally {
    visitTrendLoading.value = false
  }
}

const handleVisitTabClick = (tab: string) => {
  if (activeVisitTab.value === tab) {
    return
  }
  activeVisitTab.value = tab
  void fetchVisitTrend()
}

const createVisitChart = () => {
  const canvas = visitChartRef.value

  if (!canvas) {
    return
  }

  const ctx = canvas.getContext('2d')

  if (!ctx) {
    return
  }

  const visitGradient = ctx.createLinearGradient(0, 0, 0, 200)
  visitGradient.addColorStop(0, 'rgba(59, 130, 246, 0.15)')
  visitGradient.addColorStop(1, 'rgba(59, 130, 246, 0)')

  const config: ChartConfiguration<'line'> = {
    type: 'line',
    data: {
      labels: visitTrendPoints.value.map((item) => item.label),
      datasets: [
        {
          label: '访问量（次）',
          data: visitTrendPoints.value.map((item) => item.visitCount || 0),
          borderColor: '#3b82f6',
          backgroundColor: visitGradient,
          borderWidth: 2.5,
          pointRadius: 4,
          pointBackgroundColor: '#3b82f6',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointHoverRadius: 6,
          fill: true,
          tension: 0.4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: {
            font: { size: 11, family: 'DM Sans' },
            color: '#94a3b8',
          },
        },
        y: {
          beginAtZero: true,
          ticks: {
            font: { size: 11, family: 'DM Sans' },
            color: '#94a3b8',
            callback: (value) => Number(value).toLocaleString(),
          },
          grid: { color: '#f1f5f9' },
          border: { display: false },
        },
      },
    },
  }

  visitChart = new Chart(ctx, config)
  updateVisitChart()
}

const centerTextPlugin: Plugin<'doughnut'> = {
  id: 'centerText',
  beforeDraw: (chart) => {
    const ctx = chart.ctx
    const centerX = chart.chartArea.left + (chart.chartArea.right - chart.chartArea.left) / 2
    const centerY = chart.chartArea.top + (chart.chartArea.bottom - chart.chartArea.top) / 2

    ctx.save()
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.font = 'bold 22px DM Sans'
    ctx.fillStyle = '#1e293b'
    ctx.fillText(formatCompactCurrency(warehouseTotalValue.value), centerX, centerY - 8)
    ctx.font = '12px DM Sans'
    ctx.fillStyle = '#94a3b8'
    ctx.fillText('库存总价', centerX, centerY + 14)
    ctx.restore()
  },
}

const createWarehouseChart = () => {
  const canvas = warehouseChartRef.value

  if (!canvas) {
    return
  }

  const ctx = canvas.getContext('2d')

  if (!ctx) {
    return
  }

  const config: ChartConfiguration<'doughnut'> = {
    type: 'doughnut',
    data: {
      labels: warehouseValueCategories.value.map((item) => item.categoryName),
      datasets: [
        {
          data: warehouseValueCategories.value.map((item) => Number(item.totalValue || 0)),
          backgroundColor: warehouseChartColors(warehouseValueCategories.value.length),
          borderWidth: 0,
          hoverOffset: 4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '65%',
      plugins: {
        legend: {
          position: 'right',
          labels: {
            boxWidth: 10,
            boxHeight: 10,
            borderRadius: 5,
            padding: 16,
            font: { size: 12, family: 'DM Sans' },
            color: '#475569',
            usePointStyle: true,
            pointStyle: 'circle',
          },
        },
        tooltip: {
          callbacks: {
            label: (context) => {
              const value = Number(context.raw || 0)
              const percentage = warehouseTotalValue.value > 0
                ? (value * 100 / warehouseTotalValue.value).toFixed(1)
                : '0.0'
              return `${context.label}：${formatCurrency(value)}（${percentage}%）`
            },
          },
        },
      },
    },
    plugins: [centerTextPlugin],
  }

  warehouseChart = new Chart(ctx, config)
}

onMounted(() => {
  void fetchHomeStats()
  void fetchOperationOverview()
  void fetchWarehouseValueAnalysis()
  void fetchAnnouncements()
  void fetchFarmTasks()
  // 先写入本次首页访问，再读取当前账号的趋势，避免首次加载漏计本次访问。
  void recordHomeVisit().then(fetchVisitTrend, fetchVisitTrend)
  void nextTick(() => {
    createVisitChart()
    createWarehouseChart()
  })
})

onBeforeUnmount(() => {
  visitChart?.destroy()
  warehouseChart?.destroy()
})
</script>

<template>
  <div class="home-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="home-shell">
      <Header @toggle-sidebar="sidebarVisible = true" />

      <main class="dashboard-content">
        <section v-loading="homeStatsLoading" class="metric-grid" aria-label="核心指标" :aria-busy="homeStatsLoading">
          <article
            v-for="(card, index) in metricCards"
            :key="card.label"
            class="metric-card glass-card animate-in"
            :style="{ animationDelay: `${index * 0.05}s` }"
          >
            <div class="metric-inner">
              <div class="metric-icon" :class="`tone-${card.tone}`">
                <el-icon><component :is="card.icon" /></el-icon>
              </div>
              <div>
                <p class="metric-label">{{ card.label }}</p>
                <p class="metric-value">
                  {{ card.value }}<span v-if="card.unit">{{ card.unit }}</span>
                </p>
                <div class="metric-trend">
                  <span :class="card.trendDirection === 'up' ? 'trend-up' : 'trend-down'">
                    {{ card.trendDirection === 'up' ? '↑' : '↓' }}{{ card.trend }}
                  </span>
                  <small>较昨日</small>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section class="charts-grid">
          <article class="glass-card chart-card visit-card animate-in">
            <div class="section-head">
              <h3>访问量趋势</h3>
              <div class="tab-group">
                <button
                  v-for="tab in visitTabs"
                  :key="tab.label"
                  class="tab-btn"
                  :class="{ active: activeVisitTab === tab.label }"
                  type="button"
                  @click="handleVisitTabClick(tab.label)"
                >
                  {{ tab.label }}
                </button>
              </div>
            </div>
            <div v-loading="visitTrendLoading" class="chart-container">
              <canvas ref="visitChartRef" />
            </div>
          </article>

          <article class="glass-card chart-card animate-in">
            <h3 class="single-title">仓库物资分析</h3>
            <div
              v-loading="warehouseAnalysisLoading"
              class="chart-container crop-container"
              :aria-busy="warehouseAnalysisLoading"
            >
              <el-empty
                v-if="!warehouseAnalysisLoading && !warehouseValueCategories.length"
                description="暂无可统计的物资价值数据"
                :image-size="64"
              />
              <canvas
                v-show="warehouseValueCategories.length"
                ref="warehouseChartRef"
                role="img"
                aria-label="仓库物资各分类库存总价占比图"
              />
            </div>
          </article>
        </section>

        <section class="bottom-grid">
          <article class="glass-card task-card animate-in">
            <div class="section-head">
              <h3>农事任务</h3>
              <button class="section-link" type="button" @click="goFarmTasks">查看全部</button>
            </div>
            <div v-loading="farmTaskLoading" class="table-wrap">
              <el-empty v-if="!farmTaskLoading && !taskRows.length" description="暂无农事任务" :image-size="72" />
              <table v-else>
                <thead>
                  <tr>
                    <th>任务编号</th>
                    <th>任务名称</th>
                    <th>执行人</th>
                    <th>地块</th>
                    <th>状态</th>
                    <th>截至时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="task in taskRows" :key="task.id" class="task-row">
                    <td class="mono">{{ task.id }}</td>
                    <td class="task-name">{{ task.name }}</td>
                    <td>{{ task.owner }}</td>
                    <td>{{ task.plot }}</td>
                    <td>
                      <span class="status-badge" :class="`status-${task.statusType}`">
                        {{ task.status }}
                      </span>
                    </td>
                    <td class="mono">{{ task.time }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </article>

          <article class="glass-card side-card animate-in">
            <h3 class="single-title">快捷操作</h3>
            <div class="shortcut-grid">
              <button v-for="action in quickActions" :key="action.label" class="shortcut-btn" type="button" @click="handleQuickAction(action)">
                <div :class="`tone-${action.tone}`">
                  <el-icon><component :is="action.icon" /></el-icon>
                </div>
                <span>{{ action.label }}</span>
              </button>
            </div>
          </article>

          <article class="glass-card side-card animate-in">
            <div class="section-head">
              <h3>系统公告</h3>
              <button class="section-link" type="button" @click="goSystemAnnouncements">查看全部</button>
            </div>
            <div v-loading="announcementLoading" class="announcement-list">
              <el-empty v-if="!announcementLoading && !announcements.length" description="暂无系统公告" :image-size="72" />
              <button
                v-for="item in announcements"
                v-else
                :key="item.id"
                class="announcement-item"
                type="button"
                @click="openAnnouncementDetail(item)"
              >
                <span class="dot" :class="`tone-${getLevelMeta(item.level).tone}`" />
                <p>{{ item.title }}</p>
                <time>{{ formatAnnouncementDate(item.sendTime || item.createTime) }}</time>
              </button>
            </div>
          </article>
        </section>

        <section class="operation-strip glass-card animate-in" aria-label="运行概览">
          <div class="section-head strip-head">
            <h3>运行概览</h3>
            <a href="#">查看监测中心</a>
          </div>
          <div v-loading="operationOverviewLoading" class="operation-grid">
            <article v-for="item in operationItems" :key="item.label" class="operation-item">
              <div class="operation-icon" :class="`tone-${item.tone}`">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
              <div>
                <p>{{ item.label }}</p>
                <strong>{{ item.value }}</strong>
                <span>{{ item.desc }}</span>
              </div>
            </article>
          </div>
        </section>
      </main>
    </div>

    <el-dialog
      v-model="announcementDetailVisible"
      title="系统公告详情"
      width="640px"
      class="announcement-detail-dialog"
      destroy-on-close
    >
      <div v-loading="announcementDetailLoading" class="announcement-detail">
        <template v-if="currentAnnouncement">
          <div class="announcement-detail-head">
            <h2>{{ currentAnnouncement.title }}</h2>
            <el-tag :type="getLevelMeta(currentAnnouncement.level).type">
              {{ getLevelMeta(currentAnnouncement.level).text }}
            </el-tag>
          </div>
          <div class="announcement-meta">
            <span>发布人：{{ currentAnnouncement.publisherName || '-' }}</span>
            <span>发布时间：{{ currentAnnouncement.sendTime || currentAnnouncement.createTime || '-' }}</span>
            <span>
              已读：{{ Number(currentAnnouncement.readCount || 0).toLocaleString() }} /
              {{ Number(currentAnnouncement.recipientCount || 0).toLocaleString() }}
            </span>
          </div>
          <p class="announcement-content">{{ currentAnnouncement.content || '-' }}</p>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped src="../../common/styles/pages/home.css"></style>
