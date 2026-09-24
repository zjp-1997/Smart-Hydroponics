<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  addCropImage,
  getCropImageById,
  uploadCropImage,
  updateCropImage,
  type CropImage,
  type CropImagePayload,
  type UpdateCropImagePayload,
} from '@/api/cropImage'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getFileUrl } from '@/utils/utils'

interface CropImageFormModel {
  userId?: number
  imageUrl: string
  imageSize?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [cropImage?: CropImage]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑手机图片' : '新增手机图片'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const userOptions = ref<SmartPlantUser[]>([])
const imagePreviewUrl = ref('')

const PHONE_IMAGE_STORAGE_PREFIX = 'smart_farm_phone_picture:'

const createEmptyForm = (): CropImageFormModel => ({
  userId: undefined,
  imageUrl: '',
  imageSize: undefined,
  remark: '',
})

const form = ref<CropImageFormModel>(createEmptyForm())

const formRules = computed<FormRules<CropImageFormModel>>(() => ({
  userId: [{ required: true, message: '请选择上传用户', trigger: 'change' }],
  imageUrl: [
    { required: true, message: '请选择需要上传的图片', trigger: 'change' },
    { max: 500, message: '图片地址不能超过 500 个字符', trigger: 'blur' },
  ],
  imageSize: [{ type: 'number', min: 0, message: '图片大小不能小于 0', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}))

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(PHONE_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (!normalizedValue) {
    return ''
  }

  if (/^(blob:|data:image\/)/.test(normalizedValue)) {
    return normalizedValue
  }

  if (/^https?:\/\//i.test(normalizedValue) || normalizedValue.startsWith('/')) {
    return getFileUrl(normalizedValue)
  }

  return getStoredImageUrl(normalizedValue) || getFileUrl(normalizedValue)
}

const currentImagePreview = computed(() => imagePreviewUrl.value || resolveImageUrl(form.value.imageUrl))

const formatUserLabel = (user: SmartPlantUser) => {
  return `${user.nickname || user.username || '用户'}${user.username ? ` (${user.username})` : ''}`
}

const loadUsers = async () => {
  const result = await listSmartPlantUsers({ pageNum: 1, pageSize: 1000, status: 1 })
  userOptions.value = result.data.list
}

const resetForm = () => {
  form.value = createEmptyForm()
  imagePreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (cropImage: CropImage) => {
  form.value = {
    userId: cropImage.userId,
    imageUrl: cropImage.imageUrl || '',
    imageSize: cropImage.imageSize,
    remark: cropImage.remark || '',
  }
  imagePreviewUrl.value = ''
}

const loadCropImageDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getCropImageById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const validateImage = (rawFile: File) => {
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  const isAllowedImage = allowTypes.includes(rawFile.type)
  const isLt5M = rawFile.size / 1024 / 1024 < 5

  if (!isAllowedImage) {
    ElMessage.warning('仅支持 JPG、PNG 或 WEBP 图片')
    return false
  }

  if (!isLt5M) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }

  return true
}

const formatBytes = (value?: number) => {
  if (value === undefined || value === null) {
    return '选择图片后自动计算'
  }

  if (value < 1024) {
    return `${value} B`
  }

  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }

  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

const handleImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (rawFile && validateImage(rawFile)) {
    uploadLoading.value = true
    imagePreviewUrl.value = URL.createObjectURL(rawFile)

    try {
      const result = await uploadCropImage(rawFile)
      form.value.imageUrl = result.data.imageUrl
      form.value.imageSize = result.data.imageSize
      imagePreviewUrl.value = getFileUrl(result.data.imageUrl)
      formRef.value?.clearValidate(['imageUrl', 'imageSize'])
      ElMessage.success('图片上传成功')
    } finally {
      uploadLoading.value = false
    }
  }
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
  return removeEmptyOptionalFields({ ...form.value }) as CropImagePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as UpdateCropImagePayload
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
        ? await addCropImage(buildCreatePayload())
        : await updateCropImage(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑手机图片成功' : '新增手机图片成功')
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
    void loadUsers()

    if (props.id !== undefined && props.id !== null) {
      void loadCropImageDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="580px"
    class="phone-picture-dialog"
    modal-class="phone-picture-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading || uploadLoading">
      <el-form
        ref="formRef"
        class="phone-picture-form"
        :model="form"
        :rules="formRules"
        label-width="104px"
        autocomplete="off"
      >
        <el-form-item label="图片预览" prop="imageUrl">
          <el-upload
            class="phone-image-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :disabled="uploadLoading"
            :on-change="handleImageChange"
          >
            <img v-if="currentImagePreview" class="upload-image" :src="currentImagePreview" alt="手机图片" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="上传用户" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择上传用户" filterable clearable>
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="formatUserLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input :model-value="form.imageUrl" placeholder="选择图片后自动生成在线链接" disabled />
        </el-form-item>
        <el-form-item label="图片大小" prop="imageSize">
          <el-input :model-value="formatBytes(form.imageSize)" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark" style="align-items: center;">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading || uploadLoading" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.phone-picture-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.phone-picture-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.phone-picture-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.phone-picture-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.phone-picture-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.phone-picture-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.phone-picture-form {
  padding: 2px 2px 0;
}

.phone-picture-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.phone-picture-form :deep(.el-select),
.phone-picture-form :deep(.el-input-number) {
  width: 100%;
}

.phone-picture-form :deep(.el-input__wrapper),
.phone-picture-form :deep(.el-select__wrapper),
.phone-picture-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.phone-picture-form :deep(.el-input__wrapper),
.phone-picture-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.phone-image-uploader :deep(.el-upload) {
  width: 88px;
  height: 88px;
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

.phone-image-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.upload-icon {
  color: #94a3b8;
  font-size: 24px;
}

.upload-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #ffffff;
}

@media (max-width: 720px) {
  :global(.phone-picture-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
