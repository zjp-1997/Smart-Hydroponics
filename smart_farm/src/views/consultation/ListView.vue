<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  CircleCheckFilled,
  Delete,
  Picture,
  Refresh,
  Search,
  Tickets,
  View,
} from '@element-plus/icons-vue'
import {
  batchDeleteConsultationMessages,
  deleteConsultationMessage,
  getConsultationMessageById,
  getConsultationStatistics,
  listConsultationMessages,
  type ConsultationMessage,
  type ConsultationStatistics,
} from '@/api/consultation'
import { getFileUrl } from '@/utils/utils'

interface ConsultationRow {
  id: number
  sessionNo: string
  userLabel: string
  expertName: string
  senderName: string
  receiverName: string
  messageType: number
  messageTypeText: string
  content: string
  mediaUrl: string
  isRead: number
  readText: string
  readType: 'success' | 'warning'
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
const selectedRows = ref<ConsultationRow[]>([])
const rows = ref<ConsultationRow[]>([])
const tableLoading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<ConsultationMessage | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = ref<ConsultationStatistics>({})

const searchForm = reactive<{
  username: string
  expertName: string
  sessionNo: string
  messageType: '' | number
  dateRange: string[]
}>({
  username: '',
  expertName: '',
  sessionNo: '',
  messageType: '',
  dateRange: [],
})

const messageTypeMap: Record<number, string> = {
  1: '文本',
  2: '图片',
  3: '语音',
  4: '文件',
}

const statCards = computed<StatCard[]>(() => [
  {
    title: '消息总数',
    value: formatCount(stats.value.totalCount),
    desc: '全部专家咨询消息',
    icon: ChatDotRound,
    tone: 'blue',
  },
  {
    title: '文本消息',
    value: formatCount(stats.value.textCount),
    desc: '文字咨询内容',
    icon: Tickets,
    tone: 'green',
  },
  {
    title: '图片消息',
    value: formatCount(stats.value.imageCount),
    desc: '携带图片咨询',
    icon: Picture,
    tone: 'orange',
  },
  {
    title: '未读消息',
    value: formatCount(stats.value.unreadCount),
    desc: '等待对方查看',
    icon: CircleCheckFilled,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatCount = (value?: number | string) => Number(value ?? 0).toLocaleString()

const getUserLabel = (message: ConsultationMessage) => {
  if (message.nickname && message.username) {
    return `${message.nickname}（${message.username}）`
  }

  return message.nickname || message.username || '-'
}

const getQueryParams = () => ({
  username: searchForm.username || undefined,
  expertName: searchForm.expertName || undefined,
  sessionNo: searchForm.sessionNo || undefined,
  messageType: searchForm.messageType || undefined,
  startDate: searchForm.dateRange[0] || undefined,
  endDate: searchForm.dateRange[1] || undefined,
})

const mapToRow = (message: ConsultationMessage): ConsultationRow => {
  const isRead = message.isRead === 1 ? 1 : 0

  return {
    id: message.id ?? Date.now(),
    sessionNo: message.sessionNo || '-',
    userLabel: getUserLabel(message),
    expertName: message.expertName || '-',
    senderName: message.senderName || `用户${message.senderId || '-'}`,
    receiverName: message.receiverName || `用户${message.receiverId || '-'}`,
    messageType: message.messageType || 1,
    messageTypeText: messageTypeMap[message.messageType || 1] || '-',
    content: message.content || '-',
    mediaUrl: getFileUrl(message.mediaUrl || ''),
    isRead,
    readText: isRead ? '已读' : '未读',
    readType: isRead ? 'success' : 'warning',
    createTime: message.createTime || '-',
  }
}

const fetchStats = async () => {
  const result = await getConsultationStatistics(getQueryParams())
  stats.value = result.data || {}
}

/** 拉取咨询消息列表，供查询、分页和删除后刷新使用。 */
const fetchRows = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listConsultationMessages({
      ...getQueryParams(),
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
  await Promise.all([fetchRows(pageNum), fetchStats()])
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.expertName = ''
  searchForm.sessionNo = ''
  searchForm.messageType = ''
  searchForm.dateRange = []
  void refreshPageData(1)
}

const handleSelectionChange = (value: ConsultationRow[]) => {
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

const handleView = async (row: ConsultationRow) => {
  detailVisible.value = true
  detailLoading.value = true

  try {
    // 详情重新按 ID 查询，避免表格截断内容影响后台核查。
    const result = await getConsultationMessageById(row.id)
    currentDetail.value = {
      ...result.data,
      mediaUrl: getFileUrl(result.data.mediaUrl || ''),
    }
  } finally {
    detailLoading.value = false
  }
}

const handleDelete = async (row: ConsultationRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除该咨询消息吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteConsultationMessage(row.id)
    ElMessage.success('删除咨询消息成功')
    void refreshPageData(getNextPageAfterDelete(1))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的咨询消息')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条咨询消息吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteConsultationMessages(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除咨询消息成功')
    void refreshPageData(getNextPageAfterDelete(selectedRows.value.length))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void refreshPageData()
})
</script>

<template>
  <div class="consultation-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="consultation-shell admin-shell">
      <Header :breadcrumbs="['首页', '咨询管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="consultation-content admin-content">
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
            <el-form-item label="用户账号">
              <el-input v-model="searchForm.username" placeholder="请输入用户账号" clearable />
            </el-form-item>
            <el-form-item label="专家姓名">
              <el-input v-model="searchForm.expertName" placeholder="请输入专家姓名" clearable />
            </el-form-item>
            <el-form-item label="会话编号">
              <el-input v-model="searchForm.sessionNo" placeholder="请输入会话编号" clearable />
            </el-form-item>
            <el-form-item label="消息类型">
              <el-select v-model="searchForm.messageType" placeholder="全部类型" clearable>
                <el-option label="文本" :value="1" />
                <el-option label="图片" :value="2" />
                <el-option label="语音" :value="3" />
                <el-option label="文件" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="发送日期">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
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
              class="consultation-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="sessionNo" label="会话编号" min-width="190" align="center" header-align="center" />
              <el-table-column prop="userLabel" label="咨询用户" min-width="180" align="center" header-align="center" />
              <el-table-column prop="expertName" label="专家姓名" min-width="140" align="center" header-align="center" />
              <el-table-column prop="senderName" label="发送人" min-width="140" align="center" header-align="center" />
              <el-table-column prop="receiverName" label="接收人" min-width="140" align="center" header-align="center" />
              <el-table-column prop="messageTypeText" label="消息类型" min-width="110" align="center" header-align="center" />
              <el-table-column label="媒体" width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.messageType === 2 && row.mediaUrl"
                    class="message-image"
                    :src="row.mediaUrl"
                    :preview-src-list="[row.mediaUrl]"
                    fit="cover"
                    preview-teleported
                  />
                  <span v-else>{{ row.mediaUrl ? '附件' : '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="content" label="聊天内容" min-width="320" show-overflow-tooltip header-align="center" />
              <el-table-column label="阅读状态" width="110" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :type="row.readType" effect="light">{{ row.readText }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="发送时间" min-width="170" align="center" header-align="center" />
              <el-table-column label="操作" width="190" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="View" @click="handleView(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="咨询详情" width="760px" class="consultation-detail-dialog" align-center>
      <div v-loading="detailLoading" class="consultation-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="会话编号">{{ currentDetail?.sessionNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="咨询用户">{{ currentDetail ? getUserLabel(currentDetail) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="专家姓名">{{ currentDetail?.expertName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="消息类型">{{ messageTypeMap[currentDetail?.messageType || 1] || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送人">{{ currentDetail?.senderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="接收人">{{ currentDetail?.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发送时间">{{ currentDetail?.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阅读状态">
            <el-tag :type="currentDetail?.isRead === 1 ? 'success' : 'warning'">
              {{ currentDetail?.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentDetail?.messageType === 2 && currentDetail.mediaUrl" class="detail-image-wrap">
          <el-image
            class="detail-image"
            :src="currentDetail.mediaUrl"
            :preview-src-list="[currentDetail.mediaUrl]"
            fit="cover"
            preview-teleported
          />
        </div>

        <div class="message-block">
          <h3>聊天内容</h3>
          <p>{{ currentDetail?.content || '-' }}</p>
        </div>
      </div>
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
  grid-template-columns: repeat(4, minmax(190px, 1fr));
  align-items: center;
  gap: 14px 22px;
  padding-right: 126px;
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

.filter-form :deep(.el-date-editor),
.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-select__wrapper) {
  width: 100%;
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

.consultation-table {
  width: 100%;
}

.consultation-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.consultation-table :deep(.el-table__row) {
  height: 64px;
}

.consultation-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.message-image {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  background: #eef2f7;
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

.consultation-detail {
  min-height: 220px;
}

.detail-image-wrap {
  margin-top: 18px;
}

.detail-image {
  width: 180px;
  height: 128px;
  border-radius: 8px;
  background: #eef2f7;
}

.message-block {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fbfdff;
}

.message-block h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.2;
}

.message-block p {
  margin: 0;
  color: #334155;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
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
  .consultation-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .consultation-content {
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
