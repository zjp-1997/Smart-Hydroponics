<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  CircleCheck,
  CircleCloseFilled,
  CirclePlus,
  Delete,
  EditPen,
  Refresh,
  Search,
  Tools,
  UserFilled,
  WarningFilled,
} from '@element-plus/icons-vue'
import {
  acceptIotDeviceFaultAssignment,
  assignIotDeviceFault,
  batchDeleteIotDeviceFaults,
  deleteIotDeviceFault,
  listIotDeviceFaults,
  rejectIotDeviceFaultAssignment,
  updateIotDeviceFaultStatus,
  type IotDeviceFault,
} from '@/api/iotDeviceFault'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getCurrentRoleCode, getCurrentUserId, isAdminUser } from '@/utils/auth'
import AddOrUpdate from './AddOrUpdate.vue'

interface StatCard {
  title: string
  value: string
  desc: string
  trend: string
  trendType: 'up' | 'down'
  icon: Component
  tone: string
}

interface FaultRow {
  id: number
  deviceName: string
  deviceCode: string
  plotName: string
  faultCode: string
  faultName: string
  faultType: string
  faultTypeTone: string
  severity: string
  severityTone: string
  status: string
  statusValue: number
  statusTone: string
  assignStatus: number
  handleUser: string
  handleUserId?: number
  assigned: boolean
  assignmentRejected: boolean
  canRespondAssignment: boolean
  startTime: string
  endTime: string
  handleTime: string
  duration: string
  faultDesc: string
  handleResult: string
}

interface AssignFormModel {
  id?: number
  handleUserId?: number
}

interface FaultStats {
  total: number
  pending: number
  processing: number
  handled: number
  totalTrend: number
  pendingTrend: number
  processingTrend: number
  handledTrend: number
}

