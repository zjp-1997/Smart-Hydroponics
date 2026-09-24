<template>
	<view class="account-page">
		<!-- 顶部渐变参数与农场管理页保持一致，确保二级页面视觉统一。 -->
		<view class="page-hero">
			<view class="navbar">
				<button class="back-button" hover-class="control-pressed" aria-label="返回" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon"></text>
				</button>
				<text class="nav-title">账号设置</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="account-content" scroll-y>
			<view class="setting-section">
				<text class="section-title">账号关联</text>
				<view class="setting-card">
					<button class="setting-row" hover-class="row-pressed" @tap="openPhoneDialog">
						<text class="iconfont icon-shouji setting-icon"></text>
						<text class="setting-label">手机号</text>
						<text class="setting-status">{{ phoneDisplay }}</text>
						<text v-if="isPhoneBound" class="setting-action">修改</text>
						<text class="iconfont icon-youjiantou setting-arrow"></text>
					</button>
					<view class="row-divider"></view>
					<button class="setting-row" hover-class="row-pressed" @tap="handleUnavailable('第三方账号绑定')">
						<text class="iconfont icon-disanfangzhanghaobangding setting-icon"></text>
						<text class="setting-label">第三方账号绑定</text>
						<text class="setting-status">未绑定</text>
						<text class="iconfont icon-youjiantou setting-arrow"></text>
					</button>
				</view>
			</view>

			<view class="setting-section security-section">
				<text class="section-title">安全管理</text>
				<view class="setting-card">
					<button class="setting-row" hover-class="row-pressed" @tap="openPasswordDialog">
						<text class="iconfont icon-suoding setting-icon"></text>
						<text class="setting-label">找回密码</text>
						<text class="iconfont icon-youjiantou setting-arrow"></text>
					</button>
					<view class="row-divider"></view>
					<button class="setting-row danger-row" hover-class="danger-pressed" @tap="handleUnavailable('注销账号')">
						<text class="iconfont icon-a-typeyonghustyledunpai setting-icon danger-color"></text>
						<text class="setting-label danger-color">注销账号</text>
						<text class="iconfont icon-youjiantou setting-arrow"></text>
					</button>
				</view>
			</view>

			<button
				class="logout-button"
				hover-class="logout-pressed"
				:disabled="loggingOut"
				@tap="handleLogout"
			>
				{{ loggingOut ? '正在退出...' : '退出登录' }}
			</button>
			<view class="safe-bottom"></view>
		</scroll-view>

		<!-- 仅手机号绑定使用的遮罩与弹框，不改变账号设置页其他模块的布局和视觉。 -->
		<view v-if="phoneDialogVisible" class="phone-dialog-mask" @tap="closePhoneDialog">
			<view class="phone-dialog" role="dialog" aria-modal="true" aria-label="绑定手机号" @tap.stop>
				<text class="phone-dialog-title">{{ phoneDialogTitle }}</text>

				<view class="phone-field">
					<text class="phone-field-label">{{ isPhoneBound ? '新手机号' : '手机号' }}</text>
					<input
						class="phone-field-input"
						name="phone-binding-phone"
						type="number"
						v-model.trim="phoneForm.phone"
						maxlength="11"
						placeholder="请输入11位手机号"
						placeholder-style="color: #a8b2af;"
					/>
				</view>

				<view class="phone-field">
					<text class="phone-field-label">验证码</text>
					<view class="phone-code-row">
						<input
							class="phone-field-input phone-code-input"
							name="phone-binding-code"
							type="number"
							v-model.trim="phoneForm.code"
							maxlength="6"
							placeholder="请输入6位验证码"
							placeholder-style="color: #a8b2af;"
						/>
						<button
							class="send-code-button"
							hover-class="dialog-control-pressed"
							:disabled="sendingCode || phoneCountdown > 0"
							@tap="handleSendBindCode"
						>
							{{ phoneCountdown > 0 ? `${phoneCountdown}s` : (sendingCode ? '发送中' : '获取验证码') }}
						</button>
					</view>
				</view>

				<view class="phone-dialog-actions">
					<button class="dialog-button cancel-button" hover-class="dialog-control-pressed" :disabled="bindingPhone" @tap="closePhoneDialog">取消</button>
					<button class="dialog-button confirm-button" hover-class="dialog-control-pressed" :disabled="bindingPhone" @tap="handleBindPhone">
						{{ bindingPhone ? (isPhoneBound ? '修改中' : '绑定中') : '确定' }}
					</button>
				</view>
			</view>
		</view>

		<!-- 找回密码弹框仅包含需求指定字段，并复用手机号弹框的视觉规范。 -->
		<view v-if="passwordDialogVisible" class="phone-dialog-mask" @tap="closePasswordDialog">
			<view class="phone-dialog password-dialog" role="dialog" aria-modal="true" aria-label="找回密码" @tap.stop>
				<text class="phone-dialog-title">找回密码</text>

				<view class="phone-field">
					<text class="phone-field-label">手机号</text>
					<input class="phone-field-input" name="password-reset-phone" type="number" v-model.trim="passwordForm.phone" maxlength="11"
						placeholder="请输入已绑定手机号" placeholder-style="color: #a8b2af;" />
				</view>

				<view class="phone-field">
					<text class="phone-field-label">验证码</text>
					<view class="phone-code-row">
						<input class="phone-field-input phone-code-input" name="password-reset-code" type="number" v-model.trim="passwordForm.code"
							maxlength="6" placeholder="请输入6位验证码" placeholder-style="color: #a8b2af;" />
						<button class="send-code-button" hover-class="dialog-control-pressed"
							:disabled="sendingPasswordCode || passwordCountdown > 0" @tap="handleSendPasswordCode">
							{{ passwordCountdown > 0 ? `${passwordCountdown}s` : (sendingPasswordCode ? '发送中' : '获取验证码') }}
						</button>
					</view>
				</view>

				<view class="phone-field">
					<text class="phone-field-label">新密码</text>
					<input class="phone-field-input" name="password-reset-new-password" v-model="passwordForm.newPassword" :password="true" maxlength="32"
						placeholder="8-32位，包含字母和数字" placeholder-style="color: #a8b2af;" />
				</view>

				<view class="phone-field">
					<text class="phone-field-label">确认密码</text>
					<input class="phone-field-input" name="password-reset-confirm-password" v-model="passwordForm.confirmPassword" :password="true" maxlength="32"
						placeholder="请再次输入新密码" placeholder-style="color: #a8b2af;" />
				</view>

				<view class="phone-dialog-actions">
					<button class="dialog-button cancel-button" hover-class="dialog-control-pressed" :disabled="resettingPassword" @tap="closePasswordDialog">取消</button>
					<button class="dialog-button confirm-button" hover-class="dialog-control-pressed" :disabled="resettingPassword" @tap="handleResetPassword">
						{{ resettingPassword ? '提交中' : '确定' }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import {
	bindPhone,
	changePhone,
	logout,
	resetPassword,
	sendBindPhoneSmsCode,
	sendChangePhoneSmsCode,
	sendPasswordResetCode
} from '@/api/clientAuth.js'
import { clearAuth, getUserInfo, setUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			loggingOut: false,
			boundPhone: '',
			phoneDialogVisible: false,
			phoneForm: {
				phone: '',
				code: ''
			},
			sendingCode: false,
			bindingPhone: false,
			phoneCountdown: 0,
			phoneCountdownTimer: null,
			passwordDialogVisible: false,
			passwordForm: {
				phone: '',
				code: '',
				newPassword: '',
				confirmPassword: ''
			},
			sendingPasswordCode: false,
			resettingPassword: false,
			passwordCountdown: 0,
			passwordCountdownTimer: null
		}
	},
	computed: {
		isPhoneBound() {
			return /^1[3-9]\d{9}$/.test(this.boundPhone)
		},
		phoneDisplay() {
			// 已绑定时展示真实手机号，未绑定时保留原页面状态文案。
			return this.isPhoneBound ? this.boundPhone : '未绑定'
		},
		phoneDialogTitle() {
			return this.isPhoneBound ? '修改手机号' : '绑定手机号'
		}
	},
	onLoad() {
		// 注册阶段可能使用 U 开头的内部占位手机号，只有真实手机号才显示为已绑定。
		const user = getUserInfo() || {}
		this.boundPhone = /^1[3-9]\d{9}$/.test(user.phone || '') ? user.phone : ''
	},
	onUnload() {
		this.clearPhoneCountdown()
		this.clearPasswordCountdown()
	},
	beforeUnmount() {
		this.clearPhoneCountdown()
		this.clearPasswordCountdown()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		handleUnavailable(feature) {
			// 当前需求仅搭建入口，未实现的账号操作给予明确反馈，避免点击无响应。
			uni.showToast({ title: `${feature}功能建设中`, icon: 'none' })
		},
		openPhoneDialog() {
			// 首次绑定与修改共用弹框，但验证码场景由后端严格隔离。
			this.phoneForm.phone = ''
			this.phoneForm.code = ''
			this.phoneDialogVisible = true
		},
		closePhoneDialog() {
			if (this.bindingPhone) return
			this.phoneDialogVisible = false
		},
		isValidPhone(phone = this.phoneForm.phone) {
			// 与后端保持一致，只接受 1 开头的 11 位中国大陆手机号。
			return /^1[3-9]\d{9}$/.test(phone)
		},
		async handleSendBindCode() {
			if (this.sendingCode || this.phoneCountdown > 0) return
			if (!this.isValidPhone()) {
				uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
				return
			}

			this.sendingCode = true
			try {
				const sendCode = this.isPhoneBound ? sendChangePhoneSmsCode : sendBindPhoneSmsCode
				const result = await sendCode(this.phoneForm.phone)
				// 模拟环境没有真实短信，直接显示后端生成的验证码以完成联调。
				console.log("验证码：" + result.code)
				uni.showToast({ title: `验证码：${result.code}`, icon: 'none', duration: 3000 })
				this.startPhoneCountdown()
			} catch (error) {
				console.error('Send bind phone code failed:', error)
			} finally {
				this.sendingCode = false
			}
		},
		async handleBindPhone() {
			if (!this.isValidPhone()) {
				uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
				return
			}
			if (!/^\d{6}$/.test(this.phoneForm.code)) {
				uni.showToast({ title: '请输入6位验证码', icon: 'none' })
				return
			}
			if (this.bindingPhone) return

			this.bindingPhone = true
			try {
				const wasPhoneBound = this.isPhoneBound
				const submitPhone = wasPhoneBound ? changePhone : bindPhone
				const user = await submitPhone(this.phoneForm)
				// 后端返回最新用户信息，同步本地缓存后页面状态立即更新。
				setUserInfo(user)
				this.boundPhone = user.phone
				this.phoneDialogVisible = false
				this.clearPhoneCountdown()
				uni.showToast({ title: wasPhoneBound ? '手机号修改成功' : '手机号绑定成功', icon: 'success' })
			} catch (error) {
				console.error('Bind phone failed:', error)
			} finally {
				this.bindingPhone = false
			}
		},
		startPhoneCountdown() {
			this.clearPhoneCountdown()
			this.phoneCountdown = 60
			this.phoneCountdownTimer = setInterval(() => {
				if (this.phoneCountdown <= 1) {
					this.clearPhoneCountdown()
					return
				}
				this.phoneCountdown -= 1
			}, 1000)
		},
		clearPhoneCountdown() {
			if (this.phoneCountdownTimer) {
				clearInterval(this.phoneCountdownTimer)
				this.phoneCountdownTimer = null
			}
			this.phoneCountdown = 0
		},
		openPasswordDialog() {
			if (!this.isPhoneBound) {
				uni.showToast({ title: '请先绑定手机号', icon: 'none' })
				return
			}
			// 自动带入当前绑定手机号，用户仍可核对但不能重置其他账号密码。
			this.passwordForm = {
				phone: this.boundPhone,
				code: '',
				newPassword: '',
				confirmPassword: ''
			}
			this.passwordDialogVisible = true
		},
		closePasswordDialog() {
			if (this.resettingPassword) return
			this.passwordDialogVisible = false
		},
		async handleSendPasswordCode() {
			if (this.sendingPasswordCode || this.passwordCountdown > 0) return
			if (!this.isValidPhone(this.passwordForm.phone)) {
				uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
				return
			}
			this.sendingPasswordCode = true
			try {
				const result = await sendPasswordResetCode(this.passwordForm.phone)
				// 模拟短信接口直接返回验证码，真实短信接入后只显示发送成功提示。
				console.log("验证码：" + result.code)
				uni.showToast({ title: `验证码：${result.code}`, icon: 'none', duration: 3000 })
				this.startPasswordCountdown()
			} catch (error) {
				console.error('Send password reset code failed:', error)
			} finally {
				this.sendingPasswordCode = false
			}
		},
		async handleResetPassword() {
			if (!this.isValidPhone(this.passwordForm.phone)) {
				uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
				return
			}
			if (!/^\d{6}$/.test(this.passwordForm.code)) {
				uni.showToast({ title: '请输入6位验证码', icon: 'none' })
				return
			}
			if (!/^(?=.*[A-Za-z])(?=.*\d).{8,32}$/.test(this.passwordForm.newPassword)) {
				uni.showToast({ title: '密码需为8-32位且包含字母和数字', icon: 'none' })
				return
			}
			if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
				uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
				return
			}
			if (this.resettingPassword) return

			this.resettingPassword = true
			try {
				await resetPassword(this.passwordForm)
				// 服务端已撤销旧会话，本地同步退出并要求用户使用新密码重新登录。
				clearAuth()
				uni.showToast({ title: '密码已重置，请重新登录', icon: 'success' })
				setTimeout(() => uni.reLaunch({ url: '/pages/login/pwd_index' }), 800)
			} catch (error) {
				console.error('Reset password failed:', error)
			} finally {
				this.resettingPassword = false
			}
		},
		startPasswordCountdown() {
			this.clearPasswordCountdown()
			this.passwordCountdown = 60
			this.passwordCountdownTimer = setInterval(() => {
				if (this.passwordCountdown <= 1) {
					this.clearPasswordCountdown()
					return
				}
				this.passwordCountdown -= 1
			}, 1000)
		},
		clearPasswordCountdown() {
			if (this.passwordCountdownTimer) {
				clearInterval(this.passwordCountdownTimer)
				this.passwordCountdownTimer = null
			}
			this.passwordCountdown = 0
		},
		async handleLogout() {
			if (this.loggingOut) return
			this.loggingOut = true
			try {
				await logout()
			} catch (error) {
				// 即使网络异常也必须清除本地凭证，保证用户点击退出后立即结束本机登录态。
			} finally {
				clearAuth()
				uni.reLaunch({ url: '/pages/login/pwd_index' })
			}
		}
	}
}
</script>

