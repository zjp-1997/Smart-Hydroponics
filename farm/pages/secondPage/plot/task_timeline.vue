<template>
	<view class="timeline-page">
		<view class="page-hero">
			<view class="navbar">
				<!-- 返回按钮使用 44px 触控区，避免小图标难以点击。 -->
				<view class="nav-action" hover-class="nav-action-pressed" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon"></text>
				</view>
				<text class="nav-title">地块农事</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="page-content" scroll-y :show-scrollbar="false">
			<view v-if="loading" class="state-card">
				<view class="loading-dot"></view>
				<text class="state-text">正在加载地块农事任务...</text>
			</view>

			<template v-else-if="!loadFailed">
				<!-- 地块摘要帮助用户确认当前查看范围，字段均来自地块详情接口。 -->
				<view class="plot-card">
					<protected-image class="plot-image" :src="plot.image" mode="aspectFill" @error="handlePlotImageError"></protected-image>
					<view class="plot-main">
						<text class="plot-name">{{ plot.farm }}</text>
						<text class="crop-name">当前作物：{{ plot.name }}</text>
						<view class="plot-meta">
							<text class="plot-tag">{{ tasks.length }} 项任务</text>
							<!-- 文案表示点击后的排序方式，当前列表默认按截止时间倒序展示。 -->
							<view
								class="sort-action"
								hover-class="sort-action-pressed"
								:hover-stay-time="80"
								role="button"
								:aria-label="sortActionText"
								@tap="toggleDeadlineSort"
							>
								<text class="sort-hint">{{ sortActionText }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="section-head">
					<view class="section-title-wrap">
						<text class="section-title">农事时间序列</text>
					</view>
					<view
						class="all-records-link"
						hover-class="all-records-link-pressed"
						:hover-stay-time="80"
						@tap="openAllRecords"
					>
						<text>查看所有操作记录</text>
						<text class="iconfont icon-youjiantou all-records-arrow"></text>
					</view>
				</view>

				<view v-if="tasks.length" class="timeline-list">
					<!-- 每个节点是一条真实农事任务，不把任务执行事件混入地块任务列表。 -->
					<view
						class="timeline-item"
						v-for="(task, index) in sortedTasks"
						:key="task.id"
						hover-class="timeline-item-pressed"
						:hover-stay-time="80"
						@tap="openTaskRecord(task)"
					>
						<view class="timeline-axis">
							<view class="timeline-dot" :class="statusClass(task.status)"></view>
							<view v-if="index < sortedTasks.length - 1" class="timeline-line"></view>
						</view>
						<view class="task-card">
							<protected-image
								class="task-image"
								:src="task.cropImage"
								mode="aspectFill"
								@error="handleTaskImageError(task)"
							></protected-image>
							<view class="task-main">
								<view class="task-title-row">
									<text class="task-title">{{ task.taskTitle }}</text>
									<text class="status-badge" :class="statusClass(task.status)">{{ task.statusName }}</text>
								</view>
								<view class="task-info-row">
									<text class="info-label">执行人</text>
									<text class="info-value">{{ task.executorName || '待领取' }}</text>
								</view>
								<view class="task-info-row">
									<text class="info-label">{{ timeLabel(task) }}</text>
									<text class="info-value time-value" :class="{ overdue: task.overdue && task.status !== 3 }">
										{{ timeText(task) || '-' }}
									</text>
								</view>
							</view>
							<text class="iconfont icon-youjiantou task-arrow"></text>
						</view>
					</view>
				</view>

				<view v-else class="empty-card">
					<view class="empty-icon"><text class="iconfont icon-renwu-"></text></view>
					<text class="empty-title">暂无农事任务</text>
					<text class="empty-desc">该地块创建任务后，将按截止时间显示在这里</text>
				</view>
			</template>

			<view v-else class="state-card error-card">
				<text class="state-title">农事任务加载失败</text>
				<text class="state-text">请检查网络连接后重新加载</text>
				<view class="retry-button" hover-class="retry-button-pressed" @tap="loadPageData">重新加载</view>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getPlotDetail } from '@/api/plotList.js'
import { getFarmTasksByPlot } from '@/api/taskList.js'
import { sortTasksByDeadline } from '@/utils/taskScroll.js'

const DEFAULT_CROP_IMAGE = '/static/lecttue.png'

function createEmptyPlot() {
	return { id: null, name: '暂无作物', farm: '未知地块', image: DEFAULT_CROP_IMAGE }
}

