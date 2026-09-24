<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCloseFilled,
  CirclePlus,
  Delete,
  DocumentChecked,
  EditPen,
  Refresh,
  Search,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteSmartPlantUsers,
  deleteSmartPlantUser,
  getPendingFarmJoinRequestCount,
  listSmartPlantUsers,
  resetSmartPlantUserPassword,
  updateSmartPlantUserStatus,
  type SmartPlantUser,
} from '@/api/user'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'
import FarmJoinRequests from './FarmJoinRequests.vue'

type UserStatus = 'enabled' | 'disabled'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface UserRow {
  id: number
  avatar: string
  avatarUrl?: string
  username: string
  nickname: string
  gender: string
  phone: string
  role: string
  roleType: string
  status: UserStatus
}

interface UserStats {
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
const selectedRows = ref<UserRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const joinDialogVisible = ref(false)
// null 表示尚未取得有效统计，不能把加载或请求失败误显示成“0条待审核”。
const pendingJoinCount = ref<number | null>(null)
const joinCountLoading = ref(false)
let joinCountVersion = 0
const joinCountLabel = computed(() => joinCountLoading.value
  ? '入场申请，正在更新待审核数量'
  : pendingJoinCount.value === null
    ? '入场申请，数量加载失败，点击查看并重试'
    : `入场申请，${pendingJoinCount.value}条待审核`)

/** 独立获取待审核总数；仅采用最新请求结果，避免审批前的慢响应覆盖新数量。 */
const fetchPendingJoinCount = async () => {
  const version = ++joinCountVersion
  joinCountLoading.value = true
  try {
    const result = await getPendingFarmJoinRequestCount()
    if (version === joinCountVersion) pendingJoinCount.value = result.data
  } catch {
    // 错误提示由请求拦截器处理；保留申请入口供用户打开弹窗重新获取。
    if (version === joinCountVersion) pendingJoinCount.value = null
  } finally {
    if (version === joinCountVersion) joinCountLoading.value = false
  }
}

const addDialogVisible = ref(false)
const editUserId = ref<number | null>(null)
const statusLoadingIds = ref<number[]>([])
const userStats = ref<UserStats>({
  total: 0,
  newUsers: 0,
  enabled: 0,
  disabled: 0,
  totalTrend: 0,
  newTrend: 0,
  enabledTrend: 0,
  disabledTrend: 0,
})

const searchForm = ref({
  username: '',
  phone: '',
  status: '',
})

const roleLabelMap: Record<string, string> = {
  admin: '管理员',
  farm_owner: '农场主',
  technician: '技术人员',
  expert: '专家',
  user: '普通用户',
}

const roleTypeMap: Record<string, string> = {
  admin: 'blue',
  farm_owner: 'green',
  technician: 'orange',
  expert: 'purple',
  user: 'cyan',
}

const genderLabelMap: Record<number, string> = {
  0: '未知',
  1: '男',
  2: '女',
}

const statCards = computed<StatCard[]>(() => {
  return [
    {
      title: '用户总数',
      value: userStats.value.total.toLocaleString(),
      desc: '较昨日',
      trend: formatTrend(userStats.value.totalTrend),
      trendType: userStats.value.totalTrend >= 0 ? 'up' : 'down',
      icon: UserFilled,
      tone: 'blue',
    },
    {
      title: '新增用户',
      value: userStats.value.newUsers.toLocaleString(),
      desc: '较昨日',
      trend: formatTrend(userStats.value.newTrend),
      trendType: userStats.value.newTrend >= 0 ? 'up' : 'down',
      icon: CirclePlus,
      tone: 'green',
    },
    {
      title: '启用用户',
      value: userStats.value.enabled.toLocaleString(),
      desc: '较昨日',
      trend: formatTrend(userStats.value.enabledTrend),
      trendType: userStats.value.enabledTrend >= 0 ? 'up' : 'down',
      icon: User,
      tone: 'orange',
    },
    {
      title: '禁用用户',
      value: userStats.value.disabled.toLocaleString(),
      desc: '较昨日',
      trend: formatTrend(userStats.value.disabledTrend),
      trendType: userStats.value.disabledTrend >= 0 ? 'up' : 'down',
      icon: CircleCloseFilled,
      tone: 'gray',
    },
  ]
})

const users = ref<UserRow[]>([])
const USER_AVATAR_STORAGE_PREFIX = 'smart_farm_user_avatar:'

const isAvatarImageUrl = (value?: string) => {
  return Boolean(
    value &&
      value !== '/default-avatar.svg' &&
      (/^(https?:|blob:|data:image\/)/.test(value) || value.startsWith('/')),
  )
}

const getStoredAvatarUrl = (value?: string) => {
  if (!value?.startsWith(USER_AVATAR_STORAGE_PREFIX)) {
    return undefined
  }

  return localStorage.getItem(value) || undefined
}

const resolveAvatarUrl = (value?: string) => {
  if (isAvatarImageUrl(value)) {
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

const getUserAvatarText = (user: SmartPlantUser) => {
  return user.nickname?.slice(0, 1) || user.username?.slice(0, 1).toUpperCase() || '用'
}

/**
 * 将 smart_plant 后端 User 实体转换为表格展示结构。
 * 这样页面 UI 字段不直接依赖后端字段名，后续调整列展示时更集中。
 */
const mapSmartPlantUserToRow = (user: SmartPlantUser, avatarPreviewUrl?: string): UserRow => {
  const roleCode = user.roleCode || 'user'
  const avatarValue = user.avatar || ''

  return {
    id: user.id ?? Date.now(),
    avatar: getUserAvatarText(user),
    avatarUrl: avatarPreviewUrl || resolveAvatarUrl(avatarValue),
    username: user.username,
    nickname: user.nickname || '-',
    gender: user.gender === undefined ? '-' : (genderLabelMap[user.gender] ?? '-'),
    phone: user.phone || '-',
    role: user.roleName || roleLabelMap[roleCode] || roleCode,
    roleType: roleTypeMap[roleCode] ?? 'cyan',
    status: user.status === 0 ? 'disabled' : 'enabled',
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

const fetchUserStats = async () => {
  const [allResult, enabledResult, disabledResult] = await Promise.all([
    listSmartPlantUsers({ pageNum: 1, pageSize: 10000 }),
    listSmartPlantUsers({ pageNum: 1, pageSize: 1, status: 1 }),
    listSmartPlantUsers({ pageNum: 1, pageSize: 1, status: 0 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allUsers = allResult.data.list
  const todayUsers = allUsers.filter((user) => isSameDate(user.createTime, today))
  const yesterdayUsers = allUsers.filter((user) => isSameDate(user.createTime, yesterday))
  const todayEnabledUsers = todayUsers.filter((user) => user.status !== 0)
  const yesterdayEnabledUsers = yesterdayUsers.filter((user) => user.status !== 0)
  const todayDisabledUsers = todayUsers.filter((user) => user.status === 0)
  const yesterdayDisabledUsers = yesterdayUsers.filter((user) => user.status === 0)

  userStats.value = {
    total: allResult.data.total,
    newUsers: todayUsers.length,
    enabled: enabledResult.data.total,
    disabled: disabledResult.data.total,
    totalTrend: todayUsers.length - yesterdayUsers.length,
    newTrend: todayUsers.length - yesterdayUsers.length,
    enabledTrend: todayEnabledUsers.length - yesterdayEnabledUsers.length,
    disabledTrend: todayDisabledUsers.length - yesterdayDisabledUsers.length,
  }
}

/**
 * 调用 smart_plant 项目的用户分页接口。
 * 后端返回 PageInfo，list 用于表格，total/pageNum/pageSize 用于分页器。
 */
const fetchUsers = async (pageNum = currentPage.value, size = pageSize.value) => {
  // 进入页面、查询和增删改后同步数量，统计不使用用户表格的筛选或分页条件。
  void fetchPendingJoinCount()
  tableLoading.value = true

  try {
    const result = await listSmartPlantUsers({
      username: searchForm.value.username || undefined,
      phone: searchForm.value.phone || undefined,
      status: getSearchStatus(),
      pageNum,
      pageSize: size,
    })

    users.value = result.data.list.map((user) => mapSmartPlantUserToRow(user))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

/**
 * 条件查询统一入口。
 * 用户在筛选表单内按回车或点击查询按钮时，都从第一页重新加载用户列表。
 */
const handleSearch = () => {
  void fetchUsers(1)
}

/**
 * 状态下拉框有自己的 Enter 键展开逻辑。
 * 在捕获阶段拦截回车，先阻止下拉框弹出，再复用统一查询入口。
 */
const handleStatusEnter = (event: KeyboardEvent) => {
  event.preventDefault()
  event.stopPropagation()
  ;(event.target as HTMLElement | null)?.blur()
  handleSearch()
}

const resetSearch = () => {
  searchForm.value = {
    username: '',
    phone: '',
    status: '',
  }
  void fetchUsers(1)
}

const handleSelectionChange = (rows: UserRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editUserId.value = null
  addDialogVisible.value = true
}

const handleAddSuccess = () => {
  void fetchUsers(editUserId.value ? currentPage.value : 1)
  void fetchUserStats()
}

const handleEditUser = (row: UserRow) => {
  editUserId.value = row.id
  addDialogVisible.value = true
}

const handleCurrentPageChange = (page: number) => {
  void fetchUsers(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchUsers(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return users.value.length <= deletedCount && currentPage.value > 1
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

const handleToggleUserStatus = async (row: UserRow, enabled?: boolean | string | number) => {
  const nextEnabled = typeof enabled === 'boolean' ? enabled : row.status !== 'enabled'
  const nextStatus = nextEnabled ? 1 : 0

  setStatusUpdating(row.id, true)

  try {
    await updateSmartPlantUserStatus(row.id, nextStatus)
    ElMessage.success(nextEnabled ? '启用用户成功' : '禁用用户成功')
    void fetchUsers(currentPage.value)
    void fetchUserStats()
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的用户')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 个用户吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    const selectedIds = selectedRows.value.map((row) => row.id)
    await batchDeleteSmartPlantUsers(selectedIds)
    ElMessage.success('批量删除成功')
    void fetchUsers(getNextPageAfterDelete(selectedRows.value.length))
    void fetchUserStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteUser = async (row: UserRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteSmartPlantUser(row.id)
    ElMessage.success('删除成功')
    void fetchUsers(getNextPageAfterDelete(1))
    void fetchUserStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleResetPassword = async (row: UserRow) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入用户「${row.username}」的新登录密码，保存后后端会自动加密`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入 8-32 位新密码',
        inputPattern: /^(?=.*[A-Za-z])(?=.*\d).{8,32}$/,
        inputErrorMessage: '密码需为 8-32 位，且同时包含字母和数字',
      },
    )

    await resetSmartPlantUserPassword(row.id, value.trim())
    ElMessage.success('重置密码成功，用户可使用新密码登录')
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消重置密码')
    }
  }
}

onMounted(() => {
  void fetchUsers()
  void fetchUserStats()
})
</script>

<template>
  <div class="user-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="user-shell admin-shell">
      <Header :breadcrumbs="['首页', '用户管理', '用户管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="user-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="76px" @submit.prevent="handleSearch">
            <el-form-item label="用户名">
              <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="searchForm.status"
                placeholder="全部状态"
                clearable
                @keydown.enter.capture="handleStatusEnter"
              >
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <!-- 内嵌徽标避免遮挡相邻按钮；超过99显示99+，完整数量通过文字提示提供。 -->
              <el-button
                class="join-request-button"
                :type="pendingJoinCount ? 'primary' : 'default'"
                plain
                :icon="DocumentChecked"
                :aria-label="joinCountLabel"
                :title="joinCountLabel"
                @click="joinDialogVisible = true"
              >
                入场申请
                <el-badge
                  class="join-request-count"
                  :value="joinCountLoading ? '…' : (pendingJoinCount ?? '—')"
                  :max="99"
                  :type="pendingJoinCount ? 'danger' : 'info'"
                  aria-hidden="true"
                />
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增用户</el-button>
              <!-- 数量异步变化时播报完整语义，不改变键盘焦点。 -->
              <span class="join-count-status" role="status" aria-atomic="true">{{ joinCountLabel }}</span>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="users"
              row-key="id"
              class="user-table"
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
              <el-table-column prop="username" label="用户名" min-width="150" align="center" header-align="center" />
              <el-table-column prop="nickname" label="昵称" min-width="130" align="center" header-align="center" />
              <el-table-column prop="gender" label="性别" min-width="90" align="center" header-align="center" />
              <el-table-column prop="phone" label="手机号" min-width="150" align="center" header-align="center" />
              <el-table-column label="角色" min-width="160" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="`role-${row.roleType}`" effect="light" round>{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch
                      :model-value="row.status === 'enabled'"
                      :loading="isStatusUpdating(row.id)"
                      @change="(value: boolean | string | number) => handleToggleUserStatus(row, value)"
                    />
                    <span>{{ row.status === 'enabled' ? '启用' : '禁用' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="330" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditUser(row)">
                      编辑
                    </el-button>
                    <el-button link type="primary" @click="handleResetPassword(row)">重置密码</el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="handleToggleUserStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteUser(row)">
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

    <!-- 弹窗刷新、批准、拒绝及撤销后的数量均由独立统计接口校准。 -->
    <FarmJoinRequests v-model="joinDialogVisible" @success="handleAddSuccess" @refresh="fetchPendingJoinCount" />
    <AddOrUpdate v-model="addDialogVisible" :id="editUserId" @success="handleAddSuccess" />
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
  /* 桌面端依次排列三个查询条件、查询操作和管理操作，保证全部控件处于同一行。 */
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) max-content max-content;
  align-items: center;
  gap: 14px 12px;
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

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  white-space: nowrap;
}

.manage-actions {
  /* 管理按钮保持为一个紧凑操作组，避免徽标把按钮挤到下一行。 */
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  white-space: nowrap;
}

/* 使用统一gap管理按钮间距，清除Element Plus相邻按钮的额外左边距。 */
.manage-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.join-request-button {
  white-space: nowrap;
}

.join-request-count {
  margin-left: 8px;
}

/* 隐藏视觉上的重复文案，同时向屏幕阅读器提供完整待审核数量。 */
.join-count-status {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
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

.user-table {
  width: 100%;
}

.user-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.user-table :deep(.el-table__row) {
  height: 64px;
}

.user-table :deep(.el-table__cell) {
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

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.role-blue {
  color: #2563eb;
  background: #eff6ff;
  border-color: #dbeafe;
}

.role-green {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.role-purple {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ede9fe;
}

.role-orange {
  color: #d97706;
  background: #fffbeb;
  border-color: #fef3c7;
}

.role-cyan {
  color: #0891b2;
  background: #ecfeff;
  border-color: #cffafe;
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
    /* 可用宽度不足时整组换到下一行，防止页面产生横向滚动。 */
    grid-column: 1 / -1;
    justify-self: end;
    width: fit-content;
  }
}

@media (max-width: 960px) {
  .user-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .user-content {
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