const sidebarVisible = ref(false)
const selectedRows = ref<FaultRow[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableLoading = ref(false)
const dialogVisible = ref(false)
const assignVisible = ref(false)
const editFaultId = ref<number | null>(null)
const assignFormRef = ref<FormInstance>()
const statusLoadingIds = ref<number[]>([])
const assignLoading = ref(false)
const faults = ref<FaultRow[]>([])
const technicianOptions = ref<SmartPlantUser[]>([])
const isAdmin = computed(() => isAdminUser())
const currentUserId = computed(() => getCurrentUserId())
const isTechnician = computed(() => getCurrentRoleCode() === 'technician')

const faultStats = ref<FaultStats>({
  total: 0,
  pending: 0,
  processing: 0,
  handled: 0,
  totalTrend: 0,
  pendingTrend: 0,
  processingTrend: 0,
  handledTrend: 0,
})

const searchForm = ref({
  deviceName: '',
  faultName: '',
  faultType: undefined as number | undefined,
  severity: undefined as number | undefined,
  status: undefined as number | undefined,
})

const assignForm = ref<AssignFormModel>({
  id: undefined,
  handleUserId: undefined,
})

const assignRules = computed<FormRules<AssignFormModel>>(() => ({
  handleUserId: [{ required: true, message: '请选择处理人员', trigger: 'change' }],
}))

const faultTypeOptions = [
  { label: '通信故障', value: 1, tone: 'blue' },
  { label: '传感器故障', value: 2, tone: 'amber' },
  { label: '电源故障', value: 3, tone: 'red' },
  { label: '执行器故障', value: 4, tone: 'violet' },
  { label: '数据异常', value: 5, tone: 'cyan' },
  { label: '其他故障', value: 6, tone: 'gray' },
]

const severityOptions = [
  { label: '低', value: 1, tone: 'green' },
  { label: '中', value: 2, tone: 'blue' },
  { label: '高', value: 3, tone: 'amber' },
  { label: '严重', value: 4, tone: 'red' },
]

const statusOptions = [
  { label: '待处理', value: 0, tone: 'red' },
  { label: '处理中', value: 1, tone: 'amber' },
  { label: '已处理', value: 2, tone: 'green' },
  { label: '已关闭', value: 3, tone: 'gray' },
]

const statCards = computed<StatCard[]>(() => [
  {
    title: '故障总数',
    value: faultStats.value.total.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(faultStats.value.totalTrend),
    trendType: faultStats.value.totalTrend >= 0 ? 'up' : 'down',
    icon: WarningFilled,
    tone: 'red',
  },
  {
    title: '待处理',
    value: faultStats.value.pending.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(faultStats.value.pendingTrend),
    trendType: faultStats.value.pendingTrend >= 0 ? 'up' : 'down',
    icon: CircleCloseFilled,
    tone: 'orange',
  },
  {
    title: '处理中',
    value: faultStats.value.processing.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(faultStats.value.processingTrend),
    trendType: faultStats.value.processingTrend >= 0 ? 'up' : 'down',
    icon: Tools,
    tone: 'blue',
  },
  {
    title: '已处理',
    value: faultStats.value.handled.toLocaleString(),
    desc: '较昨日',
    trend: formatTrend(faultStats.value.handledTrend),
    trendType: faultStats.value.handledTrend >= 0 ? 'up' : 'down',
    icon: CircleCheck,
    tone: 'green',
  },
])

const formatTrend = (value: number) => `${value >= 0 ? '↑' : '↓'} ${Math.abs(value)}`

const isSameDate = (value: string | undefined, target: Date) => {
  if (!value) return false

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

const getTypeMeta = (value?: number) => faultTypeOptions.find((item) => item.value === value)

const getSeverityMeta = (value?: number) => severityOptions.find((item) => item.value === value)

const getStatusMeta = (value?: number) => statusOptions.find((item) => item.value === value)

const mapFaultToRow = (fault: IotDeviceFault): FaultRow => {
  const typeMeta = getTypeMeta(fault.faultType)
  const severityMeta = getSeverityMeta(fault.severity)
  const statusMeta = getStatusMeta(fault.status)
  const assignStatus = fault.assignStatus ?? (fault.handleUserId ? 1 : 0)
  const assigned = Boolean(fault.handleUserId) && assignStatus !== 0
  const assignmentRejected = assignStatus === 3

  return {
    id: fault.id ?? Date.now(),
    deviceName: fault.deviceName || '-',
    deviceCode: fault.deviceCode || '-',
    plotName: fault.plotName || '-',
    faultCode: fault.faultCode || '-',
    faultName: fault.faultName || '-',
    faultType: typeMeta?.label || '-',
    faultTypeTone: typeMeta?.tone || 'gray',
    severity: severityMeta?.label || '-',
    severityTone: severityMeta?.tone || 'gray',
    status: statusMeta?.label || '-',
    statusValue: fault.status ?? 0,
    statusTone: statusMeta?.tone || 'gray',
    assignStatus,
    handleUser: fault.handleNickname || fault.handleUsername || '未分配',
    handleUserId: fault.handleUserId,
    assigned,
    assignmentRejected,
    canRespondAssignment:
      isTechnician.value &&
      Boolean(fault.handleUserId) &&
      fault.handleUserId === currentUserId.value &&
      assignStatus === 1 &&
      (fault.status === 0 || fault.status === 1),
    startTime: fault.startTime || '-',
    endTime: fault.endTime || '-',
    handleTime: fault.handleTime || '-',
    duration: fault.duration === undefined || fault.duration === null ? '-' : `${fault.duration} 分钟`,
    faultDesc: fault.faultDesc || '-',
    handleResult: fault.handleResult || '-',
  }
}

const loadTechnicians = async () => {
  if (!isAdmin.value) {
    technicianOptions.value = []
    return
  }

  const result = await listSmartPlantUsers({ pageNum: 1, pageSize: 10000, status: 1 })
  technicianOptions.value = result.data.list.filter(
    (item) => item.roleCode === 'technician' || item.roleName === '技术人员',
  )
}

const fetchStats = async () => {
  const [allResult, pendingResult, processingResult, handledResult] = await Promise.all([
    listIotDeviceFaults({ pageNum: 1, pageSize: 10000 }),
    listIotDeviceFaults({ pageNum: 1, pageSize: 1, status: 0 }),
    listIotDeviceFaults({ pageNum: 1, pageSize: 1, status: 1 }),
    listIotDeviceFaults({ pageNum: 1, pageSize: 1, status: 2 }),
  ])

  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)

  const allFaults = allResult.data.list
  const todayFaults = allFaults.filter((item) => isSameDate(item.createTime, today))
  const yesterdayFaults = allFaults.filter((item) => isSameDate(item.createTime, yesterday))

  faultStats.value = {
    total: allResult.data.total,
    pending: pendingResult.data.total,
    processing: processingResult.data.total,
    handled: handledResult.data.total,
    totalTrend: todayFaults.length - yesterdayFaults.length,
    pendingTrend:
      todayFaults.filter((item) => item.status === 0).length -
      yesterdayFaults.filter((item) => item.status === 0).length,
    processingTrend:
      todayFaults.filter((item) => item.status === 1).length -
      yesterdayFaults.filter((item) => item.status === 1).length,
    handledTrend:
      todayFaults.filter((item) => item.status === 2).length -
      yesterdayFaults.filter((item) => item.status === 2).length,
  }
}

const fetchFaults = async (pageNum = currentPage.value, size = pageSize.value) => {
  tableLoading.value = true
  try {
    const result = await listIotDeviceFaults({
      deviceName: searchForm.value.deviceName || undefined,
      faultName: searchForm.value.faultName || undefined,
      faultType: searchForm.value.faultType,
      severity: searchForm.value.severity,
      status: searchForm.value.status,
      pageNum,
      pageSize: size,
    })

    faults.value = result.data.list.map((item) => mapFaultToRow(item))
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
    deviceName: '',
    faultName: '',
    faultType: undefined,
    severity: undefined,
    status: undefined,
  }
  void fetchFaults(1)
}

const handleSelectionChange = (rows: FaultRow[]) => {
  selectedRows.value = rows
}

const openAddDialog = () => {
  editFaultId.value = null
  dialogVisible.value = true
}

const handleEditFault = (row: FaultRow) => {
  editFaultId.value = row.id
  dialogVisible.value = true
}

const handleDialogSuccess = () => {
  void fetchFaults(editFaultId.value ? currentPage.value : 1)
  void fetchStats()
}

const handleCurrentPageChange = (page: number) => {
  void fetchFaults(page)
}

const handlePageSizeChange = (size: number) => {
  void fetchFaults(1, size)
}

const getNextPageAfterDelete = (deletedCount: number) => {
  return faults.value.length <= deletedCount && currentPage.value > 1
    ? currentPage.value - 1
    : currentPage.value
}

const isStatusUpdating = (id: number) => statusLoadingIds.value.includes(id)

const setStatusUpdating = (id: number, loading: boolean) => {
  statusLoadingIds.value = loading
    ? [...statusLoadingIds.value, id]
    : statusLoadingIds.value.filter((item) => item !== id)
}

const handleSetStatus = async (row: FaultRow, status: number) => {
  setStatusUpdating(row.id, true)
  try {
    let handleResult: string | undefined
    if (status === 2) {
      const promptResult = await ElMessageBox.prompt('请填写本次故障的处理结果', '完成故障', {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValue: row.handleResult === '-' ? '' : row.handleResult,
        inputValidator: (value) => Boolean(value?.trim()) || '处理结果不能为空',
      })
      handleResult = promptResult.value.trim()
    } else if (status === 3) {
      await ElMessageBox.confirm('确认关闭该故障吗？关闭后不能重新流转。', '关闭故障', {
        confirmButtonText: '确认关闭',
        cancelButtonText: '取消',
        type: 'warning',
      })
    }
    await updateIotDeviceFaultStatus(row.id, status, handleResult)
    ElMessage.success('故障状态已更新')
    void fetchFaults(currentPage.value)
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消操作')
    }
  } finally {
    setStatusUpdating(row.id, false)
  }
}

