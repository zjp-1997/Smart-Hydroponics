<template>
  <view class="identity-page">
    <view class="page-hero">
      <view class="navbar">
        <button class="back" aria-label="返回个人中心" @tap="goBack"><text class="iconfont icon-fanhui" /></button>
        <text class="nav-title">身份认证</text><view class="placeholder" />
      </view>
    </view>

    <scroll-view scroll-y class="content">
      <view class="status-card" :class="`status-${auditStatus}`">
        <text class="iconfont icon-a-typeyonghustyledunpai status-icon" />
        <view class="status-copy">
          <text class="status-title">{{ statusLabel }}</text>
          <text class="status-tip">{{ statusTip }}</text>
        </view>
      </view>

      <view class="progress-card" aria-label="认证进度">
        <view v-for="(step, index) in steps" :key="step" class="progress-item" :class="{ active: currentStep >= index + 1 }">
          <view class="step-dot">{{ index + 1 }}</view><text>{{ step }}</text>
          <view v-if="index < steps.length - 1" class="step-line" />
        </view>
      </view>

      <view v-if="auditStatus === 3 && profile.remark" class="review-note">
        <text class="note-label">审核意见</text><text class="note-value">{{ profile.remark }}</text>
      </view>

      <view v-if="loading" class="loading-card"><view class="loading-ring" /><text>正在加载认证资料…</text></view>
      <view v-else-if="loadFailed" class="loading-card error-card"><text>认证资料加载失败</text><button @tap="loadProfile">重新加载</button></view>

      <view v-else class="form-card">
        <view class="section-head">
          <view><text class="section-title">认证资料</text><text class="section-tip">带 * 的项目为提交审核必填项</text></view>
          <button v-if="auditStatus === 2 && !editing" class="edit-btn" @tap="startEditing">修改资料</button>
        </view>

        <view class="field" :class="{ invalid: fieldErrors.realName }"><text class="label"><text class="required">*</text>真实姓名</text><input v-model.trim="form.realName" :disabled="!canEdit" maxlength="50" placeholder="请输入真实姓名" @input="clearError('realName')" /><text v-if="fieldErrors.realName" class="field-error">{{ fieldErrors.realName }}</text></view>
        <view class="field" :class="{ invalid: fieldErrors.organization }"><text class="label"><text class="required">*</text>所属机构</text><input v-model.trim="form.organization" :disabled="!canEdit" maxlength="100" placeholder="请输入单位或机构名称" @input="clearError('organization')" /><text v-if="fieldErrors.organization" class="field-error">{{ fieldErrors.organization }}</text></view>
        <view class="field" :class="{ invalid: fieldErrors.jobTitle }"><text class="label"><text class="required">*</text>职称</text><input v-model.trim="form.jobTitle" :disabled="!canEdit" maxlength="50" placeholder="如：高级农艺师" @input="clearError('jobTitle')" /><text v-if="fieldErrors.jobTitle" class="field-error">{{ fieldErrors.jobTitle }}</text></view>
        <view class="field" :class="{ invalid: fieldErrors.phone }"><text class="label">联系电话</text><input v-model.trim="form.phone" :disabled="!canEdit" maxlength="20" type="number" placeholder="请输入手机号" @input="clearError('phone')" /><text v-if="fieldErrors.phone" class="field-error">{{ fieldErrors.phone }}</text></view>
        <view class="field" :class="{ invalid: fieldErrors.email }"><text class="label">电子邮箱</text><input v-model.trim="form.email" :disabled="!canEdit" maxlength="100" placeholder="请输入电子邮箱" @input="clearError('email')" /><text v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</text></view>
        <view class="field"><text class="label">擅长方向</text><input v-model.trim="form.specialty" :disabled="!canEdit" maxlength="255" placeholder="如：作物病虫害防治" /></view>
        <view class="field"><text class="label">个人简介</text><textarea v-model.trim="form.introduction" :disabled="!canEdit" maxlength="1000" placeholder="请简要介绍从业经历和专业能力" /></view>
        <view class="field"><text class="label">证书名称</text><input v-model.trim="form.certificateName" :disabled="!canEdit" maxlength="100" placeholder="如：高级农艺师资格证" /></view>

        <view class="field certificate-field" :class="{ invalid: fieldErrors.certificateUrl }">
          <text class="label"><text class="required">*</text>资质证明</text>
          <view v-if="form.certificateUrl" class="certificate-preview" @tap="previewCertificate">
            <protected-image :src="certificatePreviewUrl" mode="aspectFill" /><text>点击预览</text>
          </view>
          <button v-if="canEdit" class="upload-btn" :disabled="busy" @tap="chooseCertificate">
            {{ uploading ? '上传中…' : (form.certificateUrl ? '重新上传' : '上传证书图片') }}
          </button>
          <text class="help-text">请上传清晰、完整且在有效期内的资质证书图片</text>
          <text v-if="fieldErrors.certificateUrl" class="field-error">{{ fieldErrors.certificateUrl }}</text>
        </view>
      </view>

      <view class="safe-space" :class="{ 'with-actions': showActions }" />
    </scroll-view>

    <view v-if="showActions" class="actions">
      <button class="secondary-btn" :disabled="busy" @tap="saveDraft">{{ saving ? '保存中…' : '保存草稿' }}</button>
      <button class="primary-btn" :disabled="busy" @tap="saveAndSubmit">{{ submitting ? '提交中…' : '保存并提交审核' }}</button>
    </view>
  </view>
