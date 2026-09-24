<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Download,
  MapLocation,
  Picture,
  Refresh,
  Search,
  UploadFilled,
  VideoCamera,
} from '@element-plus/icons-vue'
import {
  batchDeleteCameraImages,
  deleteCameraImage,
  downloadCameraImage,
  listCameraImages,
  type CameraImage,
} from '@/api/cameraImage'
import { listCameraDevices, type CameraDevice } from '@/api/cameraDevice'
import { listPlots, type Plot } from '@/api/plot'
import { getFileUrl } from '@/utils/utils'

interface CameraPictureRow {
  id: number
  imageText: string
  rawImageUrl: string
  imageUrl?: string
  imageSize: string
  cameraLabel: string
  plotLabel: string
  remark: string
  captureTime: string
  createTime: string
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
const selectedRows = ref<CameraPictureRow[]>([])
const cameraPictures = ref<CameraPictureRow[]>([])
const cameraOptions = ref<CameraDevice[]>([])
const plotOptions = ref<Plot[]>([])
const tableLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dateRange = ref<[string, string] | []>([])

const CAMERA_IMAGE_STORAGE_PREFIX = 'smart_farm_camera_picture:'

const imageStats = ref({
  total: 0,
  todayAdded: 0,
  cameraCount: 0,
  totalSize: 0,
  totalTrend: 0,
  todayTrend: 0,
  cameraTrend: 0,
  sizeTrend: 0,
})

const searchForm = reactive({
  cameraId: undefined as number | undefined,
  plotId: undefined as number | undefined,
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
    title: '关联摄像头',
    value: imageStats.value.cameraCount.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(imageStats.value.cameraTrend),
    trendType: imageStats.value.cameraTrend >= 0 ? 'up' : 'down',
    icon: VideoCamera,
    tone: 'orange',
  },
  {
    title: '累计容量',
    value: formatBytes(imageStats.value.totalSize),
    desc: '较昨日',
    trend: formatBytesTrend(imageStats.value.sizeTrend),
    trendType: imageStats.value.sizeTrend >= 0 ? 'up' : 'down',
    icon: Picture,
    tone: 'gray',
  },
])

const closeSidebar = () => {
  sidebarVisible.value = false
}

const formatTrend = (value: number) => `${value >= 0 ? '+' : '-'}${Math.abs(value).toLocaleString()}`

const formatBytesTrend = (value: number) => `${value >= 0 ? '+' : '-'}${formatBytes(Math.abs(value))}`

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

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(CAMERA_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const isStoredImageKey = (value?: string) => Boolean(value?.startsWith(CAMERA_IMAGE_STORAGE_PREFIX))

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue
  }

  return getStoredImageUrl(normalizedValue)
}

const getImageText = (item: CameraImage) => (item.id ? String(item.id).slice(-1) : '图')

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

const getCameraLabel = (item: CameraImage) => item.cameraName || (item.cameraId ? `摄像头${item.cameraId}` : '-')

const getPlotLabel = (item: CameraImage) => {
  const plotName = item.plotName || (item.plotId ? `地块${item.plotId}` : '-')
  return item.plotCode ? `${plotName}（${item.plotCode}）` : plotName
}

const mapCameraImageToRow = (item: CameraImage): CameraPictureRow => ({
  id: item.id ?? Date.now(),
  imageText: getImageText(item),
  rawImageUrl: item.imageUrl,
  imageUrl: resolveImageUrl(item.imageUrl),
  imageSize: formatBytes(item.imageSize),
  cameraLabel: getCameraLabel(item),
  plotLabel: getPlotLabel(item),
  remark: item.remark || '-',
  captureTime: item.captureTime || '-',
  createTime: item.createTime || '-',
})

const formatCameraLabel = (camera: CameraDevice) => {
  const plot = camera.plotCode ? `${camera.plotName}（${camera.plotCode}）` : camera.plotName
  return plot ? `${camera.name} / ${plot}` : camera.name
}

const formatPlotLabel = (plot: Plot) => (plot.plotCode ? `${plot.plotName}（${plot.plotCode}）` : plot.plotName)

const loadFilterOptions = async () => {
  const [cameraResult, plotResult] = await Promise.all([
    listCameraDevices({ pageNum: 1, pageSize: 1000 }),
    listPlots({ pageNum: 1, pageSize: 1000, status: 1 }),
  ])
  cameraOptions.value = cameraResult.data.list
  plotOptions.value = plotResult.data.list
}

const fetchImageStats = async () => {
  const result = await listCameraImages({ pageNum: 1, pageSize: 10000 })
  const allImages = result.data.list
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const todayImages = allImages.filter((item) => isSameDate(item.captureTime || item.createTime, today))
  const yesterdayImages = allImages.filter((item) => isSameDate(item.captureTime || item.createTime, yesterday))
  const allCameraIds = new Set(allImages.map((item) => item.cameraId).filter(Boolean))
  const todayCameraIds = new Set(todayImages.map((item) => item.cameraId).filter(Boolean))
  const yesterdayCameraIds = new Set(yesterdayImages.map((item) => item.cameraId).filter(Boolean))
  const totalSize = allImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)
  const todaySize = todayImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)
  const yesterdaySize = yesterdayImages.reduce((sum, item) => sum + (item.imageSize || 0), 0)

  imageStats.value = {
    total: result.data.total,
    todayAdded: todayImages.length,
    cameraCount: allCameraIds.size,
    totalSize,
    totalTrend: todayImages.length - yesterdayImages.length,
    todayTrend: todayImages.length - yesterdayImages.length,
    cameraTrend: todayCameraIds.size - yesterdayCameraIds.size,
    sizeTrend: todaySize - yesterdaySize,
  }
}

