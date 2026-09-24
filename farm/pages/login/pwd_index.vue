<template>
	<view class="pwd-login-page">
		<view class="login-panel">
			<image class="brand-logo" src="/static/logo.jpg" mode="aspectFill"></image>

			<view class="title">账号密码登录</view>
			<view class="subtitle">输入您的账号和密码</view>

			<component :is="'form'" class="form" @submit.prevent="handleLogin">
				<label class="field-label" for="farm-login-account">用户名</label>
				<view class="input-shell">
					<input
						id="farm-login-account"
						class="input-inner"
						v-model="account"
						placeholder="请输入用户名"
						placeholder-style="color: #777777;"
						maxlength="30"
						name="farm-login-account"
						autocomplete="username"
						spellcheck="false"
					/>
				</view>
				<label class="field-label password-label" for="farm-login-password">密码</label>
				<view class="input-shell">
					<input
						id="farm-login-password"
						class="input-inner password-inner"
						v-model="password"
						placeholder="请输入密码"
						placeholder-style="color: #777777;"
						:password="!passwordVisible"
						maxlength="32"
						name="farm-login-password"
						autocomplete="current-password"
						spellcheck="false"
					/>
					<button
						class="iconfont password-eye"
						role="button"
						:aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
						:class="passwordVisible ? 'icon-xianshi' : 'icon-yincang'"
						@tap="togglePasswordVisible"
					></button>
				</view>

				<button class="login-button" role="button" :disabled="isSubmitting" @tap="handleLogin">
					{{ isSubmitting ? '登录中' : '登录' }}
				</button>
			</component>

			<button class="text-button code-link" role="button" @tap="goCodeLogin">验证码登录</button>

			<view class="register-row">
				<text class="register-text">没有账户？</text>
				<button class="text-button register-link" role="button" @tap="goRegister">注册</button>
			</view>
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
import { getCurrentClientUser, loginByPassword } from '@/api/clientAuth.js'
import { getClientHome, getToken, saveAuth, setUserInfo } from '@/utils/auth.js'

export default {
	async onShow() {
		// 冷启动复用有效登录态，并以服务端当前角色恢复对应工作台。
		if (!getToken()) return
		try {
			const user = await getCurrentClientUser()
			setUserInfo(user)
			uni.reLaunch({ url: getClientHome(user.roleCode) })
		} catch { /* 失效令牌由统一请求层清理，登录表单仍可继续使用。 */ }
	},
	data() {
		return {
			account: '',
			password: '',
			passwordVisible: false,
			agreed: false,
			isSubmitting: false
		}
	},
	methods: {
		async handleLogin() {
			// 前端先做轻量校验，减少无效网络请求；后端仍会做最终安全校验。
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

			if (!this.agreed) {
				uni.showToast({
					title: '请先阅读并同意协议',
					icon: 'none'
				})
				return
			}

			// 防重复提交，避免用户连续点击造成多次登录请求和登录态覆盖。
			if (this.isSubmitting) {
				return
			}

			this.isSubmitting = true

			try {
				// 调用 api 层，不在页面中直接拼接口地址，方便后续统一更换网关或版本号。
				const authData = await loginByPassword({
					username: this.account.trim(),
					password: this.password
				})
				// 登录成功后统一保存 token、refreshToken 和用户信息，供请求守卫自动鉴权。
				saveAuth(authData)
				// uni.showToast({
				// 	title: '登录成功',
				// 	icon: 'success'
				// })
				// 使用 reLaunch 清空登录页栈，避免登录成功后返回到登录页面。
				setTimeout(() => {
					uni.reLaunch({
						url: getClientHome(authData.roleCode || (authData.user && authData.user.roleCode))
					})
				}, 600)
			} catch (error) {
				// 请求封装已经统一 toast，这里只保留日志，便于真机调试定位。
				console.error('Password login failed:', error)
			} finally {
				this.isSubmitting = false
			}
		},
		handleAgreementChange(event) {
			this.agreed = (event.detail.value || []).includes('accepted')
		},
		togglePasswordVisible() {
			// 仅切换当前输入框显示状态，不改变已输入密码值。
			this.passwordVisible = !this.passwordVisible
		},
		goCodeLogin() {
			uni.navigateTo({
				url: '/pages/login/code_index'
			})
		},
		goRegister() {
			uni.navigateTo({
				url: '/pages/register/index'
			})
		},
		openAgreement(type) {
			uni.navigateTo({ url: `/pages/login/agreement?type=${type}` })
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #ffffff;
}

.pwd-login-page {
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

.input-shell {
	box-sizing: border-box;
	position: relative;
	display: flex;
	align-items: center;
	width: 100%;
	height: 88rpx;
	padding: 0 20rpx;
	border-radius: 8rpx;
	background-color: #efefef !important;
	overflow: hidden;
}

.input-inner {
	position: relative;
	z-index: 2;
	flex: 1;
	width: 100%;
	height: 88rpx;
	min-width: 0;
	padding: 0;
	border: 0;
	outline: none;
	background-color: transparent !important;
	font-size: 28rpx;
	line-height: 88rpx;
	color: #000000;
	-webkit-appearance: none;
	appearance: none;
}

.field-label { display: block; margin-bottom: 12rpx; font-size: 26rpx; line-height: 1.4; color: #333333; }
.password-label { margin-top: 28rpx; }

.input-inner:focus,
.input-inner:active,
.input-shell input,
.input-shell input:focus,
.input-shell input:active,
.input-shell uni-input,
.input-shell .uni-input,
.input-shell .uni-input-wrapper,
.input-shell .uni-input-input {
	outline: none;
	background-color: transparent !important;
}

.password-inner {
	padding-right: 16rpx;
}

.password-eye {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 88rpx;
	height: 88rpx;
	margin: 0;
	padding: 0;
	border-radius: 0;
	background: transparent;
	font-size: 36rpx;
	line-height: 88rpx;
	color: #8c8c8c;
}
.password-eye::before { font-family: "iconfont"; }
.password-eye::after, .text-button::after { border: 0; }

/* H5 端浏览器自动填充或聚焦时可能给 input 加浅蓝底，这里统一覆盖为设计稿的灰色输入框。 */
.input-inner:-webkit-autofill,
.input-inner:-webkit-autofill:hover,
.input-inner:-webkit-autofill:focus,
.input-inner:-webkit-autofill:active,
.input-shell input:-webkit-autofill,
.input-shell input:-webkit-autofill:hover,
.input-shell input:-webkit-autofill:focus,
.input-shell input:-webkit-autofill:active {
	-webkit-box-shadow: 0 0 0 1000px #efefef inset !important;
	-webkit-text-fill-color: #000000 !important;
	transition: background-color 9999s ease-in-out 0s;
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

.login-button::after {
	border: none;
}

.login-button[disabled] {
	background-color: #1ba291;
	color: #ffffff;
	opacity: 0.72;
}

.code-link {
	margin-top: 20rpx;
	text-align: right;
	font-size: 28rpx;
	font-weight: 600;
	line-height: 1.5;
	color: #1ba291;
}

.text-button { min-height: 72rpx; margin: 0; padding: 0 12rpx; border: 0; background: transparent; font-size: inherit; line-height: 72rpx; }

.register-row {
	display: flex;
	align-items: center;
	justify-content: center;
	margin-top: 44rpx;
	font-size: 28rpx;
	line-height: 1.5;
	color: #000000;
}

.register-text {
	color: #000000;
}

.register-link {
	margin-left: 8rpx;
	font-weight: 600;
	color: #1ba291;
}

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
	.pwd-login-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