<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100%;
	background-color: #f7f7f7;
}

.account-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	color: #26332f;
}

.page-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.navbar {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.back-button {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	justify-content: flex-start;
	box-sizing: border-box;
	width: 88rpx;
	height: 88rpx;
	margin: 0 0 0 -26rpx;
	padding-left: 26rpx;
	border-radius: 44rpx;
	background: transparent;
	line-height: normal;
}

.back-button::after,
.setting-row::after,
.logout-button::after {
	border: 0;
}

.nav-icon {
	font-size: 18px;
	color: #ffffff;
}

.nav-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.nav-placeholder {
	width: 36rpx;
	height: 58rpx;
}

.account-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx;
}

.setting-section {
	margin-bottom: 44rpx;
}

.section-title {
	display: block;
	margin: 0 0 18rpx 8rpx;
	font-size: 18px;
	font-weight: 600;
	line-height: 1.4;
	color: #26332f;
}

.setting-card {
	overflow: hidden;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(31, 78, 71, 0.08);
}

.setting-row {
	display: flex;
	align-items: center;
	box-sizing: border-box;
	width: 100%;
	height: 108rpx;
	margin: 0;
	padding: 0 28rpx;
	border-radius: 0;
	background: transparent;
	line-height: normal;
	text-align: left;
}

