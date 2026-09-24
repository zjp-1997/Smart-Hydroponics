<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  addSmartPlantUser,
  getSmartPlantUserById,
  listAssignableUserRoles,
  listAvailableFarmOwners,
  updateSmartPlantUser,
  type CreateSmartPlantUserPayload,
  type SmartPlantUser,
  type UpdateSmartPlantUserPayload,
} from '@/api/user'
import type { Role } from '@/api/role'
import { getCurrentRoleCode } from '@/utils/auth'
import { uploadManagedImage } from '@/api/managedImage'
import { getFileUrl } from '@/utils/utils'

type UserFormModel = {
  username: string
  password: string
  phone: string
  email?: string
  avatar?: string
  nickname?: string
  gender?: number
  roleId?: number
  farmOwnerId?: number
  status?: number
  region?: string
  remark?: string
}

const props = defineProps<{
  modelValue: boolean
  id?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: [user?: SmartPlantUser]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => Boolean(props.id))
const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '新增用户'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '确认新增'))

const submitLoading = ref(false)
const detailLoading = ref(false)
const uploadLoading = ref(false)
const formRef = ref<FormInstance>()
const uploadedAvatarPreviewUrl = ref('')
const roleLoading = ref(false)
const roleOptions = ref<Role[]>([])
const farmOwnerOptions = ref<SmartPlantUser[]>([])
const isFarmOwnerManager = getCurrentRoleCode() === 'farm_owner'
const selectedRoleCode = computed(() => getRoleById(form.value.roleId)?.roleCode || '')
const needsFarmOwner = computed(() => ['user', 'technician'].includes(selectedRoleCode.value))
const USER_AVATAR_STORAGE_PREFIX = 'smart_farm_user_avatar:'

const genderOptions = [
  { label: '未知', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const createEmptyForm = (): UserFormModel => ({
  username: '',
  password: '',
  phone: '',
  email: '',
  avatar: '',
  nickname: '',
  gender: undefined,
  roleId: undefined,
  farmOwnerId: undefined,
  status: 1,
  region: '',
  remark: '',
})

const form = ref<UserFormModel>(createEmptyForm())

const formRules = computed<FormRules<UserFormModel>>(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' },
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (!isEdit.value && !value) {
          callback(new Error('请输入密码'))
          return
        }

        if (value && !/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(value)) {
          callback(new Error('密码需为 8-32 位，且同时包含字母和数字'))
          return
        }

        callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' },
  ],
  roleId: [{ required: true, message: '请选择用户角色', trigger: 'change' }],
  farmOwnerId: [{
    validator: (_rule, value: number | undefined, callback) => {
      // 仅普通用户和技术人员要求绑定农场主，管理员/专家等角色无需填写。
      callback(needsFarmOwner.value && !value ? new Error('请选择绑定农场主') : undefined)
    },
    trigger: 'change',
  }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }],
}))

const isAvatarImageUrl = (value?: string) => {
  return Boolean(
    value &&
      value !== '/default-avatar.svg' &&
      (/^(https?:|blob:|data:image\/)/.test(value) || value.startsWith('/')),
  )
}

const getStoredAvatarUrl = (value?: string) => {
  if (!value?.startsWith(USER_AVATAR_STORAGE_PREFIX)) {
    return ''
  }

  return localStorage.getItem(value) || ''
}

const getRoleById = (roleId?: number) => {
  return roleOptions.value.find((item) => item.id === roleId)
}

const getRoleByCode = (roleCode?: string) => {
  return roleOptions.value.find((item) => item.roleCode === roleCode)
}

const loadRoles = async () => {
  roleLoading.value = true

  try {
    const result = await listAssignableUserRoles()
    roleOptions.value = result.data
  } finally {
    roleLoading.value = false
  }
}

const loadFarmOwners = async () => {
  const result = await listAvailableFarmOwners()
  farmOwnerOptions.value = result.data
  // 农场主的绑定目标由后端强制为本人，表单自动填充以减少操作。
  if (isFarmOwnerManager && !isEdit.value) form.value.farmOwnerId = result.data[0]?.id
}

const resetForm = () => {
  form.value = createEmptyForm()
  uploadedAvatarPreviewUrl.value = ''
  formRef.value?.clearValidate()
}

const fillForm = (user: SmartPlantUser) => {
  const role = getRoleById(user.roleId) || getRoleByCode(user.roleCode)

  form.value = {
    username: user.username || '',
    password: '',
    phone: user.phone || '',
    email: user.email || '',
    avatar: user.avatar || '',
    nickname: user.nickname || '',
    gender: user.gender,
    roleId: role?.id || user.roleId,
    farmOwnerId: user.farmOwnerId,
    status: user.status,
    region: user.region || '',
    remark: user.remark || '',
  }
  uploadedAvatarPreviewUrl.value = ''
}

