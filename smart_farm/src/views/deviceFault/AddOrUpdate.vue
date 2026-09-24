<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listIotDevices, type IotDevice } from '@/api/iotDevice'
import {
  addIotDeviceFault,
  getIotDeviceFaultById,
  updateIotDeviceFault,
  type IotDeviceFault,
  type IotDeviceFaultPayload,
  type UpdateIotDeviceFaultPayload,
} from '@/api/iotDeviceFault'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { isAdminUser } from '@/utils/auth'

interface FaultFormModel {
  deviceId?: number
  faultCode: string
  faultName: string
  faultType?: number
  severity?: number
  faultDesc?: string
  startTime?: string
  status?: number
  handleUserId?: number
  handleResult?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [fault?: IotDeviceFault]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑设备故障' : '新增设备故障'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const deviceOptions = ref<IotDevice[]>([])
const technicianOptions = ref<SmartPlantUser[]>([])
const isAdmin = computed(() => isAdminUser())
const originalStatus = ref<number>(0)
const originalAssignStatus = ref<number>(0)

const faultTypeOptions = [
  { label: '通信故障', value: 1 },
  { label: '传感器故障', value: 2 },
  { label: '电源故障', value: 3 },
  { label: '执行器故障', value: 4 },
  { label: '数据异常', value: 5 },
  { label: '其他故障', value: 6 },
]

const severityOptions = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '严重', value: 4 },
]

const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '处理中', value: 1 },
  { label: '已处理', value: 2 },
  { label: '已关闭', value: 3 },
]

const createEmptyForm = (): FaultFormModel => ({
  deviceId: undefined,
  faultCode: '',
  faultName: '',
  faultType: undefined,
  severity: 1,
  faultDesc: '',
  startTime: '',
  status: 0,
  handleUserId: undefined,
  handleResult: '',
})

const form = ref<FaultFormModel>(createEmptyForm())

const formRules = computed<FormRules<FaultFormModel>>(() => ({
  deviceId: [{ required: true, message: '请选择故障设备', trigger: 'change' }],
  faultCode: [{ required: true, message: '请输入故障编码', trigger: 'blur' }],
  faultName: [{ required: true, message: '请输入故障名称', trigger: 'blur' }],
  faultType: [{ required: true, message: '请选择故障类型', trigger: 'change' }],
  severity: [{ required: true, message: '请选择严重程度', trigger: 'change' }],
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  handleResult: form.value.status === 2 || form.value.status === 3
    ? [{ required: true, message: '完成或关闭故障前请填写处理结果', trigger: 'blur' }]
    : [],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  originalStatus.value = 0
  originalAssignStatus.value = 0
  formRef.value?.clearValidate()
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  return Object.fromEntries(
    Object.entries(data).filter(([, value]) => value !== '' && value !== undefined && value !== null),
  ) as T
}

const isTechnicianUser = (user: SmartPlantUser) => {
  return user.roleCode === 'technician' || user.roleName === '技术人员'
}

const loadOptions = async () => {
  const deviceResult = await listIotDevices({ pageNum: 1, pageSize: 10000 })
  deviceOptions.value = deviceResult.data.list

  if (!isAdmin.value) {
    technicianOptions.value = []
    return
  }

  const userResult = await listSmartPlantUsers({ pageNum: 1, pageSize: 10000, status: 1 })
  technicianOptions.value = userResult.data.list.filter(isTechnicianUser)
}

const fillForm = (fault: IotDeviceFault) => {
  originalStatus.value = fault.status ?? 0
  originalAssignStatus.value = fault.assignStatus ?? 0
  form.value = {
    deviceId: fault.deviceId,
    faultCode: fault.faultCode || '',
    faultName: fault.faultName || '',
    faultType: fault.faultType,
    severity: fault.severity ?? 1,
    faultDesc: fault.faultDesc || '',
    startTime: fault.startTime || '',
    status: fault.status ?? 0,
    handleUserId: fault.handleUserId,
    handleResult: fault.handleResult || '',
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true
  try {
    const result = await getIotDeviceFaultById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate()
  submitLoading.value = true

  try {
    const payload = removeEmptyOptionalFields({
      deviceId: form.value.deviceId!,
      faultCode: form.value.faultCode,
      faultName: form.value.faultName,
      faultType: form.value.faultType!,
      severity: form.value.severity,
      faultDesc: form.value.faultDesc,
      startTime: form.value.startTime,
      status: form.value.status,
      handleUserId: form.value.handleUserId,
      handleResult: form.value.handleResult,
    }) as IotDeviceFaultPayload

    const result = isEdit.value
      ? await updateIotDeviceFault({ id: props.id!, ...payload } as UpdateIotDeviceFaultPayload)
      : await addIotDeviceFault(payload)

    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

const isStatusOptionDisabled = (status: number) => {
  if (!isEdit.value) return status !== 0
  if (originalStatus.value === 3) return status !== 3
  if (originalStatus.value === 2) return status !== 2 && status !== 3
  if (status === 2) return originalAssignStatus.value !== 2
  if (status === 3) return originalStatus.value !== 2
  return false
}

watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) return

    resetForm()
    void loadOptions()

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
    width="680px"
    class="fault-dialog"
    modal-class="fault-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form ref="formRef" class="fault-form" :model="form" :rules="formRules" label-width="112px" autocomplete="off">
        <el-form-item label="故障设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="请选择故障设备" filterable>
            <el-option
              v-for="item in deviceOptions"
              :key="item.id"
              :label="`${item.name}（${item.deviceCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障编码" prop="faultCode">
          <el-input v-model.trim="form.faultCode" placeholder="请输入故障编码" clearable />
        </el-form-item>
        <el-form-item label="故障名称" prop="faultName">
          <el-input v-model.trim="form.faultName" placeholder="请输入故障名称" clearable />
        </el-form-item>
        <el-form-item label="故障类型" prop="faultType">
          <el-select v-model="form.faultType" placeholder="请选择故障类型">
            <el-option v-for="item in faultTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-select v-model="form.severity" placeholder="请选择严重程度">
            <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择处理状态">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
              :disabled="isStatusOptionDisabled(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="技术人员" prop="handleUserId">
          <el-select v-model="form.handleUserId" placeholder="请选择技术人员" filterable clearable>
            <el-option
              v-for="item in technicianOptions"
              :key="item.id"
              :label="item.nickname || item.username"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请选择开始时间"
            clearable
          />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDesc" class="form-wide">
          <el-input v-model="form.faultDesc" type="textarea" maxlength="255" show-word-limit placeholder="请输入故障描述" />
        </el-form-item>
        <el-form-item label="处理结果" prop="handleResult" class="form-wide">
          <el-input v-model="form.handleResult" type="textarea" maxlength="255" show-word-limit placeholder="请输入处理结果" />
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
:global(.fault-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.fault-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.fault-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.fault-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 28px;
}

:global(.fault-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 16px 24px 20px;
  border-top: 1px solid #eef2f7;
}

.fault-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 28px;
}

.fault-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.fault-form :deep(.el-select),
.fault-form :deep(.el-date-editor) {
  width: 100%;
}

.form-wide {
  grid-column: 1 / -1;
}

@media (max-width: 720px) {
  .fault-form {
    grid-template-columns: 1fr;
  }
}
</style>
