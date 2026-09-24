<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Hide, Lock, User, View } from '@element-plus/icons-vue'
import { getAdminCaptcha, loginAdmin } from '@/api/auth'
import { getCurrentUser, setAuthTokens, setCurrentUser } from '@/utils/auth'
import { refreshCurrentAdminSession } from '@/services/adminSession'
import { systemSetting } from '@/stores/systemSetting'
import { getFileUrl } from '@/utils/utils'

interface LoginForm {
  username: string
  password: string
  captcha: string
  rememberMe: boolean
}

type LoginField = 'username' | 'password' | 'captcha'

interface ErrorResponse {
  status?: number
  message?: string
  code?: string
  data?: {
    code?: number
    message?: string
  }
}

const router = useRouter()
const loginFormRef = ref<FormInstance>()
const passwordVisible = ref(false)
const submitLoading = ref(false)
const captchaCode = ref('')
const captchaKey = ref('')
const captchaImage = ref('')
const lastSubmitAt = ref(0)

// 单独保存字段错误，使输入框通过 aria-describedby 关联到可读错误信息。
const fieldErrors = reactive<Record<LoginField, string>>({
  username: '',
  password: '',
  captcha: '',
})

const loginForm = reactive<LoginForm>({
  username: '',
  password: '',
  captcha: '',
  // 仅通过令牌保持登录，不保存或回填账号密码。
  rememberMe: false,
})

const submitDisabled = computed(() => submitLoading.value)
// 配置登录背景后叠加轻度遮罩，确保自定义图片不会降低表单可读性。
const loginPageStyle = computed(() => systemSetting.loginBackgroundUrl
  ? { backgroundImage: `linear-gradient(rgba(237, 244, 249, 0.78), rgba(201, 216, 251, 0.82)), url("${getFileUrl(systemSetting.loginBackgroundUrl)}")` }
  : undefined)

const createLocalCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  captchaCode.value = Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
  captchaKey.value = ''
  captchaImage.value = ''
}

const refreshCaptcha = async () => {
  loginForm.captcha = ''

  try {
    const result = await getAdminCaptcha()
    captchaKey.value = result.data?.captchaKey || ''
    captchaImage.value = result.data?.captchaImage || ''
    captchaCode.value = ''

    if (!captchaKey.value || !captchaImage.value) {
      createLocalCaptcha()
    }
  } catch {
    createLocalCaptcha()
  }
}

const validateCaptcha = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请输入验证码'))
    return
  }

  if (captchaKey.value) {
    callback()
    return
  }

  if (value.trim().toUpperCase() !== captchaCode.value) {
    callback(new Error('验证码不正确'))
    return
  }

  callback()
}

const loginRules = reactive<FormRules<LoginForm>>({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ validator: validateCaptcha, trigger: 'blur' }],
})

const togglePasswordVisible = () => {
  passwordVisible.value = !passwordVisible.value
}

/** 同步 Element Plus 校验结果，供屏幕阅读器读取对应字段的错误。 */
const handleFieldValidate = (prop: string | string[], isValid: boolean, message: string) => {
  const field = Array.isArray(prop) ? prop.join('.') : prop
  if (field === 'username' || field === 'password' || field === 'captcha') {
    fieldErrors[field] = isValid ? '' : message
  }
}

const rememberFailure = () => {
  void refreshCaptcha()
}

const getRateLimitMessage = (message?: string) => {
  const waitText = message?.match(/(\d+)\s*秒/)?.[1]
  return waitText ? `请求过于频繁，请等待 ${waitText} 秒后重试` : '请求过于频繁，请稍后再试'
}

