<template>
	<view class="code-login-page">
		<view class="login-panel">
			<image class="brand-logo" src="/static/logo.jpg" mode="aspectFill"></image>

			<view class="title">验证码登录</view>
			<view class="subtitle">输入您的手机号以接收6位验证码</view>

			<view class="form">
				<label class="field-label" for="sms-login-phone">手机号</label>
				<input
					id="sms-login-phone"
					class="form-input"
					name="sms-login-phone"
					type="number"
					v-model="phone"
					maxlength="11"
					placeholder="请输入手机号"
					placeholder-style="color: #777777;"
				/>

				<view class="code-row">
					<view class="code-field">
						<label class="field-label" for="sms-login-code">验证码</label>
						<input
							id="sms-login-code"
							class="form-input code-input"
							name="sms-login-code"
							type="number"
							v-model="verifyCode"
							maxlength="6"
							placeholder="请输入验证码"
							placeholder-style="color: #777777;"
						/>
					</view>
					<button class="code-button" role="button" :disabled="isCounting || sendingCode" @tap="handleSendCode">
						{{ codeButtonText }}
					</button>
				</view>

				<button class="login-button" role="button" :disabled="isSubmitting" @tap="handleLogin">
					{{ isSubmitting ? '登录中' : '登录' }}
				</button>
			</view>

			<button class="text-button password-link" role="button" @tap="goPasswordLogin">账号密码登录</button>
		</view>

		<view class="agreement">
			<checkbox-group class="checkbox-hit" role="checkbox" aria-label="同意用户协议和隐私政策"
				:aria-checked="String(agreed)" @change="handleAgreementChange">
				<label class="checkbox-label">
					<checkbox class="checkbox-control" value="accepted" :checked="agreed" color="#1ba291" />
				</label>
			</checkbox-group>
			<text class="agreement-prefix">已阅读并同意</text>
			<button class="text-button agreement-link" role="button" aria-label="查看用户协议" @tap="openAgreement('user')">《用户协议》</button>
			<button class="text-button agreement-link" role="button" aria-label="查看隐私政策" @tap="openAgreement('privacy')">《隐私政策》</button>
		</view>
	</view>
</template>

<script>
import { getCurrentClientUser, loginBySms, sendLoginSmsCode } from '@/api/clientAuth.js'
import { getClientHome, getToken, saveAuth, setUserInfo } from '@/utils/auth.js'

