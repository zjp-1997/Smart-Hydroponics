<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Bottom,
  Box,
  CirclePlus,
  Coin,
  Delete,
  EditPen,
  Refresh,
  Search,
  Top,
} from '@element-plus/icons-vue'
import {
  addWarehouseRecord,
  batchDeleteWarehouseItems,
  deleteWarehouseItem,
  getWarehouseStats,
  listWarehouseItems,
  listWarehouseFaultOptions,
  updateWarehouseItemStatus,
  type WarehouseItem,
  type WarehouseRecordCreatePayload,
  type WarehouseStats,
  type WarehouseFaultOption,
} from '@/api/warehouse'
import AddOrUpdate from './AddOrUpdate.vue'
import { getCurrentUser } from '@/utils/auth'
import { getFileUrl } from '@/utils/utils'
import AdminPageFrame from '@/components/admin/AdminPageFrame.vue'
import ListFilterPanel from '@/components/admin/ListFilterPanel.vue'
import ListPagination from '@/components/admin/ListPagination.vue'

type ItemStatus = 'enabled' | 'disabled'

interface WarehouseRow {
  id: number
  imageText: string
  imageUrl?: string
  itemName: string
  itemCode: string
  farmOwnerName: string
  inboundOperatorName: string
  latestOutboundOperatorName: string
  latestOutboundRecipient: string
  category: number
  categoryName: string
  categoryTone: string
  specification: string
  unit: string
  stockQty: number
  warningQty: number
  manufacturer: string
  status: ItemStatus
  remark: string
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

const categoryMap: Record<number, string> = {
  1: '种子',
  2: '肥料',
  3: '农药',
  4: '工具',
  5: '设备耗材',
  6: '其他',
}

const categoryToneMap: Record<number, string> = {
  1: 'green',
  2: 'orange',
  3: 'blue',
  4: 'cyan',
  5: 'purple',
  6: 'gray',
}

const selectedRows = ref<WarehouseRow[]>([])
const rows = ref<WarehouseRow[]>([])
const tableLoading = ref(false)
const itemDialogVisible = ref(false)
const outboundDialogVisible = ref(false)
const editingItemId = ref<number | null>(null)
const currentOutboundItem = ref<WarehouseRow | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const statusLoadingIds = ref<number[]>([])
const outboundFormRef = ref<FormInstance>()
const faultOptions = ref<WarehouseFaultOption[]>([])
const faultOptionsLoading = ref(false)
const outboundSubmitting = ref(false)
const stats = ref<WarehouseStats>({
  totalItems: 0,
  stockQuantity: 0,
  inboundQuantity: 0,
  outboundQuantity: 0,
})

const searchForm = reactive({
  itemName: '',
  category: undefined as number | undefined,
  status: '' as '' | 'enabled' | 'disabled',
})

const outboundForm = reactive<WarehouseRecordCreatePayload>({
  requestId: '',
  itemId: 0,
  recordType: 2,
  quantity: 1,
  recipient: '',
  relatedFaultId: undefined,
  price: undefined,
  supplier: '',
  remark: '',
})

const outboundRules: FormRules<WarehouseRecordCreatePayload> = {
  quantity: [{ required: true, type: 'number', min: 0.01, message: '出库数量必须大于0', trigger: 'change' }],
  recipient: [{ required: true, whitespace: true, message: '请输入领用人或接收方', trigger: 'blur' }],
  relatedFaultId: [{ required: true, message: '请选择本次出库对应的设备故障', trigger: 'change' }],
}

const outboundOperatorName = computed(() => {
  const user = getCurrentUser()
  return user?.nickname || user?.username || '当前登录用户'
})

const faultStatusLabel = (status?: number) => {
  return ['待处理', '处理中', '已处理', '已关闭'][status ?? 0] || '未知状态'
}

const faultOptionLabel = (fault: WarehouseFaultOption) => {
  return `[${fault.faultCode}] ${fault.faultName} · ${fault.deviceName || '未知设备'} · ${faultStatusLabel(fault.status)}`
}

const statCards = computed<StatCard[]>(() => [
  {
    title: '总物资数量',
    value: stats.value.totalItems.toLocaleString(),
    desc: '库存档案',
    trend: `${stats.value.totalItems.toLocaleString()} 类`,
    trendType: 'up',
    icon: Box,
    tone: 'blue',
  },
  {
    title: '当前库存数量',
    value: formatQuantity(stats.value.stockQuantity),
    desc: '实时结存',
    trend: formatQuantity(stats.value.stockQuantity),
    trendType: 'up',
    icon: Coin,
    tone: 'green',
  },
  {
    title: '入库数量',
    value: formatQuantity(stats.value.inboundQuantity),
    desc: '累计入库',
    trend: formatQuantity(stats.value.inboundQuantity),
    trendType: 'up',
    icon: Bottom,
    tone: 'orange',
  },
  {
    title: '出库数量',
    value: formatQuantity(stats.value.outboundQuantity),
    desc: '累计出库',
    trend: formatQuantity(stats.value.outboundQuantity),
    trendType: 'down',
    icon: Top,
    tone: 'gray',
  },
])

const formatQuantity = (value?: number) => {
  return Number(value || 0).toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })
}