</template>

<script>
import {
  getExpertProfile,
  saveExpertCertification,
  submitExpertCertification,
  uploadExpertCertificate,
} from '@/api/expertWorkspace.js'
import { resolveFileUrl } from '@/utils/request.js'
import { ensureExpertAccess } from '@/utils/expertAccess.js'

const emptyForm = () => ({
  realName: '', organization: '', jobTitle: '', phone: '', email: '', specialty: '',
  introduction: '', certificateName: '', certificateUrl: '',
})

export default {
  data() {
    return {
      profile: {}, form: emptyForm(), fieldErrors: {}, loading: true, loadFailed: false,
      editing: false, saving: false, uploading: false, submitting: false,
    }
  },
  computed: {
    auditStatus() { return Number(this.profile.auditStatus ?? 0) },
    statusLabel() { return ({ 0: '认证资料未提交', 1: '认证审核中', 2: '专家身份已认证', 3: '认证未通过' })[this.auditStatus] || '认证资料未提交' },
    statusTip() {
      return ({
        0: '完善身份资料后主动提交审核',
        1: '资料已锁定，请耐心等待管理员审核',
        2: '已获得专家服务权限；修改资料需重新审核',
        3: '请根据审核意见修改资料后再次提交',
      })[this.auditStatus]
    },
    canEdit() { return this.auditStatus === 0 || this.auditStatus === 3 || (this.auditStatus === 2 && this.editing) },
    showActions() { return !this.loading && !this.loadFailed && this.canEdit },
    busy() { return this.saving || this.uploading || this.submitting },
    certificatePreviewUrl() { return resolveFileUrl(this.form.certificateUrl) },
    currentStep() { return this.auditStatus === 2 ? 3 : (this.auditStatus === 1 ? 2 : 1) },
    steps() { return ['填写资料', '平台审核', '认证完成'] },
  },
  async onShow() {
    if (!await ensureExpertAccess()) return
    await this.loadProfile()
  },
  methods: {
    async loadProfile() {
      this.loading = true
      this.loadFailed = false
      try { this.applyProfile(await getExpertProfile() || {}) }
      catch { this.loadFailed = true }
      finally { this.loading = false }
    },
    applyProfile(profile) {
      this.profile = profile
      this.form = {
        realName: profile.realName || '', organization: profile.organization || '',
        jobTitle: profile.jobTitle || '', phone: profile.phone || '', email: profile.email || '',
        specialty: profile.specialty || '', introduction: profile.introduction || '',
        certificateName: profile.certificateName || '', certificateUrl: profile.certificateUrl || '',
      }
      this.editing = false
      this.fieldErrors = {}
    },
    startEditing() {
      uni.showModal({
        title: '确认修改认证资料？',
        content: '修改后当前认证会失效，专家服务将暂停，重新提交并审核通过后恢复。',
        confirmText: '继续修改',
        success: ({ confirm }) => { if (confirm) this.editing = true },
      })
    },
    validate(requireComplete = false) {
      const errors = {}
      if (this.form.phone && !/^1[3-9]\d{9}$/.test(this.form.phone)) errors.phone = '请输入正确的11位手机号'
      if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) errors.email = '请输入正确的电子邮箱'
      if (requireComplete) {
        const required = [['realName', '真实姓名'], ['organization', '所属机构'], ['jobTitle', '职称'], ['certificateUrl', '资质证明']]
        required.forEach(([key, label]) => { if (!String(this.form[key] || '').trim()) errors[key] = `请填写或上传${label}` })
      }
      this.fieldErrors = errors
      if (!Object.keys(errors).length) return true
      return this.warn(Object.values(errors)[0])
    },
    clearError(key) { if (this.fieldErrors[key]) this.fieldErrors = { ...this.fieldErrors, [key]: '' } },
    warn(title) { uni.showToast({ title, icon: 'none' }); return false },
    async persist(showSuccess = true) {
      if (!this.validate(false)) return false
      this.saving = true
      try {
        this.applyProfile(await saveExpertCertification(this.form))
        if (showSuccess) uni.showToast({ title: '草稿已保存', icon: 'success' })
        return true
      } catch { return false }
      finally { this.saving = false }
    },
    async saveDraft() { await this.persist(true) },
    async saveAndSubmit() {
      if (!this.validate(true)) return
      this.submitting = true
      try {
        if (!await this.persist(false)) return
        this.applyProfile(await submitExpertCertification())
        uni.showToast({ title: '已提交审核', icon: 'success' })
      } catch { /* 请求层统一提示。 */ }
      finally { this.submitting = false }
    },
    chooseCertificate() {
      uni.chooseImage({
        count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
        success: async ({ tempFilePaths }) => {
          this.uploading = true
          try {
            const result = await uploadExpertCertificate(tempFilePaths[0])
            this.form.certificateUrl = result.url
            this.clearError('certificateUrl')
            if (!this.form.certificateName) this.form.certificateName = '专家资质证书'
            uni.showToast({ title: '上传成功', icon: 'success' })
          } catch { /* 请求层统一提示。 */ }
          finally { this.uploading = false }
        },
      })
    },
    previewCertificate() { uni.previewImage({ current: this.certificatePreviewUrl, urls: [this.certificatePreviewUrl] }) },
    goBack() { if (getCurrentPages().length > 1) uni.navigateBack(); else uni.reLaunch({ url: '/pages/expert/mine' }) },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { height:100%; overflow:hidden; background:#f5f8f7; }
.identity-page { position:relative; height:100vh; overflow:hidden; color:#263631; background:#f5f8f7; }
.page-hero { box-sizing:border-box; height:270rpx; padding:calc(var(--status-bar-height) + 16rpx) 32rpx 0; background:linear-gradient(145deg,#178f82,#5fc0b2); }
.navbar { display:flex; align-items:center; justify-content:space-between; height:72rpx; }
.back,.placeholder { width:80rpx; height:80rpx; }
.back { display:flex; align-items:center; margin:0; padding:0; color:#fff; background:transparent; font-size:36rpx; }
.back::after,.edit-btn::after,.upload-btn::after,.actions button::after { border:0; }
.nav-title { color:#fff; font-size:34rpx; font-weight:600; }
.content { box-sizing:border-box; height:calc(100vh - 190rpx); margin-top:-80rpx; padding:0 28rpx; }
.status-card,.progress-card,.review-note,.form-card,.loading-card { background:#fff; border-radius:24rpx; box-shadow:0 10rpx 28rpx rgba(32,83,75,.07); }
.status-card { display:flex; align-items:center; gap:22rpx; padding:30rpx; border-left:8rpx solid #ea9a34; }
.status-2 { border-color:#159579; }.status-3 { border-color:#d95858; }
.status-icon { display:flex; align-items:center; justify-content:center; flex:0 0 88rpx; height:88rpx; border-radius:24rpx; color:#167f72; background:#e4f5f1; font-size:46rpx; }
.status-copy,.section-head>view { min-width:0; }.status-title,.status-tip,.section-title,.section-tip,.label,.help-text { display:block; }
.status-title { font-size:32rpx; font-weight:700; }.status-tip { margin-top:8rpx; color:#73817c; font-size:24rpx; line-height:1.5; }
.progress-card { display:flex; margin-top:22rpx; padding:26rpx 24rpx; }
.progress-item { position:relative; display:flex; flex:1; flex-direction:column; align-items:center; gap:10rpx; color:#8b9692; font-size:22rpx; }
.step-dot { z-index:1; display:flex; align-items:center; justify-content:center; width:44rpx; height:44rpx; border-radius:50%; color:#7b8984; background:#edf1ef; font-size:22rpx; }
.step-line { position:absolute; z-index:0; top:21rpx; left:calc(50% + 28rpx); width:calc(100% - 56rpx); height:4rpx; background:#e4eae7; }
.progress-item.active { color:#147d70; font-weight:600; }.progress-item.active .step-dot,.progress-item.active .step-line { color:#fff; background:#168d7f; }
.review-note { display:flex; gap:20rpx; margin-top:22rpx; padding:24rpx 28rpx; background:#fff6f5; }
.note-label { flex:none; color:#b64242; font-weight:600; }.note-value { color:#7c4b4b; line-height:1.55; }
.form-card { margin-top:24rpx; padding:30rpx; }.section-head { display:flex; align-items:flex-start; justify-content:space-between; gap:20rpx; padding-bottom:12rpx; }
.loading-card { display:flex; align-items:center; justify-content:center; gap:18rpx; min-height:240rpx; margin-top:24rpx; color:#687670; font-size:26rpx; }
.loading-ring { width:34rpx; height:34rpx; border:5rpx solid #dbece8; border-top-color:#168d7f; border-radius:50%; animation:spin .8s linear infinite; }
.error-card { flex-direction:column; }.error-card button { min-width:180rpx; margin:12rpx 0 0; color:#167f72; background:#e8f6f3; font-size:26rpx; }
.section-title { font-size:32rpx; font-weight:700; }.section-tip { margin-top:8rpx; color:#89948f; font-size:22rpx; }
.edit-btn { flex:none; min-height:72rpx; margin:0; padding:0 24rpx; color:#147e71; background:#e8f6f3; font-size:26rpx; line-height:72rpx; border-radius:36rpx; }
.field { padding-top:26rpx; }.label { margin-bottom:12rpx; color:#45534e; font-size:26rpx; font-weight:600; }.required { color:#d84c4c; }
.field input,.field textarea { box-sizing:border-box; width:100%; min-height:86rpx; padding:20rpx 22rpx; color:#263631; background:#f7f9f8; border:2rpx solid #e4e9e7; border-radius:14rpx; font-size:28rpx; }
.field textarea { height:180rpx; line-height:1.55; }.field input[disabled],.field textarea[disabled] { color:#58645f; background:#f1f3f2; opacity:1; }
.field.invalid input,.field.invalid textarea,.field.invalid .upload-btn { border-color:#d95858; background:#fff8f7; }.field-error { display:block; margin-top:8rpx; color:#c84242; font-size:22rpx; line-height:1.4; }
.certificate-preview { display:flex; align-items:center; gap:20rpx; margin-bottom:16rpx; color:#167f72; font-size:26rpx; }.certificate-preview image { width:150rpx; height:110rpx; border-radius:12rpx; background:#eef2f0; }
.upload-btn { min-height:84rpx; margin:0; color:#167f72; background:#eff9f7; border:2rpx dashed #6cb8ae; border-radius:14rpx; font-size:28rpx; line-height:80rpx; }
.help-text { margin-top:12rpx; color:#8a9691; font-size:22rpx; line-height:1.5; }
.actions { position:fixed; z-index:20; left:50%; bottom:0; display:flex; box-sizing:border-box; width:100%; max-width:860px; gap:20rpx; padding:24rpx 28rpx calc(18rpx + env(safe-area-inset-bottom)); transform:translateX(-50%); background:rgba(245,248,247,.96); box-shadow:0 -8rpx 24rpx rgba(32,83,75,.06); }.actions button { flex:1; min-height:88rpx; margin:0; border-radius:16rpx; font-size:28rpx; line-height:88rpx; }
.secondary-btn { color:#167f72; background:#e7f5f2; }.primary-btn { color:#fff; background:#168d7f; }.actions button[disabled],.upload-btn[disabled] { opacity:.55; }
.safe-space { height:calc(32rpx + env(safe-area-inset-bottom)); }.safe-space.with-actions { height:calc(162rpx + env(safe-area-inset-bottom)); }
@media (min-width:800px) { .content { max-width:860px; margin-left:auto; margin-right:auto; } }
@keyframes spin { to { transform:rotate(360deg); } }
@media (prefers-reduced-motion:reduce) { .loading-ring { animation:none; } }
</style>
