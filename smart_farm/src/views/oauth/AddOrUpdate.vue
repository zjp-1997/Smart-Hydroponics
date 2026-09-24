<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getOauthAccountById,
  updateOauthAccount,
  type OauthAccount,
  type UpdateOauthAccountPayload,
} from '@/api/oauthAccount'
import { uploadManagedImage } from '@/api/managedImage'
import { getFileUrl } from '@/utils/utils'

type OauthFormModel = {
  userId?: number
  provider?: number
  openId: string
  unionId?: string
  nickname?: string
  avatar?: string
  accessToken?: string
  refreshToken?: string
  tokenExpireTime?: string
  bindTime?: string
  lastLoginTime?: string
  status?: number
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [account?: OauthAccount]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => props.id !== undefined && props.id !== null)
const dialogTitle = computed(() => '编辑第三方账号')

const formRef = ref<FormInstance>()
const detailLoading = ref(false)
const submitLoading = ref(false)
const uploadLoading = ref(false)
const uploadedAvatarPreviewUrl = ref('')
const OAUTH_AVATAR_STORAGE_PREFIX = 'smart_farm_oauth_avatar:'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const createEmptyForm = (): OauthFormModel => ({
  userId: undefined,
  provider: undefined,
  openId: '',
  unionId: '',
  nickname: '',
  avatar: '',
  accessToken: '',
  refreshToken: '',
  tokenExpireTime: '',
  bindTime: '',
  lastLoginTime: '',
  status: 1,
  remark: '',
})

const form = ref<OauthFormModel>(createEmptyForm())

const isAvatarImageUrl = (value?: string) => {
  return Boolean(
    value &&
      value !== '/default-avatar.svg' &&
      (/^(https?:|blob:|data:image\/)/.test(value) || value.startsWith('/')),
  )
}

const getStoredAvatarUrl = (value?: string) => {
  if (!value?.startsWith(OAUTH_AVATAR_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const currentAvatarPreview = computed(() => {
  if (uploadedAvatarPreviewUrl.value) {
    return uploadedAvatarPreviewUrl.value
  }

  if (isAvatarImageUrl(form.value.avatar)) {
    return form.value.avatar?.startsWith('/') ? getFileUrl(form.value.avatar) : form.value.avatar
  }
  return getStoredAvatarUrl(form.value.avatar)
})

const resetForm = () => {
  form.value = createEmptyForm()
  uploadedAvatarPreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (account: OauthAccount) => {
  form.value = {
    userId: account.userId,
    provider: account.provider,
    openId: account.openId || '',
    unionId: account.unionId || '',
    nickname: account.nickname || '',
    avatar: account.avatar || '',
    accessToken: '',
    refreshToken: '',
    tokenExpireTime: account.tokenExpireTime || '',
    bindTime: account.bindTime || '',
    lastLoginTime: account.lastLoginTime || '',
    status: account.status,
    remark: account.remark || '',
  }
  uploadedAvatarPreviewUrl.value = ''
}

const loadDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getOauthAccountById(id)
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

const buildUpdatePayload = (id: number) => {
  return removeEmptyOptionalFields({
    id,
    avatar: form.value.avatar,
    nickname: form.value.nickname,
    status: form.value.status,
    remark: form.value.remark,
  }) as unknown as UpdateOauthAccountPayload
}

const handleAvatarChange: UploadProps['onChange'] = async (uploadFile) => {
  const rawFile = uploadFile.raw

  if (!rawFile) {
    return
  }

  const allowTypes = ['image/jpeg', 'image/png', 'image/webp']
  const isAllowedImage = allowTypes.includes(rawFile.type)
  const isLt2M = rawFile.size / 1024 / 1024 < 2

  if (!isAllowedImage) {
    ElMessage.warning('头像仅支持 JPG、PNG 或 WEBP 图片')
    return
  }

  if (!isLt2M) {
    ElMessage.warning('头像图片大小不能超过 2MB')
    return
  }

  uploadLoading.value = true
  try {
    const result = await uploadManagedImage(rawFile, 'oauth-avatar')
    form.value.avatar = result.data.imageUrl
    uploadedAvatarPreviewUrl.value = getFileUrl(result.data.imageUrl)
    ElMessage.success('头像上传成功')
  } finally {
    uploadLoading.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) {
    return
  }

  if (!props.id) {
    ElMessage.warning('第三方账号不支持手动新增')
    return
  }

  try {
    submitLoading.value = true
    const result = await updateOauthAccount(buildUpdatePayload(props.id))

    ElMessage.success('编辑第三方账号成功')
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
      void loadDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="oauth-dialog"
    modal-class="oauth-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="oauth-form"
        :model="form"
        label-width="108px"
      >
        <el-form-item label="第三方昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入第三方昵称" clearable />
        </el-form-item>
        <el-form-item label="头像" prop="avatar" class="form-wide">
          <el-upload
            v-loading="uploadLoading"
            class="avatar-uploader"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
          >
            <img v-if="currentAvatarPreview" class="avatar-image" :src="currentAvatarPreview" alt="第三方账号头像" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="绑定时间" prop="bindTime">
          <el-date-picker
            v-model="form.bindTime"
            type="datetime"
            placeholder="请选择绑定时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            disabled
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.value"
            >
              {{ item.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="form-wide">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" :disabled="uploadLoading" @click="submitForm">
        保存修改
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.oauth-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  margin: 0 auto;
  overflow: hidden;
  border-radius: 10px;
}

:global(.oauth-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.oauth-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.oauth-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.oauth-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  overflow-y: auto;
  overflow-x: hidden;
  background: #fbfdff;
}

:global(.oauth-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.oauth-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.oauth-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.oauth-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2px;
  padding: 2px 2px 0;
}

.oauth-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.oauth-form :deep(.el-select),
.oauth-form :deep(.el-date-editor),
.oauth-form :deep(.el-input-number) {
  width: 100%;
}

.oauth-form :deep(.el-input__wrapper),
.oauth-form :deep(.el-select__wrapper),
.oauth-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.oauth-form :deep(.el-input__wrapper),
.oauth-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.oauth-form :deep(.el-input__wrapper.is-focus),
.oauth-form :deep(.el-select__wrapper.is-focused),
.oauth-form :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.form-wide {
  grid-column: auto;
  align-items: center;
}

.avatar-uploader :deep(.el-upload) {
  width: 68px;
  height: 68px;
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

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #409eff;
  background: #f0f7ff;
}

.avatar-uploader-icon {
  color: #94a3b8;
  font-size: 23px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #ffffff;
}

@media (max-width: 720px) {
  :global(.oauth-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
