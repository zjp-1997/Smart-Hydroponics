<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  addExpertProfile,
  getExpertProfileById,
  updateExpertProfile,
  uploadExpertAsset,
  type ExpertProfile,
  type ExpertProfilePayload,
  type UpdateExpertProfilePayload,
} from '@/api/expert'
import { getFileUrl } from '@/utils/utils'

/**
 * 专家新增/编辑弹框组件。
 *
 * 作用说明：
 * - 新增和编辑专家资料共用同一个组件，通过 id 是否为空判断提交方式。
 * - 管理 expert_profile 表中的核心字段，包括专家基础信息、头像、资质证书、审核状态、
 *   服务状态、账号状态、评分和咨询次数。
 * - 图片先上传到 smart_plant 后端 /uploads 目录，再把返回 URL 写入专家资料，
 *   便于管理端和 farm 用户端刷新后展示同一张图片。
 */

interface ExpertFormModel {
  realName: string
  organization?: string
  jobTitle?: string
  specialty?: string
  introduction?: string
  certificateUrl?: string
  certificateName?: string
  certificateAuditStatus?: number
  avatar?: string
  rating?: number
  consultationCount?: number
  auditStatus?: number
  serviceStatus?: number
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [expert?: ExpertProfile]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑专家信息' : '新增专家信息'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const avatarPreviewUrl = ref('')
const certificatePreviewUrl = ref('')
const EXPERT_ASSET_PREFIX = 'smart_farm_expert_asset:'

const auditStatusOptions = [
  { label: '未提交', value: 0 },
  { label: '待审核', value: 1 },
  { label: '审核通过', value: 2 },
  { label: '审核拒绝', value: 3 },
]

const serviceStatusOptions = [
  { label: '可咨询', value: 1 },
  { label: '不可咨询', value: 0 },
]

const accountStatusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const createEmptyForm = (): ExpertFormModel => ({
  realName: '',
  organization: '',
  jobTitle: '',
  specialty: '',
  introduction: '',
  certificateUrl: '',
  certificateName: '',
  certificateAuditStatus: 1,
  avatar: '',
  rating: 5,
  consultationCount: 0,
  auditStatus: 1,
  serviceStatus: 1,
  status: 1,
  remark: '',
})

const form = ref<ExpertFormModel>(createEmptyForm())

const formRules = computed<FormRules<ExpertFormModel>>(() => {
  const required = !isEdit.value

  return {
    realName: [{ required, message: '请输入专家姓名', trigger: 'blur' }],
    organization: [{ required, message: '请输入所属机构', trigger: 'blur' }],
    jobTitle: [{ required, message: '请输入职称', trigger: 'blur' }],
    certificateUrl: [{ required, message: '请上传资质证书', trigger: 'change' }],
    rating: [{ required, message: '请设置专家评分', trigger: 'change' }],
    status: [{ required, message: '请选择账号状态', trigger: 'change' }],
    serviceStatus: [{ required, message: '请选择咨询状态', trigger: 'change' }],
    auditStatus: [{ required, message: '请选择审核状态', trigger: 'change' }],
  }
})

const isImageUrl = (value?: string) => {
  return Boolean(value && /^(https?:|blob:|data:image\/)/.test(value))
}

const getStoredAssetUrl = (value?: string) => {
  if (!value?.startsWith(EXPERT_ASSET_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const getPreviewUrl = (value?: string, fallback = '') => {
  if (value?.toLowerCase().includes('default-avatar')) {
    return fallback
  }

  if (isImageUrl(value)) {
    return value || fallback
  }

  if (value?.startsWith('/')) {
    return getFileUrl(value)
  }

  // 兼容历史数据：旧版本曾把管理端 localStorage key 写入 avatar/certificateUrl。
  return getStoredAssetUrl(value) || fallback
}

const currentAvatarPreview = computed(() => {
  return avatarPreviewUrl.value || getPreviewUrl(form.value.avatar)
})

const currentCertificatePreview = computed(() => {
  return certificatePreviewUrl.value || getPreviewUrl(form.value.certificateUrl)
})

const resetForm = () => {
  form.value = createEmptyForm()
  avatarPreviewUrl.value = ''
  certificatePreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (expert: ExpertProfile) => {
  form.value = {
    realName: expert.realName || '',
    organization: expert.organization || '',
    jobTitle: expert.jobTitle || '',
    specialty: expert.specialty || '',
    introduction: expert.introduction || '',
    certificateUrl: expert.certificateUrl || '',
    certificateName: expert.certificateName || '',
    certificateAuditStatus: expert.certificateAuditStatus ?? 1,
    avatar: expert.avatar || '',
    rating: expert.rating ?? 5,
    consultationCount: expert.consultationCount ?? 0,
    auditStatus: expert.auditStatus ?? 1,
    serviceStatus: expert.serviceStatus ?? 1,
    status: expert.status ?? 1,
    remark: expert.remark || '',
  }
  avatarPreviewUrl.value = ''
  certificatePreviewUrl.value = ''
}

const loadExpertDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getExpertProfileById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
}

/**
 * 上传专家资料图片并写回表单 URL。
 *
 * 后端返回 /uploads 相对路径后，管理端和 farm 用户端都会通过同一个静态资源地址访问图片。
 */
const uploadExpertImage = async (rawFile: File, field: 'avatar' | 'certificate') => {
  uploadLoading.value = true

  try {
    const result = await uploadExpertAsset(rawFile, field)
    const imageUrl = result.data.url

    if (field === 'avatar') {
      form.value.avatar = imageUrl
      avatarPreviewUrl.value = getPreviewUrl(imageUrl)
    } else {
      form.value.certificateUrl = imageUrl
      certificatePreviewUrl.value = getPreviewUrl(imageUrl)
      void formRef.value?.validateField('certificateUrl')
    }

    ElMessage.success('图片上传成功')
  } finally {
    uploadLoading.value = false
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

const handleAvatarChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (rawFile && validateImage(rawFile)) {
    await uploadExpertImage(rawFile, 'avatar')
  }
}

const handleCertificateChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (rawFile && validateImage(rawFile)) {
    await uploadExpertImage(rawFile, 'certificate')
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
  return removeEmptyOptionalFields({ ...form.value }) as unknown as ExpertProfilePayload
}

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({ id, ...form.value }) as unknown as UpdateExpertProfilePayload
}

/**
 * 根据 id 判断新增或编辑。
 * id 为空：调用新增专家资料接口；id 不为空：调用编辑专家资料接口。
 */
const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const result =
      props.id === undefined || props.id === null
        ? await addExpertProfile(buildCreatePayload())
        : await updateExpertProfile(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑专家信息成功' : '新增专家信息成功')
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
    if (props.id !== undefined && props.id !== null) {
      void loadExpertDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="add-expert-dialog"
    modal-class="add-expert-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading || uploadLoading">
      <el-form
        ref="formRef"
        class="add-expert-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="专家姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入专家真实姓名" clearable />
        </el-form-item>
        <el-form-item label="所属机构" prop="organization">
          <el-input v-model="form.organization" placeholder="请输入所属机构" clearable />
        </el-form-item>
        <el-form-item label="职称" prop="jobTitle">
          <el-input v-model="form.jobTitle" placeholder="请输入职称" clearable />
        </el-form-item>
        <el-form-item label="擅长方向" prop="specialty">
          <el-input v-model="form.specialty" placeholder="例如：水培营养液、病虫害诊断、温室环境调控" clearable />
        </el-form-item>
        <el-form-item label="头像" prop="avatar"  class="form-wide">
          <el-upload
            class="avatar-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :disabled="uploadLoading"
            :on-change="handleAvatarChange"
          >
            <img v-if="currentAvatarPreview" class="upload-image" :src="currentAvatarPreview" alt="专家头像" />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="isEdit" label="证书名称" prop="certificateName">
          <el-input v-model="form.certificateName" placeholder="请输入证书名称" clearable />
        </el-form-item>
        <el-form-item label="资质证书" prop="certificateUrl"  class="form-wide">
          <el-upload
            class="certificate-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :disabled="uploadLoading"
            :on-change="handleCertificateChange"
          >
            <img
              v-if="currentCertificatePreview"
              class="upload-image"
              :src="currentCertificatePreview"
              alt="资质证书"
            />
            <el-icon v-else class="upload-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="认证状态" prop="certificateAuditStatus">
          <el-select v-model="form.certificateAuditStatus" placeholder="请选择认证状态">
            <el-option
              v-for="item in auditStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="专家简介" prop="introduction" class="form-wide">
          <el-input
            v-model="form.introduction"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="请输入专家简介"
          />
        </el-form-item>
        <el-form-item label="审核状态" prop="auditStatus">
          <el-select v-model="form.auditStatus" placeholder="请选择审核状态">
            <el-option
              v-for="item in auditStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="咨询状态" prop="serviceStatus">
          <el-select v-model="form.serviceStatus" placeholder="请选择咨询状态">
            <el-option
              v-for="item in serviceStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button
              v-for="item in accountStatusOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分" prop="rating">
          <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" controls-position="right" />
        </el-form-item>
        <el-form-item label="咨询次数" prop="consultationCount">
          <el-input-number v-model="form.consultationCount" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入备注，例如审核意见、资质说明或评价说明"
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
:global(.add-expert-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.add-expert-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.add-expert-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.add-expert-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.add-expert-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.add-expert-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.add-expert-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.add-expert-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.add-expert-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2px;
  padding: 2px 2px 0;
}

.add-expert-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.add-expert-form :deep(.el-select),
.add-expert-form :deep(.el-input-number) {
  width: 100%;
}

.add-expert-form :deep(.el-input__wrapper),
.add-expert-form :deep(.el-select__wrapper),
.add-expert-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.add-expert-form :deep(.el-input__wrapper),
.add-expert-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.add-expert-form :deep(.el-input__wrapper.is-focus),
.add-expert-form :deep(.el-select__wrapper.is-focused),
.add-expert-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  /* grid-column: auto; */
  align-items: center;
}

.avatar-uploader :deep(.el-upload),
.certificate-uploader :deep(.el-upload) {
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

.avatar-uploader :deep(.el-upload) {
  width: 68px;
  height: 68px;
}

.certificate-uploader :deep(.el-upload) {
  width: 116px;
  height: 74px;
}

.avatar-uploader :deep(.el-upload:hover),
.certificate-uploader :deep(.el-upload:hover) {
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
  :global(.add-expert-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
