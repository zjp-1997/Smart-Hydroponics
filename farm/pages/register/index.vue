<template>
	<view class="register-page">
		<view class="register-panel">
			<image class="brand-logo" src="/static/logo.jpg" mode="aspectFill"></image>

			<view class="title">注册</view>
			<view class="subtitle">选择身份并填写账号信息</view>

			<view class="form">
				<view class="field-label">注册身份 <text class="required">*</text></view>
				<view class="role-options">
					<button v-for="role in roles" :key="role.code" class="role-option"
						:class="{ selected: roleCode === role.code }"
						:aria-label="role.name" :aria-pressed="roleCode === role.code"
						@tap="selectRole(role.code)">{{ role.name }}</button>
				</view>
				<!-- 专家没有农场主归属，切换到专家时即隐藏并清空农场主选择。 -->
				<view v-if="needsFarmOwner" class="owner-field">
					<view class="field-label">申请加入的农场主 <text class="required">*</text></view>
					<picker v-if="farmOwners.length" mode="selector" :range="farmOwners" range-key="name"
						:value="selectedOwnerIndex < 0 ? 0 : selectedOwnerIndex" @change="selectFarmOwner">
						<view class="form-input picker-input" :class="{ placeholder: selectedOwnerIndex < 0 }">
							{{ selectedOwnerIndex < 0 ? '请选择申请加入的农场主' : farmOwners[selectedOwnerIndex].name }}
							<text class="picker-arrow">⌄</text>
						</view>
					</picker>
					<view v-else class="owner-hint" @tap="loadFarmOwners">{{ isLoadingOwners ? '正在加载农场主...' : '暂无可申请的农场主，点击重试' }}</view>
					<view class="owner-hint">提交后需农场主或管理员审核，通过后才可登录。</view>
				</view>
				<view class="field-label account-label">账号信息</view>
				<input
					class="form-input"
					name="register-account"
					v-model="account"
					maxlength="30"
					placeholder="请输入用户名"
					placeholder-style="color: #C9C9C9;"
				/>
				<input
					class="form-input"
					name="register-password"
					v-model="password"
					:password="true"
					maxlength="32"
					placeholder="请输入密码"
					placeholder-style="color: #C9C9C9;"
				/>
				<input
					class="form-input"
					name="register-confirm-password"
					v-model="confirmPassword"
					:password="true"
					maxlength="32"
					placeholder="请确认密码"
					placeholder-style="color: #C9C9C9;"
				/>

				<button class="register-button" :disabled="isSubmitting" @tap="handleRegister">
					{{ isSubmitting ? '注册中...' : '注册' }}
				</button>
			</view>
		</view>

		<view class="login-entry">
			<text class="login-text">已有账号，去</text>
			<text class="login-link" @tap="goLogin">登录</text>
		</view>
	</view>
</template>

<script>
import { getRegisterFarmOwners, registerAccount } from '@/api/clientAuth.js'

