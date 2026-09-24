<template>
	<view class="my-task-page">
		<view class="mytask-hero">
			<view class="mytask-navbar">
				<text class="iconfont icon-fanhui mytask-back" hover-class="mytask-control-pressed" @tap="handleBack"></text>
				<text class="mytask-nav-title">农事任务</text>
				<view class="mytask-nav-placeholder"></view>
			</view>
		</view>

		<view class="mytask-tabs">
			<view
				v-for="tab in statusTabs"
				:key="tab.label"
				class="mytask-tab"
				:class="{ active: activeStatus === tab.value }"
				:aria-label="`筛选${tab.label}任务`"
				hover-class="mytask-control-pressed"
				@tap="selectStatus(tab.value)"
			>
				<text>{{ tab.label }}</text>
			</view>
		</view>

		<scroll-view class="mytask-content" scroll-y>
			<view v-if="loading" class="mytask-state">正在加载农事任务...</view>
			<view v-else-if="!tasks.length" class="mytask-state">暂无{{ activeStatusLabel }}任务</view>
			<view v-else class="mytask-list">
				<view
					v-for="task in tasks"
					:key="task.id"
					class="mytask-card"
					hover-class="mytask-card-pressed"
					@tap="openTaskRecord(task)"
				>
					<view class="mytask-card-main">
						<view class="mytask-icon-wrap">
							<text class="iconfont mytask-icon" :class="task.taskIcon"></text>
						</view>
						<view class="mytask-info">
							<view class="mytask-title-row">
								<text class="mytask-title">{{ task.taskTitle }}</text>
								<text class="mytask-status" :class="statusClass(task)">{{ task.statusName }}</text>
							</view>
							<view class="mytask-meta-row">
								<text class="mytask-plot-tag">{{ formatPlotName(task.plotName) }}</text>
								<text class="mytask-crop-name">{{ task.cropName }}</text>
							</view>
							<view class="mytask-bottom-row">
								<text class="mytask-time">截至 {{ task.deadlineText || '-' }}</text>
								<button
									class="mytask-action"
									:class="{ completed: task.status === 3, disabled: isActionDisabled(task) }"
									:disabled="isActionDisabled(task)"
									hover-class="mytask-action-pressed"
									@tap.stop="handlePrimaryAction(task)"
								>
									{{ actionText(task) }}
								</button>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view class="mytask-safe-bottom"></view>
		</scroll-view>

		<view v-if="completionTask" class="mytask-mask" @tap="closeCompletion">
			<view class="mytask-dialog" @tap.stop>
				<text class="mytask-dialog-title">完成任务</text>
				<text class="mytask-dialog-subtitle">请上传一张现场图片作为完成凭证</text>
				<view class="mytask-uploader" hover-class="mytask-uploader-pressed" @tap="chooseCompletionImage">
					<protected-image v-if="completionImagePath" class="mytask-preview" :src="completionImagePath" mode="aspectFill"></protected-image>
					<template v-else>
						<text class="iconfont icon-tianjia mytask-upload-icon"></text>
						<text class="mytask-upload-label">上传完成图片</text>
						<text class="mytask-upload-hint">支持 JPG、PNG、WEBP，最大 5MB</text>
					</template>
				</view>
				<textarea v-model="completionRemark" class="mytask-remark" name="my-task-completion-remark" maxlength="500" placeholder="填写完成说明（选填）" />
				<view class="mytask-dialog-actions">
					<button class="mytask-cancel" :disabled="completionSubmitting" @tap="closeCompletion">取消</button>
					<button class="mytask-confirm" :disabled="completionSubmitting" @tap="confirmCompletion">
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
	getMyFarmTasks,
	uploadFarmTaskCompletionImage,
	MY_TASK_STATUS_OPTIONS
} from '@/api/taskList.js'

