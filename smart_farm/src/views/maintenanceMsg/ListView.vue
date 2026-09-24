<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, CirclePlus, WarningFilled, CircleCheckFilled, Search, Refresh, Delete, EditPen } from '@element-plus/icons-vue'
import { listMaintenanceMessages, getMaintenanceMessageStatistics, deleteMaintenanceMessage, batchDeleteMaintenanceMessages, type MaintenanceMessage } from '@/api/maintenanceMsg'
import AddOrUpdate from './AddOrUpdate.vue'

// 页面沿用农事消息卡片布局，同时支持故障自动发布与人工消息管理。
const sidebarVisible = ref(false)
const tableLoading = ref(false)
const rows = ref<MaintenanceMessage[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const stats = ref<Record<string, number>>({})
const searchForm = reactive({ title: '', content: '', plotName: '' })
// 发布/编辑共用表单；正文使用独立表单状态，默认模板仅用于首次填写，不覆盖用户修改。
const editingId = ref<number | null>(null)
const selectedRows = ref<MaintenanceMessage[]>([])
const deleting = ref(false)
const dialogVisible = ref(false)
const statCards = computed(() => [
  { title: '消息总数', value: stats.value.totalCount || 0, desc: '当前有效维护消息', icon: Bell, tone: 'blue' },
  { title: '今日发布', value: stats.value.todayCount || 0, desc: '今日新增消息', icon: CirclePlus, tone: 'green' },
  { title: '未读送达', value: stats.value.unreadDeliveryCount || 0, desc: '接收人尚未阅读', icon: WarningFilled, tone: 'orange' },
  { title: '已读送达', value: stats.value.readDeliveryCount || 0, desc: '接收人已阅读', icon: CircleCheckFilled, tone: 'red' },
])
// 请求序号避免连续筛选或翻页时较旧响应覆盖新结果；静默轮询不闪烁表格。
let requestId = 0
let timer: ReturnType<typeof setInterval> | undefined
const refreshPage = async (page = currentPage.value, quiet = false) => {
  currentPage.value = page
  const ownRequest = ++requestId
  if (!quiet) tableLoading.value = true
  try {
    const [list, counts] = await Promise.all([
      listMaintenanceMessages({ ...searchForm, pageNum: page, pageSize: pageSize.value }),
      getMaintenanceMessageStatistics(),
    ])
    if (ownRequest !== requestId) return
    rows.value = list.data.list || []
    total.value = list.data.total
    stats.value = counts.data || {}
  } catch {
    if (!quiet && ownRequest === requestId) ElMessage.error('维护消息加载失败，请重试')
  } finally {
    if (ownRequest === requestId) tableLoading.value = false
  }
}
// 清空三个查询条件并回到第一页。
const resetSearch = () => {
  Object.assign(searchForm, { title: '', content: '', plotName: '' })
  void refreshPage(1)
}
// 远程搜索设备/地块；序号保证旧搜索响应不会覆盖最新候选。
// 发布时由用户选择设备，不能直接指定其他农场的接收人或发布人。
const openPublish = () => {
  editingId.value = null
  dialogVisible.value = true
}
// 编辑重新读取版本与快照，设备保持原关联，避免改变历史消息的接收范围。
const openEdit = (row: MaintenanceMessage) => {
  editingId.value = row.id
  dialogVisible.value = true
}
// 标题、正文、级别一并提交；编辑回填数据库正文，不用默认模板替换。
const handleDialogSuccess = () => {
  void refreshPage(editingId.value ? currentPage.value : 1)
}
// 删除确认后同步刷新统计及列表；最后一页删空时回到仍有效的页码。
const removeMessages = async (items: MaintenanceMessage[]) => {
  if (!items.length) { ElMessage.warning('请先选择需要删除的维护消息'); return }
  if (deleting.value) return
  deleting.value = true
  try {
    await ElMessageBox.confirm(items.length === 1 ? `确定删除维护消息“${items[0]?.title}”吗？` : `确定批量删除选中的 ${items.length} 条维护消息吗？`, '删除确认', { type: 'warning' })
    const result = items.length === 1 ? await deleteMaintenanceMessage(items[0]!.id) : await batchDeleteMaintenanceMessages(items.map(item => item.id))
    selectedRows.value = []
    await refreshPage(Math.min(currentPage.value, Math.max(1, Math.ceil((total.value - result.data) / pageSize.value))))
    ElMessage.success('删除维护消息成功')
  } catch { /* 用户取消无需提示；接口错误交由统一请求层处理。 */ }
  finally { deleting.value = false }
}
// 页面可见时每15秒同步新故障通知；离开页面立即释放定时器。
onMounted(() => {
  void refreshPage()
  timer = setInterval(() => { if (!document.hidden && !tableLoading.value && !dialogVisible.value && !deleting.value && !selectedRows.value.length) void refreshPage(currentPage.value, true) }, 15000)
})
onUnmounted(() => { clearInterval(timer); requestId++ })
</script>
<template>
  <div class="notice-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="sidebarVisible = false" />

    <div class="notice-shell admin-shell">
      <Header :breadcrumbs="['首页', '消息通知管理', '维护消息管理']" @toggle-sidebar="sidebarVisible = true" />

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
            <!-- 与农事消息管理一致，批量删除使用表格选择项，发布打开消息表单。 -->
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" :loading="deleting" @click="removeMessages(selectedRows)">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openPublish">发布消息</el-button>
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
              @selection-change="(selection: MaintenanceMessage[]) => selectedRows = selection"

            >
              <el-table-column type="selection" width="58" fixed="left" align="center" />
              <el-table-column prop="id" label="ID" width="90" fixed="left" align="center" />
              <el-table-column prop="title" label="消息标题" min-width="220" show-overflow-tooltip />
              <el-table-column prop="content" label="消息内容" min-width="300" show-overflow-tooltip />
              <el-table-column prop="plotName" label="地块名称" min-width="150" show-overflow-tooltip />
              <!-- <el-table-column prop="faultId" label="故障ID" width="100" align="center" /> -->
              <el-table-column prop="sendTime" label="发布时间" min-width="180" align="center" />
              <el-table-column prop="publisherName" label="发布人" min-width="140" align="center" />
              <el-table-column label="已读数量" width="120" align="center">
                <template #default="{ row }">{{ row.readCount }} / {{ row.recipientCount }}</template>
              </el-table-column>
              <el-table-column label="操作" fixed="right" width="180" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" :icon="EditPen" @click="openEdit(row)">编辑</el-button>
                  <el-button link type="danger" :icon="Delete" :disabled="deleting" @click="removeMessages([row])">删除</el-button>
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
              @current-change="refreshPage($event)"
              @size-change="refreshPage(1)"
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
  gap: 14px 22px;
  /* 为发布和批量删除按钮预留空间，避免筛选项在中等宽度下溢出。 */
  padding-right: 270px;
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

@media (max-width: 1500px) {
  .filter-form { padding-right: 0; }
  .manage-actions { position: static; grid-column: 1 / -1; justify-content: flex-end; }
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
  /* 小屏纵向布局释放固定高度，保证统计、查询和分页均可滚动访问。 */
  .notice-shell { height: auto; min-height: 100vh; overflow: visible; }
  .notice-content { height: auto; overflow: visible; }
  .filter-form { padding-right: 0; }
  .manage-actions { position: static; }
  .table-scroll { height: 420px; flex: none; }
  .pagination-row { overflow-x: auto; }
  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
