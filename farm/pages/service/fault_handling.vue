<template>
	<view class="fault-page">
		<view class="fault-hero">
			<view class="fault-navbar">
				<text v-if="!isTechnicianHome" class="iconfont icon-fanhui fault-back" hover-class="fault-control-pressed" @tap="handleBack"></text>
				<view v-else class="fault-nav-placeholder"></view>
				<text class="fault-nav-title">故障处理</text>
				<view class="fault-nav-placeholder"></view>
			</view>
		</view>

		<!-- 农场主保留状态筛选；技术人员首页直接展示按处理进度排序的全部任务。 -->
		<view v-if="!isTechnicianHome" class="fault-tabs">
			<view
				v-for="tab in statusTabs"
				:key="tab.label"
				class="fault-tab"
				:class="{ active: activeStatus === tab.value }"
				:aria-label="`筛选${tab.label}故障`"
				hover-class="fault-control-pressed"
				@tap="selectStatus(tab.value)"
			>
				<text>{{ tab.label }}</text>
			</view>
		</view>

		<scroll-view class="fault-content" :class="{ 'technician-content': isTechnicianHome }" scroll-y>
			<view v-if="loading" class="fault-state">正在加载设备故障...</view>
			<view v-else-if="!faults.length" class="fault-state">{{ emptyStateText }}</view>
			<view v-else class="fault-list">
				<!-- 点击卡片查看完整维护过程；操作按钮阻止冒泡以保留原有接单流程。 -->
				<view v-for="fault in faults" :key="fault.id" class="fault-card" hover-class="fault-control-pressed" @tap="openFaultRecord(fault)">
					<view class="fault-card-main">
						<view class="fault-icon-wrap">
							<text class="iconfont icon-guzhangchuli fault-icon"></text>
						</view>
						<view class="fault-info">
							<view class="fault-title-row">
								<text class="fault-title">{{ fault.faultName }}</text>
								<text class="fault-status" :class="statusClass(fault)">{{ fault.statusName }}</text>
							</view>
							<view class="fault-meta-row">
								<text class="fault-plot-tag">{{ fault.plotName }}</text>
								<text class="fault-device">{{ fault.deviceName }}</text>
							</view>
						</view>
					</view>

					<view class="fault-detail">
						<view class="fault-detail-row">
							<text class="fault-detail-label">严重程度</text>
							<text class="fault-severity" :class="severityClass(fault)">{{ fault.severityName }}</text>
						</view>
						<view class="fault-detail-row">
							<text class="fault-detail-label">发生时间</text>
							<text class="fault-detail-value">{{ fault.startTimeText }}</text>
						</view>
						<view class="fault-detail-row">
							<text class="fault-detail-label">处理人员</text>
							<text class="fault-detail-value">{{ fault.handleUserName }}</text>
						</view>
						<view v-if="fault.faultDesc" class="fault-description">{{ fault.faultDesc }}</view>
						<view v-if="fault.handleResult" class="fault-result">
							<text class="fault-result-label">处理结果</text>
							<text class="fault-result-text">{{ fault.handleResult }}</text>
						</view>
					</view>

					<view class="fault-bottom-row">
						<text class="fault-code">{{ fault.faultCode || '系统检测故障' }}</text>
						<button
							class="fault-action"
							:class="{ completed: !fault.canAccept && !fault.canComplete }"
							:disabled="isActionDisabled(fault)"
							hover-class="fault-action-pressed"
								@tap.stop="handlePrimaryAction(fault)"
						>
							{{ actionText(fault) }}
						</button>
					</view>
				</view>
			</view>
			<view class="fault-safe-bottom"></view>
		</scroll-view>

		<!-- 完成弹窗复用农事任务的现场图片交互，图片必传、说明选填。 -->
		<view v-if="completionFault" class="fault-mask" @tap="closeCompletion">
			<view class="fault-dialog" @tap.stop>
				<text class="fault-dialog-title">完成任务</text>
				<text class="fault-dialog-subtitle">请上传一张现场图片作为完成凭证</text>
				<view class="fault-uploader" hover-class="fault-uploader-pressed" aria-label="上传完成图片" @tap="chooseCompletionImage">
					<protected-image v-if="completionImagePath" class="fault-image-preview" :src="completionImagePath" mode="aspectFill"></protected-image>
					<template v-else>
						<text class="iconfont icon-tianjia fault-add-icon"></text>
						<text class="fault-upload-label">上传完成图片</text>
						<text class="fault-upload-hint">支持 JPG、PNG、WEBP，最大 5MB</text>
					</template>
				</view>
				<textarea
					v-model="handleResult"
					class="fault-result-input"
					name="fault-handle-result"
					maxlength="255"
					placeholder="填写完成说明（选填）"
				/>
				<view class="fault-dialog-actions">
					<button class="fault-cancel" :disabled="submitting" @tap="closeCompletion">取消</button>
					<button class="fault-confirm" :disabled="submitting" @tap="confirmCompletion">
						{{ submitting ? '提交中...' : '确定' }}
					</button>
				</view>
			</view>
		</view>
		<TechnicianTabBar v-if="isTechnicianHome" active="home" />
	</view>
