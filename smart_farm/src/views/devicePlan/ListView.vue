<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Calendar, CirclePlus, Clock, Delete, EditPen, Monitor, Refresh, Search, VideoCamera } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Header from '@/components/Header.vue'
import LeftMenu from '@/components/LeftMenu.vue'
import { listPlots, type Plot } from '@/api/plot'
import {
  deleteCameraCapturePlan,
  deleteIotDevicePlan,
  listCameraCapturePlans,
  listIotDevicePlans,
  type CameraCapturePlan,
  type IotDevicePlan,
} from '@/api/devicePlan'
import IotPlanDialog from './IotPlanDialog.vue'
import CameraPlanDialog from './CameraPlanDialog.vue'
import { getHardwareApplyFeedback } from '@/utils/hardwareFeedback'

const sidebarVisible = ref(false)
const activeTab = ref<'iot' | 'camera'>('iot')
const plots = ref<Plot[]>([])
const iotRows = ref<IotDevicePlan[]>([])
const cameraRows = ref<CameraCapturePlan[]>([])
const iotLoading = ref(false)
const cameraLoading = ref(false)
const iotTotal = ref(0)
const cameraTotal = ref(0)
const iotPage = ref(1)
const cameraPage = ref(1)
const iotPageSize = ref(10)
const cameraPageSize = ref(10)

/** 两个标签页分别维护筛选条件，切换页面不会丢失用户输入。 */
const iotFilter = reactive({ plotId: undefined as number | undefined, typeCode: '', keyword: '' })
const cameraFilter = reactive({ plotId: undefined as number | undefined, enabled: undefined as number | undefined, keyword: '' })
const iotDialogVisible = ref(false)
const selectedDeviceId = ref<number>()
const cameraDialogVisible = ref(false)
const selectedCameraPlanId = ref<number>()

const typeOptions = [
  { label: '环境传感器', value: 'ENV_SENSOR' },
  { label: '水质检测仪', value: 'WATER_QUALITY' },
  { label: '补光灯', value: 'GROW_LIGHT' },
  { label: '水泵', value: 'WATER_PUMP' },
]
const weekdayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

/** 顶部指标基于当前筛选结果和当前页展示，不虚构硬件在线执行数据。 */
const statCards = computed(() => [
  { title: '支持设备', value: iotTotal.value, desc: '四类计划设备', icon: Monitor, tone: 'blue' },
  { title: '当前页已配置', value: iotRows.value.filter((row) => row.id).length, desc: '已保存策略', icon: Calendar, tone: 'green' },
  { title: '自动控制', value: iotRows.value.filter((row) => row.controlEnabled).length, desc: '当前页启用', icon: Clock, tone: 'orange' },
  { title: '摄像头计划', value: cameraTotal.value, desc: '图像采集策略', icon: VideoCamera, tone: 'gray' },
])

/** 加载地块下拉，后端会继续按登录用户执行数据权限过滤。 */
const fetchPlots = async () => {
  const result = await listPlots({ pageNum: 1, pageSize: 1000 })
  plots.value = result.data.list
}

/** 查询 IoT 设备和计划；未保存计划的设备也会显示，便于直接配置。 */
const fetchIotPlans = async (page = iotPage.value, size = iotPageSize.value) => {
  iotLoading.value = true
  try {
    const result = await listIotDevicePlans({
      plotId: iotFilter.plotId,
      typeCode: iotFilter.typeCode || undefined,
      keyword: iotFilter.keyword.trim() || undefined,
      pageNum: page,
      pageSize: size,
    })
    iotRows.value = result.data.list
    iotTotal.value = result.data.total
    iotPage.value = result.data.pageNum
    iotPageSize.value = result.data.pageSize
  } finally {
    iotLoading.value = false
  }
}

/** 查询摄像头作物图像采集计划。 */
const fetchCameraPlans = async (page = cameraPage.value, size = cameraPageSize.value) => {
  cameraLoading.value = true
  try {
    const result = await listCameraCapturePlans({
      plotId: cameraFilter.plotId,
      enabled: cameraFilter.enabled,
      keyword: cameraFilter.keyword.trim() || undefined,
      pageNum: page,
      pageSize: size,
    })
    cameraRows.value = result.data.list
    cameraTotal.value = result.data.total
    cameraPage.value = result.data.pageNum
    cameraPageSize.value = result.data.pageSize
  } finally {
    cameraLoading.value = false
  }
}

/** 重置 IoT 筛选并回到第一页。 */
const resetIotFilter = () => {
  Object.assign(iotFilter, { plotId: undefined, typeCode: '', keyword: '' })
  void fetchIotPlans(1)
}

