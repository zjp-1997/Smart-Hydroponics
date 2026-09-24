<template>
	<view class="task-list-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">农事管理</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="task-content" scroll-y>
			<scroll-view class="date-scroll" scroll-x :show-scrollbar="false">
				<view class="date-inner">
					<view
						class="date-card"
						v-for="item in dateItems"
						:key="item.taskDate"
						:class="{ active: activeDate === item.taskDate }"
						@tap="handleDateChange(item)"
					>
						<text class="date-week">{{ item.weekday }}</text>
						<text class="date-day">{{ item.dayText }}</text>
						<text class="date-count">({{ item.taskCount }})</text>
					</view>
				</view>
			</scroll-view>

			<view class="task-tabs">
				<view
					class="task-tab"
					v-for="tab in taskTabs"
					:key="tab.label"
					:class="{ active: activeTaskType === tab.value }"
					@tap="handleTaskTypeChange(tab)"
				>
					<text>{{ tab.label }}</text>
				</view>
			</view>

			<view v-if="loading" class="state-text">正在加载农事任务...</view>
			<view v-else-if="!tasks.length" class="state-text">暂无农事任务</view>
			<view v-else class="task-list">
				<view class="task-card" v-for="task in tasks" :key="task.id" hover-class="task-card-pressed" @tap="openTaskDetail(task)">
					<view class="task-main">
						<view class="task-icon-wrap">
							<text class="iconfont task-icon" :class="task.taskIcon"></text>
						</view>
						<view class="task-info">
							<view class="task-title-row">
								<text class="task-title">{{ task.taskTitle }}</text>
								<text class="task-status" :class="`status-${task.status}`">{{ task.statusName }}</text>
							</view>
							<view class="task-meta-row">
								<text class="plot-tag">{{ formatPlotName(task.plotName) }}</text>
								<text class="crop-name">{{ task.cropName }}</text>
							</view>
							<view class="task-bottom-row">
								<text class="task-time">截至 {{ task.deadlineText || '-' }}</text>
								<button
									class="execute-btn"
									:class="{ completed: task.status === 3, disabled: isActionDisabled(task) }"
									:disabled="isActionDisabled(task)"
									@tap.stop="handlePrimaryAction(task)"
								>
									{{ resolveActionText(task) }}
								</button>
							</view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<!-- 详情使用轻量底部面板承载执行过程，保持原任务列表布局和视觉风格不变。 -->
		<view v-if="detailVisible && detailTask" class="detail-mask" @tap="closeTaskDetail">
			<view class="detail-panel" @tap.stop>
				<view class="detail-title-row">
					<view>
						<text class="detail-title">{{ detailTask.taskTitle }}</text>
						<text class="detail-subtitle">{{ detailTask.plotName }} · {{ detailTask.statusName }}</text>
					</view>
					<text class="detail-close" @tap="closeTaskDetail">关闭</text>
				</view>

				<view class="detail-meta">
					<text>截至时间：{{ detailTask.deadlineText || '-' }}</text>
					<text>实际开始：{{ detailTask.actualStartText || '-' }}</text>
					<text>实际完成：{{ detailTask.actualEndText || '-' }}</text>
					<text>执行人员：{{ detailTask.executorName || '-' }}</text>
				</view>

				<view v-if="detailTask.status === 2" class="detail-actions">
					<button class="secondary-action" @tap="openAction('progress')">记录进度</button>
				</view>

				<!-- 动作表单渐进展示，避免在任务列表中增加长期占位的复杂控件。 -->
				<view v-if="actionVisible" class="action-form">
					<text class="action-title">{{ actionMode === 'progress' ? '记录执行进度' : '填写完成说明' }}</text>
					<view v-if="actionMode === 'progress'" class="progress-field">
						<text>当前进度 {{ progressPercent }}%</text>
						<slider :value="progressPercent" min="0" max="100" step="5" activeColor="#5AB8AD" @change="handleProgressChange" />
					</view>
					<textarea v-model="actionContent" class="action-textarea" name="task-action-content" maxlength="500" placeholder="请输入本次执行说明" />
					<view class="action-buttons">
						<button class="cancel-action" :disabled="actionSubmitting" @tap="closeAction">取消</button>
						<button class="primary-action" :disabled="actionSubmitting" @tap="submitAction">{{ actionSubmitting ? '提交中' : '确认提交' }}</button>
					</view>
				</view>

				<scroll-view class="timeline-list" scroll-y>
					<text class="timeline-heading">执行过程</text>
					<text v-if="timelineLoading" class="timeline-empty">正在加载...</text>
					<text v-else-if="!timeline.length" class="timeline-empty">暂无执行记录</text>
					<template v-else>
						<view v-for="record in timeline" :key="record.id" class="timeline-item">
							<view class="timeline-dot"></view>
							<view class="timeline-body">
								<text class="timeline-action">{{ resolveTimelineAction(record) }}</text>
								<text class="timeline-content">{{ resolveTimelineContent(record) }}</text>
								<text class="timeline-time">{{ record.operatorName }} · {{ record.executeTimeText || '-' }}</text>
							</view>
						</view>
					</template>
				</scroll-view>
			</view>
		</view>

		<!-- 完成任务弹框要求先上传现场图片，确认后才提交后端完成命令。 -->
		<view v-if="completionVisible && completionTask" class="completion-mask" @tap="closeCompletionDialog">
			<view class="completion-dialog" @tap.stop>
				<text class="completion-title">完成任务</text>
				<text class="completion-subtitle">请上传一张现场图片作为完成凭证</text>

				<view
					class="evidence-uploader"
					hover-class="evidence-uploader-pressed"
					@tap="chooseCompletionImage"
				>
					<protected-image v-if="completionImagePath" class="evidence-preview" :src="completionImagePath" mode="aspectFill"></protected-image>
					<template v-else>
						<text class="iconfont icon-tianjia evidence-add-icon"></text>
						<text class="evidence-label">上传完成图片</text>
						<text class="evidence-hint">支持 JPG、PNG、WEBP，最大 5MB</text>
					</template>
				</view>

				<textarea
					v-model="completionRemark"
					class="completion-remark"
					name="task-completion-remark"
					maxlength="500"
					placeholder="填写完成说明（选填）"
				/>
				<view class="completion-actions">
					<button class="completion-cancel" :disabled="completionSubmitting" @tap="closeCompletionDialog">取消</button>
					<button class="completion-confirm" :disabled="completionSubmitting" @tap="confirmCompletion">
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
	getFarmTasks,
	getFarmTaskTimeline,
	getTaskDateSummaries,
	submitFarmTaskProgress,
	uploadFarmTaskCompletionImage,
	TASK_TYPE_OPTIONS
} from '@/api/taskList.js'