const loadUserDetail = async (id: number) => {
  detailLoading.value = true

  try {
    const result = await getSmartPlantUserById(id)
    fillForm(result.data)
  } finally {
    detailLoading.value = false
  }
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
    const result = await uploadManagedImage(rawFile, 'user-avatar')
    form.value.avatar = result.data.imageUrl
    uploadedAvatarPreviewUrl.value = getFileUrl(result.data.imageUrl)
    ElMessage.success('头像上传成功')
  } finally {
    uploadLoading.value = false
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
  const role = getRoleById(form.value.roleId)
  const data = removeEmptyOptionalFields({
    ...form.value,
    farmOwnerId: needsFarmOwner.value ? form.value.farmOwnerId : undefined,
    roleCode: role?.roleCode,
    roleName: role?.roleName,
  }) as unknown as CreateSmartPlantUserPayload

  return data
}

const buildUpdatePayload = (id: number) => {
  const data = removeEmptyOptionalFields({
    id,
    ...form.value,
    farmOwnerId: needsFarmOwner.value ? form.value.farmOwnerId : undefined,
  }) as unknown as UpdateSmartPlantUserPayload

  if (!form.value.password) {
    delete data.password
  }

  return data
}

/**
 * 根据 id 判断新增还是编辑。
 * id 为空调用新增接口；id 不为空调用编辑接口。
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
        ? await addSmartPlantUser(buildCreatePayload())
        : await updateSmartPlantUser(buildUpdatePayload(props.id))

    ElMessage.success(isEdit.value ? '编辑用户成功' : '新增用户成功')
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
    void loadRoles()
    void loadFarmOwners()

    if (props.id !== undefined && props.id !== null) {
      void loadUserDetail(props.id)
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="add-user-dialog"
    modal-class="add-user-dialog-overlay"
    align-center
    destroy-on-close
    @closed="resetForm"
  >
    <div v-loading="detailLoading">
      <el-form
        ref="formRef"
        class="add-user-form"
        :model="form"
        :rules="formRules"
        label-width="96px"
        autocomplete="off"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            autocomplete="off"
            name="smart_farm_user_username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :placeholder="isEdit ? '不修改密码请留空' : '请输入密码'"
            autocomplete="new-password"
            name="smart_farm_user_password"
            show-password
            clearable
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" clearable />
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
            <img v-if="currentAvatarPreview" class="avatar-image" :src="currentAvatarPreview" alt="用户头像" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择性别" clearable>
            <el-option
              v-for="item in genderOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用户角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择用户角色" :loading="roleLoading">
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.roleName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <!-- 沿用现有表单行样式，仅在新增/编辑普通用户或技术员时展示绑定项。 -->
        <el-form-item v-if="needsFarmOwner" label="绑定农场主" prop="farmOwnerId">
          <el-select v-model="form.farmOwnerId" placeholder="请选择农场主" filterable :disabled="isFarmOwnerManager">
            <el-option
              v-for="owner in farmOwnerOptions"
              :key="owner.id"
              :label="owner.nickname || owner.username"
              :value="owner.id!"
            />
          </el-select>
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
        <el-form-item label="地区" prop="region">
          <el-input v-model="form.region" placeholder="请输入地区" clearable />
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
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
:global(.add-user-dialog) {
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 auto;
}

:global(.add-user-dialog-overlay .el-overlay-dialog) {
  overflow: hidden;
}

:global(.add-user-dialog .el-dialog__header) {
  flex: 0 0 auto;
  margin: 0;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #eef2f7;
}

:global(.add-user-dialog .el-dialog__title) {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

:global(.add-user-dialog .el-dialog__body) {
  flex: 1 1 auto;
  min-height: 0;
  padding: 22px 24px 6px;
  background: #fbfdff;
  max-height: none;
  overflow-y: auto;
  overflow-x: hidden;
}

:global(.add-user-dialog .el-dialog__body::-webkit-scrollbar) {
  width: 6px;
}

:global(.add-user-dialog .el-dialog__body::-webkit-scrollbar-thumb) {
  border-radius: 999px;
  background: #cbd5e1;
}

:global(.add-user-dialog .el-dialog__footer) {
  flex: 0 0 auto;
  padding: 14px 24px 20px;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
}

.add-user-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2px;
  padding: 2px 2px 0;
}

.add-user-form :deep(.el-form-item) {
  margin-bottom: 17px;
}

.add-user-form :deep(.el-select) {
  width: 100%;
}

.add-user-form :deep(.el-input__wrapper),
.add-user-form :deep(.el-select__wrapper),
.add-user-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.add-user-form :deep(.el-input__wrapper),
.add-user-form :deep(.el-select__wrapper) {
  min-height: 38px;
}

.add-user-form :deep(.el-input__wrapper.is-focus),
.add-user-form :deep(.el-select__wrapper.is-focused),
.add-user-form :deep(.el-textarea__inner:focus) {
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
  :global(.add-user-dialog) {
    width: calc(100vw - 28px) !important;
  }
}
</style>