const openAssignDialog = (row: FaultRow) => {
  if (!isAdmin.value) {
    return
  }

  assignForm.value = {
    id: row.id,
    handleUserId: undefined,
  }
  assignVisible.value = true
  void loadTechnicians()
}

const getAssignButtonType = (row: FaultRow) => {
  if (row.assignmentRejected) return 'warning'
  return row.assigned ? 'info' : 'success'
}

const getAssignButtonText = (row: FaultRow) => {
  if (row.assignmentRejected) return '重新分配'
  return row.assigned ? '已分配' : '分配'
}

const handleAcceptAssignment = async (row: FaultRow) => {
  await acceptIotDeviceFaultAssignment(row.id)
  ElMessage.success('已接受故障任务')
  void fetchFaults(currentPage.value)
  void fetchStats()
}

const handleRejectAssignment = async (row: FaultRow) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入不接受原因', '不接受任务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '可填写原因，便于重新分配',
    })

    await rejectIotDeviceFaultAssignment(row.id, value)
    ElMessage.success('已提交不接受')
    void fetchFaults(currentPage.value)
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消操作')
    }
  }
}

const submitAssign = async () => {
  if (!assignFormRef.value) return

  await assignFormRef.value.validate()
  assignLoading.value = true
  try {
    await assignIotDeviceFault(assignForm.value.id!, assignForm.value.handleUserId!)
    ElMessage.success('分配处理人成功')
    assignVisible.value = false
    void fetchFaults(currentPage.value)
    void fetchStats()
  } finally {
    assignLoading.value = false
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要删除的故障记录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要批量删除已选中的 ${selectedRows.value.length} 条故障记录吗？记录将从列表移除，历史数据仍会保留。`,
      '批量删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    await batchDeleteIotDeviceFaults(selectedRows.value.map((row) => row.id))
    ElMessage.success('批量删除成功')
    void fetchFaults(getNextPageAfterDelete(selectedRows.value.length))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消批量删除')
    }
  }
}

const handleDeleteFault = async (row: FaultRow) => {
  try {
    await ElMessageBox.confirm(`确定要删除故障「${row.faultName}」吗？记录将从列表移除，历史数据仍会保留。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteIotDeviceFault(row.id)
    ElMessage.success('删除成功')
    void fetchFaults(getNextPageAfterDelete(1))
    void fetchStats()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      ElMessage.info('已取消删除')
    }
  }
}

