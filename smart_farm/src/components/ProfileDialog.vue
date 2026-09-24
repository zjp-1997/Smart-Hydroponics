<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, Plus, UserFilled } from '@element-plus/icons-vue'
import {
  changeAdminPassword,
  downloadAdminAvatar,
  getAdminProfile,
  updateAdminProfile,
  uploadAdminAvatar,
  type AdminPasswordChangePayload,
  type AdminProfileUpdatePayload,
  type AdminUser,
} from '@/api/auth'
import { listRoles, type Role } from '@/api/role'
import { clearLocalStorage } from '@/utils/utils'
import { refreshCurrentAdminSession } from '@/services/adminSession'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  updated: []
}>()

type ProfileSection = 'basic' | 'password'

const activeSection = ref<ProfileSection>('basic')
const loading = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const profile = ref<AdminUser | null>(null)
const roleOptions = ref<Role[]>([])
const avatarUrl = ref('')

// 用户名和昵称仅用于展示；请求对象只包含允许修改的字段。
const profileForm = reactive<AdminProfileUpdatePayload>({
  phone: '',
  email: '',
  avatar: '',
  gender: 0,
  roleId: undefined,
  region: '',
  remark: '',
})

const passwordForm = reactive<AdminPasswordChangePayload>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const isAdmin = computed(() => (profile.value?.roleCode || '').toLowerCase() === 'admin')

const clearAvatarPreview = () => {
  if (avatarUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(avatarUrl.value)
  }
  avatarUrl.value = ''
}

const setAvatarPreview = (file: Blob) => {
  clearAvatarPreview()
  avatarUrl.value = URL.createObjectURL(file)
}

/** 受保护的上传资源必须通过请求层携带 JWT，再转换成本地预览地址。 */
const loadAvatarPreview = async () => {
  clearAvatarPreview()
  if (!profileForm.avatar) return
  try {
    const response = await downloadAdminAvatar(profileForm.avatar)
    setAvatarPreview(response.data)
  } catch {
    // 图片失效时展示上传占位符，避免向用户显示破图图标。
  }
}

const profileRules: FormRules<AdminProfileUpdatePayload> = {
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { max: 20, message: '手机号不能超过20个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }, { max: 100, message: '邮箱不能超过100个字符', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  region: [{ max: 100, message: '地址不能超过100个字符', trigger: 'blur' }],
  remark: [{ max: 255, message: '个人备注不能超过255个字符', trigger: 'blur' }],
}

const passwordRules: FormRules<AdminPasswordChangePayload> = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 32, message: '密码长度应为8到32个字符', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码必须同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [{ required: true, message: '请再次输入新密码', trigger: 'blur' }],
}

const applyProfile = (value: AdminUser) => {
  profile.value = value
  profileForm.phone = value.phone || ''
  profileForm.email = value.email || ''
  profileForm.avatar = value.avatar || ''
  profileForm.gender = value.gender ?? 0
  profileForm.roleId = value.roleId
  profileForm.region = value.region || ''
  profileForm.remark = value.remark || ''
}

/** 管理员加载后台登录角色供选择；其他角色仅显示本人当前角色。 */
const fetchRoleOptions = async () => {
  if (!isAdmin.value) {
    roleOptions.value = profile.value?.roleId
      ? [{ id: profile.value.roleId, roleName: profile.value.roleName || '当前角色', roleCode: profile.value.roleCode || '', status: 1 }]
      : []
    return
  }
  const result = await listRoles({ status: 1, pageNum: 1, pageSize: 100 })
  roleOptions.value = (result.data.list || []).filter((role) => ['admin', 'farm_owner'].includes(role.roleCode.toLowerCase()))
}

const openDialog = async () => {
  activeSection.value = 'basic'
  loading.value = true
  try {
    const result = await getAdminProfile()
    applyProfile(result.data)
    await Promise.all([fetchRoleOptions(), loadAvatarPreview()])
  } finally {
    loading.value = false
  }
}

/** 自定义上传请求先校验类型和大小，再把后端 URL 写入待保存表单。 */
const handleAvatarUpload = async (options: UploadRequestOptions) => {
  const file = options.file
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('头像仅支持JPG、PNG或WEBP格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过5MB')
    return
  }
  avatarUploading.value = true
  try {
    const result = await uploadAdminAvatar(file)
    profileForm.avatar = result.data.url
    setAvatarPreview(file)
    ElMessage.success('头像上传成功，请保存基本信息')
  } finally {
    avatarUploading.value = false
  }
}

const saveProfile = async () => {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const result = await updateAdminProfile({ ...profileForm })
    applyProfile(result.data)
    // 角色可能发生变化，复用统一会话入口刷新角色、数据范围和权限缓存。
    await refreshCurrentAdminSession(true)
    emit('updated')
    ElMessage.success('个人信息保存成功')
  } finally {
    saving.value = false
  }
}

const changePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await changeAdminPassword({ ...passwordForm })
    ElMessage.success('密码修改成功，请重新登录')
    clearLocalStorage()
    window.location.href = '/'
  } finally {
    saving.value = false
  }
}

const closeDialog = () => {
  clearAvatarPreview()
  emit('update:modelValue', false)
}

onBeforeUnmount(clearAvatarPreview)

watch(() => props.modelValue, (visible) => {
  if (visible) void openDialog()
})
</script>

<template>
  <el-dialog :model-value="modelValue" width="860px" class="profile-dialog" append-to-body destroy-on-close :close-on-click-modal="false" @close="closeDialog">
    <template #header><strong class="dialog-title">个人中心</strong></template>
    <div v-loading="loading" class="profile-layout">
      <aside class="profile-menu" aria-label="个人中心菜单">
        <button type="button" :class="{ active: activeSection === 'basic' }" @click="activeSection = 'basic'"><el-icon><UserFilled /></el-icon>基本信息</button>
        <button type="button" :class="{ active: activeSection === 'password' }" @click="activeSection = 'password'"><el-icon><Lock /></el-icon>修改密码</button>
      </aside>

      <section class="profile-panel">
        <template v-if="activeSection === 'basic'">
          <h3>基本信息</h3>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="88px">
            <el-form-item label="头像">
              <el-upload class="avatar-uploader" :show-file-list="false" accept="image/jpeg,image/png,image/webp" :http-request="handleAvatarUpload">
                <div v-loading="avatarUploading" class="avatar-box">
                  <img v-if="avatarUrl" :src="avatarUrl" alt="用户头像" />
                  <el-icon v-else><Plus /></el-icon>
                  <span>修改头像</span>
                </div>
              </el-upload>
            </el-form-item>
            <div class="form-grid">
              <el-form-item label="用户名"><el-input :model-value="profile?.username" disabled /></el-form-item>
              <el-form-item label="角色" prop="roleId"><el-select v-model="profileForm.roleId" :disabled="!isAdmin" class="full-width"><el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" /></el-select></el-form-item>
              <el-form-item label="昵称"><el-input :model-value="profile?.nickname" disabled /></el-form-item>
              <el-form-item label="性别"><el-select v-model="profileForm.gender" class="full-width"><el-option label="未设置" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item>
              <el-form-item label="手机号" prop="phone"><el-input v-model="profileForm.phone" maxlength="20" /></el-form-item>
              <el-form-item label="邮箱" prop="email"><el-input v-model="profileForm.email" maxlength="100" /></el-form-item>
              <el-form-item label="地址" prop="region" class="span-two"><el-input v-model="profileForm.region" maxlength="100" /></el-form-item>
              <el-form-item label="个人备注" prop="remark" class="span-two"><el-input v-model="profileForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit /></el-form-item>
            </div>
          </el-form>
        </template>

        <template v-else>
          <h3>修改密码</h3>
          <p class="password-tip">修改后当前登录会话将失效，需要使用新密码重新登录。</p>
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="96px" class="password-form">
            <el-form-item label="当前密码" prop="currentPassword"><el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" /></el-form-item>
            <el-form-item label="新密码" prop="newPassword"><el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" /></el-form-item>
          </el-form>
        </template>
      </section>
    </div>
    <template #footer>
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" :loading="saving" @click="activeSection === 'basic' ? saveProfile() : changePassword()">{{ activeSection === 'basic' ? '保存修改' : '确认修改密码' }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-title { color: #172033; font-size: 16px; }
.profile-layout { min-height: 480px; display: grid; grid-template-columns: 168px minmax(0, 1fr); border-top: 1px solid #edf1f6; }
.profile-menu { padding: 18px 12px; border-right: 1px solid #edf1f6; background: #f8fafc; }
.profile-menu button { width: 100%; display: flex; align-items: center; gap: 9px; padding: 11px 14px; border: 0; border-radius: 8px; color: #64748b; background: transparent; cursor: pointer; text-align: left; }
.profile-menu button.active { color: #2c7fba; background: #eaf3fb; font-weight: 700; }
.profile-panel { padding: 24px 34px; }
.profile-panel h3 { margin: 0 0 25px; color: #172033; font-size: 15px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 20px; }
.span-two { grid-column: 1 / -1; }
.full-width { width: 100%; }
.avatar-box { display: flex; align-items: center; gap: 14px; color: #4f6ff0; cursor: pointer; }
.avatar-box img, .avatar-box > .el-icon { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; background: #edf4f7; }
.avatar-box > .el-icon { display: grid; place-items: center; color: #259aaa; font-size: 22px; }
.password-tip { margin: -12px 0 24px 96px; color: #94a3b8; font-size: 12px; }
.password-form { max-width: 520px; }
@media (max-width: 720px) { .profile-layout { grid-template-columns: 1fr; } .profile-menu { display: flex; border-right: 0; border-bottom: 1px solid #edf1f6; } .form-grid { grid-template-columns: 1fr; } .span-two { grid-column: auto; } }
</style>