const showLoginError = (error: ErrorResponse) => {
  const status = error.status || error.data?.code
  const message = error.data?.message

  if (!status) {
    const isTimeout = error.code === 'ECONNABORTED' || error.message?.includes('timeout')
    ElMessage.error(isTimeout ? '登录请求超时，请稍后重试' : '无法连接后端服务，请确认 smart_plant 已启动且接口地址正确')
    return
  }

  if (status === 403) {
    ElMessage.error(message?.includes('HTTPS') ? '请使用 HTTPS 访问后端服务' : message || '当前账号无后台登录权限')
    return
  }

  if (status === 429) {
    ElMessage.error(getRateLimitMessage(message))
    return
  }

  if (message) {
    ElMessage.error(message)
    return
  }

  ElMessage.error(status === 401 ? '账号或密码错误' : '登录失败，请稍后再试')
}

const handleLogin = async () => {
  const now = Date.now()

  if (submitLoading.value || now - lastSubmitAt.value < 800) {
    return
  }

  lastSubmitAt.value = now

  const form = loginFormRef.value

  if (!form) {
    return
  }

  const isValid = await form.validate().catch(() => false)

  if (!isValid) {
    return
  }

  submitLoading.value = true

  try {
    const result = await loginAdmin({
      username: loginForm.username.trim(),
      password: loginForm.password,
      captcha: loginForm.captcha.trim(),
      captchaKey: captchaKey.value || undefined,
      rememberMe: loginForm.rememberMe,
    })

    if (!result.data?.token) {
      throw { status: 401, data: { message: '账号或密码错误' } }
    }

    setAuthTokens(result.data.token, result.data.refreshToken || '', loginForm.rememberMe)
    loginForm.password = ''
    const admin = result.data.admin
    setCurrentUser(
      admin
        ? {
            ...admin,
            roleCode: result.data.roleCode || admin.roleCode || admin.role,
            dataScope: result.data.dataScope,
            permissions: result.data.permissions || [],
          }
        : undefined,
      loginForm.rememberMe,
    )
    // 登录后统一读取一次服务器用户信息；失败时保留登录响应中的最小身份信息。
    const currentUser = await refreshCurrentAdminSession(true).catch(() => getCurrentUser())
    ElMessage.success(`欢迎回来，${currentUser?.nickname || currentUser?.username || loginForm.username}`)
    await router.push('/home')
  } catch (error) {
    rememberFailure()
    showLoginError(error as ErrorResponse)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  void refreshCaptcha()
})
</script>