</template>

<script>
import {
	acceptDeviceFault,
	completeDeviceFault,
	DEVICE_FAULT_STATUS_OPTIONS,
	getDeviceFaults,
	getTechnicianFaults,
	uploadDeviceFaultCompletionImage
} from '@/api/deviceFault.js'
import { getCurrentClientUser } from '@/api/clientAuth.js'
import { getClientHome, getToken, setUserInfo } from '@/utils/auth.js'
import TechnicianTabBar from '@/componment/TechnicianTabBar.vue'

export default {
	components: { TechnicianTabBar },
	data() {
		return {
			isTechnicianHome: false,
			statusTabs: DEVICE_FAULT_STATUS_OPTIONS,
			activeStatus: '',
			faults: [],
			loading: false,
			submitting: false,
			submittingFaultId: null,
			completionFault: null,
			handleResult: '',
			completionImagePath: '',
			completionImageUrl: ''
		}
	},
	computed: {
		activeStatusLabel() {
			const tab = this.statusTabs.find((item) => item.value === this.activeStatus)
			return tab && tab.value !== '' ? tab.label : ''
		},
		emptyStateText() {
			return this.isTechnicianHome ? '暂无故障任务' : `暂无${this.activeStatusLabel}故障`
		}
	},
	async onShow() {
		if (!getToken()) {
			uni.reLaunch({ url: '/pages/login/pwd_index' })
			return
		}
		try {
			const user = await getCurrentClientUser()
			setUserInfo(user)
			const role = String(user.roleCode || '').toLowerCase()
			if (role !== 'technician' && role !== 'farm_owner') {
				uni.reLaunch({ url: getClientHome(role) })
				return
			}
			this.isTechnicianHome = role === 'technician'
			// 技术人员不使用状态筛选，首页由专用接口返回本人全部故障。
			this.statusTabs = DEVICE_FAULT_STATUS_OPTIONS
			if (this.isTechnicianHome) this.activeStatus = ''
			this.fetchFaults()
		} catch { /* 登录态失效由统一请求层清理。 */ }
	},
	methods: {
		// 重新读取当前状态下的故障，确保处理动作后卡片状态及时更新。
		fetchFaults() {
			this.loading = true
			return (this.isTechnicianHome ? getTechnicianFaults() : getDeviceFaults(this.activeStatus))
				.then((faults) => {
					this.faults = faults
				})
				.catch(() => {
					this.faults = []
				})
				.finally(() => {
					this.loading = false
				})
		},
		selectStatus(status) {
			if (status === this.activeStatus || this.loading) return
			this.activeStatus = status
			if (!this.isTechnicianHome) this.fetchFaults()
		},
		openFaultRecord(fault) {
			// 仅传故障 ID，详情页重新从后端读取最新状态与维护记录。
			uni.navigateTo({ url: `/pages/service/fault_record?id=${encodeURIComponent(fault.id)}` })
		},
		handleBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({ url: '/pages/index/index' })
		},
		// 根据后端返回的动作权限决定接受故障或打开完成弹窗。
		handlePrimaryAction(fault) {
			if (fault.canAccept) {
				this.acceptFault(fault)
				return
			}
			if (fault.canComplete) {
				this.completionFault = fault
				this.handleResult = ''
				this.completionImagePath = ''
				this.completionImageUrl = ''
			}
		},
		acceptFault(fault) {
			if (this.submittingFaultId) return
			this.submittingFaultId = fault.id
			acceptDeviceFault(fault.id)
				.then(() => {
					uni.showToast({ title: '故障处理中', icon: 'success' })
					return this.fetchFaults()
				})
				.finally(() => {
					this.submittingFaultId = null
				})
		},
		closeCompletion() {
			if (this.submitting) return
			this.completionFault = null
			this.handleResult = ''
			this.completionImagePath = ''
			this.completionImageUrl = ''
		},
		// 重新选择图片时清除旧地址，保证预览图与最终提交的凭证一致。
		chooseCompletionImage() {
			if (this.submitting) return
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (result) => {
					this.completionImagePath = result.tempFilePaths && result.tempFilePaths[0] || ''
					this.completionImageUrl = ''
				}
			})
		},
		async confirmCompletion() {
			if (!this.completionFault || this.submitting) return
			if (!this.completionImagePath) {
				uni.showToast({ title: '请先上传完成图片', icon: 'none' })
				return
			}
			this.submitting = true
			try {
				// 先上传凭证，再提交完成命令；失败时保留弹窗供用户重试。
				if (!this.completionImageUrl) {
					const uploaded = await uploadDeviceFaultCompletionImage(this.completionFault.id, this.completionImagePath)
					this.completionImageUrl = uploaded.imageUrl
				}
				await completeDeviceFault(this.completionFault.id, this.handleResult.trim(), this.completionImageUrl)
				this.closeCompletionAfterSubmit()
				uni.showToast({ title: '故障已处理', icon: 'success' })
				await this.fetchFaults()
			} finally {
				this.submitting = false
			}
		},
		// 提交成功时单独重置弹窗，避免 submitting 状态阻止常规关闭方法。
		closeCompletionAfterSubmit() {
			this.completionFault = null
			this.handleResult = ''
			this.completionImagePath = ''
			this.completionImageUrl = ''
		},
		isActionDisabled(fault) {
			return Boolean(this.submittingFaultId) || (!fault.canAccept && !fault.canComplete)
		},
		actionText(fault) {
			if (this.submittingFaultId === fault.id) return '处理中'
			if (fault.canAccept) return '处理故障'
			if (fault.canComplete) return '完成任务'
			return fault.statusName
		},
		statusClass(fault) {
			return { 1: 'processing', 2: 'completed', 3: 'closed' }[fault.status] || 'pending'
		},
		severityClass(fault) {
			return { 2: 'important', 3: 'critical' }[fault.severity] || ''
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page { height: 100%; background-color: #f7f7f7; }

.fault-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 14px;
	color: #000000;
}

.fault-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.fault-navbar { position: relative; display: flex; align-items: center; justify-content: space-between; height: 58rpx; }
.fault-nav-title { position: absolute; left: 90rpx; right: 90rpx; text-align: center; font-size: 16px; line-height: 58rpx; color: #ffffff; }
.fault-back, .fault-nav-placeholder { position: relative; z-index: 2; width: 44px; height: 44px; line-height: 44px; }
.fault-back { margin-left: -12rpx; font-size: 18px; color: #ffffff; }
.fault-control-pressed, .fault-action-pressed { opacity: 0.68; }

.fault-tabs {
	box-sizing: border-box;
	position: absolute;
	z-index: 3;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	display: flex;
	align-items: center;
	justify-content: space-around;
	height: 88rpx;
	padding: 0 36rpx;
}

.fault-tab { position: relative; display: flex; align-items: center; justify-content: center; min-width: 120rpx; height: 88rpx; color: #1f2928; }
.fault-tab.active::after { position: absolute; left: 50%; bottom: 4rpx; width: 50rpx; height: 6rpx; border-radius: 6rpx; background-color: #5ab8ad; content: ""; transform: translateX(-50%); }

.fault-content { position: absolute; left: 0; right: 0; top: calc(var(--status-bar-height) + 224rpx); bottom: 0; box-sizing: border-box; padding: 0 36rpx; }
/* 技术人员移除筛选栏后，列表上移并为三入口底部导航预留空间。 */
.fault-content.technician-content { top: calc(var(--status-bar-height) + 104rpx); bottom: calc(58px + env(safe-area-inset-bottom)); }
.fault-list { padding-top: 24rpx; }
.fault-state { margin-top: 24rpx; padding: 72rpx 24rpx; border-radius: 20rpx; background-color: #ffffff; text-align: center; color: #89938f; }
.fault-card { box-sizing: border-box; margin-bottom: 24rpx; padding: 26rpx 28rpx; border-radius: 20rpx; background-color: #ffffff; box-shadow: 0 8rpx 22rpx rgba(38, 91, 84, 0.05); }
.fault-card-main, .fault-title-row, .fault-meta-row, .fault-detail-row, .fault-bottom-row { display: flex; align-items: center; }
.fault-card-main { align-items: flex-start; }
.fault-icon-wrap { display: flex; flex-shrink: 0; align-items: center; justify-content: center; width: 68rpx; height: 68rpx; margin-right: 28rpx; border-radius: 50%; background-color: #f7f7f7; }
.fault-icon { font-size: 42rpx; line-height: 42rpx; color: #5ab8ad; }
.fault-info { flex: 1; min-width: 0; }
.fault-title-row, .fault-bottom-row { justify-content: space-between; }
.fault-title { max-width: 330rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1.35; }
.fault-status { flex-shrink: 0; margin-left: 16rpx; font-size: 12px; color: #aa7a2d; }
.fault-status.processing { color: #168577; }
.fault-status.completed, .fault-status.closed { color: #66817a; }
.fault-meta-row { justify-content: flex-start; margin-top: 22rpx; }
.fault-plot-tag { box-sizing: border-box; max-width: 180rpx; height: 42rpx; padding: 0 12rpx; overflow: hidden; border-radius: 6rpx; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; line-height: 42rpx; color: #ffffff; background-color: #5ab8ad; }
.fault-device { margin-left: 28rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; line-height: 42rpx; }
.fault-detail { margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid #edf1f0; }
.fault-detail-row { justify-content: space-between; min-height: 44rpx; font-size: 12px; }
.fault-detail-label { color: #89938f; }
.fault-detail-value { max-width: 410rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #45514f; }
.fault-severity { color: #7a8582; }
.fault-severity.important { color: #c17a24; }
.fault-severity.critical { color: #c04b3b; }
.fault-description, .fault-result { margin-top: 14rpx; padding: 16rpx 18rpx; border-radius: 10rpx; background-color: #f7f9f8; font-size: 12px; line-height: 1.6; color: #56625f; }
.fault-result { display: flex; align-items: flex-start; }
.fault-result-label { flex-shrink: 0; margin-right: 16rpx; color: #168577; }
.fault-result-text { flex: 1; }
.fault-bottom-row { margin-top: 22rpx; }
.fault-code { max-width: 350rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: #89938f; }
.fault-action { box-sizing: border-box; min-width: 132rpx; height: 72rpx; margin: 0; padding: 0 18rpx; border: 0; border-radius: 8rpx; font-size: 12px; font-weight: 600; line-height: 72rpx; color: #ffffff; background-color: #5ab8ad; }
.fault-action::after, .fault-cancel::after, .fault-confirm::after { border: 0; }
.fault-action.completed { background-color: #aab8b5; }
.fault-safe-bottom { height: calc(40rpx + env(safe-area-inset-bottom)); }

/* 故障完成弹框与“我的农事”完成任务弹框保持相同布局和视觉样式。 */
.fault-mask { position: fixed; z-index: 20; left: 0; right: 0; top: 0; bottom: 0; display: flex; align-items: center; justify-content: center; padding: 36rpx; background-color: rgba(0, 0, 0, 0.42); }
.fault-dialog { box-sizing: border-box; width: 100%; max-width: 650rpx; padding: 34rpx; border-radius: 24rpx; background-color: #ffffff; }
.fault-dialog-title, .fault-dialog-subtitle, .fault-upload-label, .fault-upload-hint { display: block; text-align: center; }
.fault-dialog-title { font-size: 17px; font-weight: 500; color: #26302f; }
.fault-dialog-subtitle { margin-top: 12rpx; font-size: 13px; color: #89938f; }
.fault-uploader { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 260rpx; margin-top: 28rpx; overflow: hidden; border: 2rpx dashed #a9d9d3; border-radius: 16rpx; background-color: #f7fbfa; }
.fault-uploader-pressed { background-color: #eef8f6; }
.fault-image-preview { width: 100%; height: 100%; }
.fault-add-icon { font-size: 44rpx; color: #5ab8ad; }
.fault-upload-label { margin-top: 12rpx; font-size: 14px; color: #43514f; }
.fault-upload-hint { margin-top: 8rpx; font-size: 12px; color: #9aa5a2; }
.fault-result-input { box-sizing: border-box; width: 100%; height: 150rpx; margin-top: 24rpx; padding: 20rpx; border-radius: 12rpx; background-color: #f7f7f7; font-size: 14px; }
.fault-dialog-actions { display: flex; gap: 20rpx; margin-top: 28rpx; }
.fault-cancel, .fault-confirm { flex: 1; height: 76rpx; margin: 0; border: 0; border-radius: 12rpx; font-size: 14px; line-height: 76rpx; }
.fault-cancel { color: #5f6c69; background-color: #edf1f0; }
.fault-confirm { color: #ffffff; background-color: #5ab8ad; }

@media screen and (min-width: 768px) {
	.fault-page { width: 750rpx; margin: 0 auto; }
}
</style>