.setting-icon {
	flex-shrink: 0;
	width: 48rpx;
	font-size: 24px;
	line-height: 48rpx;
	text-align: center;
	color: #20ad9c;
}

.setting-label {
	flex: 1;
	margin-left: 24rpx;
	font-size: 16px;
	line-height: 1.4;
	color: #26332f;
}

.setting-status {
	flex-shrink: 0;
	margin-left: 16rpx;
	font-size: 14px;
	color: #a8b2af;
}

/* “修改”是已绑定状态下的明确操作提示，颜色沿用页面主品牌色。 */
.setting-action {
	flex-shrink: 0;
	margin-left: 18rpx;
	font-size: 14px;
	font-weight: 600;
	color: #159b8b;
}

.setting-arrow {
	flex-shrink: 0;
	margin-left: 18rpx;
	font-size: 20rpx;
	color: #8fa09c;
}

.row-divider {
	height: 1px;
	margin: 0 28rpx 0 100rpx;
	background-color: #edf1f0;
}

.security-section {
	margin-top: 2rpx;
}

.danger-color {
	color: #ef5952;
}

.logout-button {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	width: 100%;
	height: 92rpx;
	margin: 10rpx 0 0;
	border: 2rpx solid #17b8a4;
	border-radius: 46rpx;
	background-color: #ecf9f7;
	font-size: 17px;
	font-weight: 600;
	line-height: normal;
	color: #16ad9b;
}