<template>
  <main class="login-page" :style="loginPageStyle">
    <section class="login-card" :aria-label="`${systemSetting.systemName} 登录`">
      <div class="login-form-panel">
        <div class="login-heading">
          <p class="login-kicker">使用账号密码登录</p>
          <h1>登录</h1>
          <p>{{ systemSetting.systemDescription || systemSetting.systemName }}</p>
        </div>

        <el-form ref="loginFormRef" class="login-form" :model="loginForm" :rules="loginRules"
          label-position="top" hide-required-asterisk :show-message="false" autocomplete="on"
          @validate="handleFieldValidate" @submit.prevent="handleLogin">
          <el-form-item label="账号" prop="username" input-id="login-username">
            <el-input v-model.trim="loginForm.username" :prefix-icon="User" autocomplete="username" placeholder="请输入账号"
              id="login-username" name="username" size="large" :aria-invalid="Boolean(fieldErrors.username)"
              :aria-describedby="fieldErrors.username ? 'login-username-error' : undefined" />
            <p v-if="fieldErrors.username" id="login-username-error" class="field-error" role="alert">
              {{ fieldErrors.username }}
            </p>
          </el-form-item>

          <el-form-item label="密码" prop="password" input-id="login-password">
            <el-input v-model="loginForm.password" :prefix-icon="Lock" :show-password="false"
              :type="passwordVisible ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码"
              id="login-password" name="password" size="large" :aria-invalid="Boolean(fieldErrors.password)"
              :aria-describedby="fieldErrors.password ? 'login-password-error' : undefined">
              <template #suffix>
                <button class="password-toggle" type="button" :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                  @click="togglePasswordVisible">
                  <el-icon>
                    <component :is="passwordVisible ? View : Hide" />
                  </el-icon>
                </button>
              </template>
            </el-input>
            <p v-if="fieldErrors.password" id="login-password-error" class="field-error" role="alert">
              {{ fieldErrors.password }}
            </p>
          </el-form-item>

          <el-form-item label="验证码" prop="captcha" input-id="login-captcha">
            <div class="captcha-row">
              <el-input v-model.trim="loginForm.captcha" class="captcha-input" maxlength="4" placeholder="请输入验证码"
                id="login-captcha" name="captcha" autocomplete="off" size="large"
                :aria-invalid="Boolean(fieldErrors.captcha)"
                :aria-describedby="fieldErrors.captcha ? 'login-captcha-error' : undefined" />
              <button class="captcha-code" type="button" aria-label="刷新验证码" title="点击刷新验证码" @click="refreshCaptcha">
                <img v-if="captchaImage" class="captcha-image" :src="captchaImage" alt="验证码" />
                <span v-else>{{ captchaCode }}</span>
              </button>
            </div>
            <p v-if="fieldErrors.captcha" id="login-captcha-error" class="field-error" role="alert">
              {{ fieldErrors.captcha }}
            </p>
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="loginForm.rememberMe">
              保持登录
            </el-checkbox>
          </div>

          <el-button class="login-button" type="primary" native-type="submit" size="large" :loading="submitLoading"
            :disabled="submitDisabled">
            登录
          </el-button>
        </el-form>
      </div>

      <aside class="brand-panel">
        <div class="brand-content">
          <p class="brand-eyebrow">{{ systemSetting.systemName }}</p>
          <h2>欢迎使用 {{ systemSetting.systemName }}</h2>
          <p>{{ systemSetting.systemDescription || '集中管理用户、地块与水培数据，让种植空间稳定、高效运行。' }}</p>
        </div>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  background:
    radial-gradient(circle at 24% 20%, rgba(255, 255, 255, 0.95), transparent 34%),
    linear-gradient(115deg, #edf4f9 0%, #edf6fb 42%, #c9d8fb 100%);
  background-position: center;
  background-size: cover;
}

.login-card {
  width: min(1040px, 100%);
  min-height: 600px;
  display: grid;
  grid-template-columns: 1fr 0.9fr;
  overflow: hidden;
  border-radius: 28px;
  background: #ffffff;
  box-shadow: 0 28px 70px rgba(43, 71, 103, 0.18);
}

.login-form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 56px;
}

.login-heading {
  width: min(420px, 100%);
  margin-bottom: 26px;
  text-align: center;
}

.login-kicker {
  margin-bottom: 16px;
  color: #259aaa;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.login-heading h1 {
  color: #111827;
  font-size: 40px;
  line-height: 1.2;
  font-weight: 800;
}

.login-heading p:last-child {
  margin-top: 12px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 700;
}

.login-form {
  width: min(420px, 100%);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-form-item__label) {
  margin-bottom: 8px;
  color: #243247;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 800;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 54px;
  padding: 0 18px;
  border-radius: 8px;
  background: #edf5ff;
  box-shadow: none;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  outline: 3px solid rgba(42, 160, 173, 0.24);
  outline-offset: 2px;
  box-shadow: none;
}

.login-form :deep(.el-input__inner) {
  color: #243247;
  font-weight: 700;
}

.password-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: #9aa8bb;
  cursor: pointer;
  font-size: 16px;
}

.password-toggle:focus-visible,
.captcha-code:focus-visible,
.login-button:focus-visible {
  outline: 3px solid rgba(21, 127, 140, 0.38);
  outline-offset: 3px;
}

.login-options :deep(.el-checkbox:has(.el-checkbox__original:focus-visible)) {
  outline: 3px solid rgba(21, 127, 140, 0.32);
  outline-offset: 3px;
  border-radius: 4px;
}