/** 重置摄像头筛选并回到第一页。 */
const resetCameraFilter = () => {
  Object.assign(cameraFilter, { plotId: undefined, enabled: undefined, keyword: '' })
  void fetchCameraPlans(1)
}

/** 打开指定设备的计划配置弹框。 */
const editIotPlan = (row: IotDevicePlan) => {
  selectedDeviceId.value = row.deviceId
  iotDialogVisible.value = true
}

/** 删除已保存计划，恢复为服务端默认建议；未保存计划不显示删除操作。 */
const removeIotPlan = async (row: IotDevicePlan) => {
  await ElMessageBox.confirm(`确定删除设备「${row.deviceName}」的计划吗？`, '删除确认', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消',
  })
  await deleteIotDevicePlan(row.deviceId)
  ElMessage.success('设备计划已删除')
  void fetchIotPlans()
}

/** 打开摄像头计划新增或编辑弹框。 */
const editCameraPlan = (id?: number) => {
  selectedCameraPlanId.value = id
  cameraDialogVisible.value = true
}

/** 删除摄像头计划前二次确认，避免误操作。 */
const removeCameraPlan = async (row: CameraCapturePlan) => {
  await ElMessageBox.confirm(`确定删除计划「${row.planName}」吗？`, '删除确认', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消',
  })
  await deleteCameraCapturePlan(row.id!)
  ElMessage.success('摄像头采集计划已删除')
  void fetchCameraPlans()
}

/** 表格和保存提示共用同一硬件状态语义，并适配 Element Plus 标签类型。 */
const applyStatusMeta = (status?: number, failureReason?: string) => {
  const feedback = getHardwareApplyFeedback(status, failureReason)
  return {
    ...feedback,
    tagType: feedback.type === 'error' ? 'danger' : feedback.type === 'info' ? 'primary' : feedback.type,
  } as const
}

/** 位掩码转成紧凑、可读的星期文本。 */
const formatWeekdays = (mask: number) => {
  if (mask === 127) return '每天'
  if (mask === 31) return '工作日'
  return weekdayNames.filter((_, index) => (mask & (1 << index)) !== 0).join('、') || '-'
}

/** 汇总执行设备多个时段，完整内容通过表格提示显示。 */
const formatSchedules = (row: IotDevicePlan) => {
  if (!row.controlEnabled) return '未启用'
  const enabled = row.schedules.filter((item) => item.enabled === 1)
  return enabled.length ? enabled.map((item) => `${item.scheduleName} ${item.startTime.slice(0, 5)}–${item.endTime.slice(0, 5)}`).join('；') : '无有效时段'
}

/** 初始化两个标签的数据，使顶部指标在首次进入时完整。 */
onMounted(() => {
  void Promise.all([fetchPlots(), fetchIotPlans(), fetchCameraPlans()])
})
</script>

