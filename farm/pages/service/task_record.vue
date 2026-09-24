<template>
	<view class="record-page">
		<view class="page-hero">
			<view class="navbar">
				<!-- 返回区域保持至少 44px 的触控尺寸，便于移动端单手操作。 -->
				<view class="nav-action" hover-class="nav-action-pressed" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon"></text>
				</view>
				<text class="nav-title">农事记录</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="record-content" scroll-y :show-scrollbar="false">
			<view v-if="loading" class="state-card">
				<view class="loading-dot"></view>
				<text class="state-text">正在加载任务记录...</text>
			</view>

			<template v-else-if="task">
				<!-- 顶部任务卡片只展示后端返回的任务快照，操作记录在下方独立呈现。 -->
				<view class="task-card">
					<view class="task-summary">
						<protected-image class="crop-image" :src="task.cropImage" mode="aspectFill" @error="handleImageError"></protected-image>
						<view class="task-main">
							<view class="task-title-row">
								<view class="task-title">{{ task.taskTitle }}</view>
							</view>
							<view class="task-tags">
								<text class="plot-tag">{{ task.plotName }}</text>
								<text class="crop-name">{{ task.cropName }}</text>
								<text class="status-badge" :class="statusClass(task.status)">{{ task.statusName }}</text>
							</view>
						</view>
					</view>
					<view class="deadline-row">
						<view class="deadline-info">
							<text class="deadline-label">截止时间</text>
							<text class="deadline-value" :class="{ overdue: task.overdue }">{{ task.deadlineText || '-' }}</text>
						</view>
						<button
							v-if="task.canExecute || task.canComplete"
							class="execute-button"
							hover-class="execute-button-pressed"
							:disabled="executing || completionSubmitting"
							@tap="handlePrimaryAction"
						>
							<text class="execute-button-label">{{ actionText() }}</text>
						</button>
					</view>
				</view>

				<view class="section-head">
					<view>
						<text class="section-title">操作记录</text>
						<text class="record-count">{{ records.length }} 条</text>
					</view>
					<text class="section-hint">按发生时间排序</text>
				</view>

				<view v-if="records.length" class="timeline-card">
					<!-- 创建节点来自任务快照，其余节点来自不可变操作记录，共同组成完整状态流程。 -->
					<view class="timeline-item" v-for="(record, index) in records" :key="record.id">
						<view class="timeline-axis">
							<view class="timeline-dot" :class="actionClass(record.actionType)"></view>
							<view v-if="index < records.length - 1" class="timeline-line"></view>
						</view>
						<view class="record-body">
							<view class="record-title-row">
								<text class="record-action">{{ record.actionName }}</text>
								<view class="record-badges">
									<text v-if="record.progressPercent !== null" class="progress-badge">{{ record.progressPercent }}%</text>
									<text v-if="record.statusName" class="record-status" :class="statusClass(record.afterStatus)">{{ record.statusName }}</text>
								</view>
							</view>
							<text class="record-content-text">{{ recordDescription(record) }}</text>
							<!-- 完成事件存在现场凭证时直接展示，便于后续追溯和验收。 -->
							<protected-image
								v-if="record.attachmentUrls && record.attachmentUrls.length"
								class="record-evidence"
								:src="record.attachmentUrls[0]"
								mode="aspectFill"
							></protected-image>
							<view class="record-meta">
								<text class="operator-name">{{ record.operatorName || '系统记录' }}</text>
								<text class="execute-time">{{ record.executeTimeText || '-' }}</text>
							</view>
						</view>
					</view>
				</view>

			</template>

			<view v-else class="state-card error-card">
				<text class="state-title">任务记录加载失败</text>
				<text class="state-text">请检查网络连接后重试</text>
				<view class="retry-button" hover-class="retry-button-pressed" @tap="loadTaskRecord">重新加载</view>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>

		<view v-if="completionVisible" class="completion-mask" @tap="closeCompletionDialog">
			<view class="completion-dialog" @tap.stop>
				<text class="completion-title">完成任务</text>
				<text class="completion-subtitle">请上传一张现场图片作为完成凭证</text>
				<view class="completion-uploader" hover-class="completion-uploader-pressed" @tap="chooseCompletionImage">
					<protected-image
						v-if="completionImagePath"
						class="completion-preview"
						:src="completionImagePath"
						mode="aspectFill"
					></protected-image>
					<template v-else>
						<text class="iconfont icon-tianjia completion-upload-icon"></text>
						<text class="completion-upload-label">上传完成图片</text>
						<text class="completion-upload-hint">支持 JPG、PNG、WEBP，最大 5MB</text>
					</template>
				</view>
				<textarea
					v-model="completionRemark"
					class="completion-remark"
					maxlength="500"
					placeholder="填写完成说明（选填）"
				></textarea>
				<view class="completion-actions">
					<button class="completion-cancel" :disabled="completionSubmitting" @tap="closeCompletionDialog">取消</button>
					<button
						class="completion-confirm"
						hover-class="completion-confirm-pressed"
						:disabled="completionSubmitting"
						@tap="confirmCompletion"
					>
						{{ completionSubmitting ? '提交中...' : '确定' }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import {
	completeFarmTask,
	executeFarmTask,
	getFarmTaskDetail,
	getFarmTaskTimeline,
	uploadFarmTaskCompletionImage
} from '@/api/taskList.js'

const DEFAULT_CROP_IMAGE = '/static/lecttue.png'

export default {
	data() {
		return {
			taskId: null,
			task: null,
			records: [],
			loading: true,
			executing: false,
			completionVisible: false,
			completionImagePath: '',
			completionImageUrl: '',
			completionRemark: '',
			completionSubmitting: false
		}
	},
	onLoad(options) {
		this.taskId = options && options.id ? options.id : null
		this.loadTaskRecord()
	},
	methods: {
		async loadTaskRecord() {
			if (!this.taskId) {
				this.loading = false
				uni.showToast({ title: '农事任务ID不能为空', icon: 'none' })
				return
			}

			this.loading = true
			try {
				// 任务快照与时间线相互独立，并行加载可缩短详情页等待时间。
				const [task, records] = await Promise.all([
					getFarmTaskDetail(this.taskId),
					getFarmTaskTimeline(this.taskId)
				])
				this.task = task
				this.records = [this.pendingRecord(task), ...records]
			} catch (error) {
				this.task = null
				this.records = []
			} finally {
				this.loading = false
			}
		},
		handleBack() {
			uni.navigateBack()
		},
		handleImageError() {
			// 网络图片失效时切回本地资源，避免反复触发加载错误。
			if (this.task && this.task.cropImage !== DEFAULT_CROP_IMAGE) {
				this.task.cropImage = DEFAULT_CROP_IMAGE
			}
		},
		async handleExecute() {
			if (!this.task || !this.task.canExecute || this.executing) {
				return
			}

			this.executing = true
			try {
				const task = await executeFarmTask(
					this.task.id,
					this.createRequestId('start', this.task.id)
				)
				await this.applyTaskUpdate(task)
				uni.showModal({
					title: '提示',
					content: '任务开始执行',
					showCancel: false,
					confirmText: '知道了'
				})
			} finally {
				this.executing = false
			}
		},
		handlePrimaryAction() {
			if (!this.task || this.executing || this.completionSubmitting) {
				return
			}
			if (this.task.canExecute) {
				this.handleExecute()
				return
			}
			if (this.task.canComplete) {
				this.resetCompletionDialog()
				this.completionVisible = true
			}
		},
		actionText() {
			if (this.executing) {
				return '执行中'
			}
			return this.task && this.task.canComplete ? '完成任务' : '执行任务'
		},
		chooseCompletionImage() {
			if (this.completionSubmitting) {
				return
			}
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
		closeCompletionDialog() {
			if (this.completionSubmitting) {
				return
			}
			this.completionVisible = false
			this.resetCompletionDialog()
		},
		resetCompletionDialog() {
			this.completionImagePath = ''
			this.completionImageUrl = ''
			this.completionRemark = ''
		},
		async confirmCompletion() {
			if (!this.task || !this.task.canComplete || this.completionSubmitting) {
				return
			}
			if (!this.completionImagePath) {
				uni.showToast({ title: '请先上传完成图片', icon: 'none' })
				return
			}

			this.completionSubmitting = true
			const taskId = this.task.id
			const remark = this.completionRemark.trim() || '已上传现场图片，任务完成'
			try {
				if (!this.completionImageUrl) {
					const upload = await uploadFarmTaskCompletionImage(taskId, this.completionImagePath)
					this.completionImageUrl = upload.imageUrl
				}
				const task = await completeFarmTask(taskId, {
					requestId: this.createRequestId('complete', taskId),
					actionContent: remark,
					feedbackDetail: remark,
					resultStatus: 1,
					attachments: JSON.stringify([this.completionImageUrl])
				})
				await this.applyTaskUpdate(task)
				this.completionVisible = false
				this.resetCompletionDialog()
				uni.showModal({
					title: '提示',
					content: '任务已完成',
					showCancel: false,
					confirmText: '知道了'
				})
			} finally {
				this.completionSubmitting = false
			}
		},
		async applyTaskUpdate(task) {
			this.task = task
			this.records = [this.pendingRecord(task), ...await getFarmTaskTimeline(task.id)]
		},
		createRequestId(action, taskId) {
			return `${action}-${taskId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
		},
		statusClass(status) {
			return `status-${Number(status || 0)}`
		},
		actionClass(actionType) {
			return `action-${Number(actionType || 0)}`
		},
		pendingRecord(task) {
			return {
				id: `task-${task.id}-pending`,
				actionType: 0,
				actionName: '创建任务',
				actionContent: '农事任务已创建，等待开始执行',
				feedbackDetail: '',
				progressPercent: null,
				afterStatus: 1,
				statusName: '未开始',
				operatorName: '系统记录',
				attachmentUrls: [],
				executeTimeText: task.createTimeText
			}
		},
		recordDescription(record) {
			// 优先展示用户填写的过程反馈，没有反馈时再展示动作说明。
			return record.feedbackDetail || record.actionContent || '已记录本次农事操作'
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100vh;
	overflow: hidden;
	background-color: #f4f7f6;
}

.record-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background: linear-gradient(180deg, #6fc4ba 0, #8ed2c9 210rpx, #f4f7f6 430rpx);
	color: #1f2937;
}

.page-hero {
	box-sizing: border-box;
	height: calc(var(--status-bar-height) + 104rpx);
	padding: var(--status-bar-height) 24rpx 0;
}

.navbar {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
	color: #ffffff;
}

.nav-action,
.nav-placeholder {
	display: flex;
	align-items: center;
	width: 88rpx;
	height: 88rpx;
}

.nav-action {
	justify-content: flex-start;
	border-radius: 44rpx;
}

.nav-action-pressed {
	background-color: rgba(255, 255, 255, 0.16);
}

.nav-icon {
	font-size: 36rpx;
	color: #ffffff;
}

.nav-title {
	position: absolute;
	left: 100rpx;
	right: 100rpx;
	text-align: center;
	font-size: 18px;
	font-weight: 500;
	color: #ffffff;
}

.record-content {
	position: absolute;
	top: calc(var(--status-bar-height) + 104rpx);
	bottom: 0;
	left: 0;
	right: 0;
	box-sizing: border-box;
	height: calc(100vh - var(--status-bar-height) - 104rpx);
	padding: 16rpx 28rpx 0;
}

.task-card,
.timeline-card,
.state-card {
	box-sizing: border-box;
	border: 1rpx solid rgba(31, 41, 55, 0.04);
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 12rpx 32rpx rgba(28, 94, 86, 0.09);
}

.task-card {
	padding: 20rpx 26rpx;
}

.task-summary {
	display: flex;
	align-items: center;
}

.crop-image {
	flex-shrink: 0;
	width: 128rpx;
	height: 128rpx;
	margin-right: 24rpx;
	border: 1rpx solid #edf3f1;
	border-radius: 18rpx;
	background-color: #edf3f1;
}

.task-main {
	flex: 1;
	min-width: 0;
}

.record-title-row,
.record-meta {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.task-title-row {
	display: flex;
	align-items: flex-start;
}

.task-title {
	display: -webkit-box;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	font-size: 17px;
	font-weight: 600;
	line-height: 1.35;
	color: #17211f;
}

.status-badge {
	flex-shrink: 0;
	margin-left: auto;
	padding: 8rpx 14rpx;
	border-radius: 18rpx;
	font-size: 11px;
	line-height: 1.2;
	white-space: nowrap;
	color: #64748b;
	background-color: #f1f5f4;
}

.status-1 { color: #7c6514; background-color: #fff7d6; }
.status-2 { color: #147a6d; background-color: #dcf6f1; }
.status-3 { color: #256d3b; background-color: #e1f5e7; }
.status-4 { color: #b04432; background-color: #fff0ed; }
.status-5 { color: #64748b; background-color: #eef2f4; }

.task-tags {
	display: flex;
	align-items: center;
	gap: 10rpx;
	margin-top: 16rpx;
}

.plot-tag {
	max-width: 180rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	padding: 6rpx 14rpx;
	border-radius: 8rpx;
	font-size: 12px;
	color: #ffffff;
	background-color: #57b8ac;
}

.crop-name {
	flex-shrink: 0;
	font-size: 12px;
	color: #667570;
}

.deadline-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
	min-height: 88rpx;
	margin-top: 20rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid #eaf1ef;
	font-size: 12px;
}

.deadline-info {
	display: flex;
	align-items: center;
	min-width: 0;
}

.deadline-label { color: #89938f; }
.deadline-value { margin-left: 12rpx; font-weight: 500; color: #4b5753; }
.deadline-value.overdue { color: #c24a3a; }

.execute-button {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	min-width: 144rpx;
	height: 70rpx;
	margin: 0 0 0 16rpx;
	padding: 0;
	border: 0;
	border-radius: 12rpx;
	font-size: 12px;
	font-weight: 500;
	line-height: normal;
	color: #ffffff;
	background-color: transparent;
}

.execute-button-label {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	width: 100%;
	height: 72rpx;
	padding: 0 14rpx;
	border-radius: 12rpx;
	white-space: nowrap;
	background: linear-gradient(135deg, #62c4b8, #45aa9e);
	box-shadow: 0 6rpx 14rpx rgba(69, 170, 158, 0.18);
}

.execute-button::after { border: 0; }
.execute-button-pressed .execute-button-label { opacity: 0.82; }
.execute-button[disabled] .execute-button-label { opacity: 0.68; }

.section-head {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	margin: 38rpx 4rpx 20rpx;
}

.section-title {
	font-size: 18px;
	font-weight: 600;
	color: #17211f;
}

.record-count {
	margin-left: 14rpx;
	font-size: 12px;
	color: #6d7a76;
}

.section-hint {
	font-size: 12px;
	color: #89938f;
}

.timeline-card {
	padding: 30rpx 28rpx 12rpx;
}

.timeline-item {
	display: flex;
	min-height: 166rpx;
}

.timeline-axis {
	position: relative;
	flex-shrink: 0;
	width: 34rpx;
	margin-right: 18rpx;
}

.timeline-dot {
	position: relative;
	z-index: 2;
	box-sizing: border-box;
	width: 24rpx;
	height: 24rpx;
	margin-top: 5rpx;
	border: 6rpx solid #57b8ac;
	border-radius: 50%;
	background-color: #ffffff;
}

.timeline-dot.action-0 { border-color: #d2a838; }
.timeline-dot.action-2 { border-color: #4f9d68; }
.timeline-dot.action-3 { border-color: #3d8ea1; }
.timeline-dot.action-4 { border-color: #d29a35; }

.timeline-line {
	position: absolute;
	top: 28rpx;
	bottom: -4rpx;
	left: 11rpx;
	width: 2rpx;
	background-color: #cfe7e3;
}

.record-body {
	flex: 1;
	min-width: 0;
	padding-bottom: 30rpx;
}

.record-action {
	font-size: 15px;
	font-weight: 600;
	color: #263431;
}

.progress-badge {
	padding: 5rpx 12rpx;
	border-radius: 14rpx;
	font-size: 11px;
	color: #147a6d;
	background-color: #e0f5f1;
}

.record-badges {
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.record-status {
	padding: 5rpx 12rpx;
	border-radius: 14rpx;
	font-size: 11px;
	line-height: 1.2;
}

.record-content-text {
	display: block;
	margin-top: 12rpx;
	font-size: 13px;
	line-height: 1.55;
	color: #586661;
}

.record-evidence {
	display: block;
	width: 180rpx;
	height: 132rpx;
	margin-top: 16rpx;
	border-radius: 12rpx;
	background-color: #edf3f1;
}

.record-meta {
	margin-top: 14rpx;
	font-size: 12px;
	line-height: 1.3;
}

.operator-name { color: #52615c; }
.execute-time { color: #8a9692; }

.state-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-height: 360rpx;
	padding: 52rpx 32rpx;
	text-align: center;
}

.state-title {
	margin-top: 24rpx;
	font-size: 16px;
	font-weight: 600;
	color: #35433f;
}

.state-text {
	margin-top: 14rpx;
	font-size: 13px;
	line-height: 1.5;
	color: #7a8883;
}

.loading-dot {
	width: 28rpx;
	height: 28rpx;
	border: 4rpx solid #d7ebe7;
	border-top-color: #38a99b;
	border-radius: 50%;
	animation: loading-rotate 0.8s linear infinite;
}

.retry-button {
	display: flex;
	align-items: center;
	justify-content: center;
	min-width: 196rpx;
	height: 88rpx;
	margin-top: 30rpx;
	border-radius: 44rpx;
	font-size: 14px;
	color: #ffffff;
	background-color: #39a99b;
}

.retry-button-pressed { opacity: 0.82; }
.safe-bottom { height: calc(env(safe-area-inset-bottom) + 36rpx); }

.completion-mask {
	position: fixed;
	z-index: 20;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	padding: 36rpx;
	background-color: rgba(0, 0, 0, 0.42);
}

.completion-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 650rpx;
	padding: 34rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 18rpx 48rpx rgba(24, 69, 63, 0.18);
}

.completion-title,
.completion-subtitle,
.completion-upload-label,
.completion-upload-hint {
	display: block;
	text-align: center;
}

.completion-title {
	font-size: 17px;
	font-weight: 600;
	color: #26302f;
}

.completion-subtitle {
	margin-top: 12rpx;
	font-size: 13px;
	color: #89938f;
}

.completion-uploader {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 260rpx;
	margin-top: 28rpx;
	overflow: hidden;
	border: 2rpx dashed #a9d9d3;
	border-radius: 16rpx;
	background-color: #f7fbfa;
}

.completion-uploader-pressed { background-color: #eef8f6; }
.completion-preview { width: 100%; height: 100%; }
.completion-upload-icon { font-size: 44rpx; color: #5ab8ad; }

.completion-upload-label {
	margin-top: 12rpx;
	font-size: 14px;
	color: #43514f;
}

.completion-upload-hint {
	margin-top: 8rpx;
	font-size: 12px;
	color: #9aa5a2;
}

.completion-remark {
	box-sizing: border-box;
	width: 100%;
	height: 150rpx;
	margin-top: 24rpx;
	padding: 20rpx;
	border-radius: 12rpx;
	background-color: #f7f7f7;
	font-size: 14px;
}

.completion-actions {
	display: flex;
	gap: 20rpx;
	margin-top: 28rpx;
}

.completion-cancel,
.completion-confirm {
	flex: 1;
	height: 88rpx;
	margin: 0;
	border: 0;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 88rpx;
}

.completion-cancel { color: #5f6c69; background-color: #edf1f0; }
.completion-confirm { color: #ffffff; background-color: #5ab8ad; }
.completion-cancel::after,
.completion-confirm::after { border: 0; }
.completion-confirm-pressed { opacity: 0.82; }
.completion-cancel[disabled],
.completion-confirm[disabled] { opacity: 0.68; }

@keyframes loading-rotate {
	to { transform: rotate(360deg); }
}

@media screen and (min-width: 768px) {
	.record-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