const getNumberStatus = (value: '' | 'enabled' | 'disabled') => {
  if (value === 'enabled') {
    return 1
  }

  if (value === 'disabled') {
    return 0
  }

  return undefined
}

const createRequestId = () =>
  globalThis.crypto?.randomUUID?.() ?? `warehouse-${Date.now()}-${Math.random().toString(36).slice(2)}`

const defaultImageUrl = (category?: number) => {
  switch (category) {
    case 1:
      return '/warehouse-images/seed.svg'
    case 2:
      return '/warehouse-images/fertilizer.svg'
    case 3:
      return '/warehouse-images/pesticide.svg'
    case 4:
      return '/warehouse-images/tool.svg'
    case 5:
      return '/warehouse-images/consumable.svg'
    default:
      return '/warehouse-images/other.svg'
  }
}

/** 静态分类图由前端提供，上传图片则补全 smart_plant 服务地址。 */
const resolveImageUrl = (imageUrl?: string, category?: number) => {
  const url = imageUrl || defaultImageUrl(category)
  return url.startsWith('/warehouse-images/') ? url : getFileUrl(url)
}

const mapItemToRow = (item: WarehouseItem): WarehouseRow => ({
  id: item.id ?? Date.now(),
  imageText: item.itemName?.trim().slice(0, 1) || '物',
  imageUrl: resolveImageUrl(item.imageUrl, item.category),
  itemName: item.itemName || '-',
  itemCode: item.itemCode || '-',
  farmOwnerName: item.farmOwnerName || '未分配',
  inboundOperatorName: item.inboundOperatorName || '-',
  latestOutboundOperatorName: item.latestOutboundOperatorName || '-',
  latestOutboundRecipient: item.latestOutboundRecipient || '-',
  category: item.category,
  categoryName: categoryMap[item.category] || '-',
  categoryTone: categoryToneMap[item.category] || 'gray',
  specification: item.specification || '-',
  unit: item.unit || '-',
  stockQty: Number(item.stockQty || 0),
  warningQty: Number(item.warningQty || 0),
  manufacturer: item.manufacturer || '-',
  status: item.status === 0 ? 'disabled' : 'enabled',
  remark: item.remark || '-',
  createTime: item.createTime || '-',
})

const fetchStats = async () => {
  const result = await getWarehouseStats()
  stats.value = result.data
}

const fetchItems = async (page = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true

  try {
    const result = await listWarehouseItems({
      itemName: searchForm.itemName || undefined,
      category: searchForm.category,
      status: getNumberStatus(searchForm.status),
      pageNum: page,
      pageSize: size,
    })

    rows.value = result.data.list.map((item) => mapItemToRow(item))
    total.value = result.data.total
    currentPage.value = result.data.pageNum
    pageSize.value = result.data.pageSize
    selectedRows.value = []
  } finally {
    tableLoading.value = false
  }
}

const refreshPage = () => {
  void fetchItems(currentPage.value)
  void fetchStats()
}

const resetSearch = () => {
  searchForm.itemName = ''
  searchForm.category = undefined
  searchForm.status = ''
  void fetchItems(1)
}

const openInboundDialog = () => {
  editingItemId.value = null
  itemDialogVisible.value = true
}

const openEditDialog = (row: WarehouseRow) => {
  editingItemId.value = row.id
  itemDialogVisible.value = true
}

const handleItemSuccess = () => {
  void fetchItems(editingItemId.value ? currentPage.value : 1)
  void fetchStats()
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return rows.value.length <= deletedCount && currentPage.value > 1 ? currentPage.value - 1 : currentPage.value
}

const handleSelectionChange = (selection: WarehouseRow[]) => {
  selectedRows.value = selection
}

