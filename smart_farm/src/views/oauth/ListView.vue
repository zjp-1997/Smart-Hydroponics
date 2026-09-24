<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  Refresh,
  Search,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteOauthAccounts,
  deleteOauthAccount,
  listOauthAccounts,
  updateOauthAccountStatus,
  type OauthAccount,
} from '@/api/oauthAccount'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

type AccountStatus = 'enabled' | 'disabled'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface AccountRow {
  id: number
  openId: string
  nickname: string
  avatar: string
  avatarUrl?: string
  bindTime: string
  status: AccountStatus
}

interface AccountStats {
  total: number
  newUsers: number
  enabled: number
  disabled: number
  totalTrend: number
  newTrend: number
  enabledTrend: number
  disabledTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<AccountRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const editAccountId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])

const searchForm = ref({
  nickname: '',
  provider: undefined as number | undefined,
  status: '',
})

const accountStats = ref<AccountStats>({
  total: 0,
  newUsers: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  newTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const providerLabelMap: Record<number, string> = {
  1: '微信',
  2: 'QQ',
  3: '支付宝',
  4: '苹果',
}

const providerOptions = [
  { label: '微信', value: 1 },
  { label: 'QQ', value: 2 },
  { label: '支付宝', value: 3 },
  { label: '苹果', value: 4 },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '用户总数',
    value: accountStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(accountStats.value.totalTrend),
    trendType: accountStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: UserFilled,
    tone: 'blue',
  },
  {
    title: '新增用户',
    value: accountStats.value.newUsers.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(accountStats.value.newTrend),
    trendType: accountStats.value.newTrend >= 0 ? 'up' : 'down',
    icon: CirclePlus,
    tone: 'green',
  },
  {
    title: '启用用户',
    value: accountStats.value.enabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(accountStats.value.enabledTrend),
    trendType: accountStats.value.enabledTrend >= 0 ? 'up' : 'down',
    icon: User,
    tone: 'orange',
  },
  {
    title: '禁用用户',
    value: accountStats.value.disabled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(accountStats.value.disabledTrend),
    trendType: accountStats.value.disabledTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const rows = ref<AccountRow[]>([])
const OAUTH_AVATAR_STORAGE_PREFIX = 'smart_farm_oauth_avatar:'

const isImageUrl = (value?: string) => {
  return Boolean(
    value &&
      value !== '/default-avatar.svg' &&
      (/^(https?:|blob:|data:image\/)/.test(value) || value.startsWith('/')),
  )
}

const getStoredAvatarUrl = (value?: string) => {
  if (!value?.startsWith(OAUTH_AVATAR_STORAGE_PREFIX)) {
    return undefined
  }

  return localStorage.getItem(value) || undefined
}

const resolveAvatarUrl = (value?: string) => {
  if (isImageUrl(value)) {
    return value?.startsWith('/') ? getFileUrl(value) : value
  }

  return getStoredAvatarUrl(value)
}

const formatTrend = (value: number) => {
  return `${value >= 0 ? '↑' : '↓'} ${Math.abs(value)}`
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

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return '-'
  }

  return value.slice(0, 19)
}

const getAvatarText = (account: OauthAccount) => {
  return account.nickname?.slice(0, 1) || providerLabelMap[account.provider || 0]?.slice(0, 1) || '账'
}

const mapAccountToRow = (account: OauthAccount): AccountRow => {
  return {
    id: account.id ?? Date.now(),
    openId: account.openId || '-',
    nickname: account.nickname || '-',
    avatar: getAvatarText(account),
    avatarUrl: resolveAvatarUrl(account.avatar),
    bindTime: formatDateTime(account.bindTime),
    status: account.status === 0 ? 'disabled' : 'enabled',
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

const fetchStats = async () => {
  const [allResult, enabledResult, disabledResult] = await Promise.all([
    listOauthAccounts({ pageNum: 1, pageSize: 10000 }),
    listOauthAccounts({ pageNum: 1, pageSize: 1, status: 1 }),
    listOauthAccounts({ pageNum: 1, pageSize: 1, status: 0 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allAccounts = allResult.data.list
  const getAccountStatTime = (account: OauthAccount) => account.createTime || account.bindTime
  const todayAccounts = allAccounts.filter((account) => isSameDate(getAccountStatTime(account), today))
  const yesterdayAccounts = allAccounts.filter((account) => isSameDate(getAccountStatTime(account), yesterday))
  const todayEnabledAccounts = todayAccounts.filter((account) => account.status !== 0)
  const yesterdayEnabledAccounts = yesterdayAccounts.filter((account) => account.status !== 0)
  const todayDisabledAccounts = todayAccounts.filter((account) => account.status === 0)
  const yesterdayDisabledAccounts = yesterdayAccounts.filter((account) => account.status === 0)

  accountStats.value = {
    total: allResult.data.total,
    newUsers: todayAccounts.length,
    enabled: enabledResult.data.total,
    disabled: disabledResult.data.total,
    totalTrend: todayAccounts.length - yesterdayAccounts.length,
    newTrend: todayAccounts.length - yesterdayAccounts.length,
    enabledTrend: todayEnabledAccounts.length - yesterdayEnabledAccounts.length,
    disabledTrend: todayDisabledAccounts.length - yesterdayDisabledAccounts.length,
  }
}

const fetchAccounts = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listOauthAccounts({
      nickname: searchForm.value.nickname || undefined,
      provider: searchForm.value.provider,
      status: getSearchStatus(),
      pageNum,
      pageSize: size,
    })

    rows.value = result.data.list.map((account) => mapAccountToRow(account))
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
    nickname: '',
    provider: undefined,
    status: '',
  }
  void fetchAccounts(1)
}

const handleSelectionChange = (items: AccountRow[]) => {
  selectedRows.value = items
}

const handleEdit = (row: AccountRow) => {
  editAccountId.value = row.id
  dialogVisible.value = true
}

const handleSuccess = () => {
  void fetchAccounts(currentPage.value)
  void fetchStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchAccounts(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchAccounts(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1
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

const handleToggleStatus = async (row: AccountRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateOauthAccountStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用第三方账号成功' : '禁用第三方账号成功')
    void fetchAccounts(currentPage.value)
    void fetchStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleDelete = async (row: AccountRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除第三方账号「${row.openId}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteOauthAccount(row.id)
    ElMessage.success('删除成功')
    void fetchAccounts(getNextPageAfterDelete(1))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的第三方账号')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 个第三方账号吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteOauthAccounts(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchAccounts(getNextPageAfterDelete(selectedRows.value.length))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void fetchAccounts()
  void fetchStats()
})
</script>

<template>
  <div class="oauth-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="oauth-shell admin-shell">
      <Header :breadcrumbs="['首页', '用户管理', '第三方账号管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="oauth-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="68px">
            <el-form-item label="昵称">
              <el-input v-model="searchForm.nickname" placeholder="请输入第三方昵称" clearable />
            </el-form-item>
            <el-form-item label="平台">
              <el-select v-model="searchForm.provider" placeholder="全部平台" clearable>
                <el-option
                  v-for="item in providerOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
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
              <el-button type="primary" :icon="Search" @click="fetchAccounts(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
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
              class="oauth-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="头像" width="100" align="center" header-align="center">
                <template #default="{ row }">
                  <el-avatar v-if="row.avatarUrl" class="table-avatar" :size="34" :src="row.avatarUrl" fit="contain" />
                  <span v-else class="default-avatar">{{ row.avatar }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="第三方昵称" min-width="180" align="center" header-align="center" />
              <el-table-column prop="bindTime" label="绑定时间" min-width="190" align="center" header-align="center" />
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
                    <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editAccountId" @success="handleSuccess" />
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
  overflow: hidden;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(286px, 300px) minmax(220px, 235px) minmax(205px, 220px) minmax(330px, 1fr);
  align-items: center;
  gap: 14px 16px;
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
.filter-form :deep(.el-input-number) {
  width: 100%;
  height: 40px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  min-height: 40px;
  min-width: 0;
  white-space: nowrap;
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

.oauth-table {
  width: 100%;
}

.oauth-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.oauth-table :deep(.el-table__row) {
  height: 64px;
}

.oauth-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.table-avatar :deep(img) {
  object-fit: contain;
  background: #ffffff;
}

.default-avatar {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #ffffff;
  background: #409eff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  text-transform: none;
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.22);
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
  gap: 4px;
  white-space: nowrap;
}

.provider-wechat {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.provider-qq {
  color: #2563eb;
  background: #eff6ff;
  border-color: #dbeafe;
}

.provider-alipay {
  color: #0891b2;
  background: #ecfeff;
  border-color: #cffafe;
}

.provider-apple {
  color: #334155;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.provider-unknown {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ede9fe;
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
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }

  .filter-actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 960px) {
  .oauth-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .oauth-content {
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

  .filter-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
