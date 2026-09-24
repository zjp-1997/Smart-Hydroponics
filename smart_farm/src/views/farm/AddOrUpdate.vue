<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import {
  addFarm,
  getFarmById,
  updateFarm,
  uploadFarmImage,
  type Farm,
  type FarmPayload,
  type UpdateFarmPayload,
} from '@/api/farm'
import { listSmartPlantUsers, type SmartPlantUser } from '@/api/user'
import { getCurrentUser, getCurrentUserId, isAdminUser } from '@/utils/auth'
import { locateWithAmap, reverseGeocodeWithAmap } from '@/utils/amap'
import { getFileUrl } from '@/utils/utils'

interface FarmFormModel {
  userId?: number
  farmName: string
  imgUrl?: string
  contactPhone?: string
  address?: string
  coordinate?: string
  totalArea?: number
  areaUnit?: string
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [farm?: Farm]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑农场' : '新增农场'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const locating = ref(false)
const userOptions = ref<SmartPlantUser[]>([])
const currentUser = computed(() => getCurrentUser())
const isAdmin = computed(() => isAdminUser(currentUser.value))
const currentImagePreview = ref('')

const areaUnitOptions = ['亩', '平方米', '公顷']

const createEmptyForm = (): FarmFormModel => ({
  userId: isAdmin.value ? undefined : getCurrentUserId(),
  farmName: '',
  imgUrl: '',
  contactPhone: '',
  address: '',
  coordinate: '',
  totalArea: undefined,
  areaUnit: '亩',
  status: 1,
  remark: '',
})

const form = ref<FarmFormModel>(createEmptyForm())

const formRules = computed<FormRules<FarmFormModel>>(() => ({
  userId: [{ required: true, message: '请选择所属用户', trigger: 'change' }],
  farmName: [
    { required: true, message: '请输入农场名称', trigger: 'blur' },
    { min: 1, max: 100, message: '农场名称长度不能超过 100 个字符', trigger: 'blur' },
  ],
  contactPhone: [
    {
      pattern: /^$|^1[3-9]\d{9}$/,
      message: '请输入正确的 11 位手机号',
      trigger: 'blur',
    },
  ],
  coordinate: [
    {
      pattern: /^$|^-?(180(\.0+)?|1[0-7]\d(\.\d+)?|\d{1,2}(\.\d+)?),\s*-?(90(\.0+)?|[1-8]?\d(\.\d+)?)$/,
      message: '经纬度格式必须为经度,纬度',
      trigger: 'blur',
    },
  ],
  totalArea: [{ type: 'number', min: 0, message: '农场面积不能小于 0', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

const getUserOptionLabel = (user: SmartPlantUser) => {
  return user.nickname || user.username || `用户${user.id}`
}

const selectedOwner = computed(() => userOptions.value.find((item) => item.id === form.value.userId))

const fillOwnerInfo = (user?: SmartPlantUser) => {
  form.value.contactPhone = user?.phone || ''
}

const loadUserOptions = async () => {
  if (!isAdmin.value) {
    const user = currentUser.value as SmartPlantUser | null
    userOptions.value = user?.id && user.roleCode?.toLowerCase() === 'farm_owner' ? [user] : []
    fillOwnerInfo(userOptions.value[0])
    return
  }

  const result = await listSmartPlantUsers({
    pageNum: 1,
    pageSize: 1000,
    roleCode: 'farm_owner',
    status: 1,
  })

  userOptions.value = result.data.list
  fillOwnerInfo(selectedOwner.value)
}

const resetForm = () => {
  form.value = createEmptyForm()
  currentImagePreview.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (farm: Farm) => {
  form.value = {
    userId: farm.userId,
    farmName: farm.farmName || '',
    imgUrl: farm.imgUrl || '',
    contactPhone: farm.contactPhone || '',
    address: farm.address || '',
    coordinate: farm.coordinate || formatCoordinate(farm.longitude, farm.latitude),
    totalArea: farm.totalArea,
    areaUnit: farm.areaUnit || '亩',
    status: farm.status ?? 1,
    remark: farm.remark || '',
  }
  currentImagePreview.value = farm.imgUrl ? getFileUrl(farm.imgUrl) : ''
}

const loadFarmDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getFarmById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

const formatCoordinate = (longitude?: number, latitude?: number) => {
  if (longitude === undefined || longitude === null || latitude === undefined || latitude === null) {
    return ''
  }

  return `${longitude},${latitude}`
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
    ...form.value,
    userId: isAdmin.value ? form.value.userId : getCurrentUserId(),
  }) as FarmPayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({
    id,
    ...form.value,
    userId: isAdmin.value ? form.value.userId : getCurrentUserId(),
  }) as UpdateFarmPayload
}

const handleOwnerChange = () => {
  fillOwnerInfo(selectedOwner.value)
}

const beforeUploadImage = (file: File) => {
  const isAllowedType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  const isAllowedSize = file.size / 1024 / 1024 <= 5

  if (!isAllowedType) {
    ElMessage.error('农场图片仅支持 JPG、PNG、WEBP 格式')
    return false
  }

  if (!isAllowedSize) {
    ElMessage.error('农场图片不能超过5MB')
    return false
  }

  return true
}

const handleImageChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (!rawFile || !beforeUploadImage(rawFile)) {
    return
  }

  const previousImageUrl = form.value.imgUrl
  const previousImagePreview = currentImagePreview.value
  uploadLoading.value = true

  try {
    const result = await uploadFarmImage(rawFile)
    form.value.imgUrl = result.data.url
    currentImagePreview.value = getFileUrl(result.data.url)
    ElMessage.success('农场图片上传成功')
  } catch {
    form.value.imgUrl = previousImageUrl
    currentImagePreview.value = previousImagePreview
  } finally {
    uploadLoading.value = false
  }
}

const fillAddressByCoordinate = async (coordinate: string) => {
  try {
    // 表单保存 WGS-84 坐标，查询地址前由公共高德工具统一转换为 GCJ-02。
    const [longitude, latitude] = coordinate.split(/[,，\s]+/).map(Number)
    if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return
    form.value.address = await reverseGeocodeWithAmap(longitude!, latitude!)
  } catch {
    ElMessage.warning('已获取经纬度，地址自动填写失败')
  }
}

const locateCoordinate = async () => {
  locating.value = true
  try {
    // 开发和生产环境统一调用高德定位插件，并以 WGS-84 格式保存业务坐标。
    const location = await locateWithAmap()
    const coordinate = `${location.longitude.toFixed(6)},${location.latitude.toFixed(6)}`
    form.value.coordinate = coordinate
    if (location.address) {
      form.value.address = location.address
    } else {
      await fillAddressByCoordinate(coordinate)
    }
    void formRef.value?.validateField('coordinate')
    void formRef.value?.validateField('address')
  } catch {
    ElMessage.error('定位失败，请检查浏览器定位权限或高德地图配置')
  } finally {
    locating.value = false
  }
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
        ? await addFarm(buildCreatePayload())
        : await updateFarm(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑农场成功' : '新增农场成功')
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
    void loadUserOptions()

    if (props.id !== undefined && props.id !== null) {
      void loadFarmDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="620px"
    class="farm-dialog"
    modal-class="farm-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="farm-form"
        :model="form"
        :rules="formRules"
        label-width="104px"
        autocomplete="off"
      >
        <el-form-item v-if="isAdmin" label="所属用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="请选择农场主"
            filterable
            clearable
            @change="handleOwnerChange"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="getUserOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="农场名称" prop="farmName">
          <el-input v-model.trim="form.farmName" placeholder="请输入农场名称" clearable />
        </el-form-item>
        <el-form-item label="农场图片" prop="imgUrl">
          <el-upload
            v-loading="uploadLoading"
            class="avatar-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageChange"
          >
            <img v-if="currentImagePreview" class="avatar-image" :src="currentImagePreview" alt="农场图片" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model.trim="form.contactPhone" placeholder="选择农场主后自动填充" clearable />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model.trim="form.address" placeholder="请输入地址" clearable />
        </el-form-item>
        <el-form-item label="农场面积" prop="totalArea">
          <el-input-number v-model="form.totalArea" :min="0" :precision="2" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="面积单位" prop="areaUnit">
          <el-select v-model="form.areaUnit" placeholder="请选择面积单位" filterable allow-create>
            <el-option v-for="item in areaUnitOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="经纬度" prop="coordinate">
          <div class="coordinate-row">
            <el-input v-model.trim="form.coordinate" placeholder="经度,纬度" clearable />
            <el-button :icon="Location" :loading="locating" @click="locateCoordinate" />
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
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
      <el-button type="primary" :loading="submitLoading" :disabled="uploadLoading || locating" @click="submitForm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.farm-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.farm-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.farm-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.farm-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.farm-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.farm-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.farm-form {
  padding: 2px 2px 0;
}

.farm-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.farm-form :deep(.el-select),
.farm-form :deep(.el-input-number) {
  width: 100%;
}

.farm-form :deep(.el-input__wrapper),
.farm-form :deep(.el-select__wrapper),
.farm-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.farm-form :deep(.el-input__wrapper),
.farm-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.avatar-uploader :deep(.el-upload) {
  width: 68px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.avatar-uploader-icon {
  color: #94a3b8;
  font-size: 24px;
}

.avatar-image {
  width: 68px;
  height: 68px;
  display: block;
  object-fit: cover;
}

.coordinate-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 40px;
  gap: 8px;
}

.coordinate-row :deep(.el-button) {
  width: 40px;
  height: 38px;
  padding: 0;
}

@media (max-width: 720px) {
  :global(.farm-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
