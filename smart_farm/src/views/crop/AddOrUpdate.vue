<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  addCrop,
  getCropById,
  updateCrop,
  type Crop,
  type CropPayload,
  type UpdateCropPayload,
} from '@/api/crop'
import { listCropTypes, type CropType } from '@/api/cropType'
import { uploadManagedImage } from '@/api/managedImage'
import { getFileUrl } from '@/utils/utils'

interface CropFormModel {
  typeId?: number | null
  cropName: string
  variety?: string
  growthDays?: number
  suitableTemperature?: string
  suitableHumidity?: string
  suitablePh?: string
  imageUrl?: string
  description?: string
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [crop?: Crop]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑作物信息' : '新增作物信息'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const typeOptions = ref<CropType[]>([])
const imagePreviewUrl = ref('')

const CROP_IMAGE_STORAGE_PREFIX = 'smart_farm_crop_image:'

const createEmptyForm = (): CropFormModel => ({
  typeId: null,
  cropName: '',
  variety: '',
  growthDays: undefined,
  suitableTemperature: '',
  suitableHumidity: '',
  suitablePh: '',
  imageUrl: '',
  description: '',
  status: 1,
  remark: '',
})

const form = ref<CropFormModel>(createEmptyForm())

const formRules = computed<FormRules<CropFormModel>>(() => ({
  cropName: [
    { required: true, message: '请输入作物名称', trigger: 'blur' },
    { min: 1, max: 50, message: '作物名称长度不能超过 50 个字符', trigger: 'blur' },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  growthDays: [{ type: 'number', min: 0, message: '生长周期不能小于 0', trigger: 'change' }],
}))

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(CROP_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue
  }

  return getStoredImageUrl(normalizedValue)
}

const currentImagePreview = computed(() => imagePreviewUrl.value || resolveImageUrl(form.value.imageUrl))

const loadTypeOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000 })

  typeOptions.value = result.data.list.filter((item) => item.status !== 0)
}

const resetForm = () => {
  form.value = createEmptyForm()
  imagePreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (crop: Crop) => {
  form.value = {
    typeId: crop.typeId ?? null,
    cropName: crop.cropName || '',
    variety: crop.variety || '',
    growthDays: crop.growthDays,
    suitableTemperature: crop.suitableTemperature || '',
    suitableHumidity: crop.suitableHumidity || '',
    suitablePh: crop.suitablePh || '',
    imageUrl: crop.imageUrl || '',
    description: crop.description || '',
    status: crop.status ?? 1,
    remark: crop.remark || '',
  }
  imagePreviewUrl.value = ''
}

const loadCropDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getCropById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const validateImage = (rawFile: File) => {
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  const isAllowedImage = allowTypes.includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isAllowedImage) {
    ElMessage.warning('仅支持 JPG、PNG 或 WEBP 图片')
    return false
  }

  if (!isLt2M) {
    ElMessage.warning('图片大小不能超过 2MB')
    return false
  }

  return true
}

const handleImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (rawFile && validateImage(rawFile)) {
    uploadLoading.value = true
    try {
      const result = await uploadManagedImage(rawFile, 'crop')
      form.value.imageUrl = result.data.imageUrl
      imagePreviewUrl.value = getFileUrl(result.data.imageUrl)
      ElMessage.success('作物图片上传成功')
    } finally {
      uploadLoading.value = false
    }
  }
}

const removeEmptyOptionalFields = <T extends Record<string, unknown>>(data: T) => {
  Object.keys(data).forEach((key) => {
    if (data[key] === '' || data[key] === undefined) {
      delete data[key]
    }
  })

  return data
}

const buildCreatePayload = () => {
  return removeEmptyOptionalFields({ ...form.value }) as CropPayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as UpdateCropPayload
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
        ? await addCrop(buildCreatePayload())
        : await updateCrop(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑作物信息成功' : '新增作物信息成功')
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
    void loadTypeOptions()

    if (props.id !== undefined && props.id !== null) {
      void loadCropDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="crop-dialog"
    modal-class="crop-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="crop-form"
        :model="form"
        :rules="formRules"
        label-width="104px"
        autocomplete="off"
      >
        <el-form-item label="作物图片" prop="imageUrl">
          <el-upload
            v-loading="uploadLoading"
            class="crop-image-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageChange"
          >
            <img v-if="currentImagePreview" class="upload-image" :src="currentImagePreview" alt="作物图片" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="作物名称" prop="cropName">
          <el-input v-model.trim="form.cropName" placeholder="请输入作物名称" clearable />
        </el-form-item>
        <el-form-item label="作物类型" prop="typeId">
          <el-select v-model="form.typeId" placeholder="请选择作物类型" clearable>
            <el-option
              v-for="item in typeOptions"
              :key="item.id"
              :label="item.typeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="品种" prop="variety">
          <el-input v-model="form.variety" placeholder="请输入品种名称" clearable />
        </el-form-item>
        <el-form-item label="生长周期" prop="growthDays">
          <el-input-number v-model="form.growthDays" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="适宜温度" prop="suitableTemperature">
          <el-input v-model="form.suitableTemperature" placeholder="例如：18-25℃" clearable />
        </el-form-item>
        <el-form-item label="适宜湿度" prop="suitableHumidity">
          <el-input v-model="form.suitableHumidity" placeholder="例如：60%-80%" clearable />
        </el-form-item>
        <el-form-item label="适宜PH" prop="suitablePh">
          <el-input v-model="form.suitablePh" placeholder="例如：5.8-6.5" clearable />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="作物说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="请输入作物说明"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark" style="align-items: center;">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" :disabled="uploadLoading" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.crop-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.crop-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.crop-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.crop-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.crop-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.crop-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.crop-form {
  padding: 2px 2px 0;
}

.crop-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.crop-form :deep(.el-select),
.crop-form :deep(.el-input-number) {
  width: 100%;
}

.crop-form :deep(.el-input__wrapper),
.crop-form :deep(.el-select__wrapper),
.crop-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.crop-form :deep(.el-input__wrapper),
.crop-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.crop-image-uploader :deep(.el-upload) {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.crop-image-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.upload-icon {
  color: #94a3b8;
  font-size: 23px;
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #ffffff;
}

@media (max-width: 720px) {
  :global(.crop-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
