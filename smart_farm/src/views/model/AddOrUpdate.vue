<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addAiModel,
  getAiModelById,
  updateAiModel,
  type AiModel,
  type AiModelPayload,
  type UpdateAiModelPayload,
} from '@/api/model'

interface AiModelFormModel {
  modelName: string
  baseUrl: string
  apiKey: string
  model: string
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [model?: AiModel]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑模型' : '新增模型'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)

const createEmptyForm = (): AiModelFormModel => ({
  modelName: '',
  baseUrl: '',
  apiKey: '',
  model: '',
  status: 1,
  remark: '',
})

const form = ref<AiModelFormModel>(createEmptyForm())

const formRules = computed<FormRules<AiModelFormModel>>(() => ({
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { min: 2, max: 100, message: '模型名称长度为 2-100 个字符', trigger: 'blur' },
  ],
  baseUrl: [
    { required: true, message: '请输入模型服务地址', trigger: 'blur' },
    { min: 8, max: 500, message: '模型服务地址长度为 8-500 个字符', trigger: 'blur' },
  ],
  apiKey: [
    {
      validator: (_rule, value: string, callback) => {
        // 新增时必须填写密钥；编辑时留空代表沿用原密钥，避免明文密钥在页面中来回传递。
        if (!isEdit.value && !String(value || '').trim()) {
          callback(new Error('请输入 API Key'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  model: [
    { required: true, message: '请输入模型标识', trigger: 'blur' },
    { min: 2, max: 100, message: '模型标识长度为 2-100 个字符', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (model: AiModel) => {
  form.value = {
    modelName: model.modelName || '',
    baseUrl: model.baseUrl || '',
    apiKey: '',
    model: model.model || '',
    status: model.status ?? 1,
    remark: model.remark || '',
  }
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getAiModelById(id)
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
    modelName: form.value.modelName.trim(),
    baseUrl: form.value.baseUrl.trim(),
    apiKey: form.value.apiKey.trim(),
    model: form.value.model.trim(),
    status: form.value.status,
    remark: form.value.remark?.trim(),
  }) as AiModelPayload
}

const buildUpdatePayload = (id: number) => {
  // 编辑时如果 apiKey 为空则不提交该字段，后端会保留原 API Key。
  return removeEmptyOptionalFields({ id, ...buildCreatePayload() }) as UpdateAiModelPayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result = isEdit.value
      ? await updateAiModel(buildUpdatePayload(props.id!))
      : await addAiModel(buildCreatePayload())

    ElMessage.success(isEdit.value ? '编辑模型成功' : '新增模型成功')
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
    width="620px"
    class="model-dialog"
    modal-class="model-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="model-form"
        :model="form"
        :rules="formRules"
        label-width="104px"
        autocomplete="off"
      >
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model.trim="form.modelName" placeholder="请输入模型展示名称" clearable />
        </el-form-item>
        <el-form-item label="模型地址" prop="baseUrl">
          <el-input v-model.trim="form.baseUrl" placeholder="例如 https://api.example.com/v1" clearable />
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model.trim="form.apiKey"
            type="password"
            show-password
            :placeholder="isEdit ? '不修改密钥请留空' : '请输入 API Key'"
            clearable
          />
        </el-form-item>
        <el-form-item label="模型标识" prop="model">
          <el-input v-model.trim="form.model" placeholder="例如 gpt-4o-mini" clearable />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入模型用途、供应商或调用说明"
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
:global(.model-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.model-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.model-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.model-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.model-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.model-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.model-form {
  padding: 2px 2px 0;
}

.model-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.model-form :deep(.el-input__wrapper),
.model-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.model-form :deep(.el-input__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.model-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
