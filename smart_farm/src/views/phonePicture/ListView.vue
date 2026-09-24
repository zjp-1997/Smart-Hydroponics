<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CirclePlus,
  Delete,
  Download,
  EditPen,
  Picture,
  Refresh,
  Search,
  UploadFilled,
  UserFilled,
} from '@element-plus/icons-vue'
import {
  batchDeleteCropImages,
  deleteCropImage,
  downloadCropImage,
  listCropImages,
  type CropImage,
} from '@/api/cropImage'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getFileUrl } from '@/utils/utils'
import AddOrUpdate from './AddOrUpdate.vue'

interface PhonePictureRow {
  id: number
  imageText: string
  rawImageUrl: string
  imageUrl?: string
  imageSize: string
  userLabel: string
  remark: string
  createTime: string
  updateTime: string
}

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

const sidebarVisible = ref(false)
const selectedRows = ref<PhonePictureRow[]>([])
const phonePictures = ref<PhonePictureRow[]>([])
const userOptions = ref<SmartPlantUser[]>([])
const tableLoading = ref(false)
const addDialogVisible = ref(false)
const editCropImageId = ref<number | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dateRange = ref<[string, string] | []>([])

const PHONE_IMAGE_STORAGE_PREFIX = 'smart_farm_phone_picture:'

const imageStats = ref({
  total: 0,
  uploaderCount: 0,
  totalSize: 0,
  todayAdded: 0,
  totalTrend: 0,
  uploaderTrend: 0,
  sizeTrend: 0,
  todayTrend: 0,
})

const searchForm = reactive({
  userId: undefined as number | undefined,
})

