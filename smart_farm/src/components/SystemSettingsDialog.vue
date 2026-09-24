<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Brush, Calendar, InfoFilled, Monitor, UploadFilled } from '@element-plus/icons-vue'
import {
  getAdminSystemSetting,
  updateSystemSetting,
  uploadSystemAsset,
  type SystemAssetType,
  type SystemSetting,
  type SystemSettingUpdatePayload,
} from '@/api/systemSetting'
import { applySystemSetting } from '@/stores/systemSetting'
import { getFileUrl } from '@/utils/utils'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; updated: [] }>()

type SettingSection = 'basic' | 'appearance' | 'time' | 'upload' | 'site'

const activeSection = ref<SettingSection>('basic')
const loading = ref(false)
const saving = ref(false)
const uploadingType = ref<SystemAssetType>()
const formRef = ref<FormInstance>()
const allowedTypes = ref<string[]>([])

// 表单字段与后端更新白名单保持一致，version用于防止并发覆盖。
const form = reactive<SystemSettingUpdatePayload>({
  systemName: '', logoUrl: '', faviconUrl: '', systemDescription: '', copyrightInfo: '',
  homeTitle: '', loginBackgroundUrl: '', themeColor: '#239aaa', timezone: 'Asia/Shanghai',
  datetimeFormat: 'yyyy-MM-dd HH:mm:ss', defaultPageSize: 10, maxUploadSizeMb: 5,
  allowedUploadTypes: '', recordNumber: '', officialWebsite: '', contactEmail: '',
  servicePhone: '', version: 0,
})

const sections = [
  { key: 'basic' as const, label: '基本信息', icon: InfoFilled },
  { key: 'appearance' as const, label: '外观设置', icon: Brush },
  { key: 'time' as const, label: '时间规格', icon: Calendar },
  { key: 'upload' as const, label: '上传参数', icon: UploadFilled },
  { key: 'site' as const, label: '站点信息', icon: Monitor },
]

const timezoneOptions = [
  { label: '中国标准时间（Asia/Shanghai）', value: 'Asia/Shanghai' },
  { label: '协调世界时（UTC）', value: 'UTC' },
  { label: '东京时间（Asia/Tokyo）', value: 'Asia/Tokyo' },
  { label: '新加坡时间（Asia/Singapore）', value: 'Asia/Singapore' },
]
const datetimeOptions = ['yyyy-MM-dd HH:mm:ss', 'yyyy/MM/dd HH:mm:ss', 'dd/MM/yyyy HH:mm:ss', 'MM/dd/yyyy hh:mm:ss a']
const safeUploadTypes = ['jpg', 'jpeg', 'png', 'webp', 'pdf', 'xlsx', 'xls', 'csv', 'doc', 'docx', 'txt', 'zip']

const rules: FormRules<SystemSettingUpdatePayload> = {
  systemName: [{ required: true, message: '请输入系统名称', trigger: 'blur' }, { max: 100, message: '不能超过100个字符', trigger: 'blur' }],
  homeTitle: [{ required: true, message: '请输入首页标题', trigger: 'blur' }, { max: 100, message: '不能超过100个字符', trigger: 'blur' }],
  themeColor: [{ required: true, pattern: /^#[0-9a-fA-F]{6}$/, message: '请选择有效主题色', trigger: 'change' }],
  timezone: [{ required: true, message: '请选择时区', trigger: 'change' }],
  datetimeFormat: [{ required: true, message: '请选择日期时间格式', trigger: 'change' }],
  defaultPageSize: [{ required: true, message: '请输入分页默认条数', trigger: 'change' }],
  maxUploadSizeMb: [{ required: true, message: '请输入上传大小限制', trigger: 'change' }],
  officialWebsite: [{ type: 'url', message: '请输入有效的HTTP或HTTPS地址', trigger: 'blur' }],
  contactEmail: [{ type: 'email', message: '请输入有效的联系邮箱', trigger: 'blur' }],
}

/** 将接口值完整回填，并把逗号分隔扩展名转换为多选数据。 */
const applySetting = (value: SystemSetting) => {
  Object.assign(form, value)
  allowedTypes.value = (value.allowedUploadTypes || '').split(',').map((item) => item.trim()).filter(Boolean)
}

const openDialog = async () => {
  activeSection.value = 'basic'
  loading.value = true
  try {
    const result = await getAdminSystemSetting()
    applySetting(result.data)
  } finally {
    loading.value = false
  }
}

/** 品牌图片上传成功后仅写入表单，点击保存后才正式成为当前配置。 */
const handleAssetUpload = async (type: SystemAssetType, options: UploadRequestOptions) => {
  const file = options.file
  const isImage = ['image/jpeg', 'image/png', 'image/webp', 'image/x-icon', 'image/vnd.microsoft.icon'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('仅支持JPG、PNG、WEBP或ICO图片')
    return
  }
  if (file.size > form.maxUploadSizeMb * 1024 * 1024) {
    ElMessage.warning(`图片大小不能超过${form.maxUploadSizeMb}MB`)
    return
  }
  uploadingType.value = type
  try {
    const result = await uploadSystemAsset(type, file)
    if (type === 'logo') form.logoUrl = result.data.url
    if (type === 'favicon') form.faviconUrl = result.data.url
    if (type === 'loginBackground') form.loginBackgroundUrl = result.data.url
    ElMessage.success('图片上传成功，请保存系统设置')
  } finally {
    uploadingType.value = undefined
  }
}

// 模板使用具名处理器保留UploadRequestOptions类型，避免内联回调退化为any。
const uploadLogo = (options: UploadRequestOptions) => handleAssetUpload('logo', options)
const uploadFavicon = (options: UploadRequestOptions) => handleAssetUpload('favicon', options)
const uploadLoginBackground = (options: UploadRequestOptions) => handleAssetUpload('loginBackground', options)

const saveSetting = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!allowedTypes.value.length) {
    activeSection.value = 'upload'
    ElMessage.warning('请至少选择一种允许上传的文件类型')
    return
  }
  saving.value = true
  try {
    form.allowedUploadTypes = allowedTypes.value.join(',')
    const result = await updateSystemSetting({ ...form })
    applySetting(result.data)
    applySystemSetting(result.data)
    window.dispatchEvent(new CustomEvent('system-setting-updated'))
    emit('updated')
    ElMessage.success('系统设置保存成功')
  } finally {
    saving.value = false
  }
}

