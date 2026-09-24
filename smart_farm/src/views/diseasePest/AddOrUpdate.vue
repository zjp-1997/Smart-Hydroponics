<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import {
  addDiseasePest,
  getDiseasePestById,
  updateDiseasePest,
  type DiseasePest,
  type DiseasePestPayload,
  type UpdateDiseasePestPayload,
} from '@/api/diseasePest'
import { listCropTypes, type CropType } from '@/api/cropType'
import { uploadManagedImage } from '@/api/managedImage'
import { getFileUrl } from '@/utils/utils'

interface DiseasePestFormModel {
  cropTypeId?: number | null
  affectedCrops?: string
  name: string
  type?: number
  symptom?: string
  cause?: string
  suitableStage?: string
  occurrencePeriod?: string
  livingHabits?: string
  suitableEnvironment?: string
  transmissionRoute?: string
  coverImage?: string
  imageUrls: string[]
  status?: number
  sortOrder?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [diseasePest?: DiseasePest]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑病虫害基本信息' : '新增病虫害基本信息'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const detailUploadLoading = ref(false)
const cropTypeOptions = ref<CropType[]>([])
const uploadedCoverPreviewUrl = ref('')
const DISEASE_COVER_STORAGE_PREFIX = 'smart_farm_disease_cover:'

const typeOptions = [
  { label: '病害', value: 1, tagType: 'danger' },
  { label: '虫害', value: 2, tagType: 'warning' },
  { label: '生理性病害', value: 3, tagType: 'info' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const createEmptyForm = (): DiseasePestFormModel => ({
  cropTypeId: undefined,
  affectedCrops: '',
  name: '',
  type: 1,
  symptom: '',
  cause: '',
  suitableStage: '',
  occurrencePeriod: '',
  livingHabits: '',
  suitableEnvironment: '',
  transmissionRoute: '',
  coverImage: '',
  imageUrls: [],
  status: 1,
  sortOrder: 0,
  remark: '',
})

const form = ref<DiseasePestFormModel>(createEmptyForm())

const formRules = computed<FormRules<DiseasePestFormModel>>(() => ({
  name: [
    { required: true, message: '请输入病虫害名称', trigger: 'blur' },
    { max: 100, message: '病虫害名称不能超过 100 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择病虫害类型', trigger: 'change' }],
  symptom: [{ max: 500, message: '症状描述不能超过 500 个字符', trigger: 'blur' }],
  cause: [{ max: 500, message: '发生原因不能超过 500 个字符', trigger: 'blur' }],
  suitableStage: [{ max: 100, message: '易发阶段不能超过 100 个字符', trigger: 'blur' }],
  affectedCrops: [{ max: 255, message: '危害作物不能超过 255 个字符', trigger: 'blur' }],
  occurrencePeriod: [{ max: 100, message: '发生时期不能超过 100 个字符', trigger: 'blur' }],
  livingHabits: [{ max: 1000, message: '生活习性不能超过 1000 个字符', trigger: 'blur' }],
  suitableEnvironment: [{ max: 1000, message: '适宜环境不能超过 1000 个字符', trigger: 'blur' }],
  transmissionRoute: [{ max: 1000, message: '传播途径不能超过 1000 个字符', trigger: 'blur' }],
}))

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredCoverUrl = (value?: string) => {
  if (!value?.startsWith(DISEASE_COVER_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

/**
 * 封面图使用 Element Plus 上传组件做本地预览。
 * 当前后端只接收图片地址字符串，所以这里把本地图片缓存为 dataURL key，提交 key 给后端保存。
 */
const currentCoverPreview = computed(() => {
  if (uploadedCoverPreviewUrl.value) {
    return uploadedCoverPreviewUrl.value
  }

  if (isImageUrl(form.value.coverImage)) {
    return form.value.coverImage?.startsWith('/') ? getFileUrl(form.value.coverImage) : form.value.coverImage
  }

  return getStoredCoverUrl(form.value.coverImage)
})

const validateImage = (rawFile: File) => {
  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  const isAllowedImage = allowTypes.includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isAllowedImage) {
    ElMessage.warning('图片仅支持 JPG、PNG 或 WEBP 格式')
    return false
  }

  if (!isLt2M) {
    ElMessage.warning('单张图片大小不能超过 2MB')
    return false
  }

  return true
}

const handleCoverChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (rawFile && validateImage(rawFile)) {
    uploadLoading.value = true
    try {
      const result = await uploadManagedImage(rawFile, 'disease')
      form.value.coverImage = result.data.imageUrl
      uploadedCoverPreviewUrl.value = getFileUrl(result.data.imageUrl)
      ElMessage.success('封面图上传成功')
    } finally {
      uploadLoading.value = false
    }
  }
}

/** 上传详情图片并把后端持久化地址加入 JSON 数组，最多保留 8 张。 */
const handleDetailImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (!rawFile || !validateImage(rawFile)) {
    return
  }
  if (form.value.imageUrls.length >= 8) {
    ElMessage.warning('详情图片最多上传 8 张')
    return
  }

  detailUploadLoading.value = true
  try {
    const result = await uploadManagedImage(rawFile, 'disease')
    form.value.imageUrls = [...form.value.imageUrls, result.data.imageUrl]
    ElMessage.success('详情图片上传成功')
  } finally {
    detailUploadLoading.value = false
  }
}

/** 从 JSON 图片数组中移除指定图片，数据库中的其他图片顺序保持不变。 */
const removeDetailImage = (index: number) => {
  form.value.imageUrls = form.value.imageUrls.filter((_, itemIndex) => itemIndex !== index)
}

/** 将数据库相对路径转换为浏览器可访问的完整图片地址。 */
const getDetailImageUrl = (url: string) => (url.startsWith('/') ? getFileUrl(url) : url)

const loadOptions = async () => {
  const result = await listCropTypes({ pageNum: 1, pageSize: 1000, status: 1 })
  cropTypeOptions.value = result.data.list
}

const resetForm = () => {
  form.value = createEmptyForm()
  uploadedCoverPreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (diseasePest: DiseasePest) => {
  form.value = {
    cropTypeId: diseasePest.cropTypeId,
    affectedCrops: diseasePest.affectedCrops || '',
    name: diseasePest.name || '',
    type: diseasePest.type ?? 1,
    symptom: diseasePest.symptom || '',
    cause: diseasePest.cause || '',
    suitableStage: diseasePest.suitableStage || '',
    occurrencePeriod: diseasePest.occurrencePeriod || '',
    livingHabits: diseasePest.livingHabits || '',
    suitableEnvironment: diseasePest.suitableEnvironment || '',
    transmissionRoute: diseasePest.transmissionRoute || '',
    coverImage: diseasePest.coverImage || '',
    imageUrls: Array.isArray(diseasePest.imageUrls) ? [...diseasePest.imageUrls] : [],
    status: diseasePest.status ?? 1,
    sortOrder: diseasePest.sortOrder ?? 0,
    remark: diseasePest.remark || '',
  }
  uploadedCoverPreviewUrl.value = ''
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getDiseasePestById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
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

/** 组装新增接口 payload，过滤掉空字符串，避免覆盖数据库默认值。 */
const buildCreatePayload = () => {
  return removeEmptyOptionalFields({ ...form.value }) as unknown as DiseasePestPayload
}

/** 组装编辑接口 payload，额外带上当前编辑记录 id。 */
const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as unknown as UpdateDiseasePestPayload
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
        ? await addDiseasePest(buildCreatePayload())
        : await updateDiseasePest(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑病虫害基本信息成功' : '新增病虫害基本信息成功')
    emit('success', result.data)
    dialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

const initializeDialog = async () => {
  resetForm()
  await loadOptions()

  if (props.id !== undefined && props.id !== null) {
    await loadDetail(props.id)
  }
}

watch(
  () => dialogVisible.value,
  (visible) => {
    if (!visible) {
      return
    }

    void initializeDialog()
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="760px"
    class="disease-dialog"
    modal-class="disease-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="disease-form"
        :model="form"
        :rules="formRules"
        label-width="112px"
        autocomplete="off"
      >
        <!-- 基础信息用于管理端检索和 farm 详情页顶部摘要。 -->
        <div class="form-section-title">基础信息</div>
        <el-form-item label="病虫害名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="请输入病虫害名称" clearable />
        </el-form-item>
        <el-form-item label="作物类型" prop="cropTypeId">
          <el-select v-model="form.cropTypeId" placeholder="通用病虫害" filterable clearable>
            <el-option
              v-for="item in cropTypeOptions"
              :key="item.id"
              :label="item.typeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型">
            <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="易发阶段" prop="suitableStage" class="form-wide">
          <el-input v-model.trim="form.suitableStage" placeholder="如：苗期、花期、成熟期" clearable />
        </el-form-item>
        <el-form-item label="危害作物" prop="affectedCrops" class="form-wide">
          <el-input v-model.trim="form.affectedCrops" placeholder="如：十字花科蔬菜、茄科蔬菜、瓜类" clearable />
        </el-form-item>
        <el-form-item label="发生时期" prop="occurrencePeriod">
          <el-input v-model.trim="form.occurrencePeriod" placeholder="如：春秋季为主" clearable />
        </el-form-item>
        <el-form-item label="展示顺序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button v-for="item in statusOptions" :key="item.value" :label="item.value">
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 封面保持单图；详情图片单独写入 image_urls JSON 数组。 -->
        <div class="form-section-title">图片资料</div>
        <el-form-item label="封面图片" prop="coverImage" class="form-wide">
          <el-upload
            v-loading="uploadLoading"
            class="cover-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleCoverChange"
          >
            <img v-if="currentCoverPreview" class="cover-image" :src="currentCoverPreview" alt="病虫害封面图" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="详情图片" prop="imageUrls" class="form-wide">
          <div class="detail-images">
            <div v-for="(url, index) in form.imageUrls" :key="`${url}-${index}`" class="detail-image-card">
              <el-image
                class="detail-image"
                :src="getDetailImageUrl(url)"
                fit="cover"
                :preview-src-list="form.imageUrls.map(getDetailImageUrl)"
                :initial-index="index"
                preview-teleported
              />
              <el-button
                class="detail-image-remove"
                type="danger"
                circle
                :icon="Delete"
                aria-label="删除详情图片"
                @click="removeDetailImage(index)"
              />
            </div>
            <el-upload
              v-if="form.imageUrls.length < 8"
              v-loading="detailUploadLoading"
              class="detail-image-uploader"
              accept="image/jpeg,image/png,image/webp"
              multiple
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleDetailImageChange"
            >
              <el-icon><Plus /></el-icon>
              <span>添加图片</span>
            </el-upload>
          </div>
          <p class="field-help">最多 8 张，按当前顺序展示；支持 JPG、PNG、WEBP，单张不超过 2MB。</p>
        </el-form-item>

        <!-- 识别与发生规律在详情页分区展示，录入时保持对应字段清晰可辨。 -->
        <div class="form-section-title">症状与发生规律</div>
        <el-form-item label="症状描述" prop="symptom" class="form-wide">
          <el-input
            v-model="form.symptom"
            type="textarea"
            :rows="3"
            placeholder="请输入症状描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="发生原因" prop="cause" class="form-wide">
          <el-input
            v-model="form.cause"
            type="textarea"
            :rows="3"
            placeholder="请输入发生原因"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="生活习性" prop="livingHabits" class="form-wide">
          <el-input v-model="form.livingHabits" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入生活习性" />
        </el-form-item>
        <el-form-item label="适宜环境" prop="suitableEnvironment" class="form-wide">
          <el-input v-model="form.suitableEnvironment" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入适宜温湿度、连作等环境条件" />
        </el-form-item>
        <el-form-item label="传播途径" prop="transmissionRoute" class="form-wide">
          <el-input v-model="form.transmissionRoute" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入传播或扩散途径" />
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" :disabled="uploadLoading || detailUploadLoading" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.disease-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.disease-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.disease-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.disease-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.disease-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.disease-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.disease-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.disease-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.disease-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
  padding: 2px 2px 0;
}

.form-section-title {
  grid-column: 1 / -1;
  margin: 4px 0 14px;
  padding-left: 10px;
  border-left: 3px solid #409eff;
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
  line-height: 22px;
}

.disease-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.disease-form :deep(.el-select),
.disease-form :deep(.el-input-number) {
  width: 100%;
}

.disease-form :deep(.el-input__wrapper),
.disease-form :deep(.el-select__wrapper),
.disease-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.disease-form :deep(.el-input__wrapper),
.disease-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.disease-form :deep(.el-input__wrapper.is-focus),
.disease-form :deep(.el-select__wrapper.is-focused),
.disease-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  grid-column: 1 / -1;
  align-items: center;
}

.cover-uploader :deep(.el-upload) {
  width: 78px;
  height: 78px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.cover-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.cover-uploader-icon {
  color: #94a3b8;
  font-size: 24px;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #ffffff;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}

.detail-image-card,
.detail-image-uploader :deep(.el-upload) {
  position: relative;
  width: 104px;
  height: 104px;
  overflow: hidden;
  border: 1px solid #dbe4ee;
  border-radius: 8px;
  background: #f8fafc;
}

.detail-image {
  width: 100%;
  height: 100%;
}

.detail-image-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 32px;
  height: 32px;
}

.detail-image-uploader :deep(.el-upload) {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  color: #64748b;
  cursor: pointer;
}

.detail-image-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  color: #409eff;
}

.field-help {
  width: 100%;
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 20px;
}

@media (max-width: 720px) {
  :global(.disease-dialog) {
    width: calc(100vw - 28px) !important;
  }

  .disease-form {
    grid-template-columns: 1fr;
  }
}
</style>
