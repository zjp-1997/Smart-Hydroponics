<template>
	<view class="profile-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui back" hover-class="control-pressed" @tap="goBack"></text>
				<text class="nav-title">个人信息</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="profile-content" scroll-y>
			<view class="avatar-card">
				<view class="avatar-wrap" hover-class="control-pressed" aria-label="修改头像" @tap="chooseAvatar">
					<protected-image class="avatar" :src="avatarPreview" mode="aspectFill" @error="useDefaultAvatar"></protected-image>
					<view class="camera-badge"><text class="iconfont icon-paizhao"></text></view>
				</view>
				<text class="role-tag">{{ roleLabel }}</text>
				<text class="avatar-tip">点击头像更换照片</text>
			</view>

			<view class="form-card">
				<!-- 每个输入项直接对应 user 表中的一个可编辑字段。 -->
				<label class="form-row">
					<text class="field-label">用户名</text>
					<input v-model="form.username" class="field-input" maxlength="50" placeholder="请输入用户名" />
				</label>
				<label class="form-row">
					<text class="field-label">手机号</text>
					<input v-model="form.phone" class="field-input" type="number" maxlength="11" placeholder="请输入手机号" />
				</label>
				<label class="form-row">
					<text class="field-label">昵称</text>
					<input v-model="form.nickname" class="field-input" maxlength="50" placeholder="请输入昵称" />
				</label>
				<view class="form-row">
					<text class="field-label">性别</text>
					<view class="gender-options">
						<view v-for="item in genderOptions" :key="item.value" class="gender-option"
							:class="{ active: form.gender === item.value }" hover-class="control-pressed" @tap="form.gender = item.value">
							{{ item.label }}
						</view>
					</view>
				</view>
				<label class="form-row">
					<text class="field-label">邮箱</text>
					<input v-model="form.email" class="field-input" maxlength="100" placeholder="请输入邮箱" />
				</label>
				<label class="signature-row">
					<view class="signature-title"><text class="field-label">个性签名</text><text class="counter">{{ form.remark.length }}/255</text></view>
					<textarea v-model="form.remark" class="signature-input" maxlength="255" placeholder="介绍一下自己吧" />
				</label>
			</view>

			<button class="save-button" :disabled="submitting" hover-class="save-pressed" @tap="saveProfile">
				{{ submitting ? '保存中...' : '保存修改' }}
			</button>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getCurrentClientUser, updateCurrentClientProfile, uploadCurrentClientAvatar } from '@/api/clientAuth.js'
import { getClientHome, getToken, setUserInfo } from '@/utils/auth.js'
import { resolveFileUrl } from '@/utils/request.js'

const DEFAULT_AVATAR = '/static/avatar.png'