const closeDialog = () => emit('update:modelValue', false)
watch(() => props.modelValue, (visible) => { if (visible) void openDialog() })
</script>

<template>
  <el-dialog :model-value="modelValue" width="920px" class="system-setting-dialog" append-to-body destroy-on-close
    :close-on-click-modal="false" @close="closeDialog">
    <template #header><strong class="dialog-title">系统设置</strong></template>
    <div v-loading="loading" class="setting-layout">
      <aside class="setting-menu" aria-label="系统设置菜单">
        <button v-for="item in sections" :key="item.key" type="button" :class="{ active: activeSection === item.key }"
          @click="activeSection = item.key">
          <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
        </button>
      </aside>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="112px" class="setting-panel">
        <section v-show="activeSection === 'basic'">
          <h3>基本信息</h3>
          <el-form-item label="系统名称" prop="systemName"><el-input v-model="form.systemName" maxlength="100" /></el-form-item>
          <div class="asset-grid">
            <el-form-item label="Logo">
              <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp" :http-request="uploadLogo">
                <div v-loading="uploadingType === 'logo'" class="asset-card logo-card">
                  <img v-if="form.logoUrl" :src="getFileUrl(form.logoUrl)" alt="系统Logo" /><el-icon v-else><UploadFilled /></el-icon><span>上传Logo</span>
                </div>
              </el-upload>
            </el-form-item>
            <el-form-item label="网站图标">
              <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/webp,.ico" :http-request="uploadFavicon">
                <div v-loading="uploadingType === 'favicon'" class="asset-card favicon-card">
                  <img v-if="form.faviconUrl" :src="getFileUrl(form.faviconUrl)" alt="网站图标" /><el-icon v-else><UploadFilled /></el-icon><span>上传图标</span>
                </div>
              </el-upload>
            </el-form-item>
          </div>
          <el-form-item label="系统描述"><el-input v-model="form.systemDescription" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
          <el-form-item label="版权信息"><el-input v-model="form.copyrightInfo" maxlength="255" /></el-form-item>
        </section>

        <section v-show="activeSection === 'appearance'">
          <h3>外观设置</h3>
          <el-form-item label="首页标题" prop="homeTitle"><el-input v-model="form.homeTitle" maxlength="100" /></el-form-item>
          <el-form-item label="系统主题" prop="themeColor">
            <div class="theme-row"><el-color-picker v-model="form.themeColor" /><el-input v-model="form.themeColor" maxlength="7" /></div>
          </el-form-item>
          <el-form-item label="登录页背景">
            <el-upload class="background-upload" :show-file-list="false" accept="image/jpeg,image/png,image/webp" :http-request="uploadLoginBackground">
              <div v-loading="uploadingType === 'loginBackground'" class="background-card">
                <img v-if="form.loginBackgroundUrl" :src="getFileUrl(form.loginBackgroundUrl)" alt="登录页背景" />
                <div v-else><el-icon><UploadFilled /></el-icon><span>上传登录页背景</span></div>
              </div>
            </el-upload>
          </el-form-item>
        </section>

        <section v-show="activeSection === 'time'">
          <h3>时间规格</h3>
          <el-form-item label="时区设置" prop="timezone"><el-select v-model="form.timezone" class="full-width"><el-option v-for="item in timezoneOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="日期时间格式" prop="datetimeFormat"><el-select v-model="form.datetimeFormat" class="full-width"><el-option v-for="item in datetimeOptions" :key="item" :label="item" :value="item" /></el-select></el-form-item>
          <el-alert title="时间格式使用Java日期规则，保存后供系统列表和报表统一接入。" type="info" :closable="false" show-icon />
        </section>

        <section v-show="activeSection === 'upload'">
          <h3>上传参数</h3>
          <el-form-item label="分页默认条数" prop="defaultPageSize"><el-input-number v-model="form.defaultPageSize" :min="5" :max="200" :step="5" /></el-form-item>
          <el-form-item label="上传大小限制" prop="maxUploadSizeMb"><el-input-number v-model="form.maxUploadSizeMb" :min="1" :max="50" /><span class="unit">MB</span></el-form-item>
          <el-form-item label="允许文件类型">
            <el-select v-model="allowedTypes" multiple filterable class="full-width" placeholder="请选择允许的扩展名">
              <el-option v-for="item in safeUploadTypes" :key="item" :label="`.${item}`" :value="item" />
            </el-select>
          </el-form-item>
        </section>

        <section v-show="activeSection === 'site'">
          <h3>站点信息</h3>
          <el-form-item label="备案号"><el-input v-model="form.recordNumber" maxlength="100" /></el-form-item>
          <el-form-item label="官网地址" prop="officialWebsite"><el-input v-model="form.officialWebsite" placeholder="https://example.com" maxlength="255" /></el-form-item>
          <el-form-item label="联系邮箱" prop="contactEmail"><el-input v-model="form.contactEmail" maxlength="100" /></el-form-item>
          <el-form-item label="客服电话"><el-input v-model="form.servicePhone" maxlength="30" /></el-form-item>
        </section>
      </el-form>
    </div>
    <template #footer><el-button @click="closeDialog">取消</el-button><el-button type="primary" :loading="saving" @click="saveSetting">保存设置</el-button></template>
  </el-dialog>