export default {
	data() {
		return {
			dateItems: [],
			tasks: [],
			taskTabs: TASK_TYPE_OPTIONS,
			activeDate: '',
			activeTaskType: '',
			loading: false,
			submittingTaskId: null,
			// 详情和动作面板分离列表状态，避免刷新列表时丢失用户正在查看的过程。
			detailVisible: false,
			detailTask: null,
			timeline: [],
			timelineLoading: false,
			actionVisible: false,
			actionMode: 'progress',
			actionContent: '',
			progressPercent: 50,
			actionSubmitting: false,
			completionVisible: false,
			completionTask: null,
			completionImagePath: '',
			completionImageUrl: '',
			completionRemark: '',
			completionSubmitting: false
		}
	},
	onLoad() {
		this.initTaskPage()
	},
	methods: {
		initTaskPage() {
			// 页面初始化时加载 7 天日期摘要，并默认选中第一天查询任务列表。
			getTaskDateSummaries(this.todayText())
				.then((items) => {
					this.dateItems = items
					this.activeDate = items.length ? items[0].taskDate : this.todayText()
					return this.fetchTasks()
				})
				.catch(() => {
					this.dateItems = []
					this.tasks = []
				})
		},
		fetchTasks() {
			this.loading = true
			// 任务列表的日期和类型筛选统一交给后端处理，前端只负责展示当前 tab 结果。
			return getFarmTasks({
				taskDate: this.activeDate,
				taskType: this.activeTaskType
			})
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
		handleBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack()
				return
			}
			// 当任务页被直接打开时，返回首页比停留空白返回栈更符合移动端操作预期。
			uni.switchTab({
				url: '/pages/index/index'
			})
		},
		handleDateChange(item) {
			if (!item || item.taskDate === this.activeDate) {
				return
			}
			this.activeDate = item.taskDate
			this.fetchTasks()
		},
		handleTaskTypeChange(tab) {
			if (!tab || tab.value === this.activeTaskType) {
				return
			}
			this.activeTaskType = tab.value
			this.fetchTasks()
		},
		handleExecute(task) {
			if (!task.canExecute || this.submittingTaskId) {
				return
			}
			this.submittingTaskId = task.id
			// 每次业务动作生成独立幂等号，弱网重试不会产生重复的开始事件。
			executeFarmTask(task.id, this.createRequestId('start', task.id))
				.then((updatedTask) => {
					this.replaceTask(updatedTask)
					// 使用明确弹框确认状态变更，避免用户错过轻量 Toast。
					uni.showModal({
						title: '提示',
						content: '任务开始执行',
						showCancel: false,
						confirmText: '知道了'
					})
					return getTaskDateSummaries(this.todayText())
				})
				.then((items) => {
					this.dateItems = items
				})
				.finally(() => {
					this.submittingTaskId = null
				})
		},
		// 列表主按钮按后端状态分派动作：未开始执行、进行中完成、已完成禁用。
		handlePrimaryAction(task) {
			if (task.canExecute) {
				this.handleExecute(task)
				return
			}
			if (task.status === 2 && task.canComplete) {
				this.openCompletionDialog(task)
			}
		},
		isActionDisabled(task) {
			return Boolean(this.submittingTaskId) || task.status === 3 || task.status === 5
		},
		openCompletionDialog(task) {
			this.completionTask = task
			this.completionImagePath = ''
			this.completionImageUrl = ''
			this.completionRemark = ''
			this.completionVisible = true
		},
		closeCompletionDialog() {
			if (this.completionSubmitting) {
				return
			}
			this.completionVisible = false
			this.completionTask = null
			this.completionImagePath = ''
			this.completionImageUrl = ''
			this.completionRemark = ''
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
					// 用户重新选择图片后清空旧上传结果，确保完成记录引用当前预览图片。
					this.completionImageUrl = ''
				}
			})
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
				// 先取得服务端凭证地址，再将其随完成命令写入不可变操作记录。
				if (!this.completionImageUrl) {
					const uploadResult = await uploadFarmTaskCompletionImage(taskId, this.completionImagePath)
					this.completionImageUrl = uploadResult.imageUrl
				}
				const updatedTask = await completeFarmTask(taskId, {
					requestId: this.createRequestId('complete', taskId),
					actionContent: remark,
					feedbackDetail: remark,
					resultStatus: 1,
					attachments: JSON.stringify([this.completionImageUrl])
				})
				this.replaceTask(updatedTask)
				this.closeCompletionAfterSuccess()
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
		closeCompletionAfterSuccess() {
			// 成功后直接清理弹框数据，避免 submitting 状态阻止普通关闭方法。
			this.completionVisible = false
			this.completionTask = null
			this.completionImagePath = ''
			this.completionImageUrl = ''
			this.completionRemark = ''
		},
		openTaskDetail(task) {
			if (!task || !task.id) {
				uni.showToast({ title: '农事任务信息不完整', icon: 'none' })
				return
			}
			// 任务卡片统一进入独立农事记录页，保留页面栈以支持系统返回手势。
			uni.navigateTo({
				url: `/pages/service/task_record?id=${encodeURIComponent(task.id)}`
			})
		},
		closeTaskDetail() {
			this.detailVisible = false
			this.detailTask = null
			this.timeline = []
		},
		fetchTimeline(taskId) {
			this.timelineLoading = true
			return getFarmTaskTimeline(taskId)
				.then((records) => {
					this.timeline = records
				})
				.finally(() => {
					this.timelineLoading = false
				})
		},
		openAction(mode) {
			this.actionMode = mode
			this.actionContent = ''
			this.progressPercent = 50
			this.actionVisible = true
		},
		closeAction() {
			if (!this.actionSubmitting) {
				this.actionVisible = false
			}
		},
		submitAction() {
			if (!this.detailTask || this.actionSubmitting) {
				return
			}
			const content = this.actionContent.trim()
			if (!content) {
				uni.showToast({ title: '请填写执行说明', icon: 'none' })
				return
			}
			this.actionSubmitting = true
			const taskId = this.detailTask.id
			const requestId = this.createRequestId(this.actionMode, taskId)
			const actionPromise = this.actionMode === 'progress'
				? submitFarmTaskProgress(taskId, {
					requestId,
					actionContent: content,
					progressPercent: this.progressPercent
				})
				: completeFarmTask(taskId, {
					requestId,
					actionContent: content,
					feedbackDetail: content,
					resultStatus: 1
				})
			actionPromise
				.then((updatedTask) => {
					this.replaceTask(updatedTask)
					this.detailTask = updatedTask
					this.actionVisible = false
					uni.showToast({ title: this.actionMode === 'progress' ? '进度已保存' : '任务已完成', icon: 'none' })
					return this.fetchTimeline(taskId)
				})
				.finally(() => {
					this.actionSubmitting = false
				})
		},
		// 用后端返回的完整快照替换列表项，避免前端自行推断状态或实际时间。
		replaceTask(updatedTask) {
			this.tasks = this.tasks.map((item) => item.id === updatedTask.id ? updatedTask : item)
		},
		// 请求号包含动作、任务、时间和随机数，在不引入第三方依赖的情况下满足幂等需要。
		createRequestId(action, taskId) {
			return `${action}-${taskId}-${Date.now()}-${Math.random().toString(16).slice(2)}`
		},
		handleProgressChange(event) {
			this.progressPercent = Number(event.detail.value || 0)
		},
		resolveTimelineAction(record) {
			return { 1: '开始执行', 2: '完成任务', 3: '进度反馈', 4: '优化建议' }[record.actionType] || '执行记录'
		},
		resolveTimelineContent(record) {
			const content = record.feedbackDetail || record.actionContent || '-'
			return record.progressPercent == null ? content : `${record.progressPercent}% · ${content}`
		},
		resolveActionText(task) {
			if (task.status === 1 || task.status === 4) {
				return this.submittingTaskId === task.id ? '执行中' : '执行任务'
			}
			if (task.status === 2) {
				return '完成任务'
			}
			if (task.status === 3) {
				return '已完成'
			}
			return task.statusName
		},
		formatPlotName(plotName) {
			// 任务卡片中的地块标签空间有限，优先展示“3号地”这类核心编号。
			const name = plotName || '-'
			const matched = name.match(/[A-Za-z0-9一二三四五六七八九十百]+号(?:地块|地)?/)
			return matched ? matched[0].replace('地块', '地') : name
		},
		todayText() {
			const date = new Date()
			const month = String(date.getMonth() + 1).padStart(2, '0')
			const day = String(date.getDate()).padStart(2, '0')
			return `${date.getFullYear()}-${month}-${day}`
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.task-list-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
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

.nav-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 16px;
	line-height: 58rpx;
	color: #ffffff;
}

