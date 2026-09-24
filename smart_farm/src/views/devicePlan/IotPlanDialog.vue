<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { CirclePlus, Delete } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getIotDevicePlan,
  saveIotDevicePlan,
  type IotDevicePlanPayload,
} from '@/api/devicePlan'
import { getConfigurationSaveFeedback } from '@/utils/hardwareFeedback'

/** 弹框由列表传入设备ID，保存成功后通知父页面刷新。 */
const props = defineProps<{ modelValue: boolean; deviceId?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; success: [] }>()

interface EditableSchedule {
  scheduleName: string
  weekdays: number[]
  /** 起止时间分开保存，允许结束时间早于开始时间来表达跨午夜运行。 */
  startTime: string
  endTime: string
  enabled: boolean
}

/** 表单仅保留用户可修改字段，硬件应用状态由后端维护。 */
const form = reactive({
  deviceId: 0,
  deviceName: '',
  deviceCode: '',
  typeCode: '',
  typeName: '',
  collectionEnabled: true,
  collectionIntervalMinutes: 60,
  controlEnabled: false,
  timezone: 'Asia/Shanghai',
  /** 生效起止日期允许单独填写，避免编辑开放式计划时丢失日期。 */
  effectiveFrom: '' as string,
  effectiveTo: '' as string,
  version: undefined as number | undefined,
  schedules: [] as EditableSchedule[],
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const actuatorType = computed(() => ['GROW_LIGHT', 'WATER_PUMP'].includes(form.typeCode))
const weekdayOptions = [
  { label: '周一', value: 1 }, { label: '周二', value: 2 }, { label: '周三', value: 3 },
  { label: '周四', value: 4 }, { label: '周五', value: 5 }, { label: '周六', value: 6 },
  { label: '周日', value: 7 },
]
const timezoneOptions = [
  { label: '中国标准时间（Asia/Shanghai）', value: 'Asia/Shanghai' },
  { label: '协调世界时（UTC）', value: 'UTC' },
]

/** Element Plus 规则负责基础必填，跨字段及重叠校验由后端兜底。 */
const rules: FormRules = {
  collectionIntervalMinutes: [{ required: true, message: '请输入采集间隔', trigger: 'blur' }],
  timezone: [{ required: true, message: '请选择时区', trigger: 'change' }],
}

/** 将后端星期位掩码转换成复选框数组。 */
const maskToWeekdays = (mask: number) => weekdayOptions
  .filter((item) => (mask & (1 << (item.value - 1))) !== 0)
  .map((item) => item.value)

/** 将星期复选框数组压缩为后端存储位掩码。 */
const weekdaysToMask = (weekdays: number[]) => weekdays
  .reduce((mask, day) => mask | (1 << (day - 1)), 0)

/** 新增一个默认工作日时段，减少用户首次配置成本。 */
const addSchedule = () => {
  form.schedules.push({
    scheduleName: form.typeCode === 'WATER_PUMP' ? '日常供水' : '日间补光',
    weekdays: [1, 2, 3, 4, 5, 6, 7],
    startTime: form.typeCode === 'WATER_PUMP' ? '08:00' : '06:00',
    endTime: form.typeCode === 'WATER_PUMP' ? '08:30' : '18:00',
    enabled: true,
  })
}

/** 删除尚未保存的时段行。 */
const removeSchedule = (index: number) => form.schedules.splice(index, 1)

/** 打开弹框时重新读取服务端最新版本，避免编辑过期数据。 */
const loadPlan = async () => {
  if (!props.deviceId) return
  loading.value = true
  try {
    const result = await getIotDevicePlan(props.deviceId)
    const plan = result.data
    Object.assign(form, {
      deviceId: plan.deviceId,
      deviceName: plan.deviceName || '',
      deviceCode: plan.deviceCode || '',
      typeCode: plan.typeCode || '',
      typeName: plan.typeName || '',
      collectionEnabled: plan.collectionEnabled,
      collectionIntervalMinutes: plan.collectionIntervalMinutes,
      controlEnabled: plan.controlEnabled,
      timezone: plan.timezone || 'Asia/Shanghai',
      effectiveFrom: plan.effectiveFrom || '',
      effectiveTo: plan.effectiveTo || '',
      version: plan.version,
      schedules: plan.schedules.map((item) => ({
        scheduleName: item.scheduleName,
        weekdays: maskToWeekdays(item.weekdaysMask),
        startTime: item.startTime.slice(0, 5),
        endTime: item.endTime.slice(0, 5),
        enabled: item.enabled === 1,
      })),
    })
  } finally {
    loading.value = false
  }
}

/** 保存前执行易理解的前端校验，服务端仍执行完整安全校验。 */
const submit = async () => {
  await formRef.value?.validate()
  if (form.controlEnabled && !form.schedules.some((item) => item.enabled)) {
    ElMessage.warning('启用自动控制时至少需要一个有效时段')
    return
  }
  if (form.effectiveFrom && form.effectiveTo && form.effectiveFrom > form.effectiveTo) {
    ElMessage.warning('计划结束日期不能早于开始日期')
    return
  }
  if (form.schedules.some((item) => !item.scheduleName.trim() || !item.weekdays.length || !item.startTime || !item.endTime)) {
    ElMessage.warning('请完整填写每个运行时段的名称、星期和时间')
    return
  }
  const payload: IotDevicePlanPayload = {
    deviceId: form.deviceId,
    collectionEnabled: form.collectionEnabled,
    collectionIntervalMinutes: form.collectionIntervalMinutes,
    controlEnabled: actuatorType.value && form.controlEnabled,
    timezone: form.timezone,
    effectiveFrom: form.effectiveFrom || undefined,
    effectiveTo: form.effectiveTo || undefined,
    version: form.version,
    schedules: actuatorType.value ? form.schedules.map((item, index) => ({
      scheduleName: item.scheduleName.trim(),
      weekdaysMask: weekdaysToMask(item.weekdays),
      startTime: item.startTime,
      endTime: item.endTime,
      enabled: item.enabled ? 1 : 0,
      sortOrder: index,
    })) : [],
  }
  saving.value = true
  try {
    const result = await saveIotDevicePlan(payload)
    // 使用后端返回的应用状态反馈，保存成功不等同于硬件已经执行。
    const feedback = getConfigurationSaveFeedback(result.data.applyStatus, result.data.lastApplyError)
    ElMessage({ type: feedback.type, message: feedback.text })
    emit('update:modelValue', false)
    emit('success')
  } finally {
    saving.value = false
  }
}

watch(() => props.modelValue, (visible) => {
  if (visible) void loadPlan()
})
</script>

<template>
  <el-dialog :model-value="modelValue" title="配置设备计划" width="860px" destroy-on-close
    @update:model-value="emit('update:modelValue', $event)">
    <div v-loading="loading" class="plan-dialog-body">
      <div class="device-summary">
        <div><span>设备名称</span><strong>{{ form.deviceName || '-' }}</strong></div>
        <div><span>设备编号</span><strong>{{ form.deviceCode || '-' }}</strong></div>
        <div><span>设备类型</span><strong>{{ form.typeName || '-' }}</strong></div>
      </div>

      <el-alert title="当前只保存软件期望配置；硬件接入后由设备通信适配器负责下发并回写应用状态。"
        type="warning" :closable="false" show-icon />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px" class="plan-form">
        <section class="form-section">
          <h3>周期采集策略</h3>
          <el-form-item label="启用周期采集"><el-switch v-model="form.collectionEnabled" /></el-form-item>
          <el-form-item label="采集间隔" prop="collectionIntervalMinutes">
            <el-input-number v-model="form.collectionIntervalMinutes" :min="1" :max="1440" :step="5" />
            <span class="field-unit">分钟</span>
            <span class="field-help">允许 1—1440 分钟，保存后产生新配置版本。</span>
          </el-form-item>
          <el-form-item label="生效日期">
            <div class="date-boundary-row">
              <el-date-picker v-model="form.effectiveFrom" type="date" value-format="YYYY-MM-DD"
                placeholder="立即生效" clearable />
              <span>至</span>
              <el-date-picker v-model="form.effectiveTo" type="date" value-format="YYYY-MM-DD"
                placeholder="长期有效" clearable />
            </div>
          </el-form-item>
          <el-form-item label="计划时区" prop="timezone">
            <el-select v-model="form.timezone"><el-option v-for="item in timezoneOptions" :key="item.value"
              :label="item.label" :value="item.value" /></el-select>
          </el-form-item>
        </section>

        <section v-if="actuatorType" class="form-section">
          <div class="section-title-row">
            <div><h3>自动运行时段</h3><p>允许多个时段，结束时间早于开始时间时按跨日处理。</p></div>
            <el-button :icon="CirclePlus" plain @click="addSchedule">新增时段</el-button>
          </div>
          <el-form-item label="启用自动控制"><el-switch v-model="form.controlEnabled" /></el-form-item>
          <div v-if="!form.schedules.length" class="empty-schedule">尚未添加运行时段</div>
          <article v-for="(item, index) in form.schedules" :key="index" class="schedule-card">
            <div class="schedule-head">
              <strong>时段 {{ index + 1 }}</strong>
              <el-switch v-model="item.enabled" active-text="启用" inactive-text="停用" />
              <el-button type="danger" link :icon="Delete" @click="removeSchedule(index)">删除</el-button>
            </div>
            <el-form-item label="时段名称"><el-input v-model="item.scheduleName" maxlength="100" /></el-form-item>
            <el-form-item label="运行星期">
              <el-checkbox-group v-model="item.weekdays"><el-checkbox-button v-for="day in weekdayOptions"
                :key="day.value" :value="day.value">{{ day.label }}</el-checkbox-button></el-checkbox-group>
            </el-form-item>
            <el-form-item label="启停时间">
              <div class="time-boundary-row">
                <el-time-picker v-model="item.startTime" format="HH:mm" value-format="HH:mm" placeholder="开启时间" />
                <span>至</span>
                <el-time-picker v-model="item.endTime" format="HH:mm" value-format="HH:mm" placeholder="关闭时间" />
              </div>
            </el-form-item>
          </article>
        </section>
      </el-form>
    </div>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存计划</el-button></template>
  </el-dialog>
</template>

<style scoped>
.plan-dialog-body { display: flex; flex-direction: column; gap: 16px; }
.device-summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding: 14px; background: #f8fafc; border: 1px solid #e2e8f0; }
.device-summary div { min-width: 0; }
.device-summary span, .field-help { display: block; color: #64748b; font-size: 12px; }
.device-summary strong { display: block; margin-top: 6px; color: #0f172a; overflow-wrap: anywhere; }
.plan-form { display: flex; flex-direction: column; gap: 16px; }
.form-section { padding: 18px 18px 4px; border: 1px solid #e2e8f0; background: #fff; }
.form-section h3 { margin: 0 0 16px; color: #0f172a; font-size: 16px; }
.section-title-row { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 8px; }
.section-title-row h3 { margin-bottom: 4px; }
.section-title-row p { margin: 0; color: #64748b; font-size: 12px; }
.field-unit { margin-left: 8px; color: #475569; }
.field-help { margin-left: 12px; }
.date-boundary-row, .time-boundary-row { display: flex; align-items: center; gap: 8px; width: 100%; }
.date-boundary-row :deep(.el-date-editor), .time-boundary-row :deep(.el-date-editor) { flex: 1 1 0; width: 0; }
.schedule-card { margin: 0 0 14px 112px; padding: 14px 14px 2px; border: 1px solid #dbeafe; background: #f8fbff; }
.schedule-head { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.schedule-head strong { margin-right: auto; color: #1e3a5f; }
.empty-schedule { margin: 0 0 16px 112px; padding: 18px; color: #64748b; text-align: center; border: 1px dashed #cbd5e1; }
@media (max-width: 720px) {
  .device-summary { grid-template-columns: 1fr; }
  .schedule-card, .empty-schedule { margin-left: 0; }
  .section-title-row { flex-direction: column; }
  .field-help { width: 100%; margin: 8px 0 0; }
  .date-boundary-row, .time-boundary-row { align-items: stretch; flex-direction: column; }
  .date-boundary-row :deep(.el-date-editor), .time-boundary-row :deep(.el-date-editor) { width: 100%; }
}
</style>