.logout-button[disabled] {
	opacity: 0.6;
}

.control-pressed,
.row-pressed {
	background-color: rgba(24, 184, 164, 0.08);
}

.danger-pressed {
	background-color: rgba(239, 89, 82, 0.08);
}

.logout-pressed {
	background-color: #dff5f1;
}

.safe-bottom {
	height: calc(40rpx + env(safe-area-inset-bottom));
}

/* 半透明遮罩只服务于手机号绑定弹框，并为系统状态栏预留安全区。 */
.phone-dialog-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 1000;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	padding: calc(var(--status-bar-height) + 32rpx) 44rpx calc(32rpx + env(safe-area-inset-bottom));
	background: rgba(18, 35, 32, 0.46);
}

/* 弹框宽度保持移动端舒适阅读尺寸，圆角与原设置卡片保持一致。 */
.phone-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 660rpx;
	padding: 38rpx 36rpx 32rpx;
	border-radius: 28rpx;
	background: #ffffff;
	box-shadow: 0 24rpx 64rpx rgba(18, 70, 62, 0.2);
}

/* 四字段密码弹框在横屏或小屏设备中允许内部滚动，避免操作按钮被系统区域遮挡。 */
.password-dialog {
	max-height: calc(100vh - var(--status-bar-height) - 64rpx - env(safe-area-inset-bottom));
	overflow-y: auto;
}

