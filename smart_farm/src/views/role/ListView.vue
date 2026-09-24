<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox, ElTree } from 'element-plus'
import {
  Connection,
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  Key,
  Refresh,
  Search,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  assignRolePermissions,
  batchDeleteRoles,
  deleteRole,
  getRoleDetail,
  listRolePermissionTree,
  listRoles,
  updateRole,
  type MenuPermissionTreeNode,
  type Permission,
  type Role,
} from '@/api/role'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import AddOrUpdate from './AddOrUpdate.vue'

interface RoleRow {
  id: number
  roleName: string
  roleCode: string
  remark: string
  userCount: number
  status: number
  createTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

interface UserCountResult {
  total: number
  byRoleId: Map<number, number>
  byRoleCode: Map<string, number>
}

const sidebarVisible = ref(false)
const selectedRows = ref<RoleRow[]>([])
const roles = ref<RoleRow[]>([])
const tableLoading = ref(false)
const statusLoadingIds = ref<number[]>([])
const dialogVisible = ref(false)
const editRoleId = ref<number | null>(null)
const permissionDialogVisible = ref(false)
const permissionDialogLoading = ref(false)
const permissionSubmitLoading = ref(false)
const permissionRole = ref<RoleRow | null>(null)
const permissionTree = ref<MenuPermissionTreeNode[]>([])
const permissionTreeRef = ref<InstanceType<typeof ElTree>>()
const selectedPermissionIds = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userCounts = ref<UserCountResult>({
  total: 0,
  byRoleId: new Map(),
  byRoleCode: new Map(),
})

const roleStats = ref({
  total: 0,
  assigned: 0,
  empty: 0,
  users: 0,
})

const searchForm = reactive({
  roleName: '',
  roleCode: '',
  status: '',
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '角色总数',
    value: roleStats.value.total.toLocaleString(),
    desc: '系统可分配角色',
    icon: Key,
    tone: 'blue',
  },
  {
    title: '已关联角色',
    value: roleStats.value.assigned.toLocaleString(),
    desc: '至少关联 1 个用户',
    icon: UserFilled,
    tone: 'green',
  },
  {
    title: '未分配角色',
    value: roleStats.value.empty.toLocaleString(),
    desc: '当前无关联用户',
    icon: CircleCloseFilled,
    tone: 'orange',
  },
  {
    title: '用户总数',
    value: roleStats.value.users.toLocaleString(),
    desc: '按角色统计来源',
    icon: CirclePlus,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const increaseMapCount = <K,>(map: Map<K, number>, key: K | undefined) => {
  if (key === undefined || key === null || key === '') {
    return
  }

  map.set(key, (map.get(key) || 0) + 1)
}

const buildUserCounts = (users: SmartPlantUser[]): UserCountResult => {
  const byRoleId = new Map<number, number>()
  const byRoleCode = new Map<string, number>()

  users.forEach((user) => {
    increaseMapCount(byRoleId, user.roleId)
    increaseMapCount(byRoleCode, user.roleCode)
  })

  return {
    total: users.length,
    byRoleId,
    byRoleCode,
  }
}

const fetchAllUsers = async () => {
  const firstPage = await listSmartPlantUsers({ pageNum: 1, pageSize: 1000 })

  if (firstPage.data.pages <= 1) {
    return firstPage.data.list
  }

  const restPages = await Promise.all(
    Array.from({ length: firstPage.data.pages - 1 }, (_, index) =>
      listSmartPlantUsers({ pageNum: index + 2, pageSize: 1000 }),
    ),
  )

  return [
    ...firstPage.data.list,
    ...restPages.flatMap((result) => result.data.list),
  ]
}

const getRoleUserCount = (role: Role) => {
  const idCount = role.id === undefined ? undefined : userCounts.value.byRoleId.get(role.id)
  return idCount ?? userCounts.value.byRoleCode.get(role.roleCode) ?? 0
}

const mapRoleToRow = (role: Role): RoleRow => ({
  id: role.id ?? Date.now(),
  roleName: role.roleName || '-',
  roleCode: role.roleCode || '-',
  remark: role.remark || role.description || '-',
  userCount: getRoleUserCount(role),
  status: role.status === 0 ? 0 : 1,
  createTime: role.createTime || '-',
})

const fetchUserCounts = async () => {
  userCounts.value = buildUserCounts(await fetchAllUsers())
}

const fetchRoleStats = async () => {
  const result = await listRoles({ pageNum: 1, pageSize: 10000 })
  const allRoles = result.data.list
  const assigned = allRoles.filter((role) => getRoleUserCount(role) > 0).length

  roleStats.value = {
    total: result.data.total,
    assigned,
    empty: Math.max(result.data.total - assigned, 0),
    users: userCounts.value.total,
  }
}

const fetchRoles = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listRoles({
      roleName: searchForm.roleName || undefined,
      roleCode: searchForm.roleCode || undefined,
      status:
        searchForm.status === '0' || searchForm.status === '1'
          ? Number(searchForm.status)
          : undefined,
      pageNum,
      pageSize: size,
    })

    roles.value = result.data.list.map((item) => mapRoleToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshPage = async (pageNum = currentPage.value, size = pageSize.value) => {
  await fetchUserCounts()
  await Promise.all([fetchRoles(pageNum, size), fetchRoleStats()])
}

const resetSearch = () => {
  searchForm.roleName = ''
  searchForm.roleCode = ''
  searchForm.status = ''
  void refreshPage(1)
}

const handleSelectionChange = (rows: RoleRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editRoleId.value = null
  dialogVisible.value = true
}

const handleEditRole = (row: RoleRow) => {
  editRoleId.value = row.id
  dialogVisible.value = true
}

const openPermissionDialog = async (row: RoleRow) => {
  permissionRole.value = row
  permissionDialogVisible.value = true
  permissionDialogLoading.value = true

  try {
    const [permissionResult, roleDetailResult] = await Promise.all([
      listRolePermissionTree(),
      getRoleDetail(row.id),
    ])

    permissionTree.value = permissionResult.data
    selectedPermissionIds.value = roleDetailResult.data.permissions
      .filter((permission) => permission.type === 1)
      .map((permission) => permission.id)
      .filter((id): id is number => id !== undefined)
    await nextTick()
    permissionTreeRef.value?.setCheckedKeys(selectedPermissionIds.value)
  } finally {
    permissionDialogLoading.value = false
  }
}

const submitPermissionDialog = async () => {
  if (!permissionRole.value) {
    return
  }

  permissionSubmitLoading.value = true

  try {
    const permissionIds = (permissionTreeRef.value?.getCheckedKeys(false) || []).map(Number)
    await assignRolePermissions(permissionRole.value.id, permissionIds)
    ElMessage.success('分配菜单成功')
    permissionDialogVisible.value = false
    void refreshPage(currentPage.value)
  } finally {
    permissionSubmitLoading.value = false
  }
}

const handleDialogSuccess = () => {
  void refreshPage(editRoleId.value ? currentPage.value : 1)
}

const setStatusUpdating = (id: number, updating: boolean) => {
  statusLoadingIds.value = updating
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleToggleRoleStatus = async (row: RoleRow) => {
  const nextStatus = row.status === 1 ? 0 : 1
  setStatusUpdating(row.id, true)

  try {
    await updateRole({ id: row.id, status: nextStatus })
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '启用角色成功' : '禁用角色成功')
    void fetchRoles(currentPage.value)
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const handleCurrentPageChange = (page: number) => {
  void refreshPage(page)
}

const handlePageSizeChange = (size: number) => {
  void refreshPage(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return roles.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const handleDeleteRole = async (row: RoleRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${row.roleName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteRole(row.id)
    ElMessage.success('删除角色成功')
    void refreshPage(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的角色')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 个角色吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteRoles(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除角色成功')
    void refreshPage(getNextPageAfterDelete(selectedRows.value.length))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void refreshPage()
})
</script>

<template>
  <div class="role-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="role-shell admin-shell">
      <Header :breadcrumbs="['首页', '系统设置', '角色管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="role-content admin-content">
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
            <el-form-item label="角色名称">
              <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
            </el-form-item>
            <el-form-item label="角色编码">
              <el-input v-model="searchForm.roleCode" placeholder="请输入角色编码" clearable />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="1" />
                <el-option label="禁用" value="0" />
              </el-select>
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="refreshPage(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增角色</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="roles"
              row-key="id"
              class="role-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="roleName" label="角色名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="角色编码" min-width="150" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="role-code-chip" effect="plain" round>{{ row.roleCode }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" min-width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="row.status === 1 ? 'status-enabled' : 'status-disabled'" effect="light" round>
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" header-align="center" />
              <el-table-column label="操作" width="330" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditRole(row)">
                      编辑
                    </el-button>
                    <el-button link type="primary" :icon="Connection" @click="openPermissionDialog(row)">
                      菜单
                    </el-button>
                    <el-button
                      link
                      :type="row.status === 1 ? 'warning' : 'success'"
                      :loading="statusLoadingIds.includes(row.id)"
                      @click="handleToggleRoleStatus(row)"
                    >
                      {{ row.status === 1 ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteRole(row)">
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

    <AddOrUpdate v-model="dialogVisible" :id="editRoleId" @success="handleDialogSuccess" />

    <el-dialog
      v-model="permissionDialogVisible"
      title="分配菜单权限"
      width="520px"
      class="role-dialog"
      modal-class="role-dialog-overlay"
      align-center
      destroy-on-close
    >
      <div v-loading="permissionDialogLoading" class="permission-panel">
        <div class="permission-summary">
          <span>当前角色：</span>
          <strong>{{ permissionRole?.roleName || '-' }}</strong>
        </div>

        <el-tree
          ref="permissionTreeRef"
          :data="permissionTree"
          :props="{ label: 'permissionName', children: 'children' }"
          node-key="id"
          show-checkbox
          default-expand-all
          :indent="24"
          empty-text="暂无可分配的菜单权限"
          class="permission-tree"
        />
      </div>

      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitLoading" @click="submitPermissionDialog">
          保存权限
        </el-button>
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
  grid-template-columns: repeat(3, minmax(220px, 1fr));
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

.role-table {
  width: 100%;
}

.role-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.role-table :deep(.el-table__row) {
  height: 64px;
}

.role-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.role-code-chip {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
  font-size: 12px;
  font-weight: 600;
}

.status-enabled {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.status-disabled {
  color: #64748b;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.user-count {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

:global(.role-dialog) {
  width: min(520px, calc(100vw - 32px)) !important;
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.role-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.role-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.role-dialog .el-dialog__title) {
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

:global(.role-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 18px 24px;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.role-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 16px 24px;
  border-top: 1px solid #eef2f7;
}

.permission-panel {
  min-height: 260px;
}

.permission-summary {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
}

.permission-summary strong {
  color: #303133;
  font-size: 14px;
}

.permission-tree {
  max-height: 440px;
  overflow-y: auto;
  padding: 4px 0;
  scrollbar-color: #cbd5e1 transparent;
  scrollbar-width: thin;
}

.permission-tree :deep(.el-tree-node__content) {
  height: 32px;
  padding-right: 8px;
  border-radius: 4px;
}

.permission-tree :deep(.el-tree-node__content:hover) {
  background: #f5f7fa;
}

:global(.role-dialog .permission-tree .el-tree-node__expand-icon) {
  width: 12px;
  height: 12px;
  color: #a8abb2;
  transform: none;
}

:global(.role-dialog .permission-tree .el-tree-node__expand-icon:not(.is-leaf) svg) {
  display: none !important;
}

:global(.role-dialog .permission-tree .el-tree-node__expand-icon:not(.is-leaf)::before) {
  width: 0;
  height: 0;
  content: '';
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 6px solid currentColor;
}

:global(.role-dialog .permission-tree .el-tree-node__expand-icon.expanded::before) {
  border-top: 6px solid currentColor;
  border-right: 5px solid transparent;
  border-bottom: 0;
  border-left: 5px solid transparent;
}

:global(.role-dialog .permission-tree .el-tree-node__expand-icon.is-leaf) {
  display: none;
}

.permission-tree :deep(.el-checkbox__inner) {
  margin-right: 8px;
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
  .role-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .role-content {
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

  .permission-tree { max-height: 360px; }
}
</style>
