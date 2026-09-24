<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addDeviceType,
  getDeviceTypeById,
  updateDeviceType,
  type DeviceType,
  type DeviceTypePayload,
  type UpdateDeviceTypePayload,
} from '@/api/deviceType'

interface DeviceTypeFormModel {
  typeCode: string
  typeName: string
  category?: number
  description?: string
  status?: number
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [deviceType?: DeviceType]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑设备类型' : '新增设备类型'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)

const categoryOptions = [
  { label: '传感器', value: 1 },
  { label: '执行器', value: 2 },
]

const createEmptyForm = (): DeviceTypeFormModel => ({
  typeCode: '',
  typeName: '',
  category: undefined,
  description: '',
  status: 1,
})

const form = ref<DeviceTypeFormModel>(createEmptyForm())

const formRules = computed<FormRules<DeviceTypeFormModel>>(() => ({
  typeCode: [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    { min: 2, max: 50, message: '类型编码长度为 2-50 个字符', trigger: 'blur' },
  ],
  typeName: [
    { required: true, message: '请输入类型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '类型名称长度为 2-50 个字符', trigger: 'blur' },
  ],
  category: [{ required: true, message: '请选择设备分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (deviceType: DeviceType) => {
  form.value = {
    typeCode: deviceType.typeCode || '',
    typeName: deviceType.typeName || '',
    category: deviceType.category,
    description: deviceType.description || '',
    status: deviceType.status ?? 1,
  }
}

const loadDeviceTypeDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getDeviceTypeById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '') {
      delete data[key]
    }
  })

  return data
}

const buildCreatePayload = () => {
  return removeEmptyOptionalFields({
    ...form.value,
    typeCode: form.value.typeCode.trim().toUpperCase(),
    typeName: form.value.typeName.trim(),
  }) as DeviceTypePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...buildCreatePayload() }) as UpdateDeviceTypePayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result = isEdit.value
      ? await updateDeviceType(buildUpdatePayload(props.id!))
      : await addDeviceType(buildCreatePayload())

    ElMessage.success(isEdit.value ? '编辑设备类型成功' : '新增设备类型成功')
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

    if (isEdit.value) {
      void loadDeviceTypeDetail(props.id!)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="device-type-dialog"
    modal-class="device-type-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="device-type-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model.trim="form.typeCode" placeholder="请输入唯一类型编码" clearable />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model.trim="form.typeName" placeholder="请输入设备类型名称" clearable />
        </el-form-item>
        <el-form-item label="设备分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择设备分类">
            <el-option
              v-for="item in categoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入该类型设备的用途或能力说明"
          />
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
:global(.device-type-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.device-type-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.device-type-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.device-type-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.device-type-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.device-type-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.device-type-form {
  padding: 2px 2px 0;
}

.device-type-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.device-type-form :deep(.el-select) {
  width: 100%;
}

.device-type-form :deep(.el-input__wrapper),
.device-type-form :deep(.el-select__wrapper),
.device-type-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.device-type-form :deep(.el-input__wrapper),
.device-type-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.device-type-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