const fetchCameraImages = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listCameraImages({
      cameraId: searchForm.cameraId,
      plotId: searchForm.plotId,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      pageNum,
      pageSize: size,
    })

    cameraPictures.value = result.data.list.map((item) => mapCameraImageToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const resetSearch = () => {
  searchForm.cameraId = undefined
  searchForm.plotId = undefined
  dateRange.value = []
  void fetchCameraImages(1)
}

const handleSelectionChange = (rows: CameraPictureRow[]) => {
  selectedRows.value = rows
}

const handleCurrentPageChange = (page: number) => {
  void fetchCameraImages(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchCameraImages(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) =>
  cameraPictures.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value

const getExtensionFromDataUrl = (value: string) => {
  const mimeType = value.match(/^data:image\/([^;]+);/i)?.[1]?.toLowerCase()
  return mimeType === 'jpeg' ? 'jpg' : mimeType
}

const getExtensionFromUrl = (value?: string) => {
  const path = value?.split(/[?#]/, 1)[0] || ''
  const extension = path.match(/\.([a-z0-9]+)$/i)?.[1]?.toLowerCase()
  return extension === 'jpeg' ? 'jpg' : extension
}

const getDownloadFileName = (row: CameraPictureRow, storedUrl = '') => {
  const extension = getExtensionFromDataUrl(storedUrl) || getExtensionFromUrl(row.rawImageUrl)
  return extension ? `camera-picture-${row.id}.${extension}` : `camera-picture-${row.id}`
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

const handleDownloadCameraImage = async (row: CameraPictureRow) => {
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

  const response = await downloadCameraImage(row.id)
  const blobUrl = URL.createObjectURL(response.data)

  try {
    triggerDownload(blobUrl, getResponseFileName(response.headers['content-disposition'], getDownloadFileName(row)))
    ElMessage.success('图片下载已开始')
  } finally {
    setTimeout(() => URL.revokeObjectURL(blobUrl), 1000)
  }
}

const handleDeleteCameraImage = async (row: CameraPictureRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除摄像头图片 #${row.id} 吗？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteCameraImage(row.id)
    ElMessage.success('删除摄像头图片成功')
    void fetchCameraImages(getNextPageAfterDelete(1))
    void fetchImageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的摄像头图片')
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

    await batchDeleteCameraImages(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除摄像头图片成功')
    void fetchCameraImages(getNextPageAfterDelete(selectedRows.value.length))
    void fetchImageStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

onMounted(() => {
  void loadFilterOptions()
  void fetchCameraImages()
  void fetchImageStats()
})
</script>

<template>
  <div class="camera-picture-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="camera-picture-shell admin-shell">
      <Header :breadcrumbs="['首页', '图片管理', '摄像头图片管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="camera-picture-content admin-content">
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
            <el-form-item label="摄像头">
              <el-select v-model="searchForm.cameraId" placeholder="全部摄像头" filterable clearable>
                <el-option
                  v-for="item in cameraOptions"
                  :key="item.id"
                  :label="formatCameraLabel(item)"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="所属地块">
              <el-select v-model="searchForm.plotId" placeholder="全部地块" filterable clearable>
                <el-option
                  v-for="item in plotOptions"
                  :key="item.id"
                  :label="formatPlotLabel(item)"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="date-filter-item" label="拍摄日期">
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
              <el-button type="primary" :icon="Search" @click="fetchCameraImages(1)">查询</el-button>
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
              :data="cameraPictures"
              row-key="id"
              class="camera-picture-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="图片" width="112" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="camera-thumb"
                    :src="row.imageUrl"
                    fit="contain"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                  />
                  <span v-else class="default-camera-image">{{ row.imageText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="imageSize" label="图片大小" min-width="120" align="center" header-align="center" />
              <el-table-column prop="captureTime" label="拍摄时间" min-width="170" align="center" header-align="center" />
              <el-table-column prop="remark" label="备注" min-width="190" align="center" header-align="center" show-overflow-tooltip />

              <el-table-column label="操作" width="170" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="success" :icon="Download" @click="handleDownloadCameraImage(row)">
                      下载
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteCameraImage(row)">
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
  grid-template-columns: minmax(190px, 1fr) minmax(190px, 1fr) minmax(300px, 1.35fr) auto;
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
  min-width: 300px;
}

.filter-actions,
.manage-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  white-space: nowrap;
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

.camera-picture-table {
  width: 100%;
}

.camera-picture-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.camera-picture-table :deep(.el-table__row) {
  height: 64px;
}

.camera-picture-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.camera-thumb {
  width: 42px;
  height: 42px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.default-camera-image {
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

.plot-cell {
  display: inline-flex;
  align-items: center;
  gap: 5px;
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
    min-width: 0;
  }

  .manage-actions {
    position: static;
    width: fit-content;
  }
}

@media (max-width: 960px) {
  .camera-picture-shell {
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .camera-picture-content {
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