export default {
	async onShow() {
		// code_index 是应用首页；冷启动时先向服务端确认令牌和最新角色。
		if (!getToken()) return
		try {
			const user = await getCurrentClientUser()
			setUserInfo(user)
			uni.reLaunch({ url: getClientHome(user.roleCode) })
		} catch { /* 失效令牌由请求层处理，保留验证码登录表单。 */ }
	},
	data() {
		return {
			phone: '',
			verifyCode: '',
			agreed: false,
			isCounting: false,
			sendingCode: false,
			isSubmitting: false,
			countdown: 60,
			timer: null
		}
	},
	computed: {
		codeButtonText() {
			// 保持原登录页按钮文案，仅在发码成功后展示原有倒计时。
			return this.isCounting ? `${this.countdown}秒` : '获取验证码'
		}
	},
	onUnload() {
		this.clearTimer()
	},
	beforeUnmount() {
		this.clearTimer()
	},
	methods: {
		isValidPhone() {
			return /^1[3-9]\d{9}$/.test(this.phone)
		},
		async handleSendCode() {
			if (this.isCounting || this.sendingCode) {
				return
			}

			if (!this.isValidPhone()) {
				uni.showToast({
					title: '请输入正确手机号',
					icon: 'none'
				})
				return
			}

			this.sendingCode = true
			try {
				const result = await sendLoginSmsCode(this.phone)
				// 模拟接口会返回验证码；真实短信接入后应改为“验证码已发送”。
				console.log("验证码：" + result.code)
				uni.showToast({ title: `验证码：${result.code}`, icon: 'none', duration: 3000 })
				this.isCounting = true
				this.countdown = 60
				this.timer = setInterval(() => {
					if (this.countdown <= 1) {
						this.clearTimer()
						return
					}
					this.countdown -= 1
				}, 1000)
			} catch (error) {
				console.error('Send login code failed:', error)
			} finally {
				this.sendingCode = false
			}
		},
		async handleLogin() {
			if (!this.isValidPhone()) {
				uni.showToast({
					title: '请输入正确手机号',
					icon: 'none'
				})
				return
			}

			if (!/^\d{6}$/.test(this.verifyCode)) {
				uni.showToast({
					title: '请输入6位验证码',
					icon: 'none'
				})
				return
			}

			if (!this.agreed) {
				uni.showToast({
					title: '请先阅读并同意协议',
					icon: 'none'
				})
				return
			}

			if (this.isSubmitting) return
			this.isSubmitting = true
			try {
				// 验证码由后端校验并一次性消费，前端不保存也不自行比对正确性。
				
				const authData = await loginBySms({
					phone: this.phone,
					code: this.verifyCode
				})
				saveAuth(authData)
				// 验证码登录与密码登录共用角色分流，专家直接进入咨询消息页。
				uni.reLaunch({ url: getClientHome(authData.roleCode || (authData.user && authData.user.roleCode)) })
			} catch (error) {
				console.error('SMS login failed:', error)
			} finally {
				this.isSubmitting = false
			}
		},
		handleAgreementChange(event) {
			this.agreed = (event.detail.value || []).includes('accepted')
		},
		goPasswordLogin() {
			uni.navigateTo({
				url: '/pages/login/pwd_index'
			})
		},
		openAgreement(type) {
			uni.navigateTo({ url: `/pages/login/agreement?type=${type}` })
		},
		clearTimer() {
			if (this.timer) {
				clearInterval(this.timer)
				this.timer = null
			}
			this.isCounting = false
			this.countdown = 60
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #ffffff;
}

.code-login-page {
	position: relative;
	box-sizing: border-box;
	min-height: 100vh;
	padding: 150rpx 56rpx calc(44rpx + env(safe-area-inset-bottom));
	background-color: #ffffff;
	color: #000000;
}

.login-panel {
	width: 100%;
}

.brand-logo {
	display: block;
	width: 150rpx;
	height: 150rpx;
	margin: 0 auto 76rpx;
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
	margin-top: 22rpx;
	font-size: 26rpx;
	font-weight: 600;
	line-height: 1.4;
	color: #000000;
}

.form {
	margin-top: 42rpx;
}
.field-label { display: block; margin-bottom: 12rpx; font-size: 26rpx; line-height: 1.4; color: #333333; }

.form-input {
	box-sizing: border-box;
	width: 100%;
	height: 88rpx;
	padding: 0 20rpx;
	border-radius: 8rpx;
	background-color: #efefef;
	font-size: 28rpx;
	line-height: 88rpx;
	color: #000000;
}

.code-row {
	display: flex;
	align-items: flex-end;
	width: 100%;
	margin-top: 44rpx;
}
.code-field { flex: 1; min-width: 0; }

.code-input {
	flex: 1;
	min-width: 0;
}

.code-button {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	width: 214rpx;
	height: 88rpx;
	margin: 0 0 0 48rpx;
	padding: 0;
	border-radius: 8rpx;
	background-color: #1ba291;
	font-size: 32rpx;
	font-weight: 600;
	line-height: 88rpx;
	color: #ffffff;
}

.code-button[disabled] {
	background-color: #1ba291;
	color: #ffffff;
	opacity: 0.78;
}

.login-button {
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

.code-button::after,
.login-button::after {
	border: none;
}

.password-link {
	margin-top: 20rpx;
	text-align: right;
	font-size: 28rpx;
	font-weight: 600;
	line-height: 1.5;
	color: #1ba291;
}

.text-button { min-height: 72rpx; margin: 0; padding: 0 12rpx; border: 0; background: transparent; font-size: inherit; line-height: 72rpx; }
.text-button::after { border: 0; }

.agreement {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-wrap: wrap;
	margin-top: 40rpx;
	min-height: 88rpx;
	font-size: 24rpx;
	line-height: 1.4;
	color: #000000;
}

.checkbox-hit,
.checkbox-label {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 72rpx;
	height: 72rpx;
}
.checkbox-hit { flex-shrink: 0; margin-right: 2rpx; }
.checkbox-control { transform: scale(.82); }

.agreement-prefix {
	color: #555555;
}

.agreement-link {
	min-height: 72rpx;
	padding: 0 4rpx;
	line-height: 72rpx;
	color: #000000;
}

@media screen and (min-width: 768px) {
	.code-login-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