.phone-dialog-title {
	display: block;
	font-size: 20px;
	font-weight: 700;
	line-height: 1.4;
	text-align: center;
	color: #26332f;
}

.phone-field {
	margin-top: 30rpx;
}

/* 可见标签避免仅依赖 placeholder，输入后用户仍能确认字段含义。 */
.phone-field-label {
	display: block;
	margin-bottom: 12rpx;
	font-size: 14px;
	font-weight: 600;
	line-height: 1.4;
	color: #4f615c;
}

.phone-field-input {
	box-sizing: border-box;
	width: 100%;
	height: 88rpx;
	padding: 0 24rpx;
	border: 2rpx solid #dce6e3;
	border-radius: 14rpx;
	background: #f8fbfa;
	font-size: 16px;
	line-height: 88rpx;
	color: #26332f;
}

.phone-code-row {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.phone-code-input {
	flex: 1;
	min-width: 0;
}

.send-code-button {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	width: 204rpx;
	height: 88rpx;
	margin: 0;
	padding: 0 12rpx;
	border-radius: 14rpx;
	background: #e8f7f4;
	font-size: 14px;
	font-weight: 600;
	line-height: normal;
	color: #159b8b;
}

.phone-dialog-actions {
	display: flex;
	align-items: center;
	gap: 20rpx;
	margin-top: 40rpx;
}

.dialog-button {
	display: flex;
	flex: 1;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	height: 88rpx;
	margin: 0;
	padding: 0;
	border-radius: 44rpx;
	font-size: 16px;
	font-weight: 600;
	line-height: normal;
}

.cancel-button {
	border: 2rpx solid #cfdad7;
	background: #ffffff;
	color: #52635f;
}

.confirm-button {
	background: #1ba291;
	color: #ffffff;
}

.send-code-button::after,
.dialog-button::after {
	border: 0;
}

.send-code-button[disabled],
.dialog-button[disabled] {
	opacity: 0.58;
}

/* 按压反馈只改变透明度，不移动控件，避免弹框操作时产生布局抖动。 */
.dialog-control-pressed {
	opacity: 0.78;
}

/* H5 键盘操作时保留清晰焦点反馈，方便无鼠标用户识别当前位置。 */
.phone-field-input:focus,
.send-code-button:focus,
.dialog-button:focus {
	border-color: #1ba291;
	box-shadow: 0 0 0 4rpx rgba(27, 162, 145, 0.16);
}

@media screen and (min-width: 768px) {
	.account-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