.nav-icon,
.nav-placeholder {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
}

.nav-icon {
	text-align: left;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.task-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 40rpx;
}

.date-scroll {
	width: 100%;
	white-space: nowrap;
}

.date-inner {
	display: flex;
	align-items: center;
	width: max-content;
	padding: 6rpx 0 26rpx;
}

.date-card {
	box-sizing: border-box;
	display: flex;
	flex-shrink: 0;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 114rpx;
	height: 128rpx;
	margin-right: 36rpx;
	border-radius: 12rpx;
	background-color: #ffffff;
	color: #C8C8C8;
}

.date-card:last-child {
	margin-right: 0;
}

.date-card.active {
	background-color: #5AB8AD;
	color: #ffffff;
}

.date-week,
.date-day,
.date-count {
	font-size: 14px;
	font-weight: normal;
	line-height: 1.35;
	color: inherit;
}

.date-day {
	margin-top: 4rpx;
}

.task-tabs {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 70rpx;
}

.task-tab {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 70rpx;
	font-size: 14px;
	line-height: 70rpx;
	color: #000000;
}

.task-tab.active::after {
	position: absolute;
	left: 50%;
	bottom: 0;
	width: 50rpx;
	height: 6rpx;
	border-radius: 6rpx;
	background-color: #5AB8AD;
	content: "";
	transform: translateX(-50%);
}