onMounted(() => {
  void fetchFaults()
  void fetchStats()
  void loadTechnicians()
})
</script>

<template>
  <div class="fault-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="closeSidebar" />

    <div class="fault-shell admin-shell">
      <Header :breadcrumbs="['首页', '设备管理', '设备故障管理']" @toggle-sidebar="sidebarVisible = true" />

      <main class="fault-content admin-content">
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
          <el-form class="filter-form" :model="searchForm" label-width="82px">
            <el-form-item label="设备名称">
              <el-input v-model="searchForm.deviceName" placeholder="请输入设备名称" clearable />
            </el-form-item>
            <el-form-item label="故障名称">
              <el-input v-model="searchForm.faultName" placeholder="请输入故障名称" clearable />
            </el-form-item>
            <el-form-item label="故障类型">
              <el-select v-model="searchForm.faultType" placeholder="全部类型" clearable>
                <el-option v-for="item in faultTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <!-- <el-form-item label="严重程度">
              <el-select v-model="searchForm.severity" placeholder="全部程度" clearable>
                <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item> -->
            <div class="filter-actions">
              <el-button type="primary" :icon="Search" @click="fetchFaults(1)">查询</el-button>
              <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            </div>
            <div class="manage-actions">
              <el-button type="danger" plain :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
              <el-button type="primary" :icon="CirclePlus" @click="openAddDialog">新增故障</el-button>
            </div>
          </el-form>
        </section>

        <section class="table-card">
          <div class="table-scroll">
            <el-table
              v-loading="tableLoading"
              :data="faults"
              row-key="id"
              class="fault-table"
              height="100%"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="58" fixed="left" align="center" header-align="center" />
              <el-table-column prop="faultCode" label="故障编码" min-width="130" align="center" header-align="center" />
              <!-- <el-table-column prop="deviceCode" label="设备编码" min-width="140" align="center" header-align="center" /> -->
              <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" header-align="center" />
              <el-table-column prop="plotName" label="所属地块" min-width="150" align="center" header-align="center" />
              <el-table-column prop="faultName" label="故障名称" min-width="150" align="center" header-align="center" />
              <el-table-column label="故障类型" min-width="130" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="fault-chip" :class="`chip-${row.faultTypeTone}`" effect="plain" round>
                    {{ row.faultType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="严重程度" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="fault-chip" :class="`chip-${row.severityTone}`" effect="plain" round>
                    {{ row.severity }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="处理状态" min-width="120" align="center" header-align="center">
                <template #default="{ row }">
                  <el-tag class="fault-chip" :class="`chip-${row.statusTone}`" effect="plain" round>
                    {{ row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="handleUser" label="处理人" min-width="130" align="center" header-align="center" />
              <el-table-column prop="startTime" label="开始时间" min-width="170" align="center" header-align="center" />
              <el-table-column prop="handleTime" label="处理时间" min-width="170" align="center" header-align="center" />
              <el-table-column prop="endTime" label="结束时间" min-width="170" align="center" header-align="center" />
              <el-table-column prop="faultDesc" label="故障描述" min-width="220" show-overflow-tooltip header-align="center" />
              <el-table-column label="操作" width="360" fixed="right" align="center" header-align="center">
                <template #default="{ row }">
                  <div class="table-actions">
                    <el-button link type="primary" :icon="EditPen" @click="handleEditFault(row)">编辑</el-button>
                    <el-button
                      v-if="isAdmin"
                      link
                      :type="getAssignButtonType(row)"
                      :icon="UserFilled"
                      :disabled="row.assigned && !row.assignmentRejected"
                      @click="openAssignDialog(row)"
                    >
                      {{ getAssignButtonText(row) }}
                    </el-button>
                    <el-button
                      v-if="row.canRespondAssignment"
                      link
                      type="success"
                      :icon="CircleCheck"
                      @click="handleAcceptAssignment(row)"
                    >
                      接受
                    </el-button>
                    <el-button
                      v-if="row.canRespondAssignment"
                      link
                      type="warning"
                      :icon="CircleCloseFilled"
                      @click="handleRejectAssignment(row)"
                    >
                      不接受
                    </el-button>
                    <el-dropdown trigger="click" @command="(status: number) => handleSetStatus(row, status)">
                      <el-button link type="warning" :loading="isStatusUpdating(row.id)">状态</el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item
                            v-for="item in statusOptions"
                            :key="item.value"
                            :command="item.value"
                            :disabled="row.statusValue === item.value
                              || row.statusValue === 3
                              || (row.statusValue === 2 && item.value !== 3)
                              || (item.value === 2 && row.assignStatus !== 2)
                              || (item.value === 3 && row.statusValue !== 2)"
                          >
                            {{ item.label }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                    <el-button link type="danger" :icon="Delete" @click="handleDeleteFault(row)">删除</el-button>
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

    <AddOrUpdate v-model="dialogVisible" :id="editFaultId" @success="handleDialogSuccess" />

    <el-dialog v-model="assignVisible" title="分配处理人" width="420px" align-center destroy-on-close>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="96px">
        <el-form-item label="处理人" prop="handleUserId">
          <el-select v-model="assignForm.handleUserId" placeholder="请选择处理人" filterable>
            <el-option
              v-for="item in technicianOptions"
              :key="item.id"
              :label="item.nickname || item.username"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">确认分配</el-button>
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
  min-height: 108px;
  padding: 20px 24px;
  border: 1px solid #f1f5f9;
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
}

.tone-red {
  background: linear-gradient(135deg, #f43f5e, #e11d48);
}

.tone-orange {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.tone-blue {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
}

.tone-green {
  background: linear-gradient(135deg, #22c55e, #16a34a);
}

.stat-card p {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 14px;
}

.stat-card strong {
  display: block;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
}

.stat-trend {
  display: inline-flex;
  gap: 8px;
  margin-top: 10px;
  font-size: 13px;
}

.stat-trend small {
  color: #94a3b8;
}

.trend-up {
  color: #16a34a;
}

.trend-down {
  color: #dc2626;
}

.filter-card {
  flex: 0 0 auto;
  margin-top: 14px;
  padding: 16px 18px 4px;
  border: 1px solid #eef2f7;
}

.filter-form {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr)) auto auto;
  gap: 12px 14px;
  align-items: flex-start;
}

.filter-actions,
.manage-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.table-card {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  margin-top: 14px;
  border: 1px solid #eef2f7;
}

.table-scroll {
  flex: 1 1 auto;
  min-height: 0;
}

.fault-table {
  width: 100%;
}

.fault-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
}

.fault-table :deep(.el-table__cell) {
  color: #334155;
  font-size: 13px;
  text-align: center;
}

.fault-chip {
  border-radius: 999px;
  font-weight: 700;
}

.chip-green {
  color: #15803d;
  background: #f0fdf4;
  border-color: #86efac;
}

.chip-blue {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #93c5fd;
}

.chip-amber {
  color: #b45309;
  background: #fffbeb;
  border-color: #fcd34d;
}

.chip-red {
  color: #be123c;
  background: #fff1f2;
  border-color: #fda4af;
}

.chip-violet {
  color: #6d28d9;
  background: #f5f3ff;
  border-color: #c4b5fd;
}

.chip-cyan {
  color: #0e7490;
  background: #ecfeff;
  border-color: #67e8f9;
}

.chip-gray {
  color: #475569;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.pagination-row {
  flex: 0 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #eef2f7;
  color: #64748b;
}

@media (max-width: 1200px) {
  .fault-shell {
    margin-left: 0;
  }

  .stats-grid,
  .filter-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