<template>
  <div class="plan-page admin-page">
    <LeftMenu :visible="sidebarVisible" @close="sidebarVisible = false" />
    <div class="plan-shell admin-shell">
      <Header :breadcrumbs="['首页', '设备管理', '设备计划管理']" @toggle-sidebar="sidebarVisible = true" />
      <main class="plan-content admin-content">
        <section class="stats-grid" aria-label="计划统计">
          <article v-for="card in statCards" :key="card.title" class="stat-card">
            <div class="stat-icon" :class="`tone-${card.tone}`"><el-icon><component :is="card.icon" /></el-icon></div>
            <div><p>{{ card.title }}</p><strong>{{ card.value.toLocaleString() }}</strong><span>{{ card.desc }}</span></div>
          </article>
        </section>

        <el-alert class="hardware-alert" title="硬件准备阶段" description="当前页面管理软件期望配置。计划保存后显示“待下发”，不会自动修改设备实时状态，也不会生成虚假采集数据。"
          type="warning" :closable="false" show-icon />

        <section class="workspace-card">
          <el-tabs v-model="activeTab" class="plan-tabs">
            <el-tab-pane label="IoT 采集与控制" name="iot">
              <el-form class="filter-form" :model="iotFilter" label-width="76px">
                <el-form-item label="关键词"><el-input v-model="iotFilter.keyword" placeholder="设备名称或编号" clearable /></el-form-item>
                <el-form-item label="所属地块"><el-select v-model="iotFilter.plotId" placeholder="全部地块" clearable filterable>
                  <el-option v-for="plot in plots" :key="plot.id" :label="plot.plotName" :value="plot.id!" /></el-select></el-form-item>
                <el-form-item label="设备类型"><el-select v-model="iotFilter.typeCode" placeholder="全部类型" clearable>
                  <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
                <div class="filter-actions"><el-button type="primary" :icon="Search" @click="fetchIotPlans(1)">查询</el-button>
                  <el-button :icon="Refresh" @click="resetIotFilter">重置</el-button></div>
              </el-form>

              <el-table v-loading="iotLoading" :data="iotRows" row-key="deviceId" height="100%" class="plan-table">
                <el-table-column prop="deviceName" label="设备名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="deviceCode" label="设备编号" min-width="140" show-overflow-tooltip />
                <el-table-column prop="typeName" label="类型" min-width="115" align="center" />
                <el-table-column prop="plotName" label="地块" min-width="130" show-overflow-tooltip />
                <el-table-column label="周期采集" min-width="130" align="center">
                  <template #default="{ row }"><el-tag :type="row.collectionEnabled ? 'success' : 'info'" effect="plain">
                    {{ row.collectionEnabled ? `每 ${row.collectionIntervalMinutes} 分钟` : '未启用' }}</el-tag></template>
                </el-table-column>
                <el-table-column label="自动控制" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ formatSchedules(row) }}</template>
                </el-table-column>
                <el-table-column label="应用状态" min-width="110" align="center">
                  <template #default="{ row }"><el-tooltip v-if="row.id" :disabled="row.applyStatus !== 3"
                    :content="row.lastApplyError || '设备未返回具体失败原因'" placement="top">
                    <el-tag :type="applyStatusMeta(row.applyStatus, row.lastApplyError).tagType" effect="plain">
                      {{ applyStatusMeta(row.applyStatus).text }}</el-tag>
                  </el-tooltip><el-tag v-else type="info" effect="plain">未配置</el-tag></template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right" align="center">
                  <template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="editIotPlan(row)">配置</el-button>
                    <el-button v-if="row.id" link type="danger" :icon="Delete" @click="removeIotPlan(row)">删除</el-button></template>
                </el-table-column>
              </el-table>
              <div class="pagination-row"><span>共 {{ iotTotal.toLocaleString() }} 台设备</span><el-pagination
                v-model:current-page="iotPage" v-model:page-size="iotPageSize" background layout="sizes, prev, pager, next, jumper"
                :total="iotTotal" :page-sizes="[10, 20, 50, 100]" @current-change="fetchIotPlans" @size-change="(size: number) => fetchIotPlans(1, size)" /></div>
            </el-tab-pane>

            <el-tab-pane label="摄像头图像采集" name="camera">
              <el-form class="filter-form camera-filter" :model="cameraFilter" label-width="76px">
                <el-form-item label="关键词"><el-input v-model="cameraFilter.keyword" placeholder="计划或摄像头名称" clearable /></el-form-item>
                <el-form-item label="所属地块"><el-select v-model="cameraFilter.plotId" placeholder="全部地块" clearable filterable>
                  <el-option v-for="plot in plots" :key="plot.id" :label="plot.plotName" :value="plot.id!" /></el-select></el-form-item>
                <el-form-item label="启用状态"><el-select v-model="cameraFilter.enabled" placeholder="全部状态" clearable>
                  <el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
                <div class="filter-actions"><el-button type="primary" :icon="Search" @click="fetchCameraPlans(1)">查询</el-button>
                  <el-button :icon="Refresh" @click="resetCameraFilter">重置</el-button></div>
                <div class="manage-actions"><el-button type="primary" :icon="CirclePlus" @click="editCameraPlan()">新增计划</el-button></div>
              </el-form>

              <el-table v-loading="cameraLoading" :data="cameraRows" row-key="id" height="100%" class="plan-table">
                <el-table-column prop="planName" label="计划名称" min-width="150" show-overflow-tooltip />
                <el-table-column prop="cameraName" label="摄像头" min-width="150" show-overflow-tooltip />
                <el-table-column prop="plotName" label="地块" min-width="130" show-overflow-tooltip />
                <el-table-column label="采集周期" min-width="120" align="center"><template #default="{ row }">每 {{ row.intervalMinutes }} 分钟</template></el-table-column>
                <el-table-column label="采集星期" min-width="160" show-overflow-tooltip><template #default="{ row }">{{ formatWeekdays(row.weekdaysMask) }}</template></el-table-column>
                <el-table-column label="每日时段" min-width="140" align="center"><template #default="{ row }">{{ row.startTime.slice(0, 5) }}–{{ row.endTime.slice(0, 5) }}</template></el-table-column>
                <el-table-column label="下次采集" min-width="170" align="center"><template #default="{ row }">{{ row.nextCaptureTime || '-' }}</template></el-table-column>
                <el-table-column label="状态" min-width="110" align="center"><template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'" effect="plain">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
                <el-table-column label="应用状态" min-width="110" align="center"><template #default="{ row }"><el-tooltip
                  :disabled="row.applyStatus !== 3" :content="row.lastApplyError || '设备未返回具体失败原因'" placement="top">
                  <el-tag :type="applyStatusMeta(row.applyStatus, row.lastApplyError).tagType" effect="plain">{{ applyStatusMeta(row.applyStatus).text }}</el-tag>
                </el-tooltip></template></el-table-column>
                <el-table-column label="操作" width="150" fixed="right" align="center"><template #default="{ row }"><el-button link type="primary" :icon="EditPen" @click="editCameraPlan(row.id)">编辑</el-button>
                  <el-button link type="danger" :icon="Delete" @click="removeCameraPlan(row)">删除</el-button></template></el-table-column>
              </el-table>
              <div class="pagination-row"><span>共 {{ cameraTotal.toLocaleString() }} 条计划</span><el-pagination
                v-model:current-page="cameraPage" v-model:page-size="cameraPageSize" background layout="sizes, prev, pager, next, jumper"
                :total="cameraTotal" :page-sizes="[10, 20, 50, 100]" @current-change="fetchCameraPlans" @size-change="(size: number) => fetchCameraPlans(1, size)" /></div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </main>
    </div>

    <IotPlanDialog v-model="iotDialogVisible" :device-id="selectedDeviceId" @success="fetchIotPlans()" />
    <CameraPlanDialog v-model="cameraDialogVisible" :id="selectedCameraPlanId" @success="fetchCameraPlans()" />
  </div>
