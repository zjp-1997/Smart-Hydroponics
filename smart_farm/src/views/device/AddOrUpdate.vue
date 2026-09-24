<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

// ================== 设备 API ==================
import {
  addIotDevice,
  getIotDeviceById,
  updateIotDevice,
  type IotDevice,
  type IotDevicePayload,
  type UpdateIotDevicePayload,
} from '@/api/iotDevice'

// ⭐ 直接复用：地块接口（和种植批次一样）
import { listPlots, type Plot } from '@/api/plot'
import { listDeviceTypes, type DeviceType } from '@/api/deviceType'

// ================== 表单模型 ==================
interface DeviceFormModel {
  name: string
  typeId?: number
  controlStatus?: number
  onlineStatus?: number
  healthStatus?: number
  installTime?: string
  plotId?: number   // 通过地块下拉选择安装位置，对应后端 iot_device.plot_id。
}

// ================== props ==================
const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

// ================== emit ==================
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [device?: IotDevice]
}>()

// ================== 弹窗 ==================
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

// ================== 状态 ==================
const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑设备信息' : '新增设备信息'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const submitLoading = ref(false)
const detailLoading = ref(false)
const formRef = ref<FormInstance>()

// ================== ⭐ 地块数据 ==================
const plotOptions = ref<Plot[]>([])
const deviceTypeOptions = ref<Array<{ label: string; value: number }>>([])

// ================== 枚举 ==================
const controlStatusOptions = [
  { label: '期望开启', value: 1 },
  { label: '期望关闭', value: 0 },
]

const onlineStatusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
]

const healthStatusOptions = [
  { label: '正常', value: 0 },
  { label: '故障', value: 1 },
  { label: '维护中', value: 2 },
]

// ================== 初始化表单 ==================
const formatDateTime = (date: Date) => {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const createEmptyForm = (): DeviceFormModel => ({
  name: '',
  typeId: undefined,
  controlStatus: 0,
  onlineStatus: 0,
  healthStatus: 0,
  installTime: formatDateTime(new Date()),
  plotId: undefined,
})

const form = ref<DeviceFormModel>(createEmptyForm())
const isUnhealthyDevice = computed(() => form.value.healthStatus === 1 || form.value.healthStatus === 2)

// ================== 校验 ==================
const formRules = computed<FormRules<DeviceFormModel>>(() => ({
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  typeId: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  controlStatus: [{ required: true, message: '请选择期望状态', trigger: 'change' }],
  installTime: [{ required: true, message: '请选择安装时间', trigger: 'change' }],
  plotId: [{ required: true, message: '请选择安装地块', trigger: 'change' }],
}))

// ================== ⭐ 加载地块 ==================
const loadPlotOptions = async () => {
  const res = await listPlots({ pageNum: 1, pageSize: 1000 })
  plotOptions.value = res.data.list
}

const mapDeviceTypeToOption = (item: DeviceType) => ({
  label: item.typeName || item.typeCode || `设备类型${item.id}`,
  value: item.id || 0,
})

const loadDeviceTypeOptions = async () => {
  const result = await listDeviceTypes({ pageNum: 1, pageSize: 1000, status: 1 })
  deviceTypeOptions.value = result.data.list
    .filter((item) => item.id !== undefined && item.id !== null)
    .map(mapDeviceTypeToOption)
}

// ================== 表单操作 ==================
const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (device: IotDevice) => {
  form.value = {
    name: device.name || '',
    typeId: device.typeId,
    controlStatus: device.controlStatus ?? 0,
    onlineStatus: device.onlineStatus ?? 0,
    healthStatus: device.healthStatus ?? 0,
    installTime: device.installTime || device.createTime || '',
    plotId: device.plotId || undefined,
  }
}

// ================== 加载详情 ==================
const loadDeviceDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const result = await getIotDeviceById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

// ================== 提交 ==================
const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate()
  submitLoading.value = true

  try {
    // 设备编码由后端统一生成，避免前端手动输入造成重复或格式不一致。
    const payload: IotDevicePayload = {
      plotId: form.value.plotId!,
      name: form.value.name,
      typeId: form.value.typeId!,
      controlStatus: form.value.controlStatus,
      onlineStatus: form.value.onlineStatus,
      healthStatus: form.value.healthStatus,
      ...(form.value.installTime ? { installTime: form.value.installTime } : {}),
    }

    const result =
      isEdit.value
        ? await updateIotDevice({ id: props.id!, ...payload } as UpdateIotDevicePayload)
        : await addIotDevice(payload)

    // 表单只保存设备资料和期望状态，不宣称真实硬件已经执行。
    ElMessage.success(isEdit.value ? '设备配置已保存' : '设备信息已创建')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

// ================== 生命周期 ==================
watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) return

    resetForm()
    void loadPlotOptions() // ⭐ 加载地块
    void loadDeviceTypeOptions()

    if (isEdit.value) {
      void loadDeviceDetail(props.id!)
    }
  },
)

watch(
  () => form.value.healthStatus,
  (healthStatus) => {
    if (healthStatus === 1 || healthStatus === 2) {
      form.value.controlStatus = 0
      form.value.onlineStatus = 0
    }
  },
)
</script>

<template>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px" class="device-dialog"
    modal-class="device-dialog-overlay" align-center destroy-on-close @closed="resetForm">
    <div v-loading="detailLoading">
      <el-form ref="formRef" class="device-form" :model="form" :rules="formRules" label-width="112px"
        autocomplete="off">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入设备名称" clearable />
        </el-form-item>
        <el-form-item label="设备类型" prop="typeId">
          <el-select v-model="form.typeId" placeholder="请选择设备类型" filterable>
            <el-option v-for="item in deviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望状态" prop="controlStatus">
          <el-select v-model="form.controlStatus" placeholder="请选择期望状态" :disabled="isUnhealthyDevice">
            <el-option v-for="item in controlStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态" prop="onlineStatus">
          <el-select v-model="form.onlineStatus" placeholder="请选择在线状态" :disabled="isUnhealthyDevice">
            <el-option v-for="item in onlineStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康状态" prop="healthStatus">
          <el-select v-model="form.healthStatus" placeholder="请选择健康状态">
            <el-option v-for="item in healthStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="安装时间" prop="installTime">
          <el-date-picker v-model="form.installTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择安装时间" clearable />
        </el-form-item>
        <el-form-item label="安装地块" prop="plotId" class="form-wide">
          <el-select v-model="form.plotId" placeholder="请选择安装地块" filterable>
            <el-option v-for="item in plotOptions" :key="item.id" :label="item.plotName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.device-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.device-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.device-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.device-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.device-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.device-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.device-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.device-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.device-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px 0;
  padding: 2px 2px 0;
}

.device-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.device-form :deep(.el-select),
.device-form :deep(.el-date-editor) {
  width: 100%;
}

.device-form :deep(.el-input__wrapper),
.device-form :deep(.el-select__wrapper),
.device-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.device-form :deep(.el-input__wrapper),
.device-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.device-form :deep(.el-input__wrapper.is-focus),
.device-form :deep(.el-select__wrapper.is-focused),
.device-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  :global(.device-dialog) {
    width: calc(100vw - 28px) !important;
  }

  .device-form {
    grid-template-columns: 1fr;
  }
}
</style>
