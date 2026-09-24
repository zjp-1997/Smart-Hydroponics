<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listCrops, type Crop } from '@/api/crop'
import {
  addPlantingBatch,
  getPlantingBatchById,
  updatePlantingBatch,
  type PlantingBatch,
  type PlantingBatchPayload,
  type UpdatePlantingBatchPayload,
} from '@/api/plantingBatch'
import { listPlots, type Plot } from '@/api/plot'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getCurrentUser, getCurrentUserId, isAdminUser } from '@/utils/auth'
import { uploadManagedImage } from '@/api/managedImage'
import { getFileUrl } from '@/utils/utils'

interface BatchFormModel {
  plotId?: number
  cropId?: number
  userId?: number
  batchNo?: string
  plantingArea?: number
  areaUnit?: string
  plantedAt?: string
  expectedHarvestAt?: string
  actualHarvestAt?: string
  expectedYieldAmount?: number
  cropImage?: string
  status?: number
  yieldAmount?: number
  yieldUnit?: string
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [batch?: PlantingBatch]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑种植批次' : '新增种植批次'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const plotOptions = ref<Plot[]>([])
const cropOptions = ref<Crop[]>([])
const userOptions = ref<SmartPlantUser[]>([])
const imagePreviewUrl = ref('')
const currentUser = computed(() => getCurrentUser())
const isAdmin = computed(() => isAdminUser(currentUser.value))

const areaUnitOptions = ['亩', '平方米', '公顷']
const yieldUnitOptions = ['kg', '斤', '吨']
const BATCH_CROP_IMAGE_STORAGE_PREFIX = 'smart_farm_planting_batch_crop_image:'

const statusOptions = [
  { label: '种植中', value: 1 },
  { label: '已采收', value: 2 },
  { label: '已失败', value: 3 },
  { label: '已取消', value: 4 },
]

const createEmptyForm = (): BatchFormModel => ({
  plotId: undefined,
  cropId: undefined,
  userId: isAdmin.value ? undefined : getCurrentUserId(),
  batchNo: '',
  plantingArea: undefined,
  areaUnit: '亩',
  plantedAt: '',
  expectedHarvestAt: '',
  actualHarvestAt: '',
  expectedYieldAmount: undefined,
  cropImage: '',
  status: 1,
  yieldAmount: undefined,
  yieldUnit: 'kg',
  remark: '',
})

const form = ref<BatchFormModel>(createEmptyForm())

const formRules = computed<FormRules<BatchFormModel>>(() => ({
  plotId: [{ required: true, message: '请选择地块', trigger: 'change' }],
  cropId: [{ required: true, message: '请选择作物', trigger: 'change' }],
  userId: [{ required: true, message: '请选择所属用户', trigger: 'change' }],
  batchNo: [{ max: 50, message: '批次编号长度不能超过 50 个字符', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  plantingArea: [{ type: 'number', min: 0, message: '种植面积不能小于 0', trigger: 'change' }],
  expectedYieldAmount: [{ type: 'number', min: 0, message: '预计产量不能小于 0', trigger: 'change' }],
  actualHarvestAt:
    form.value.status === 2
      ? [{ required: true, message: '已采收批次必须填写实际采收日期', trigger: 'change' }]
      : [],
  yieldAmount: [
    ...(form.value.status === 2
      ? [{ required: true, message: '已采收批次必须填写实际产量', trigger: 'change' }]
      : []),
    { type: 'number', min: 0, message: '实际产量不能小于 0', trigger: 'change' },
  ],
}))

const isImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  return Boolean(
    normalizedValue &&
      (/^(https?:|blob:|data:image\/)/.test(normalizedValue) || normalizedValue.startsWith('/')),
  )
}

const getStoredImageUrl = (value?: string) => {
  if (!value?.startsWith(BATCH_CROP_IMAGE_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const resolveImageUrl = (value?: string) => {
  const normalizedValue = value?.trim()

  if (isImageUrl(normalizedValue)) {
    return normalizedValue?.startsWith('/') ? getFileUrl(normalizedValue) : normalizedValue || ''
  }

  return getStoredImageUrl(normalizedValue)
}

const currentImagePreview = computed(() => imagePreviewUrl.value || resolveImageUrl(form.value.cropImage))

const getUserOptionLabel = (user: SmartPlantUser) => {
  const name = user.nickname || user.username || `用户${user.id}`
  return user.username && user.nickname ? `${name}（${user.username}）` : name
}

const selectedPlot = computed(() => {
  return plotOptions.value.find((item) => item.id === form.value.plotId)
})

const availableUserOptions = computed(() => {
  if (!isAdmin.value) {
    return currentUser.value?.id ? [currentUser.value as SmartPlantUser] : []
  }

  const plotUserId = selectedPlot.value?.userId

  if (!plotUserId) {
    return userOptions.value
  }

  return userOptions.value.filter((item) => item.id === plotUserId)
})

const syncUserWithSelectedPlot = () => {
  const plotUserId = selectedPlot.value?.userId

  if (plotUserId) {
    form.value.userId = plotUserId
  }
}

const syncPlantingAreaWithSelectedPlot = (force = false) => {
  if (selectedPlot.value && (force || form.value.plantingArea === undefined)) {
    form.value.plantingArea = selectedPlot.value.area
    form.value.areaUnit = selectedPlot.value.areaUnit || '亩'
  }
}

const handlePlotChange = () => {
  if (!form.value.plotId) {
    form.value.userId = undefined
    return
  }

  syncUserWithSelectedPlot()
  syncPlantingAreaWithSelectedPlot(true)
}

const loadOptions = async () => {
  const userRequest = isAdmin.value
    ? listSmartPlantUsers({ pageNum: 1, pageSize: 1000, status: 1 })
    : Promise.resolve({
        data: {
          list: currentUser.value?.id ? [currentUser.value as SmartPlantUser] : [],
        },
      })
  const [plotResult, cropResult, userResult] = await Promise.all([
    listPlots({ pageNum: 1, pageSize: 1000, status: 1 }),
    listCrops({ pageNum: 1, pageSize: 1000, status: 1 }),
    userRequest,
  ])

  plotOptions.value = plotResult.data.list
  cropOptions.value = cropResult.data.list
  userOptions.value = userResult.data.list
  syncUserWithSelectedPlot()
  syncPlantingAreaWithSelectedPlot()
}

const resetForm = () => {
  form.value = createEmptyForm()
  imagePreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (batch: PlantingBatch) => {
  form.value = {
    plotId: batch.plotId,
    cropId: batch.cropId,
    userId: batch.userId,
    batchNo: batch.batchNo || '',
    plantingArea: batch.plantingArea,
    areaUnit: batch.areaUnit || '亩',
    plantedAt: batch.plantedAt || '',
    expectedHarvestAt: batch.expectedHarvestAt || '',
    actualHarvestAt: batch.actualHarvestAt || '',
    expectedYieldAmount: batch.expectedYieldAmount,
    cropImage: batch.cropImage || '',
    status: batch.status ?? 1,
    yieldAmount: batch.yieldAmount,
    yieldUnit: batch.yieldUnit || 'kg',
    remark: batch.remark || '',
  }
  imagePreviewUrl.value = ''
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getPlantingBatchById(id)
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
      const result = await uploadManagedImage(rawFile, 'planting-batch')
      form.value.cropImage = result.data.imageUrl
      imagePreviewUrl.value = getFileUrl(result.data.imageUrl)
      ElMessage.success('作物图片上传成功')
    } finally {
      uploadLoading.value = false
    }
  }
}

const normalizeSubmitPayload = <T extends Record<string, unknown>>(data: T) => {
  return removeEmptyOptionalFields(data)
}

const buildCreatePayload = () => {
  return normalizeSubmitPayload({
    ...form.value,
    userId: isAdmin.value ? form.value.userId : getCurrentUserId(),
  }) as unknown as PlantingBatchPayload
}

const buildUpdatePayload = (id: number) => {
  return normalizeSubmitPayload({
    id,
    ...form.value,
    userId: isAdmin.value ? form.value.userId : getCurrentUserId(),
  }) as unknown as UpdatePlantingBatchPayload
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
        ? await addPlantingBatch(buildCreatePayload())
        : await updatePlantingBatch(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑种植批次成功' : '新增种植批次成功')
    emit('success', result.data)
    dialogVisible.value = false
  } catch (error: any) {
    if (error?.message === 'Network Error') {
      ElMessage.error('新增批次失败：无法连接后端服务，请确认 smart_plant 后端已启动')
    }
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
    void loadOptions()

    if (props.id !== undefined && props.id !== null) {
      void loadDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="620px"
    class="batch-dialog"
    modal-class="batch-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="batch-form"
        :model="form"
        :rules="formRules"
        label-width="112px"
        autocomplete="off"
      >
        <el-form-item label="地块" prop="plotId">
          <el-select v-model="form.plotId" placeholder="请选择地块" filterable clearable @change="handlePlotChange">
            <el-option v-for="item in plotOptions" :key="item.id" :label="item.plotName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="作物" prop="cropId">
          <el-select v-model="form.cropId" placeholder="请选择作物" filterable clearable>
            <el-option v-for="item in cropOptions" :key="item.id" :label="item.cropName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="所属用户" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择所属用户" filterable clearable :disabled="Boolean(form.plotId)">
            <el-option
              v-for="item in availableUserOptions"
              :key="item.id"
              :label="getUserOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <!-- <el-form-item label="批次编号" prop="batchNo">
          <el-input v-model.trim="form.batchNo" placeholder="请输入批次编号" clearable />
        </el-form-item> -->
        <el-form-item label="种植面积" prop="plantingArea">
          <el-input-number v-model="form.plantingArea" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="面积单位" prop="areaUnit">
          <el-select v-model="form.areaUnit" placeholder="请选择面积单位" filterable allow-create>
            <el-option v-for="item in areaUnitOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="种植日期" prop="plantedAt">
          <el-date-picker v-model="form.plantedAt" type="date" value-format="YYYY-MM-DD" placeholder="请选择种植日期" />
        </el-form-item>
        <el-form-item label="预计采收" prop="expectedHarvestAt">
          <el-date-picker v-model="form.expectedHarvestAt" type="date" value-format="YYYY-MM-DD" placeholder="请选择预计采收日期" />
        </el-form-item>
        <el-form-item label="实际采收" prop="actualHarvestAt">
          <el-date-picker v-model="form.actualHarvestAt" type="date" value-format="YYYY-MM-DD" placeholder="请选择实际采收日期" />
        </el-form-item>
        <el-form-item label="生长阶段">
          <el-input model-value="由作物生长期配置自动判断" disabled />
        </el-form-item>
        <el-form-item label="预计产量" prop="expectedYieldAmount">
          <el-input-number v-model="form.expectedYieldAmount" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="已生长天数" class="form-wide">
          <el-input model-value="根据种植日期自动计算" disabled />
        </el-form-item>
        <el-form-item label="作物图片" prop="cropImage" class="form-wide">
          <el-upload
            v-loading="uploadLoading"
            class="batch-image-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageChange"
          >
            <img v-if="currentImagePreview" class="upload-image" :src="currentImagePreview" alt="作物图片" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态" prop="status" class="form-wide">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际产量" prop="yieldAmount">
          <el-input-number v-model="form.yieldAmount" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="产量单位" prop="yieldUnit">
          <el-select v-model="form.yieldUnit" placeholder="请选择产量单位" filterable allow-create>
            <el-option v-for="item in yieldUnitOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide_1">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="255"
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
:global(.batch-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.batch-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.batch-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.batch-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.batch-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.batch-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.batch-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
  padding: 2px 2px 0;
}

.batch-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.batch-form :deep(.el-select),
.batch-form :deep(.el-input-number),
.batch-form :deep(.el-date-editor) {
  width: 100%;
}

.batch-form :deep(.el-input__wrapper),
.batch-form :deep(.el-select__wrapper),
.batch-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.batch-form :deep(.el-input__wrapper),
.batch-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.batch-image-uploader :deep(.el-upload) {
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

.batch-image-uploader :deep(.el-upload:hover) {
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

.form-wide {
  grid-column: auto;
  align-items: center;
}

.form-wide_1 {
  grid-column: 1 / -1;
  align-items: center;
}

@media (max-width: 720px) {
  :global(.batch-dialog) {
    width: calc(100vw - 28px) !important;
  }

  .batch-form {
    grid-template-columns: 1fr;
  }
}
</style>
