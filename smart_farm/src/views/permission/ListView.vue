<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled,
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  Key,
  Refresh,
  Search,
} from '@element-plus/icons-vue'
import {
  deletePermission,
  listPermissions,
  updatePermission,
} from '@/api/permission'
import type { Permission } from '@/api/role'
import AddOrUpdate from './AddOrUpdate.vue'

interface PermissionRow {
  id: number
  permissionName: string
  permissionCode: string
  path: string
  component: string
  status: number
  sort: number
  createTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const permissions = ref<PermissionRow[]>([])
const tableLoading = ref(false)
const statusLoadingIds = ref<number[]>([])
const dialogVisible = ref(false)
const editPermissionId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const permissionStats = ref({
  total: 0,
  enabled: 0,
  routed: 0,
  disabled: 0,
})

const searchForm = reactive({
  permissionName: '',
  permissionCode: '',
  status: '',
})


const statCards = computed<StatCard[]>(() => [
  {
    title: '菜单总数',
    value: permissionStats.value.total.toLocaleString(),
    desc: '系统菜单资源',
    icon: Key,
    tone: 'blue',
  },
  {
    title: '启用菜单',
    value: permissionStats.value.enabled.toLocaleString(),
    desc: '用于菜单控制',
    icon: CircleCheckFilled,
    tone: 'green',
  },
  {
    title: '页面菜单',
    value: permissionStats.value.routed.toLocaleString(),
    desc: '配置路由入口',
    icon: CirclePlus,
    tone: 'orange',
  },
  {
    title: '禁用菜单',
    value: permissionStats.value.disabled.toLocaleString(),
    desc: '暂不展示菜单',
    icon: CircleCloseFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const mapPermissionToRow = (permission: Permission): PermissionRow => ({
  id: permission.id ?? Date.now(),
  permissionName: permission.permissionName || '-',
  permissionCode: permission.permissionCode || '-',
  path: permission.path || '-',
  component: permission.component || '-',
  status: permission.status === 0 ? 0 : 1,
  sort: permission.sort ?? 100,
  createTime: permission.createTime || '-',
})

const getSearchStatus = () => (searchForm.status === '' ? undefined : Number(searchForm.status))

const fetchPermissionStats = async () => {
  const [allResult, enabledResult, disabledResult, routedResult] = await Promise.all([
    listPermissions({ pageNum: 1, pageSize: 1, type: 1 }),
    listPermissions({ pageNum: 1, pageSize: 1, type: 1, status: 1 }),
    listPermissions({ pageNum: 1, pageSize: 1, type: 1, status: 0 }),
    listPermissions({ pageNum: 1, pageSize: 10000, type: 1 }),
  ])

  permissionStats.value = {
    total: allResult.data.total,
    enabled: enabledResult.data.total,
    routed: routedResult.data.list.filter((permission) => Boolean(permission.path)).length,
    disabled: disabledResult.data.total,
  }
}

const fetchPermissions = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listPermissions({
      permissionName: searchForm.permissionName || undefined,
      permissionCode: searchForm.permissionCode || undefined,
      type: 1,
      status: getSearchStatus(),
      pageNum,
      pageSize: size,
    })

    permissions.value = result.data.list.map(mapPermissionToRow)
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
  } finally {
    tableLoading.value = false
  }
}

const refreshPage = async (pageNum = currentPage.value, size = pageSize.value) => {
  await Promise.all([fetchPermissions(pageNum, size), fetchPermissionStats()])
}

const resetSearch = () => {
  searchForm.permissionName = ''
  searchForm.permissionCode = ''
  searchForm.status = ''
  void refreshPage(1)
}

const openAddDialog = () => {
  editPermissionId.value = null
  dialogVisible.value = true
}

const openEditDialog = (row: PermissionRow) => {
  editPermissionId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void refreshPage(editPermissionId.value ? currentPage.value : 1)
}

const handleTogglePermissionStatus = async (row: PermissionRow) => {
  const nextStatus = row.status === 1 ? 0 : 1
  statusLoadingIds.value.push(row.id)

  try {
    await updatePermission({ id: row.id, status: nextStatus })
    ElMessage.success(nextStatus === 1 ? '启用菜单成功' : '禁用菜单成功')
    await refreshPage(currentPage.value)
  } finally {
    statusLoadingIds.value = statusLoadingIds.value.filter((id) => id !== row.id)
  }
}

const handleDeletePermission = async (row: PermissionRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除菜单「${row.permissionName}」吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deletePermission(row.id)
    ElMessage.success('删除菜单成功')
    void refreshPage(permissions.value.length <= 1 && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleCurrentPageChange = (page: number) => {
  void refreshPage(page)
}

const handlePageSizeChange = (size: number) => {
  void refreshPage(1, size)
}

onMounted(() => {
  void refreshPage()
})
</script>

<template>
  <div class="permission-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="permission-shell admin-shell">
      <Header :breadcrumbs="['首页', '系统设置', '菜单管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="permission-content admin-content">
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
            <el-form-item label="菜单名称">
              <el-input v-model="searchForm.permissionName" placeholder="请输入菜单名称" clearable />
            </el-form-item>
            <el-form-item label="菜单编码">
              <el-input v-model="searchForm.permissionCode" placeholder="请输入菜单编码" clearable />
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
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增菜单</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table v-loading="tableLoading" :data="permissions" row-key="id" class="permission-table" height="100%">
              <el-table-column prop="permissionName" label="菜单名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="菜单编码" min-width="170" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="permission-code-chip" effect="plain" round>{{ row.permissionCode }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="path" label="路由路径" min-width="160" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="component" label="组件路径" min-width="180" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="状态" min-width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="row.status === 1 ? 'status-enabled' : 'status-disabled'" effect="light" round>
                    {{ row.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sort" label="排序" min-width="90" align="center" header-align="center" />
              <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" header-align="center" />
              <el-table-column label="操作" width="260" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">
                      编辑
                    </el-button>
                    <el-button
                      link
                      :type="row.status === 1 ? 'warning' : 'success'"
                      :loading="statusLoadingIds.includes(row.id)"
                      @click="handleTogglePermissionStatus(row)"
                    >
                      {{ row.status === 1 ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeletePermission(row)">
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

    <AddOrUpdate v-model="dialogVisible" :id="editPermissionId" @success="handleDialogSuccess" />
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
  grid-template-columns: repeat(5, minmax(160px, 1fr));
  align-items: center;
  gap: 14px 18px;
  padding-right: 124px;
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

.permission-table {
  width: 100%;
}

.permission-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.permission-table :deep(.el-table__row) {
  height: 64px;
}

.permission-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.permission-code-chip {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
  font-size: 12px;
  font-weight: 600;
}

.type-1 {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
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
  .permission-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .permission-content {
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