export default {
	data() {
		return {
			form: { username: '', phone: '', nickname: '', gender: 0, email: '', avatar: '', remark: '' },
			avatarPreview: DEFAULT_AVATAR,
			pendingAvatarPath: '',
			submitting: false,
			roleLabel: '用户',
			genderOptions: [
				{ label: '保密', value: 0 },
				{ label: '男', value: 1 },
				{ label: '女', value: 2 }
			]
		}
	},
	async onLoad() {
		if (!getToken()) {
			uni.reLaunch({ url: '/pages/login/pwd_index' })
			return
		}
		try {
			const user = await getCurrentClientUser()
			const role = String(user.roleCode || '').toLowerCase()
			if (!['farm_owner', 'user', 'technician', 'expert'].includes(role)) {
				uni.reLaunch({ url: getClientHome(role) })
				return
			}
			// 共用一套表单，通过身份标签说明当前正在编辑哪类移动端账号。
			this.roleLabel = { farm_owner: '农场主', user: '普通用户', technician: '技术人员', expert: '专家' }[role]
			this.fillForm(user)
		} catch { /* 登录失效由统一请求层处理。 */ }
	},
	methods: {
		fillForm(user) {
			// U 开头的注册占位手机号不展示给用户，留空保存时后端会继续保留原值。
			const phone = /^1[3-9]\d{9}$/.test(user.phone || '') ? user.phone : ''
			this.form = {
				username: user.username || '',
				phone,
				nickname: user.nickname || '',
				gender: [0, 1, 2].includes(user.gender) ? user.gender : 0,
				email: user.email || '',
				avatar: user.avatar || '',
				remark: user.remark || ''
			}
			this.avatarPreview = user.avatar ? resolveFileUrl(user.avatar) : DEFAULT_AVATAR
		},
		goBack() {
			uni.navigateBack()
		},
		useDefaultAvatar() {
			if (!this.pendingAvatarPath) this.avatarPreview = DEFAULT_AVATAR
		},
		chooseAvatar() {
			if (this.submitting) return
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (result) => {
					const file = result.tempFiles && result.tempFiles[0]
					if (file && file.size > 5 * 1024 * 1024) {
						uni.showToast({ title: '头像大小不能超过5MB', icon: 'none' })
						return
					}
					this.pendingAvatarPath = result.tempFilePaths && result.tempFilePaths[0] || ''
					if (this.pendingAvatarPath) this.avatarPreview = this.pendingAvatarPath
				}
			})
		},
		validateForm() {
			if (!this.form.username.trim()) return '请输入用户名'
			if (this.form.phone && !/^1[3-9]\d{9}$/.test(this.form.phone)) return '请输入正确的11位手机号'
			if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email.trim())) return '请输入正确的邮箱地址'
			return ''
		},
		async saveProfile() {
			if (this.submitting) return
			const errorMessage = this.validateForm()
			if (errorMessage) {
				uni.showToast({ title: errorMessage, icon: 'none' })
				return
			}
			this.submitting = true
			try {
				// 新头像先上传取得后端地址，再与其他个人信息一次性保存。
				if (this.pendingAvatarPath) {
					const uploaded = await uploadCurrentClientAvatar(this.pendingAvatarPath)
					this.form.avatar = uploaded.url
				}
				const user = await updateCurrentClientProfile({
					...this.form,
					username: this.form.username.trim(),
					phone: this.form.phone.trim(),
					nickname: this.form.nickname.trim(),
					email: this.form.email.trim(),
					remark: this.form.remark.trim()
				})
				setUserInfo(user)
				this.pendingAvatarPath = ''
				this.fillForm(user)
				uni.showToast({ title: '个人信息已更新', icon: 'success' })
				setTimeout(() => uni.navigateBack(), 500)
			} finally {
				this.submitting = false
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page { height: 100%; background-color: #f7f7f7; }
.profile-page { position: relative; height: 100vh; overflow: hidden; background: #f7f7f7; color: #26302f; }
.page-hero { box-sizing: border-box; min-height: 270rpx; padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0; background: linear-gradient(180deg, rgba(27,162,145,.7), rgba(90,184,173,0)); }
.navbar { position: relative; display: flex; align-items: center; justify-content: space-between; height: 58rpx; }
.nav-title { position: absolute; left: 90rpx; right: 90rpx; text-align: center; color: #fff; font-size: 16px; line-height: 58rpx; }
.back, .nav-placeholder { position: relative; z-index: 2; width: 44px; height: 44px; line-height: 44px; }
.back { margin-left: -12rpx; color: #fff; font-size: 18px; }
.profile-content { position: absolute; top: calc(var(--status-bar-height) + 104rpx); right: 0; bottom: 0; left: 0; box-sizing: border-box; padding: 0 36rpx; }
.avatar-card { display: flex; flex-direction: column; align-items: center; padding: 28rpx 0 30rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(38,91,84,.05); }
.avatar-wrap { position: relative; width: 132rpx; height: 132rpx; }
.avatar { display: block; width: 132rpx; height: 132rpx; border: 6rpx solid #eef8f6; border-radius: 50%; background: #fff; box-sizing: border-box; }
.camera-badge { position: absolute; right: -2rpx; bottom: 2rpx; display: flex; align-items: center; justify-content: center; width: 42rpx; height: 42rpx; border: 4rpx solid #fff; border-radius: 50%; background: #5ab8ad; color: #fff; font-size: 22rpx; }
.role-tag { margin-top: 14rpx; padding: 6rpx 18rpx; border-radius: 18rpx; background: #e7f7f3; color: #168577; font-size: 12px; }
.avatar-tip { margin-top: 10rpx; color: #89938f; font-size: 12px; }
.form-card { margin-top: 24rpx; overflow: hidden; padding: 0 28rpx; border-radius: 22rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(38,91,84,.04); }
.form-row { display: flex; align-items: center; min-height: 96rpx; border-bottom: 1rpx solid #edf1f0; }
.field-label { flex-shrink: 0; width: 150rpx; color: #45514f; font-size: 15px; }
.field-input { flex: 1; height: 96rpx; color: #26302f; font-size: 15px; text-align: right; }
.gender-options { display: flex; flex: 1; justify-content: flex-end; gap: 12rpx; }
.gender-option { min-width: 84rpx; height: 52rpx; border-radius: 26rpx; background: #f3f6f5; color: #7b8784; font-size: 13px; line-height: 52rpx; text-align: center; }
.gender-option.active { background: #e1f4f1; color: #168577; font-weight: 500; }
.signature-row { display: block; padding: 26rpx 0 24rpx; }
.signature-title { display: flex; align-items: center; justify-content: space-between; }
.counter { color: #aab3b1; font-size: 12px; }
.signature-input { box-sizing: border-box; width: 100%; height: 150rpx; margin-top: 18rpx; padding: 18rpx; border-radius: 12rpx; background: #f7f9f8; color: #26302f; font-size: 14px; line-height: 1.55; }
.save-button { height: 88rpx; margin: 34rpx 0 0; border: 0; border-radius: 44rpx; background: #5ab8ad; color: #fff; font-size: 16px; line-height: 88rpx; }
.save-button::after { border: 0; }
.save-button[disabled] { opacity: .62; }
.control-pressed, .save-pressed { opacity: .7; }
.safe-bottom { height: calc(40rpx + env(safe-area-inset-bottom)); }
@media screen and (min-width: 768px) { .profile-page { width: 750rpx; margin: 0 auto; } }
</style>
