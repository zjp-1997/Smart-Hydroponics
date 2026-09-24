<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  addCropType,
  getCropTypeById,
  listCropTypes,
  updateCropType,
  type CropType,
  type CropTypePayload,
  type UpdateCropTypePayload,
} from '@/api/cropType'

interface CropTypeFormModel {
  parentId?: number | null
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
  success: [cropType?: CropType]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑作物类型' : '新增作物类型'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const parentOptions = ref<CropType[]>([])

const createEmptyForm = (): CropTypeFormModel => ({
  parentId: null,
  typeName: '',
  description: '',
  status: 1,
})

const form = ref<CropTypeFormModel>(createEmptyForm())

const formRules = computed<FormRules<CropTypeFormModel>>(() => ({
  typeName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '分类名称长度为 2-50 个字符', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const loadParentOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000 })

  parentOptions.value = result.data.list.filter((item) => item.id !== props.id)
}

const resetForm = () => {
  form.value = createEmptyForm()
  formRef.value?.clearValidate()
}

const fillForm = (cropType: CropType) => {
  form.value = {
    parentId: cropType.parentId ?? null,
    typeName: cropType.typeName || '',
    description: cropType.description || '',
    status: cropType.status ?? 1,
  }
}

const loadCropTypeDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getCropTypeById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const removeEmptyFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '') {
      delete data[key]
    }
  })

  return data
}

const buildCreatePayload = () => {
  return removeEmptyFields({ ...form.value }) as CropTypePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyFields({ id, ...form.value }) as UpdateCropTypePayload
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result =
      props.id === undefined || props.id === null
        ? await addCropType(buildCreatePayload())
        : await updateCropType(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑作物类型成功' : '新增作物类型成功')
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
    void loadParentOptions()

    if (props.id !== undefined && props.id !== null) {
      void loadCropTypeDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="540px"
    class="crop-type-dialog"
    modal-class="crop-type-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="crop-type-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="分类名称" prop="typeName">
          <el-input v-model.trim="form.typeName" placeholder="请输入分类名称" clearable />
        </el-form-item>
        <el-form-item label="父分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择父分类" clearable>
            <el-option :value="null" label="一级分类" />
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="item.typeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入作物类型描述"
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
:global(.crop-type-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.crop-type-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.crop-type-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.crop-type-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.crop-type-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.crop-type-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.crop-type-form {
  padding: 2px 2px 0;
}

.crop-type-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.crop-type-form :deep(.el-select) {
  width: 100%;
}

.crop-type-form :deep(.el-input__wrapper),
.crop-type-form :deep(.el-select__wrapper),
.crop-type-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.crop-type-form :deep(.el-input__wrapper),
.crop-type-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

@media (max-width: 720px) {
  :global(.crop-type-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