const removeItem = async (row: WarehouseRow) => {
  try {
    await ElMessageBox.confirm(`确定删除物资「${row.itemName}」吗？物资将从列表移除，历史出入库流水仍会保留。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteWarehouseItem(row.id)
    ElMessage.success('删除物资成功')
    void fetchItems(getNextPageAfterDelete(1))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的物资')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条物资吗？物资将从列表移除，历史出入库流水仍会保留。`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await batchDeleteWarehouseItems(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除物资成功')
    void fetchItems(getNextPageAfterDelete(selectedRows.value.length))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const toggleItemStatus = async (row: WarehouseRow) => {
  const status = row.status === 'disabled' ? 1 : 0
  setStatusUpdating(row.id, true)

  try {
    await updateWarehouseItemStatus(row.id, status)
    ElMessage.success(status === 1 ? '启用物资成功' : '停用物资成功')
    void fetchItems(currentPage.value)
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const openOutboundDialog = async (row: WarehouseRow) => {
  currentOutboundItem.value = row
  Object.assign(outboundForm, {
    requestId: createRequestId(),
    itemId: row.id,
    recordType: 2,
    quantity: 1,
    recipient: '',
    relatedFaultId: undefined,
    price: undefined,
    supplier: '',
    remark: '',
  })
  outboundFormRef.value?.clearValidate()
  faultOptions.value = []
  outboundDialogVisible.value = true
  faultOptionsLoading.value = true
  try {
    const result = await listWarehouseFaultOptions(row.id)
    faultOptions.value = result.data
  } finally {
    faultOptionsLoading.value = false
  }
}

const submitOutbound = async () => {
  await outboundFormRef.value?.validate()
  outboundSubmitting.value = true
  try {
    await addWarehouseRecord(outboundForm)
    ElMessage.success('物资出库成功')
    outboundDialogVisible.value = false
    refreshPage()
  } finally {
    outboundSubmitting.value = false
  }
}

const handleCurrentPageChange = (page: number) => {
  void fetchItems(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchItems(1, size)
}

onMounted(() => {
  void fetchItems()
  void fetchStats()
})
</script>

<template>
  <AdminPageFrame
    :breadcrumbs="['首页', '仓库管理', '仓库管理']"
    page-class="warehouse-page"
    shell-class="warehouse-shell"
    content-class="warehouse-content"
  >
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

        <ListFilterPanel :model="searchForm">
            <el-form-item label="物资名称">
              <el-input v-model="searchForm.itemName" placeholder="请输入物资名称" clearable />
            </el-form-item>
            <el-form-item label="分类">
              <el-select v-model="searchForm.category" placeholder="全部分类" clearable>
                <el-option v-for="(label, value) in categoryMap" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
            <template #actions>
              <el-button type="primary" :icon="Search" @click="fetchItems(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </template>
            <template #manage>
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">
                批量删除
              </el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openInboundDialog">新增物资</el-button>
            </template>
        </ListFilterPanel>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="rows"
              row-key="id"
              class="warehouse-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column label="图片" width="100" align="center" header-align="center">
                <template #default="{ row }">
                  <el-image
                    v-if="row.imageUrl"
                    class="item-thumb"
                    :src="row.imageUrl"
                    fit="cover"
                    :preview-src-list="[row.imageUrl]"
                    preview-teleported
                  />
                  <span v-else class="default-item-image" :class="`mark-${row.categoryTone}`">{{ row.imageText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="itemName" label="物资名称" min-width="150" align="center" header-align="center" />
              <el-table-column prop="farmOwnerName" label="所属农场主" min-width="130" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="分类" min-width="150" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag :class="`category-${row.categoryTone}`" effect="light" round>{{ row.categoryName }}</el-tag>
                </template>
              </el-table-column>
              <!-- <el-table-column prop="itemCode" label="物资编码" min-width="170" align="center" header-align="center" show-overflow-tooltip /> -->
              <el-table-column prop="inboundOperatorName" label="入库人" min-width="130" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="latestOutboundOperatorName" label="最近出库人" min-width="140" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="latestOutboundRecipient" label="最近领用人" min-width="140" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column prop="specification" label="规格型号" min-width="130" align="center" header-align="center" />
              <el-table-column label="库存数量" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <span :class="{ 'warning-stock': row.stockQty <= row.warningQty }">{{ formatQuantity(row.stockQty) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="warningQty" label="预警数量" min-width="120" align="center" header-align="center" />
              <el-table-column prop="unit" label="单位" min-width="90" align="center" header-align="center" />
              <el-table-column prop="manufacturer" label="生产厂家" min-width="150" align="center" header-align="center" show-overflow-tooltip />
              <el-table-column label="状态" min-width="140" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="status-cell">
                    <el-switch
                      :model-value="row.status === 'enabled'"
                      :loading="isStatusUpdating(row.id)"
                      @change="() => toggleItemStatus(row)"
                    />
                    <span>{{ row.status === 'enabled' ? '启用' : '禁用' }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="180" align="center" header-align="center" />
              <el-table-column label="操作" width="330" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="openEditDialog(row)">
                      编辑
                    </el-button>
                    <el-button link type="primary" :icon="Top" :disabled="row.stockQty <= 0" @click="openOutboundDialog(row)">
                      出库
                    </el-button>
                    <el-button
                      link
                      :type="row.status === 'enabled' ? 'warning' : 'success'"
                      :loading="isStatusUpdating(row.id)"
                      @click="toggleItemStatus(row)"
                    >
                      {{ row.status === 'enabled' ? '禁用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" :icon="Delete" @click="removeItem(row)">
                      删除
                    </el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <ListPagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            @current-change="handleCurrentPageChange"
            @size-change="handlePageSizeChange"
          />
        </section>

    <template #overlay>
      <AddOrUpdate v-model="itemDialogVisible" :id="editingItemId" @success="handleItemSuccess" />

    <el-dialog v-model="outboundDialogVisible" title="物资出库" width="min(620px, calc(100vw - 24px))">
      <el-form ref="outboundFormRef" :model="outboundForm" :rules="outboundRules" label-width="96px">
        <el-form-item label="物资名称">
          <el-input :model-value="currentOutboundItem?.itemName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :model-value="`${formatQuantity(currentOutboundItem?.stockQty)} ${currentOutboundItem?.unit || ''}`" disabled />
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="outboundForm.quantity" :min="0.01" :max="currentOutboundItem?.stockQty || undefined" />
        </el-form-item>
        <el-form-item label="出库人">
          <el-input :model-value="outboundOperatorName" disabled />
        </el-form-item>
        <el-form-item label="关联故障" prop="relatedFaultId">
          <el-select
            v-model="outboundForm.relatedFaultId"
            :loading="faultOptionsLoading"
            filterable
            clearable
            placeholder="请选择物资使用对应的设备故障"
            no-data-text="暂无可关联故障，请先创建设备故障"
          >
            <el-option
              v-for="fault in faultOptions"
              :key="fault.id"
              :label="faultOptionLabel(fault)"
              :value="fault.id"
            />
          </el-select>
          <span class="outbound-help">用于追溯物资实际使用的设备、故障及处理人</span>
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="outboundForm.price" :min="0" />
        </el-form-item>
        <el-form-item label="给谁了" prop="recipient">
          <el-input v-model="outboundForm.recipient" maxlength="100" placeholder="请输入领用人或接收方" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="outboundForm.remark" type="textarea" :rows="3" placeholder="请输入出库说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="outboundSubmitting" @click="submitOutbound">确认出库</el-button>
      </template>
      </el-dialog>
    </template>
  </AdminPageFrame>
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

.warehouse-table {
  width: 100%;
}

.warehouse-table :deep(.el-table__header th) {
  height: 52px;
  color: #334155;
  background: #fafcff;
  font-size: 13px;
  font-weight: 800;
}

.warehouse-table :deep(.el-table__row) {
  height: 64px;
}

.warehouse-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
}

.item-thumb,
.default-item-image {
  width: 42px;
  height: 42px;
  border-radius: 6px;
  box-shadow: 0 6px 12px rgba(15, 23, 42, 0.1);
}

.item-thumb {
  display: inline-block;
  overflow: hidden;
  background: #f8fafc;
}

.default-item-image {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  line-height: 1;
  text-transform: none;
}

.mark-blue {
  background: #409eff;
}

.mark-green {
  background: #10b981;
}

.mark-purple {
  background: #7c3aed;
}

.mark-orange {
  background: #f59e0b;
}

.mark-cyan {
  background: #0891b2;
}

.mark-gray {
  background: #64748b;
}

.category-blue {
  color: #2563eb;
  background: #eff6ff;
  border-color: #dbeafe;
}

.category-green {
  color: #059669;
  background: #ecfdf5;
  border-color: #d1fae5;
}

.category-purple {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #ede9fe;
}

.category-orange {
  color: #d97706;
  background: #fffbeb;
  border-color: #fef3c7;
}

.category-cyan {
  color: #0891b2;
  background: #ecfeff;
  border-color: #cffafe;
}

.category-gray {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.warning-stock {
  color: #d97706;
  font-weight: 800;
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

:deep(.el-dialog .el-select),
:deep(.el-dialog .el-input-number) {
  width: 100%;
}

.outbound-help {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 720px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