export default {
	data() {
		return {
			plotId: null,
			plot: createEmptyPlot(),
			tasks: [],
			// 默认 false 表示截止时间倒序，即最新日期优先展示。
			deadlineAscending: false,
			loading: true,
			loadFailed: false
		}
	},
	computed: {
		// 切换排序时不修改接口原始数据，避免多次点击产生不可预测的原地排序结果。
		sortedTasks() {
			return sortTasksByDeadline(this.tasks, this.deadlineAscending)
		},
		// 排序入口显示下一次点击将执行的动作，与用户指定的交互文案保持一致。
		sortActionText() {
			return this.deadlineAscending ? '按截止时间由远及近' : '按截止时间由近及远'
		}
	},
	onLoad(options) {
		this.plotId = options && options.plotId ? options.plotId : null
		this.loadPageData()
	},
	methods: {
		toggleDeadlineSort() {
			this.deadlineAscending = !this.deadlineAscending
		},
		async loadPageData() {
			if (!this.plotId) {
				this.loading = false
				this.loadFailed = true
				uni.showToast({ title: '地块ID不能为空', icon: 'none' })
				return
			}

			this.loading = true
			this.loadFailed = false
			try {
				// 地块摘要与任务列表互不依赖，并行请求可以减少页面等待时间。
				const [detail, tasks] = await Promise.all([
					getPlotDetail(this.plotId),
					getFarmTasksByPlot(this.plotId)
				])
				this.plot = detail.plot || createEmptyPlot()
				this.tasks = tasks
			} catch (error) {
				this.tasks = []
				this.loadFailed = true
			} finally {
				this.loading = false
			}
		},
		handleBack() {
			uni.navigateBack()
		},
		openTaskRecord(task) {
			if (!task || !task.id) {
				uni.showToast({ title: '农事任务信息不完整', icon: 'none' })
				return
			}
			// 单任务操作过程继续由既有记录页展示，避免两个页面职责重叠。
			uni.navigateTo({ url: `/pages/service/task_record?id=${encodeURIComponent(task.id)}` })
		},
		openAllRecords() {
			if (!this.plotId) {
				uni.showToast({ title: '地块信息尚未加载', icon: 'none' })
				return
			}
			uni.navigateTo({
				url: `/pages/secondPage/plot/operation_timeline?plotId=${encodeURIComponent(this.plotId)}`
			})
		},
		statusClass(status) {
			return `status-${Number(status || 0)}`
		},
		timeLabel(task) {
			if (task.status === 3 && task.actualEndText) return '完成时间'
			if (task.status === 2 && task.actualStartText) return '开始时间'
			return '截止时间'
		},
		timeText(task) {
			if (task.status === 3 && task.actualEndText) return task.actualEndText
			if (task.status === 2 && task.actualStartText) return task.actualStartText
			return task.deadlineText
		},
		handlePlotImageError() {
			if (this.plot.image !== DEFAULT_CROP_IMAGE) this.plot.image = DEFAULT_CROP_IMAGE
		},
		handleTaskImageError(task) {
			// 单条远程图片失效时仅替换该节点，其他任务不受影响。
			if (task && task.cropImage !== DEFAULT_CROP_IMAGE) task.cropImage = DEFAULT_CROP_IMAGE
		}
	}
}
</script>

<!-- 显式隔离时间序列样式，防止 plot-card、task-card 等通用类名影响首页。 -->
<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100vh;
	overflow: hidden;
	background-color: #f4f7f6;
}