export default {
	data() {
		return {
			roles: [
				{ code: 'user', name: '普通用户' },
				{ code: 'technician', name: '技术人员' },
				{ code: 'expert', name: '专家' }
			],
			roleCode: '',
			farmOwners: [],
			selectedOwnerIndex: -1,
			isLoadingOwners: false,
			account: '',
			password: '',
			confirmPassword: '',
			isSubmitting: false
		}
	},
	computed: {
		needsFarmOwner() {
			return this.roleCode === 'user' || this.roleCode === 'technician'
		}
	},
	methods: {
		async loadFarmOwners() {
			if (this.isLoadingOwners) return
			this.isLoadingOwners = true
			try {
				const owners = await getRegisterFarmOwners()
				this.farmOwners = Array.isArray(owners) ? owners : []
			} catch (error) {
				// 请求层会显示网络或服务端错误，注册页保持可重试状态。
				this.farmOwners = []
			} finally {
				this.isLoadingOwners = false
			}
		},
		selectRole(roleCode) {
			this.roleCode = roleCode
			// 切换身份时清空旧归属，避免把普通用户的农场主提交给专家。
			this.selectedOwnerIndex = -1
			if (this.needsFarmOwner && !this.farmOwners.length) this.loadFarmOwners()
		},
		selectFarmOwner(event) {
			this.selectedOwnerIndex = Number(event.detail.value)
		},
		async handleRegister() {
			// 注册页做基础输入校验，提升交互体验；后端负责唯一性和安全规则兜底。
			if (!this.roleCode) {
				uni.showToast({ title: '请选择注册身份', icon: 'none' })
				return
			}
			if (this.needsFarmOwner && this.selectedOwnerIndex < 0) {
				uni.showToast({ title: '请选择申请加入的农场主', icon: 'none' })
				return
			}
			if (!this.account.trim()) {
				uni.showToast({
					title: '请输入账号',
					icon: 'none'
				})
				return
			}

			if (!this.password) {
				uni.showToast({
					title: '请输入密码',
					icon: 'none'
				})
				return
			}

			if (!/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(this.password)) {
				uni.showToast({
					title: '密码需为8-32位，且包含字母和数字',
					icon: 'none'
				})
				return
			}

			if (this.password !== this.confirmPassword) {
				uni.showToast({
					title: '两次密码不一致',
					icon: 'none'
				})
				return
			}

			// 防止连续点击注册按钮导致重复请求和重复账号写入尝试。
			if (this.isSubmitting) {
				return
			}

			this.isSubmitting = true

			try {
				// 当前 UI 没有昵称输入框，因此默认使用账号作为 nickname，后端也允许不传。
				await registerAccount({
					username: this.account.trim(),
					password: this.password,
					confirmPassword: this.confirmPassword,
					nickname: this.account.trim(),
					roleCode: this.roleCode,
					farmOwnerId: this.needsFarmOwner ? this.farmOwners[this.selectedOwnerIndex].id : null
				})
                this.password = ''
                this.confirmPassword = ''
                uni.showModal({
                    title: this.needsFarmOwner ? '入场申请已提交' : '注册成功',
                    content: this.needsFarmOwner
                        ? '请等待所选农场主或管理员审核。审核通过后才可登录并访问农场。'
                        : '请使用刚创建的账号登录，并完善专家认证资料。',
                    showCancel: false,
                    success: () => uni.redirectTo({ url: '/pages/login/pwd_index' })
                })
			} catch (error) {
				// 统一错误提示由 request 后置守卫负责，这里只记录调试信息。
				console.error('Account register failed:', error)
			} finally {
				this.isSubmitting = false
			}
		},
		goLogin() {
			uni.navigateTo({
				url: '/pages/login/pwd_index'
			})
		}
	}
}
</script>

<style>
page {
	background-color: #ffffff;
}

.register-page {
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
	min-height: 100vh;
	padding: 80rpx 58rpx calc(60rpx + env(safe-area-inset-bottom));
	background-color: #ffffff;
	color: #000000;
}

.register-panel {
	width: 100%;
}

.brand-logo {
	display: block;
	width: 150rpx;
	height: 150rpx;
	margin: 0 auto 44rpx;
	border-radius: 50%;
	background-color: #efefef;
}

.title {
	font-size: 44rpx;
	font-weight: 700;
	line-height: 1.25;
	color: #000000;
}

.subtitle {
	margin-top: 20rpx;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 1.4;
	color: #000000;
}

.form {
	margin-top: 44rpx;
}

.field-label {
	font-size: 26rpx;
	font-weight: 600;
	color: #333333;
}

.required {
	color: #c74747;
}

.role-options {
	display: flex;
	gap: 12rpx;
	margin-top: 18rpx;
}

.role-option {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 88rpx;
	margin: 0;
	padding: 0 8rpx;
	border: 2rpx solid #dedede;
	border-radius: 8rpx;
	background: #ffffff;
	color: #333333;
	font-size: 25rpx;
}

.role-option::after {
	border: none;
}

.role-option.selected {
	border-color: #1ba291;
	background: #eaf8f5;
	color: #137d70;
	font-weight: 600;
}

.owner-field, .account-label {
	margin-top: 36rpx;
}

.picker-input {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 18rpx;
}

.picker-input.placeholder, .owner-hint {
	color: #777777;
}

.picker-arrow {
	font-size: 34rpx;
}

.owner-hint {
	margin-top: 18rpx;
	font-size: 24rpx;
}

.form-input {
	box-sizing: border-box;
	width: 100%;
	height: 88rpx;
	margin-top: 44rpx;
	padding: 0 20rpx;
	border-radius: 8rpx;
	background-color: #efefef;
	font-size: 28rpx;
	line-height: 88rpx;
	color: #000000;
}

.register-button {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	width: 100%;
	height: 88rpx;
	margin-top: 44rpx;
	padding: 0;
	border-radius: 8rpx;
	background-color: #1ba291;
	font-size: 32rpx;
	font-weight: 600;
	line-height: 88rpx;
	color: #ffffff;
}

.register-button::after {
	border: none;
}

.register-button[disabled] {
	background-color: #1ba291;
	color: #ffffff;
	opacity: 0.72;
}

.login-entry {
	display: flex;
	align-items: center;
	justify-content: center;
	min-height: 44rpx;
	margin-top: auto;
	padding-top: 44rpx;
	font-size: 28rpx;
	line-height: 1.5;
	color: #000000;
}

.login-text {
	color: #000000;
}

.login-link {
	color: #1ba291;
}

@media screen and (min-width: 768px) {
	.register-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
