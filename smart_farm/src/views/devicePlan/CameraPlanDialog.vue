<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listCameraDevices, type CameraDevice } from '@/api/cameraDevice'
import {
  addCameraCapturePlan,
  getCameraCapturePlan,
  updateCameraCapturePlan,
  type CameraCapturePlanPayload,
} from '@/api/devicePlan'
import { getConfigurationSaveFeedback } from '@/utils/hardwareFeedback'

/** 摄像头计划新增与编辑共用同一弹框，通过id区分模式。 */
const props = defineProps<{ modelValue: boolean; id?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const cameraOptions = ref<CameraDevice[]>([])
const weekdayOptions = [
  { label: '周一', value: 1 }, { label: '周二', value: 2 }, { label: '周三', value: 3 },
  { label: '周四', value: 4 }, { label: '周五', value: 5 }, { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]

/** 创建稳定的默认表单，确保每次打开新增弹框不会残留上次输入。 */
const defaultForm = () => ({
  id: undefined as number | undefined,
  deviceId: undefined as number | undefined,
  planName: '作物定时采集',
  intervalMinutes: 60,
  weekdays: [1, 2, 3, 4, 5, 6, 7] as number[],
  timeRange: ['06:00', '18:00'] as [string, string],
  timezone: 'Asia/Shanghai',
  enabled: true,
  remark: '',
  version: undefined as number | undefined,
})
const form = reactive(defaultForm())

/** 规则提供就近错误提示，后端继续执行权限和完整性校验。 */
const rules: FormRules = {
  deviceId: [{ required: true, message: '请选择摄像头', trigger: 'change' }],
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  intervalMinutes: [{ required: true, message: '请输入抓拍间隔', trigger: 'blur' }],
  timezone: [{ required: true, message: '请选择时区', trigger: 'change' }],
}

/** 星期数组与数据库位掩码之间的双向转换。 */
const weekdaysToMask = (days: number[]) => days.reduce((mask, day) => mask | (1 << (day - 1)), 0)
const maskToWeekdays = (mask: number) => weekdayOptions.filter((day) => (mask & (1 << (day.value - 1))) !== 0).map((day) => day.value)

/** 加载数据权限范围内的摄像头下拉选项。 */
const fetchCameraOptions = async () => {
  const result = await listCameraDevices({ pageNum: 1, pageSize: 1000 })
  cameraOptions.value = result.data.list
}

/** 根据新增或编辑模式初始化表单。 */
const initialize = async () => {
  loading.value = true
  Object.assign(form, defaultForm())
  try {
    await fetchCameraOptions()
    if (props.id) {
      const result = await getCameraCapturePlan(props.id)
      const plan = result.data
      Object.assign(form, {
        id: plan.id,
        deviceId: plan.deviceId,
        planName: plan.planName,
        intervalMinutes: plan.intervalMinutes,
        weekdays: maskToWeekdays(plan.weekdaysMask),
        timeRange: [plan.startTime.slice(0, 5), plan.endTime.slice(0, 5)],
        timezone: plan.timezone,
        enabled: plan.enabled === 1,
        remark: plan.remark || '',
        version: plan.version,
      })
    }
  } finally {
    loading.value = false
  }
}

/** 保存计划并提示其当前仍处于软件待下发状态。 */
const submit = async () => {
  await formRef.value?.validate()
  if (!form.weekdays.length) {
    ElMessage.warning('请至少选择一个采集星期')
    return
  }
  if (!form.timeRange[0] || !form.timeRange[1]) {
    ElMessage.warning('请选择完整的每日采集时段')
    return
  }
  const payload: CameraCapturePlanPayload = {
    id: form.id,
    deviceId: form.deviceId!,
    planName: form.planName.trim(),
    intervalMinutes: form.intervalMinutes,
    weekdaysMask: weekdaysToMask(form.weekdays),
    startTime: form.timeRange[0],
    endTime: form.timeRange[1],
    timezone: form.timezone,
    enabled: form.enabled,
    remark: form.remark.trim() || undefined,
    version: form.version,
  }
  saving.value = true
  try {
    const result = form.id
      ? await updateCameraCapturePlan(payload)
      : await addCameraCapturePlan(payload)
    // 根据计划真实应用状态显示待下发、已确认或失败，避免统一成功提示。
    const feedback = getConfigurationSaveFeedback(result.data.applyStatus, result.data.lastApplyError)
    ElMessage({ type: feedback.type, message: feedback.text })
    emit('update:modelValue', false)
    emit('success')
  } finally {
    saving.value = false
  }
}

watch(() => props.modelValue, (visible) => { if (visible) void initialize() })
</script>

<template>
  <el-dialog :model-value="modelValue" :title="id ? '编辑摄像头采集计划' : '新增摄像头采集计划'"
    width="680px" destroy-on-close @update:model-value="emit('update:modelValue', $event)">
    <div v-loading="loading">
      <el-alert title="当前保存的是软件期望配置，不会在未接入硬件时生成虚假图片或执行记录。"
        type="warning" :closable="false" show-icon />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="camera-plan-form">
        <el-form-item label="目标摄像头" prop="deviceId">
          <el-select v-model="form.deviceId" filterable placeholder="请选择摄像头">
            <el-option v-for="camera in cameraOptions" :key="camera.id" :value="camera.id!"
              :label="`${camera.name} · ${camera.plotName || '未关联地块'}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划名称" prop="planName"><el-input v-model="form.planName" maxlength="100" /></el-form-item>
        <el-form-item label="抓拍间隔" prop="intervalMinutes">
          <el-input-number v-model="form.intervalMinutes" :min="1" :max="1440" :step="5" />
          <span class="field-unit">分钟</span>
        </el-form-item>
        <el-form-item label="采集星期">
          <el-checkbox-group v-model="form.weekdays"><el-checkbox-button v-for="day in weekdayOptions"
            :key="day.value" :value="day.value">{{ day.label }}</el-checkbox-button></el-checkbox-group>
        </el-form-item>
        <el-form-item label="每日时段">
          <el-time-picker v-model="form.timeRange" is-range format="HH:mm" value-format="HH:mm"
            start-placeholder="开始时间" end-placeholder="结束时间" range-separator="至" />
        </el-form-item>
        <el-form-item label="计划时区" prop="timezone">
          <el-select v-model="form.timezone"><el-option label="中国标准时间（Asia/Shanghai）" value="Asia/Shanghai" />
            <el-option label="协调世界时（UTC）" value="UTC" /></el-select>
        </el-form-item>
        <el-form-item label="启用计划"><el-switch v-model="form.enabled" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
      </el-form>
    </div>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存计划</el-button></template>
  </el-dialog>
</template>

<style scoped>
.camera-plan-form { margin-top: 20px; }
.field-unit { margin-left: 8px; color: #475569; }
.camera-plan-form :deep(.el-select), .camera-plan-form :deep(.el-date-editor) { width: 100%; }
@media (max-width: 720px) { .camera-plan-form { margin-top: 14px; } }
</style>