</template>

<style scoped>
.dialog-title { color: #172033; font-size: 16px; }
.setting-layout { min-height: 500px; display: grid; grid-template-columns: 176px minmax(0, 1fr); border-top: 1px solid #edf1f6; }
.setting-menu { padding: 18px 12px; border-right: 1px solid #edf1f6; background: #f8fafc; }
.setting-menu button { width: 100%; display: flex; align-items: center; gap: 9px; padding: 11px 14px; border: 0; border-radius: 8px; color: #64748b; background: transparent; cursor: pointer; text-align: left; }
.setting-menu button.active { color: var(--system-theme-color, #239aaa); background: color-mix(in srgb, var(--system-theme-color, #239aaa) 11%, white); font-weight: 700; }
.setting-panel { min-width: 0; padding: 24px 38px; }
.setting-panel section { max-width: 610px; }
.setting-panel h3 { margin: 0 0 25px; color: #172033; font-size: 15px; font-weight: 700; }
.full-width { width: 100%; }
.asset-grid { display: grid; grid-template-columns: 1.25fr 1fr; gap: 14px; }
.asset-grid :deep(.el-form-item__content) { align-items: flex-start; }
.asset-card { min-width: 150px; height: 78px; display: flex; align-items: center; justify-content: center; gap: 10px; border: 1px dashed #cbd5e1; border-radius: 8px; color: var(--system-theme-color, #239aaa); background: #fafcff; overflow: hidden; cursor: pointer; }
.asset-card img { max-width: 94px; max-height: 56px; object-fit: contain; }
.favicon-card { min-width: 126px; }
.favicon-card img { width: 40px; height: 40px; }
.theme-row { width: 100%; display: grid; grid-template-columns: 42px minmax(0, 180px); gap: 12px; align-items: center; }
.background-upload, .background-upload :deep(.el-upload) { width: 100%; }
.background-card { width: 100%; height: 205px; display: grid; place-items: center; overflow: hidden; border: 1px dashed #cbd5e1; border-radius: 10px; color: var(--system-theme-color, #239aaa); background: #f8fafc; cursor: pointer; }
.background-card img { width: 100%; height: 100%; object-fit: cover; }
.background-card div { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.background-card .el-icon { font-size: 28px; }
.unit { margin-left: 10px; color: #64748b; }
@media (max-width: 760px) { .setting-layout { grid-template-columns: 1fr; } .setting-menu { display: flex; overflow-x: auto; border-right: 0; border-bottom: 1px solid #edf1f6; } .setting-menu button { min-width: 118px; } .setting-panel { padding: 22px 18px; } .asset-grid { grid-template-columns: 1fr; } }
</style>
