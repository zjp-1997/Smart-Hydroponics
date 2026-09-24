<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addCameraDevice,
  getCameraDeviceById,
  updateCameraDevice,
  type CameraDevice,
  type CameraDevicePayload,
  type UpdateCameraDevicePayload,
} from '@/api/cameraDevice'
import { listPlots, type Plot } from '@/api/plot'

interface CameraFormModel {
  deviceId?: number
  plotId?: number
  name: string
  streamProtocol: string
  streamUrl?: string
  snapshotUrl?: string
  resolution?: string
  onlineStatus?: number
  direction?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [camera?: CameraDevice]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑监控设备' : '新增监控设备'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const submitLoading = ref(false)
const detailLoading = ref(false)
const plotLoading = ref(false)
const formRef = ref<FormInstance>()
const plotOptions = ref<Plot[]>([])

const protocolOptions = [
  { label: 'RTSP', value: 'RTSP' },
  { label: 'GB28181', value: 'GB28181' },
  { label: 'HTTP', value: 'HTTP' },
]

const onlineStatusOptions = [
  { label: '在线', value: 1 },
  { label: '离线', value: 0 },
]

const createEmptyForm = (): CameraFormModel => ({
  deviceId: undefined,
  plotId: undefined,
  name: '',
  streamProtocol: 'RTSP',
  streamUrl: '',
  snapshotUrl: '',
  resolution: '1920x1080',
  onlineStatus: 0,
  direction: '',
})

const form = ref<CameraFormModel>(createEmptyForm())

const formRules = computed<FormRules<CameraFormModel>>(() => ({
  deviceId: [{ required: true, message: '请输入来源设备编号', trigger: 'blur' }],
  plotId: [{ required: true, message: '请选择所属地块', trigger: 'change' }],
  name: [
    { required: true, message: '请输入监控设备名称', trigger: 'blur' },
    { min: 2, max: 100, message: '名称长度为 2-100 个字符', trigger: 'blur' },
  ],
  streamProtocol: [{ required: true, message: '请选择视频流协议', trigger: 'change' }],
  onlineStatus: [{ required: true, message: '请选择在线状态', trigger: 'change' }],
  streamUrl: [{ max: 500, message: '视频流地址不能超过 500 个字符', trigger: 'blur' }],
  snapshotUrl: [{ max: 500, message: '截图地址不能超过 500 个字符', trigger: 'blur' }],
  resolution: [{ max: 50, message: '分辨率不能超过 50 个字符', trigger: 'blur' }],
  direction: [{ max: 50, message: '监控方向不能超过 50 个字符', trigger: 'blur' }],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '' || data[key] === undefined || data[key] === null) {
      delete data[key]
    }
  })

  return data
}

const buildCreatePayload = () => {
  return removeEmptyOptionalFields({
    deviceId: form.value.deviceId,
    plotId: form.value.plotId,
    name: form.value.name.trim(),
    streamProtocol: form.value.streamProtocol,
    streamUrl: form.value.streamUrl?.trim(),
    snapshotUrl: form.value.snapshotUrl?.trim(),
    resolution: form.value.resolution?.trim(),
    onlineStatus: form.value.onlineStatus,
    direction: form.value.direction?.trim(),
  }) as CameraDevicePayload
}

const buildUpdatePayload = (id: number) => {
  return { id, ...buildCreatePayload() } as UpdateCameraDevicePayload
}

const fillForm = (camera: CameraDevice) => {
  form.value = {
    deviceId: camera.deviceId,
    plotId: camera.plotId,
    name: camera.name || '',
    streamProtocol: camera.streamProtocol || 'RTSP',
    streamUrl: camera.streamUrl || '',
    snapshotUrl: camera.snapshotUrl || '',
    resolution: camera.resolution || '1920x1080',
    onlineStatus: camera.onlineStatus ?? 0,
    direction: camera.direction || '',
  }
}

const loadPlotOptions = async () => {
  plotLoading.value = true

  try {
    const result = await listPlots({ pageNum: 1, pageSize: 1000, status: 1 })
    plotOptions.value = result.data.list
  } finally {
    plotLoading.value = false
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getCameraDeviceById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result = isEdit.value
      ? await updateCameraDevice(buildUpdatePayload(props.id!))
      : await addCameraDevice(buildCreatePayload())

    ElMessage.success(isEdit.value ? '编辑监控设备成功' : '新增监控设备成功')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) {
      return
    }

    resetForm()
    void loadPlotOptions()

    if (isEdit.value) {
      void loadDetail(props.id!)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="660px"
    class="camera-dialog"
    modal-class="camera-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form ref="formRef" class="camera-form" :model="form" :rules="formRules" label-width="112px" autocomplete="off">
        <el-form-item label="来源设备编号" prop="deviceId">
          <el-input-number v-model="form.deviceId" :min="1" :max="999999999" controls-position="right" />
        </el-form-item>
        <el-form-item label="所属地块" prop="plotId">
          <el-select v-model="form.plotId" placeholder="请选择所属地块" filterable :loading="plotLoading">
            <el-option
              v-for="item in plotOptions"
              :key="item.id"
              :label="item.plotCode ? `${item.plotName}（${item.plotCode}）` : item.plotName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入监控设备名称" clearable />
        </el-form-item>
        <el-form-item label="视频协议" prop="streamProtocol">
          <el-select v-model="form.streamProtocol" placeholder="请选择视频协议">
            <el-option v-for="item in protocolOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="在线状态" prop="onlineStatus">
          <el-select v-model="form.onlineStatus" placeholder="请选择在线状态">
            <el-option v-for="item in onlineStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分辨率" prop="resolution">
          <el-input v-model.trim="form.resolution" placeholder="例如 1920x1080" clearable />
        </el-form-item>
        <el-form-item label="监控方向" prop="direction">
          <el-input v-model.trim="form.direction" placeholder="例如 东侧苗床" clearable />
        </el-form-item>
        <el-form-item label="视频流地址" prop="streamUrl" class="form-wide">
          <el-input v-model.trim="form.streamUrl" placeholder="请输入视频流地址" clearable />
        </el-form-item>
        <el-form-item label="截图地址" prop="snapshotUrl" class="form-wide">
          <el-input v-model.trim="form.snapshotUrl" placeholder="请输入截图地址" clearable />
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
:global(.camera-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.camera-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.camera-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.camera-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.camera-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.camera-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.camera-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
  padding: 2px 2px 0;
}

.camera-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.camera-form :deep(.el-select),
.camera-form :deep(.el-input-number) {
  width: 100%;
}

.camera-form :deep(.el-input__wrapper),
.camera-form :deep(.el-select__wrapper) {
  min-height: 38px;
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.camera-form :deep(.el-input__wrapper.is-focus),
.camera-form :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  :global(.camera-dialog) {
    width: calc(100vw - 28px) !important;
  }

  .camera-form {
    grid-template-columns: 1fr;
  }
}
</style>