.field-error {
  width: 100%;
  margin: 6px 0 0;
  color: #c2413b;
  font-size: 13px;
  line-height: 1.45;
  font-weight: 600;
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 12px;
}

.captcha-code {
  height: 54px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0;
  overflow: hidden;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: linear-gradient(135deg, #e0f2fe, #ecfeff);
  color: #0f766e;
  cursor: pointer;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.18em;
}

.captcha-image {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 8px;
  object-fit: cover;
}

.login-options {
  min-height: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: -4px 0 18px;
}

.login-options :deep(.el-checkbox.is-checked .el-checkbox__label) {
  color: #259aaa;
  /* 选中时文字颜色 */
}

.login-options :deep(.el-checkbox.is-checked .el-checkbox__inner) {
  background-color: #259aaa;
  /* 复选框背景 */
  border-color: #259aaa;
  /* 边框颜色 */
}

.login-options :deep(.el-checkbox__inner::after) {
  border-color: #ffffff;
  /* 对勾颜色（可选，默认白色） */
}

.login-button {
  width: 164px;
  height: 56px;
  display: block;
  margin: 0 auto;
  border: 0;
  border-radius: 8px;
  background: #259aaa;
  font-weight: 700;
  box-shadow: 0 12px 24px rgba(37, 154, 170, 0.28);
}

.login-button:hover,
.login-button:focus {
  background: #1f8d9d;
}

.brand-panel {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 72px 54px;
  color: #ffffff;
  background: linear-gradient(145deg, var(--system-theme-color, #2c7fba) 0%, #1aa39e 100%);
  border-top-left-radius: 150px;
  border-bottom-left-radius: 150px;
}

.brand-content {
  max-width: 430px;
  text-align: center;
}

.brand-eyebrow {
  margin-bottom: 26px;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.48em;
  text-transform: uppercase;
  opacity: 0.75;
}

.brand-content h2 {
  font-size: 36px;
  line-height: 1.25;
  font-weight: 900;
}

.brand-content p:last-child {
  margin-top: 22px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 17px;
  line-height: 1.8;
  font-weight: 700;
}

/* 笔记本的浏览器可用高度较小时，压缩纵向留白，避免登录页产生滚动条。 */
@media (min-width: 861px) and (max-height: 760px) {
  .login-page {
    min-height: 100dvh;
    padding-block: 16px;
  }

  .login-card {
    min-height: calc(100dvh - 32px);
  }

  .login-form-panel {
    padding-block: 24px;
  }

  .login-heading {
    margin-bottom: 14px;
  }

  .login-kicker {
    margin-bottom: 8px;
  }

  .login-heading h1 {
    font-size: 36px;
  }

  .login-heading p:last-child {
    margin-top: 8px;
  }

  .login-form :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  .login-form :deep(.el-form-item__label) {
    margin-bottom: 4px;
  }

  .login-form :deep(.el-input__wrapper),
  .captcha-code {
    min-height: 48px;
    height: 48px;
  }

  .login-options {
    min-height: 24px;
    margin-bottom: 10px;
  }

  .login-button {
    height: 50px;
  }

  .brand-panel {
    padding-block: 40px;
  }
}

@media (max-width: 860px) {
  .login-page {
    padding: 28px 16px;
  }

  .login-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    order: -1;
    min-height: 250px;
    border-top-left-radius: 0;
    border-bottom-left-radius: 110px;
    border-bottom-right-radius: 110px;
  }

  .login-form-panel {
    padding: 42px 24px 48px;
  }

  .brand-content h2,
  .login-heading h1 {
    font-size: 32px;
  }
}

@media (max-width: 520px) {
  .login-card {
    border-radius: 20px;
  }

  .brand-panel {
    min-height: 220px;
    padding: 40px 22px;
  }

  .brand-eyebrow {
    letter-spacing: 0.28em;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }

  .login-options {
    align-items: flex-start;
    flex-direction: column;
  }

  .login-button {
    width: 100%;
  }
}
</style>