.timeline-page {
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

.nav-action-pressed { background-color: rgba(255, 255, 255, 0.16); }
.nav-icon { font-size: 36rpx; color: #ffffff; }

.nav-title {
	position: absolute;
	left: 100rpx;
	right: 100rpx;
	text-align: center;
	font-size: 18px;
	color: #ffffff;
}

.page-content {
	position: absolute;
	top: calc(var(--status-bar-height) + 104rpx);
	bottom: 0;
	left: 0;
	right: 0;
	box-sizing: border-box;
	height: calc(100vh - var(--status-bar-height) - 104rpx);
	padding: 16rpx 28rpx 0;
}

.plot-card,
.state-card,
.empty-card {
	box-sizing: border-box;
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(31, 78, 71, 0.08);
}

.plot-card {
	display: flex;
	align-items: center;
	min-height: 170rpx;
	padding: 20rpx;
}

.plot-image {
	flex-shrink: 0;
	width: 124rpx;
	height: 124rpx;
	border-radius: 16rpx;
	background-color: #edf3f1;
}

.plot-main { flex: 1; min-width: 0; margin-left: 24rpx; }
.plot-name { display: block; font-size: 17px; font-weight: 600; color: #1f2937; }
.crop-name { display: block; margin-top: 12rpx; font-size: 13px; color: #66736f; }
.plot-meta { display: flex; align-items: center; flex-wrap: wrap; margin-top: 18rpx; gap: 12rpx; }
.plot-tag { padding: 6rpx 14rpx; border-radius: 8rpx; background-color: #5ab9ae; font-size: 12px; color: #ffffff; }
.sort-action {
	display: flex;
	align-items: center;
	min-height: 88rpx;
	margin: -22rpx -12rpx;
	padding: 0 12rpx;
	border-radius: 44rpx;
}
.sort-action-pressed { background-color: #e7f5f2; }
.sort-hint { font-size: 12px; color: #66736f; white-space: nowrap; }

.section-head {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	padding: 34rpx 8rpx 22rpx;
}

.section-title-wrap { display: flex; align-items: baseline; }
.section-title { font-size: 16px; font-weight: 600; color: #27332f; }
.all-records-link {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	min-width: 224rpx;
	height: 88rpx;
	margin: -26rpx -8rpx -24rpx 0;
	padding: 0 8rpx 0 20rpx;
	border-radius: 44rpx;
	box-sizing: border-box;
	font-size: 12px;
	color: #267f74;
}
.all-records-link-pressed { background-color: #e7f5f2; }
.all-records-arrow { margin-left: 6rpx; font-size: 22rpx; }

.timeline-list { padding-bottom: 8rpx; }
.timeline-item { display: flex; align-items: stretch; min-height: 204rpx; border-radius: 18rpx; }
.timeline-item-pressed .task-card { background-color: #f2faf8; }

.timeline-axis {
	position: relative;
	display: flex;
	flex-shrink: 0;
	justify-content: center;
	width: 44rpx;
}

.timeline-dot {
	position: relative;
	z-index: 2;
	box-sizing: border-box;
	width: 22rpx;
	height: 22rpx;
	margin-top: 32rpx;
	border: 6rpx solid #ffffff;
	border-radius: 50%;
	background-color: #8a9692;
	box-shadow: 0 0 0 4rpx #8a9692;
}

.timeline-dot.status-2 { background-color: #1ba291; box-shadow: 0 0 0 4rpx #1ba291; }
.timeline-dot.status-3 { background-color: #36a269; box-shadow: 0 0 0 4rpx #36a269; }
.timeline-dot.status-4 { background-color: #d97706; box-shadow: 0 0 0 4rpx #d97706; }
.timeline-dot.status-5 { background-color: #9ca3af; box-shadow: 0 0 0 4rpx #9ca3af; }

.timeline-line {
	position: absolute;
	top: 54rpx;
	bottom: -32rpx;
	width: 3rpx;
	background-color: #b9ddd8;
}

.task-card {
	position: relative;
	display: flex;
	flex: 1;
	min-width: 0;
	margin: 0 0 24rpx 12rpx;
	padding: 22rpx 48rpx 22rpx 20rpx;
	border-radius: 18rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(31, 78, 71, 0.07);
	transition: background-color 0.12s;
}

.task-image {
	flex-shrink: 0;
	width: 116rpx;
	height: 116rpx;
	border-radius: 14rpx;
	background-color: #edf3f1;
}

.task-main { flex: 1; min-width: 0; margin-left: 20rpx; }
.task-title-row { display: flex; align-items: center; justify-content: space-between; gap: 12rpx; }
.task-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 14px; font-weight: 500; color: #27332f; }

.status-badge {
	flex-shrink: 0;
	padding: 5rpx 12rpx;
	border-radius: 999rpx;
	background-color: #eef1f0;
	font-size: 11px;
	color: #59645f;
}

.status-badge.status-2 { background-color: #ddf4f0; color: #08796d; }
.status-badge.status-3 { background-color: #e4f5eb; color: #217a4a; }
.status-badge.status-4 { background-color: #fff0d7; color: #9a5200; }
.status-badge.status-5 { background-color: #ecefee; color: #59645f; }

.task-info-row { display: flex; align-items: center; margin-top: 16rpx; font-size: 12px; line-height: 1.35; }
.info-label { flex-shrink: 0; width: 116rpx; color: #66736f; }
.info-value { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #37423e; }
.time-value { font-variant-numeric: tabular-nums; }
.time-value.overdue { color: #a95700; }

.task-arrow {
	position: absolute;
	right: 16rpx;
	top: 50%;
	transform: translateY(-50%);
	font-size: 26rpx;
	color: #8a9692;
}

.state-card,
.empty-card { margin-top: 24rpx; padding: 80rpx 32rpx; text-align: center; }
.state-card { display: flex; flex-direction: column; align-items: center; }
.loading-dot { width: 24rpx; height: 24rpx; margin-bottom: 22rpx; border: 5rpx solid #cae8e4; border-top-color: #1ba291; border-radius: 50%; animation: spin 0.8s linear infinite; }
.state-title,
.empty-title { display: block; font-size: 16px; font-weight: 600; color: #27332f; }
.state-text,
.empty-desc { display: block; margin-top: 14rpx; font-size: 13px; line-height: 1.6; color: #66736f; }
.empty-icon { display: flex; align-items: center; justify-content: center; width: 88rpx; height: 88rpx; margin: 0 auto 22rpx; border-radius: 50%; background-color: #e7f5f2; color: #1ba291; font-size: 44rpx; }
.retry-button { display: flex; align-items: center; justify-content: center; min-width: 192rpx; height: 88rpx; margin-top: 28rpx; border-radius: 44rpx; background-color: #1ba291; color: #ffffff; }
.retry-button-pressed { opacity: 0.82; }
.safe-bottom { height: calc(env(safe-area-inset-bottom) + 32rpx); }

@keyframes spin { to { transform: rotate(360deg); } }

@media (prefers-reduced-motion: reduce) {
	.loading-dot { animation: none; }
}

@media screen and (min-width: 768px) {
	.timeline-page { width: 750rpx; margin: 0 auto; }
}
</style>