</template>

<style scoped>
.plan-content { gap: 10px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 18px; flex: 0 0 auto; }
.stat-card { min-height: 112px; display: flex; align-items: center; gap: 20px; padding: 20px 24px; border: 1px solid #f1f5f9; background: #fff; box-shadow: 0 8px 28px rgba(15, 23, 42, .04); }
.stat-icon { width: 56px; height: 56px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 50%; color: #fff; font-size: 27px; }
.tone-blue { background: linear-gradient(135deg, #2f80ed, #2563eb); }.tone-green { background: linear-gradient(135deg, #41c79a, #10b981); }
.tone-orange { background: linear-gradient(135deg, #ffbe35, #f59e0b); }.tone-gray { background: linear-gradient(135deg, #9aa3b2, #64748b); }
.stat-card p { margin: 0; color: #475569; font-size: 14px; }.stat-card strong { display: block; margin: 7px 0; color: #111827; font-size: 25px; }.stat-card span { color: #64748b; font-size: 12px; }
.hardware-alert { flex: 0 0 auto; }
.workspace-card { flex: 1 1 auto; min-height: 0; padding: 0 18px 14px; background: #fff; box-shadow: 0 8px 28px rgba(15, 23, 42, .04); }
.plan-tabs, .plan-tabs :deep(.el-tabs__content), .plan-tabs :deep(.el-tab-pane) { height: 100%; }
.plan-tabs :deep(.el-tab-pane) { display: flex; flex-direction: column; min-height: 0; }
.plan-tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.filter-form { position: relative; display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)) auto; align-items: center; gap: 12px 18px; margin-bottom: 12px; padding-right: 0; }
.camera-filter { padding-right: 130px; }.filter-form :deep(.el-form-item) { margin-bottom: 0; }.filter-form :deep(.el-input__wrapper), .filter-form :deep(.el-select__wrapper) { min-height: 40px; border-radius: 2px; }
.filter-actions, .manage-actions { display: flex; gap: 10px; }.filter-actions :deep(.el-button), .manage-actions :deep(.el-button) { min-height: 40px; }.manage-actions { position: absolute; right: 0; top: 0; }
.plan-table { flex: 1 1 auto; min-height: 300px; }.plan-table :deep(th.el-table__cell) { height: 52px; color: #334155; background: #fafcff; font-size: 13px; font-weight: 800; }.plan-table :deep(.el-table__row) { height: 62px; }
.pagination-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding-top: 14px; color: #475569; font-size: 14px; }
@media (max-width: 1280px) { .stats-grid { grid-template-columns: repeat(2, 1fr); }.filter-form { grid-template-columns: repeat(2, minmax(220px, 1fr)); }.camera-filter { padding-right: 0; }.manage-actions { position: static; } }
@media (max-width: 960px) { .plan-shell { margin-left: 0; } }
@media (max-width: 720px) { .plan-content { padding: 12px; }.stats-grid, .filter-form { grid-template-columns: 1fr; }.pagination-row { align-items: flex-start; flex-direction: column; }.workspace-card { padding: 0 12px 12px; } }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto !important; transition-duration: 0.01ms !important; } }
</style>