const statCards = computed<StatCard[]>(() => [
  {
    title: '图片总数',
    value: imageStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(imageStats.value.totalTrend),
    trendType: imageStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: Picture,
    tone: 'blue',
  },
  {
    title: '今日新增',
    value: imageStats.value.todayAdded.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(imageStats.value.todayTrend),
    trendType: imageStats.value.todayTrend >= 0 ? 'up' : 'down',
    icon: UploadFilled,
    tone: 'green',
  },
  {
    title: '上传用户',
    value: imageStats.value.uploaderCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(imageStats.value.uploaderTrend),
    trendType: imageStats.value.uploaderTrend >= 0 ? 'up' : 'down',
    icon: UserFilled,
    tone: 'orange',
  },
  {
    title: '累计容量',
    value: formatBytes(imageStats.value.totalSize),
    desc: '较昨日',
    trend: formatBytesTrend(imageStats.value.sizeTrend),
    trendType: imageStats.value.sizeTrend >= 0 ? 'up' : 'down',
    icon: CirclePlus,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => {
  return `${value >= 0 ? '+' : '-'}${Math.abs(value).toLocaleString()}`
}

const formatBytesTrend = (value: number) => {
  return `${value >= 0 ? '+' : '-'}${formatBytes(Math.abs(value))}`
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

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(PHONE_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const isStoredImageKey = (value?: string) => {
  return Boolean(value?.startsWith(PHONE_IMAGE_STORAGE_PREFIX))
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (!normalizedValue) {
    return ''
  }

  if (/^(blob:|data:image\/)/.test(normalizedValue)) {
    return normalizedValue
  }

  if (/^https?:\/\//i.test(normalizedValue) || normalizedValue.startsWith('/')) {
    return getFileUrl(normalizedValue)
  }

  return getStoredImageUrl(normalizedValue) || getFileUrl(normalizedValue)
}

const getImageText = (item: CropImage) => {
  return item.id ? String(item.id).slice(-1) : '图'
}

const formatBytes = (value?: number) => {
  if (value === undefined || value === null) {
    return '-'
  }

  if (value < 1024) {
    return `${value} B`
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }

  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

const getUserLabel = (item: CropImage) => {
  return item.nickname || item.username || (item.userId ? `用户${item.userId}` : '-')
}

const mapCropImageToRow = (item: CropImage): PhonePictureRow => {
  return {
    id: item.id ?? Date.now(),
    imageText: getImageText(item),
    rawImageUrl: item.imageUrl,
    imageUrl: resolveImageUrl(item.imageUrl),
    imageSize: formatBytes(item.imageSize),
    userLabel: getUserLabel(item),
    remark: item.remark || '-',
    createTime: item.createTime || '-',
    updateTime: item.updateTime || '-',
  }
}

const loadUsers = async () => {
  const result = await listSmartPlantUsers({ pageNum: 1, pageSize: 1000, status: 1 })
  userOptions.value = result.data.list
}

const formatUserLabel = (user: SmartPlantUser) => {
  return `${user.nickname || user.username || '用户'}${user.username ? ` (${user.username})` : ''}`
}

const fetchImageStats = async () => {
  const result = await listCropImages({ pageNum: 1, pageSize: 10000 })
  const allImages = result.data.list
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const todayImages = allImages.filter((item) => isSameDate(item.createTime, today))
  const yesterdayImages = allImages.filter((item) => isSameDate(item.createTime, yesterday))
  const allUploaderIds = new Set(allImages.map((item) => item.userId).filter(Boolean))
  const todayUploaderIds = new Set(todayImages.map((item) => item.userId).filter(Boolean))
  const yesterdayUploaderIds = new Set(yesterdayImages.map((item) => item.userId).filter(Boolean))
  const totalSize = allImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)
  const todaySize = todayImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)
  const yesterdaySize = yesterdayImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)

  imageStats.value = {
    total: result.data.total,
    uploaderCount: allUploaderIds.size,
    totalSize,
    todayAdded: todayImages.length,
    totalTrend: todayImages.length - yesterdayImages.length,
    uploaderTrend: todayUploaderIds.size - yesterdayUploaderIds.size,
    sizeTrend: todaySize - yesterdaySize,
    todayTrend: todayImages.length - yesterdayImages.length,
  }
}

const fetchCropImages = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCropImages({
      userId: searchForm.userId,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      pageNum,
      pageSize: size,
    })

    phonePictures.value = result.data.list.map((item) => mapCropImageToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.userId = undefined
  dateRange.value = []
  void fetchCropImages(1)
}

const handleSelectionChange = (rows: PhonePictureRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editCropImageId.value = null
  addDialogVisible.value = true
}

const handleEditCropImage = (row: PhonePictureRow) => {
  editCropImageId.value = row.id
  addDialogVisible.value = true
}

const handleSaveSuccess = () => {
  void fetchCropImages(editCropImageId.value ? currentPage.value : 1)
  void fetchImageStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchCropImages(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchCropImages(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return phonePictures.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const getExtensionFromDataUrl = (value: string) => {
  const mimeType = value.match(/^data:image\/([^;]+);/i)?.[1]?.toLowerCase()
  return mimeType === 'jpeg' ? 'jpg' : mimeType
}

const getExtensionFromUrl = (value?: string) => {
  const path = value?.split(/[?#]/, 1)[0] || ''
  const extension = path.match(/\.([a-z0-9]+)$/i)?.[1]?.toLowerCase()
  return extension === 'jpeg' ? 'jpg' : extension
}

const getDownloadFileName = (row: PhonePictureRow, storedUrl = '') => {
  const extension = getExtensionFromDataUrl(storedUrl) || getExtensionFromUrl(row.rawImageUrl)
  return extension ? `phone-picture-${row.id}.${extension}` : `phone-picture-${row.id}`
}

const triggerDownload = (url: string, fileName: string) => {
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const getResponseFileName = (contentDisposition: string | undefined, fallback: string) => {
  if (!contentDisposition) {
    return fallback
  }

  const encodedName = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encodedName) {
    try {
      return decodeURIComponent(encodedName)
    } catch {
      return encodedName
    }
  }

  const quotedName = contentDisposition.match(/filename="?([^";]+)"?/i)?.[1]
  return quotedName || fallback
}

const handleDownloadCropImage = async (row: PhonePictureRow) => {
  if (isStoredImageKey(row.rawImageUrl)) {
    const storedUrl = getStoredImageUrl(row.rawImageUrl)
    if (!storedUrl) {
      ElMessage.warning('本地缓存图片不存在，无法下载')
      return
    }

    triggerDownload(storedUrl, getDownloadFileName(row, storedUrl))
    ElMessage.success('图片下载已开始')
    return
  }

  const response = await downloadCropImage(row.id)
  const blobUrl = URL.createObjectURL(response.data)

  try {
    triggerDownload(blobUrl, getResponseFileName(response.headers['content-disposition'], getDownloadFileName(row)))
    ElMessage.success('图片下载已开始')
  } finally {
    setTimeout(() => URL.revokeObjectURL(blobUrl), 1000)
  }
}

const handleDeleteCropImage = async (row: PhonePictureRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除图片 #${row.id} 吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCropImage(row.id)
    ElMessage.success('删除手机图片成功')
    void fetchCropImages(getNextPageAfterDelete(1))
    void fetchImageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的手机图片')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 张图片吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteCropImages(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除手机图片成功')
    void fetchCropImages(getNextPageAfterDelete(selectedRows.value.length))
    void fetchImageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void loadUsers()
  void fetchCropImages()
  void fetchImageStats()
})
</script>

<template>
  <div class="phone-picture-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="phone-picture-shell admin-shell">
      <Header :breadcrumbs="['首页', '图片管理', '手机图片管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="phone-picture-content admin-content">
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
            <el-form-item label="上传用户">
              <el-select v-model="searchForm.userId" placeholder="全部用户" filterable clearable>
                <el-option
                  v-for="item in userOptions"
                  :key="item.id"
                  :label="formatUserLabel(item)"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="date-filter-item" label="上传日期">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                range-separator="至"
              />
            </el-form-item>
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchCropImages(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增图片</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="phonePictures"
              row-key="id"
              class="phone-picture-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="图片预览" width="112" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="phone-thumb"
                    :src="row.imageUrl"
                    fit="contain"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                  />
                  <span v-else class="default-phone-image">{{ row.imageText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="imageSize" label="图片大小" min-width="120" align="center" header-align="center" />
              <el-table-column prop="userLabel" label="上传用户" min-width="150" align="center" header-align="center" />
              <el-table-column prop="createTime" label="上传时间" min-width="170" align="center" header-align="center" />
              <el-table-column prop="remark" label="备注" min-width="190" align="center" header-align="center" show-overflow-tooltip />
              
              <el-table-column label="操作" width="240" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditCropImage(row)">
                      编辑
                    </el-button>
                    <el-button link type="success" :icon="Download" @click="handleDownloadCropImage(row)">
                      下载
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteCropImage(row)">
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

    <AddOrUpdate v-model="addDialogVisible" :id="editCropImageId" @success="handleSaveSuccess" />
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
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(210px, 1fr));
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
.filter-form :deep(.el-select__wrapper),
.filter-form :deep(.el-date-editor) {
  height: 40px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e5e7eb inset;
}

.filter-form :deep(.el-date-editor) {
  width: 100%;
}

.date-filter-item {
  grid-column: span 2;
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

.phone-picture-table {
  width: 100%;
}

.phone-picture-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.phone-picture-table :deep(.el-table__row) {
  height: 64px;
}

.phone-picture-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.phone-thumb {
  width: 42px;
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.default-phone-image {
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
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.22);
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

  .date-filter-item {
    grid-column: span 2;
  }

  .manage-actions {
    position: static;
    width: fit-content;
  }
}

@media (max-width: 960px) {
  .phone-picture-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .phone-picture-content {
    padding: 12px;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: 1fr;
  }

  .date-filter-item {
    grid-column: span 1;
  }

  .stat-card {
    min-height: 96px;
    padding: 18px;
  }
}
</style>