export default {
	data() {
		return {
			statusTabs: MY_TASK_STATUS_OPTIONS,
			activeStatus: '',
			tasks: [],
			loading: false,
			submittingTaskId: null,
			completionTask: null,
			completionImagePath: '',
			completionImageUrl: '',
			completionRemark: '',
			completionSubmitting: false
		}
	},
	computed: {
		activeStatusLabel() {
			const tab = this.statusTabs.find((item) => item.value === this.activeStatus)
			return tab && tab.value !== '' ? tab.label : ''
		}
	},
	onShow() {
		this.fetchTasks()
	},
	methods: {
		fetchTasks() {
			this.loading = true
			return getMyFarmTasks(this.activeStatus)
				.then((tasks) => {
					this.tasks = tasks
				})
				.catch(() => {
					this.tasks = []
				})
				.finally(() => {
					this.loading = false
				})
		},
		selectStatus(status) {
			if (status === this.activeStatus || this.loading) {
				return
			}
			this.activeStatus = status
			this.fetchTasks()
		},
		handleBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({ url: '/pages/index/mine' })
		},
		openTaskRecord(task) {
			if (!task || !task.id) {
				return
			}
			uni.navigateTo({
				url: `/pages/service/task_record?id=${encodeURIComponent(task.id)}`
			})
		},
		handlePrimaryAction(task) {
			if (task.canExecute) {
				this.startTask(task)
				return
			}
			if (task.status === 2 && task.canComplete) {
				this.completionTask = task
				this.completionImagePath = ''
				this.completionImageUrl = ''
				this.completionRemark = ''
			}
		},
		startTask(task) {
			if (this.submittingTaskId) {
				return
			}
			this.submittingTaskId = task.id
			executeFarmTask(task.id, this.createRequestId('start', task.id))
				.then(() => {
					uni.showToast({ title: '任务开始执行', icon: 'success' })
					return this.fetchTasks()
				})
				.finally(() => {
					this.submittingTaskId = null
				})
		},
		isActionDisabled(task) {
			return Boolean(this.submittingTaskId) || task.status === 3 || task.status === 5
		},
		actionText(task) {
			if (this.submittingTaskId === task.id) {
				return '执行中'
			}
			if (task.canExecute) {
				return '执行任务'
			}
			if (task.status === 2) {
				return '完成任务'
			}
			return task.status === 3 ? '已完成' : task.statusName
		},
		statusClass(task) {
			if (task.overdue || task.status === 4) {
				return 'overdue'
			}
			return { 2: 'running', 3: 'completed' }[task.status] || ''
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
		closeCompletion() {
			if (this.completionSubmitting) {
				return
			}
			this.resetCompletion()
		},
		async confirmCompletion() {
			if (!this.completionTask || this.completionSubmitting) {
				return
			}
			if (!this.completionImagePath) {
				uni.showToast({ title: '请先上传完成图片', icon: 'none' })
				return
			}
			this.completionSubmitting = true
			const taskId = this.completionTask.id
			const remark = this.completionRemark.trim() || '已上传现场图片，任务完成'
			try {
				if (!this.completionImageUrl) {
					const upload = await uploadFarmTaskCompletionImage(taskId, this.completionImagePath)
					this.completionImageUrl = upload.imageUrl
				}
				await completeFarmTask(taskId, {
					requestId: this.createRequestId('complete', taskId),
					actionContent: remark,
					feedbackDetail: remark,
					resultStatus: 1,
					attachments: JSON.stringify([this.completionImageUrl])
				})
				this.resetCompletion()
				uni.showToast({ title: '任务已完成', icon: 'success' })
				await this.fetchTasks()
			} finally {
				this.completionSubmitting = false
			}
		},
		resetCompletion() {
			this.completionTask = null
			this.completionImagePath = ''
			this.completionImageUrl = ''
			this.completionRemark = ''
		},
		createRequestId(action, taskId) {
			return `${action}-${taskId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
		},
		formatPlotName(plotName) {
			const name = plotName || '-'
			const matched = name.match(/[A-Za-z0-9一二三四五六七八九十百]+号(?:地块|地)?/)
			return matched ? matched[0].replace('地块', '地') : name
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100%;
	background-color: #f7f7f7;
}

.my-task-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 14px;
	color: #000000;
}

.mytask-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.mytask-navbar {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.mytask-nav-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 16px;
	line-height: 58rpx;
	color: #ffffff;
}

.mytask-back,
.mytask-nav-placeholder {
	position: relative;
	z-index: 2;
	width: 44px;
	height: 44px;
	line-height: 44px;
}

.mytask-back {
	margin-left: -12rpx;
	font-size: 18px;
	color: #ffffff;
}

.mytask-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 224rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx;
}

.mytask-tabs {
	box-sizing: border-box;
	position: absolute;
	z-index: 3;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
	padding: 0 36rpx;
}

.mytask-tab {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	min-width: 104rpx;
	height: 88rpx;
	font-size: 14px;
	color: #1f2928;
}

.mytask-tab.active::after {
	position: absolute;
	left: 50%;
	bottom: 4rpx;
	width: 50rpx;
	height: 6rpx;
	border-radius: 6rpx;
	background-color: #5ab8ad;
	content: "";
	transform: translateX(-50%);
}

.mytask-list {
	padding-top: 24rpx;
}

.mytask-state {
	margin-top: 24rpx;
	padding: 72rpx 24rpx;
	border-radius: 20rpx;
	background-color: #ffffff;
	text-align: center;
	font-size: 14px;
	color: #89938f;
}

.mytask-card {
	box-sizing: border-box;
	margin-bottom: 24rpx;
	padding: 26rpx 28rpx;
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(38, 91, 84, 0.05);
}

.mytask-card-pressed {
	background-color: #f2faf8;
}

.mytask-control-pressed,
.mytask-action-pressed {
	opacity: 0.68;
}

.mytask-card-main,
.mytask-title-row,
.mytask-meta-row,
.mytask-bottom-row {
	display: flex;
	align-items: center;
}

.mytask-card-main {
	align-items: flex-start;
}

.mytask-icon-wrap {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 68rpx;
	height: 68rpx;
	margin-right: 28rpx;
	border-radius: 50%;
	background-color: #f7f7f7;
}

.mytask-icon {
	font-size: 42rpx;
	line-height: 42rpx;
	color: #5ab8ad;
}

.mytask-info {
	flex: 1;
	min-width: 0;
}

.mytask-title-row,
.mytask-bottom-row {
	justify-content: space-between;
}

.mytask-title {
	max-width: 330rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 14px;
	line-height: 1.35;
}

.mytask-status {
	flex-shrink: 0;
	margin-left: 16rpx;
	font-size: 12px;
	line-height: 1.35;
	color: #7d8986;
}

.mytask-status.running { color: #168577; }
.mytask-status.completed { color: #66817a; }
.mytask-status.overdue { color: #c04b3b; }

.mytask-meta-row {
	justify-content: flex-start;
	margin-top: 22rpx;
}

.mytask-plot-tag {
	box-sizing: border-box;
	min-width: 86rpx;
	height: 42rpx;
	padding: 0 12rpx;
	border-radius: 6rpx;
	text-align: center;
	font-size: 12px;
	line-height: 42rpx;
	color: #ffffff;
	background-color: #5ab8ad;
}

.mytask-crop-name {
	margin-left: 28rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 12px;
	line-height: 42rpx;
}

.mytask-bottom-row {
	margin-top: 22rpx;
	margin-left: -96rpx;
	width: calc(100% + 96rpx);
}

.mytask-time {
	font-size: 12px;
	line-height: 1.3;
	color: #89938f;
}

.mytask-action {
	box-sizing: border-box;
	min-width: 124rpx;
	height: 64rpx;
	margin: 0;
	padding: 0 18rpx;
	border: 0;
	border-radius: 8rpx;
	font-size: 12px;
	line-height: 64rpx;
	color: #ffffff;
	background-color: #5ab8ad;
}

.mytask-action::after,
.mytask-cancel::after,
.mytask-confirm::after {
	border: 0;
}

.mytask-action.disabled {
	opacity: 0.72;
}

.mytask-action.completed {
	background-color: #aab8b5;
}

.mytask-safe-bottom {
	height: calc(40rpx + env(safe-area-inset-bottom));
}

.mytask-mask {
	position: fixed;
	z-index: 20;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 36rpx;
	background-color: rgba(0, 0, 0, 0.42);
}

.mytask-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 650rpx;
	padding: 34rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
}

.mytask-dialog-title,
.mytask-dialog-subtitle,
.mytask-upload-label,
.mytask-upload-hint {
	display: block;
	text-align: center;
}

.mytask-dialog-title {
	font-size: 17px;
	font-weight: 500;
	color: #26302f;
}

.mytask-dialog-subtitle {
	margin-top: 12rpx;
	font-size: 13px;
	color: #89938f;
}

.mytask-uploader {
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

.mytask-uploader-pressed {
	background-color: #eef8f6;
}

.mytask-preview {
	width: 100%;
	height: 100%;
}

.mytask-upload-icon {
	font-size: 44rpx;
	color: #5ab8ad;
}

.mytask-upload-label {
	margin-top: 12rpx;
	font-size: 14px;
	color: #43514f;
}

.mytask-upload-hint {
	margin-top: 8rpx;
	font-size: 12px;
	color: #9aa5a2;
}

.mytask-remark {
	box-sizing: border-box;
	width: 100%;
	height: 150rpx;
	margin-top: 24rpx;
	padding: 20rpx;
	border-radius: 12rpx;
	background-color: #f7f7f7;
	font-size: 14px;
}

.mytask-dialog-actions {
	display: flex;
	gap: 20rpx;
	margin-top: 28rpx;
}

.mytask-cancel,
.mytask-confirm {
	flex: 1;
	height: 76rpx;
	margin: 0;
	border: 0;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 76rpx;
}

.mytask-cancel {
	color: #5f6c69;
	background-color: #edf1f0;
}

.mytask-confirm {
	color: #ffffff;
	background-color: #5ab8ad;
}

@media screen and (min-width: 768px) {
	.my-task-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