.task-list {
	padding-top: 24rpx;
}

.task-card {
	box-sizing: border-box;
	margin-bottom: 24rpx;
	padding: 26rpx 28rpx;
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(38, 91, 84, 0.05);
}

.task-card-pressed {
	background-color: #f2faf8;
}

.task-main {
	display: flex;
	align-items: flex-start;
}

.task-icon-wrap {
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

.task-icon {
	font-size: 42rpx;
	line-height: 42rpx;
	color: #5AB8AD;
}

.task-info {
	flex: 1;
	min-width: 0;
}

.task-title-row,
.task-meta-row,
.task-bottom-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.task-title {
	max-width: 330rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 14px;
	line-height: 1.35;
	color: #000000;
}

.task-status {
	flex-shrink: 0;
	margin-left: 16rpx;
	font-size: 12px;
	line-height: 1.35;
	color: #7d8986;
}

.task-status.status-2 { color: #168577; }
.task-status.status-3 { color: #66817a; }
.task-status.status-4 { color: #c04b3b; }

.task-meta-row {
	justify-content: flex-start;
	margin-top: 22rpx;
}

.plot-tag {
	box-sizing: border-box;
	min-width: 86rpx;
	height: 42rpx;
	padding: 0 12rpx;
	border-radius: 6rpx;
	text-align: center;
	font-size: 12px;
	line-height: 42rpx;
	color: #ffffff;
	background-color: #5AB8AD;
}

.crop-name {
	margin-left: 28rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 12px;
	line-height: 42rpx;
	color: #000000;
}

.task-bottom-row {
	margin-top: 22rpx;
	/* 底部时间需要与左侧圆形任务图标左边对齐，因此向左补回图标宽度和间距。 */
	margin-left: -96rpx;
	width: calc(100% + 96rpx);
}

.task-time {
	font-size: 12px;
	line-height: 1.3;
	color: #89938f;
}

.execute-btn {
	box-sizing: border-box;
	min-width: 116rpx;
	height: 58rpx;
	margin: 0;
	padding: 0 18rpx;
	border-radius: 8rpx;
	border: 0;
	text-align: center;
	font-size: 12px;
	line-height: 58rpx;
	color: #ffffff;
	background-color: #5AB8AD;
}

.execute-btn::after {
	border: 0;
}

.execute-btn.disabled {
	opacity: 0.78;
}

.execute-btn.completed {
	color: #ffffff;
	background-color: #b9c0be;
}

.state-text {
	box-sizing: border-box;
	margin-top: 110rpx;
	padding: 40rpx 20rpx;
	text-align: center;
	font-size: 14px;
	line-height: 1.4;
	color: #C8C8C8;
}

/* 执行详情沿用页面现有白色卡片和青绿色主色，仅在需要时覆盖显示。 */
.detail-mask {
	position: fixed;
	z-index: 1000;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	display: flex;
	align-items: flex-end;
	background: rgba(0, 0, 0, 0.36);
}

.detail-panel {
	box-sizing: border-box;
	width: 100%;
	max-height: 82vh;
	overflow-y: auto;
	padding: 34rpx 36rpx calc(34rpx + env(safe-area-inset-bottom));
	border-radius: 24rpx 24rpx 0 0;
	background: #ffffff;
}

.detail-title-row,
.detail-actions,
.action-buttons {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.detail-title,
.detail-subtitle {
	display: block;
}

.detail-title {
	font-size: 17px;
	font-weight: 600;
	color: #111111;
}

.detail-subtitle,
.detail-close {
	margin-top: 10rpx;
	font-size: 13px;
	color: #777777;
}

.detail-close {
	min-width: 88rpx;
	min-height: 88rpx;
	margin-top: 0;
	text-align: right;
	line-height: 88rpx;
}

.detail-meta {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 18rpx;
	margin-top: 24rpx;
	padding: 24rpx;
	border-radius: 12rpx;
	background: #f7f7f7;
}

.detail-meta text {
	font-size: 12px;
	line-height: 1.5;
	color: #555555;
}

.detail-actions {
	margin-top: 24rpx;
}

.secondary-action,
.primary-action,
.action-button,
.cancel-action {
	flex: 1;
	height: 88rpx;
	margin: 0;
	border-radius: 10rpx;
	font-size: 14px;
	line-height: 88rpx;
}

.secondary-action,
.action-button.cancel,
.cancel-action {
	border: 1px solid #5AB8AD;
	color: #5AB8AD;
	background: #ffffff;
}

.primary-action,
.action-button.confirm {
	border: 1px solid #5AB8AD;
	color: #ffffff;
	background: #5AB8AD;
}

.action-form {
	margin-top: 24rpx;
	padding: 24rpx;
	border-radius: 12rpx;
	background: #f7f7f7;
}

.action-title,
.timeline-heading {
	display: block;
	margin-bottom: 18rpx;
	font-size: 14px;
	font-weight: 600;
	color: #222222;
}

.progress-label {
	display: block;
	font-size: 13px;
	color: #555555;
}

.progress-field text {
	display: block;
	font-size: 13px;
	color: #555555;
}

.action-input,
.action-form textarea,
.action-form .action-content,
.action-textarea {
	box-sizing: border-box;
	width: 100%;
	min-height: 150rpx;
	margin: 18rpx 0;
	padding: 18rpx;
	border: 1px solid #e5e5e5;
	border-radius: 10rpx;
	font-size: 14px;
	line-height: 1.5;
	background: #ffffff;
}

.timeline-scroll,
.timeline-list {
	max-height: 360rpx;
	margin-top: 26rpx;
}

.timeline-empty {
	padding: 36rpx 0;
	text-align: center;
	font-size: 13px;
	color: #999999;
}

.timeline-item {
	position: relative;
	margin-left: 10rpx;
	padding: 0 0 28rpx 30rpx;
	border-left: 2rpx solid #d9efec;
}

.timeline-dot {
	position: absolute;
	left: -9rpx;
	top: 7rpx;
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
	background: #5AB8AD;
}

.timeline-title,
.timeline-action,
.timeline-content,
.timeline-time {
	display: block;
}

/* 完成弹框保持单一主操作，上传区域本身提供预览和清晰的可点击反馈。 */
.completion-mask {
	position: fixed;
	z-index: 1200;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 36rpx;
	background-color: rgba(20, 34, 31, 0.48);
}

.completion-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 640rpx;
	padding: 36rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 20rpx 56rpx rgba(0, 0, 0, 0.16);
}

.completion-title,
.completion-subtitle {
	display: block;
}

.completion-title {
	font-size: 18px;
	font-weight: 600;
	color: #1f2b28;
}

.completion-subtitle {
	margin-top: 12rpx;
	font-size: 13px;
	line-height: 1.5;
	color: #65736f;
}

.evidence-uploader {
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: 260rpx;
	margin-top: 28rpx;
	overflow: hidden;
	border: 2rpx dashed #8ecfc7;
	border-radius: 16rpx;
	background-color: #f3fbf9;
}

.evidence-uploader-pressed {
	background-color: #e8f7f4;
}

.evidence-preview {
	width: 100%;
	height: 100%;
}

.evidence-add-icon {
	font-size: 46rpx;
	color: #45ab9f;
}

.evidence-label {
	margin-top: 12rpx;
	font-size: 14px;
	font-weight: 500;
	color: #277e73;
}

.evidence-hint {
	margin-top: 8rpx;
	font-size: 11px;
	color: #7f918c;
}

.completion-remark {
	box-sizing: border-box;
	width: 100%;
	height: 128rpx;
	margin-top: 24rpx;
	padding: 18rpx 20rpx;
	border: 1px solid #dde6e3;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 1.5;
	color: #26332f;
	background-color: #ffffff;
}

.completion-actions {
	display: flex;
	align-items: center;
	gap: 20rpx;
	margin-top: 28rpx;
}

.completion-cancel,
.completion-confirm {
	flex: 1;
	height: 88rpx;
	margin: 0;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 88rpx;
}

.completion-cancel {
	border: 1px solid #9bb0aa;
	color: #52645f;
	background-color: #ffffff;
}

.completion-confirm {
	border: 1px solid #5ab8ad;
	color: #ffffff;
	background-color: #5ab8ad;
}

.completion-cancel::after,
.completion-confirm::after {
	border: 0;
}

.completion-cancel[disabled],
.completion-confirm[disabled] {
	opacity: 0.58;
}

.timeline-title,
.timeline-action {
	font-size: 14px;
	font-weight: 600;
	color: #222222;
}

.timeline-content {
	margin-top: 8rpx;
	font-size: 13px;
	line-height: 1.5;
	color: #555555;
}

.timeline-time {
	margin-top: 6rpx;
	font-size: 12px;
	color: #999999;
}

@media screen and (min-width: 768px) {
	.task-list-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
