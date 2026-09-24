<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  Clock,
  Refresh,
  Search,
  UserFilled,
} from '@element-plus/icons-vue'
import { listAdminLoginLogs, type AdminLoginLog } from '@/api/loginLog'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface LoginLogRow {
  id: number
  username: string
  ip: string
  userAgent: string
  loginTime: string
  result: 'success' | 'failure'
  resultText: string
  failureReason: string
}

interface LoginLogStats {
  total: number
  today: number
  success: number
  failure: number
  totalTrend: number
  todayTrend: number
  successTrend: number
  failureTrend: number
}

const sidebarVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const logs = ref<LoginLogRow[]>([])
const loginLogStats = ref<LoginLogStats>({
  total: 0,
  today: 0,
  success: 0,
  failure: 0,
  totalTrend: 0,
  todayTrend: 0,
  successTrend: 0,
  failureTrend: 0,
})

const searchForm = ref<{
  username: string
  ip: string
  result: '' | 'success' | 'failure'
  timeRange: string[]
}>({
  username: '',
  ip: '',
  result: '',
  timeRange: [],
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '日志总数',
    value: loginLogStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(loginLogStats.value.totalTrend),
    trendType: loginLogStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: UserFilled,
    tone: 'blue',
  },
  {
    title: '今日登录',
    value: loginLogStats.value.today.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(loginLogStats.value.todayTrend),
    trendType: loginLogStats.value.todayTrend >= 0 ? 'up' : 'down',
    icon: Clock,
    tone: 'green',
  },
  {
    title: '成功登录',
    value: loginLogStats.value.success.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(loginLogStats.value.successTrend),
    trendType: loginLogStats.value.successTrend >= 0 ? 'up' : 'down',
    icon: CircleCheckFilled,
    tone: 'orange',
  },
  {
    title: '失败登录',
    value: loginLogStats.value.failure.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(loginLogStats.value.failureTrend),
    trendType: loginLogStats.value.failureTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => {
  return `${value >= 0 ? '↑' : '↓'} ${Math.abs(value)}`
}

const pad = (value: number) => String(value).padStart(2, '0')

const formatDateTime = (date: Date) => {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const getDayRange = (offset = 0) => {
  const start = new Date()
  start.setDate(start.getDate() + offset)
  start.setHours(0, 0, 0, 0)

  const end = new Date(start)
  end.setHours(23, 59, 59, 999)

  return {
    startTime: formatDateTime(start),
    endTime: formatDateTime(end),
  }
}

const getSearchSuccess = () => {
  if (searchForm.value.result === 'success') {
    return 1
  }

  if (searchForm.value.result === 'failure') {
    return 0
  }

  return undefined
}

const getSearchTimeRange = () => {
  const [startTime, endTime] = searchForm.value.timeRange

  return {
    startTime: startTime || undefined,
    endTime: endTime || undefined,
  }
}

const mapLogToRow = (log: AdminLoginLog): LoginLogRow => {
  const success = log.success === 1

  return {
    id: log.id,
    username: log.username || '-',
    ip: log.ip || '-',
    userAgent: log.userAgent || '-',
    loginTime: log.loginTime || '-',
    result: success ? 'success' : 'failure',
    resultText: success ? '成功' : '失败',
    failureReason: success ? '-' : log.failureReason || '未知原因',
  }
}

const fetchLoginLogStats = async () => {
  const today = getDayRange()
  const yesterday = getDayRange(-1)

  const [allResult, successResult, failureResult, todayResult, yesterdayResult, todaySuccessResult, yesterdaySuccessResult, todayFailureResult, yesterdayFailureResult] =
    await Promise.all([
      listAdminLoginLogs({ pageNum: 1, pageSize: 1 }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 1 }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 0 }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, ...today }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, ...yesterday }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 1, ...today }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 1, ...yesterday }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 0, ...today }),
      listAdminLoginLogs({ pageNum: 1, pageSize: 1, success: 0, ...yesterday }),
    ])

  loginLogStats.value = {
    total: allResult.data.total,
    today: todayResult.data.total,
    success: successResult.data.total,
    failure: failureResult.data.total,
    totalTrend: todayResult.data.total - yesterdayResult.data.total,
    todayTrend: todayResult.data.total - yesterdayResult.data.total,
    successTrend: todaySuccessResult.data.total - yesterdaySuccessResult.data.total,
    failureTrend: todayFailureResult.data.total - yesterdayFailureResult.data.total,
  }
}

const fetchLoginLogs = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const timeRange = getSearchTimeRange()
    const result = await listAdminLoginLogs({
      username: searchForm.value.username || undefined,
      ip: searchForm.value.ip || undefined,
      success: getSearchSuccess(),
      startTime: timeRange.startTime,
      endTime: timeRange.endTime,
      pageNum,
      pageSize: size,
    })

    logs.value = result.data.list.map(mapLogToRow)
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.value = {
    username: '',
    ip: '',
    result: '',
    timeRange: [],
  }
  void fetchLoginLogs(1)
}

const handleCurrentPageChange = (page: number) => {
  void fetchLoginLogs(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchLoginLogs(1, size)
}

onMounted(() => {
  void fetchLoginLogs()
  void fetchLoginLogStats()
})
</script>

<template>
  <div class="login-log-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="login-log-shell admin-shell">
      <Header :breadcrumbs="['首页', '日志管理', '登录日志']" @toggle-sidebar="sidebarVisible = true" />

      <main class="login-log-content admin-content">
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
            <el-form-item label="用户名">
              <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="登录IP">
              <el-input v-model="searchForm.ip" placeholder="请输入登录IP" clearable />
            </el-form-item>
            <el-form-item label="结果">
              <el-select v-model="searchForm.result" placeholder="全部结果" clearable>
                <el-option label="成功" value="success" />
                <el-option label="失败" value="failure" />
              </el-select>
            </el-form-item>
            <el-form-item label="时间">
              <el-date-picker
                v-model="searchForm.timeRange"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                range-separator="至"
                clearable
              />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchLoginLogs(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="logs"
              row-key="id"
              class="login-log-table"
              height="100%"
            >
              <el-table-column prop="username" label="用户名" min-width="150" align="center" header-align="center" />
              <el-table-column prop="ip" label="登录IP" min-width="150" align="center" header-align="center" />
              <el-table-column label="登录结果" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="row.result === 'success' ? 'result-success' : 'result-failure'" effect="light" round>
                    {{ row.resultText }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="failureReason" label="失败原因" min-width="150" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="loginTime" label="登录时间" min-width="180" align="center" header-align="center" />
              <el-table-column prop="userAgent" label="登录设备" min-width="320" align="center" header-align="center" show-overflow-tooltip />
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

.tone-gray {
  background: linear-gradient(135deg, #9aa3b2, #64748b);
}

.filter-card {
  flex: 0 0 auto;
  margin-top: 10px;
  padding: 14px 18px;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(220px, 1fr)) auto;
  align-items: center;
  gap: 14px 22px;
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

.filter-form :deep(.el-date-editor) {
  width: 100%;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
}

.filter-actions :deep(.el-button) {
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

.login-log-table {
  width: 100%;
}

.login-log-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.login-log-table :deep(.el-table__row) {
  height: 64px;
}

.login-log-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.result-success {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.result-failure {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fee2e2;
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
  }
}

@media (max-width: 960px) {
  .login-log-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .login-log-content {
    padding: 12px;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
