<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  CircleCheckFilled,
  CirclePlus,
  Delete,
  EditPen,
  Refresh,
  Search,
  UserFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteFarmMessages,
  deleteFarmMessage,
  getFarmMessageStatistics,
  listFarmMessages,
  type FarmMessage,
} from '@/api/farmMsg'
import AddOrUpdate from './AddOrUpdate.vue'

interface StatCard {
  title: string
  value: string
  desc: string
  icon: Component
  tone: string
}

// 页面基础状态：侧栏、表格、分页和弹窗集中声明，便于维护。
const sidebarVisible = ref(false)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const selectedRows = ref<FarmMessage[]>([])
const rows = ref<FarmMessage[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editingId = ref<number | null>(null)
const stats = ref<Record<string, number>>({})

// 列表不再展示内部农事任务标题，查询入口按用户可见的消息标题命名。
const searchForm = reactive({
  title: '',
  content: '',
  plotName: '',
  dateRange: [] as string[],
})


// 消息级别枚举与后端 notification.level 字段保持一致。
const levelMap: Record<number, { text: string; type: 'info' | 'warning' | 'danger' }> = {
  1: { text: '普通', type: 'info' },
  2: { text: '重要', type: 'warning' },
  3: { text: '紧急', type: 'danger' },
}

const defaultLevelMeta: { text: string; type: 'info' | 'warning' | 'danger' } = {
  text: '普通',
  type: 'info',
}

const taskTypeMap: Record<number, string> = {
  1: '浇水',
  2: '施肥',
  3: '打药',
  4: '采收',
  5: '巡检',
  6: '除草',
  7: '补光',
  8: '其他',
}

const taskStatusMap: Record<number, string> = {
  1: '未开始',
  2: '进行中',
  3: '已完成',
  4: '已逾期',
  5: '已取消',
}

// 前端校验把消息标题、内容和关联任务前置拦截，减少无效请求进入后端。

// 统计卡片按消息组聚合，重点展示消息发布和已读未读送达情况。
const statCards = computed<StatCard[]>(() => [
  {
    title: '消息总数',
    value: formatCount(stats.value.totalCount),
    desc: '当前有效农事消息',
    icon: Bell,
    tone: 'blue',
  },
  {
    title: '今日发布',
    value: formatCount(stats.value.todayCount),
    desc: '今日新增消息',
    icon: CirclePlus,
    tone: 'green',
  },
  {
    title: '未读送达',
    value: formatCount(stats.value.unreadDeliveryCount),
    desc: '接收人尚未阅读',
    icon: WarningFilled,
    tone: 'orange',
  },
  {
    title: '已读送达',
    value: formatCount(stats.value.readDeliveryCount),
    desc: '接收人已阅读',
    icon: CircleCheckFilled,
    tone: 'red',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: number) => Number(value || 0).toLocaleString()

const getLevelMeta = (level?: number) => levelMap[level || 1] ?? defaultLevelMeta

const getTaskTypeName = (taskType?: number) => (taskType == null ? '-' : taskTypeMap[taskType] || '-')

const getTaskStatusName = (taskStatus?: number) => (taskStatus == null ? '-' : taskStatusMap[taskStatus] || '-')

const fetchStats = async () => {
  const result = await getFarmMessageStatistics()
  stats.value = result.data || {}
}

const fetchRows = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const [startDate, endDate] = searchForm.dateRange
    const result = await listFarmMessages({
      title: searchForm.title || undefined,
      content: searchForm.content || undefined,
      plotName: searchForm.plotName || undefined,
      startDate,
      endDate,
      pageNum,
      pageSize: size,
    })
    rows.value = result.data.list || []
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshPage = async (pageNum = currentPage.value, size = pageSize.value) => {
  await Promise.all([fetchRows(pageNum, size), fetchStats()])
}

const resetSearch = () => {
  searchForm.title = ''
  searchForm.content = ''
  searchForm.plotName = ''
  searchForm.dateRange = []
  void refreshPage(1)
}

const openPublishDialog = () => {
  editingId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: FarmMessage) => {
  editingId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void refreshPage(editingId.value ? currentPage.value : 1)
}

const handleSelectionChange = (selection: FarmMessage[]) => {
  selectedRows.value = selection
}

const handleCurrentPageChange = (page: number) => {
  void fetchRows(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchRows(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
}

const handleDelete = async (row: FarmMessage) => {
  try {
    await ElMessageBox.confirm(`确定要删除农事消息“${row.title}”吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteFarmMessage(row.id)
    ElMessage.success('删除农事消息成功')
    await refreshPage(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的农事消息')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要批量删除已选中的 ${selectedRows.value.length} 条农事消息吗？`, '批量删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await batchDeleteFarmMessages(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除农事消息成功')
    await refreshPage(getNextPageAfterDelete(selectedRows.value.length))
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
  <div class="notice-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="notice-shell admin-shell">
      <Header :breadcrumbs="['首页', '消息通知管理', '农事消息管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="notice-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="76px" @submit.prevent="refreshPage(1)">
            <el-form-item label="消息标题">
              <el-input v-model="searchForm.title" placeholder="请输入消息标题" clearable />
            </el-form-item>
            <el-form-item label="消息内容">
              <el-input v-model="searchForm.content" placeholder="请输入消息内容" clearable />
            </el-form-item>
            <el-form-item label="地块名称">
              <el-input v-model="searchForm.plotName" placeholder="请输入地块名称" clearable />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" native-type="submit">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openPublishDialog">发布消息</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="rows"
              row-key="id"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" />
              <el-table-column prop="id" label="ID" width="90" fixed="left" align="center" />
              <el-table-column label="农事类型" width="110" align="center">
                <template #default="{ row }">{{ getTaskTypeName(row.taskType) }}</template>
              </el-table-column>
              <el-table-column prop="plotName" label="地块名称" min-width="140" align="center" show-overflow-tooltip />
              <el-table-column prop="title" label="消息标题" min-width="200" show-overflow-tooltip />
              <el-table-column prop="content" label="消息内容" min-width="280" show-overflow-tooltip />
              <el-table-column label="级别" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="getLevelMeta(row.level).type">{{ getLevelMeta(row.level).text }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="任务状态" width="110" align="center">
                <template #default="{ row }">{{ getTaskStatusName(row.taskStatus) }}</template>
              </el-table-column>
              <el-table-column prop="publisherName" label="发布人" min-width="130" align="center" />
              <el-table-column label="已读数量" width="130" align="center">
                <template #default="{ row }">
                  {{ Number(row.readCount || 0).toLocaleString() }} / {{ Number(row.recipientCount || 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="sendTime" label="发布时间" min-width="170" align="center" />
              <el-table-column label="操作" width="180" fixed="right" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
                  <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-row">
            <span>共 {{ total.toLocaleString() }} 条</span>
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
        </section>
      </main>
    </div>

    <AddOrUpdate v-model="dialogVisible" :id="editingId" @success="handleDialogSuccess" />
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.stat-card,
.filter-card,
.table-card {
  background: #ffffff;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.04);
}

.stat-card {
  min-height: 112px;
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px 26px;
  border: 1px solid #f1f5f9;
}

.stat-icon {
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  font-size: 30px;
}

.stat-card p,
.stat-card span {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin: 9px 0 7px;
  color: #111827;
  font-size: 26px;
  line-height: 1;
}

.tone-blue { background: #2563eb; }
.tone-orange { background: #f59e0b; }
.tone-green { background: #10b981; }
.tone-red { background: #ef4444; }

.filter-card {
  margin-top: 10px;
  padding: 14px 18px;
}

.filter-form {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(220px, 1fr));
  align-items: center;
  gap: 14px 22px;
  /* 右侧预留管理按钮空间，使查询按钮和发布按钮在宽屏下保持稳定布局。 */
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
.filter-form :deep(.el-select__wrapper),
.filter-form :deep(.el-date-editor.el-input__wrapper) {
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

.manage-actions {
  position: absolute;
  right: 0;
  top: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
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

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  color: #334155;
  font-size: 14px;
}

@media (max-width: 1280px) {
  .stats-grid,
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .notice-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
