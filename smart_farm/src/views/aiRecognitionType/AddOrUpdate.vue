<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addAiRecognitionType,
  getAiRecognitionTypeById,
  updateAiRecognitionType,
  type AiRecognitionType,
  type AiRecognitionTypePayload,
  type UpdateAiRecognitionTypePayload,
} from '@/api/aiRecognitionType'

interface AiRecognitionTypeFormModel {
  typeCode: string
  typeName: string
  description?: string
  status?: number
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [recognitionType?: AiRecognitionType]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑AI识别类型' : '新增AI识别类型'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)

const createEmptyForm = (): AiRecognitionTypeFormModel => ({
  typeCode: '',
  typeName: '',
  description: '',
  status: 1,
})

const form = ref<AiRecognitionTypeFormModel>(createEmptyForm())

const formRules = computed<FormRules<AiRecognitionTypeFormModel>>(() => ({
  typeCode: [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    { min: 2, max: 50, message: '类型编码长度为 2-50 个字符', trigger: 'blur' },
  ],
  typeName: [
    { required: true, message: '请输入类型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '类型名称长度为 2-50 个字符', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (recognitionType: AiRecognitionType) => {
  form.value = {
    typeCode: recognitionType.typeCode || '',
    typeName: recognitionType.typeName || '',
    description: recognitionType.description || '',
    status: recognitionType.status ?? 1,
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getAiRecognitionTypeById(id)
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
  }) as AiRecognitionTypePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...buildCreatePayload() }) as UpdateAiRecognitionTypePayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result = isEdit.value
      ? await updateAiRecognitionType(buildUpdatePayload(props.id!))
      : await addAiRecognitionType(buildCreatePayload())

    ElMessage.success(isEdit.value ? '编辑AI识别类型成功' : '新增AI识别类型成功')
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
      void loadDetail(props.id!)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="ai-type-dialog"
    modal-class="ai-type-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="ai-type-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model.trim="form.typeCode" placeholder="例如 CROP_RECOGNITION" clearable />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model.trim="form.typeName" placeholder="请输入识别类型名称" clearable />
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
            placeholder="请输入该识别类型适用的业务场景"
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
:global(.ai-type-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.ai-type-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.ai-type-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.ai-type-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.ai-type-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.ai-type-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.ai-type-form {
  padding: 2px 2px 0;
}

.ai-type-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.ai-type-form :deep(.el-input__wrapper),
.ai-type-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.ai-type-form :deep(.el-input__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.ai-type-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
