<template>
	<view class="records-page">
		<view class="page-hero">
			<view class="navbar">
				<view class="nav-action" hover-class="nav-action-pressed" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon"></text>
				</view>
				<text class="nav-title">所有操作记录</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="page-content" scroll-y :show-scrollbar="false">
			<view class="section-head">
				<view class="section-title-wrap">
					<text class="section-title">操作记录</text>
					<text class="record-count">{{ records.length }} 条</text>
				</view>
				<!-- 文案表示点击后将执行的排序方式，默认优先展示最新操作。 -->
				<view
					class="sort-action"
					hover-class="sort-action-pressed"
					:hover-stay-time="80"
					role="button"
					:aria-label="sortActionText"
					@tap="toggleTimeSort"
				>
					<text class="section-hint">{{ sortActionText }}</text>
				</view>
			</view>

			<view v-if="loading" class="state-card">
				<view class="loading-dot"></view>
				<text class="state-text">正在加载操作记录...</text>
			</view>

			<view v-else-if="loadFailed" class="state-card">
				<text class="state-title">操作记录加载失败</text>
				<text class="state-text">请检查网络连接后重试</text>
				<view class="retry-button" hover-class="retry-button-pressed" @tap="loadRecords">重新加载</view>
			</view>

			<view v-else-if="records.length" class="timeline-card">
				<view class="timeline-item" v-for="(record, index) in sortedRecords" :key="record.id">
					<view class="timeline-axis">
						<view class="timeline-dot" :class="actionClass(record.actionType)"></view>
						<view v-if="index < sortedRecords.length - 1" class="timeline-line"></view>
					</view>
					<view class="record-body">
						<view class="record-title-row">
							<text class="record-action">{{ record.actionName }}</text>
							<view class="record-badges">
								<text v-if="record.progressPercent !== null" class="progress-badge">{{ record.progressPercent }}%</text>
								<text v-if="record.statusName" class="status-badge" :class="statusClass(record.afterStatus)">
									{{ record.statusName }}
								</text>
							</view>
						</view>
						<text class="task-title">{{ record.taskTitle }}</text>
						<text class="record-content">{{ recordDescription(record) }}</text>
						<protected-image
							v-if="record.attachmentUrls && record.attachmentUrls.length"
							class="record-evidence"
							:src="record.attachmentUrls[0]"
							:alt="`${record.taskTitle}操作图片`"
							mode="aspectFill"
							hover-class="record-evidence-pressed"
							:hover-stay-time="80"
							role="button"
							aria-label="预览操作记录图片"
							@tap="previewRecordImages(record)"
						></protected-image>
						<view class="record-meta">
							<text class="operator-name">{{ record.operatorName || '系统记录' }}</text>
							<text class="execute-time">{{ record.executeTimeText || '-' }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-else class="state-card">
				<text class="state-title">暂无操作记录</text>
				<text class="state-text">该地块创建农事任务后，操作过程会显示在这里</text>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getPlotFarmTaskTimeline } from '@/api/taskList.js'
import { sortRecordsByTime } from '@/utils/taskScroll.js'

export default {
	data() {
		return {
			plotId: null,
			records: [],
			// 默认倒序排列，进入页面时优先看到最近发生的操作。
			timeAscending: false,
			loading: true,
			loadFailed: false
		}
	},
	computed: {
		sortedRecords() {
			return sortRecordsByTime(this.records, this.timeAscending)
		},
		// 当前文案表示下一次点击将采用的排序方向。
		sortActionText() {
			return this.timeAscending ? '按时间顺序排列' : '按时间倒序排列'
		}
	},
	onLoad(options) {
		this.plotId = options && options.plotId ? options.plotId : null
		this.loadRecords()
	},
	methods: {
		toggleTimeSort() {
			this.timeAscending = !this.timeAscending
		},
		previewRecordImages(record) {
			const urls = record && Array.isArray(record.attachmentUrls) ? record.attachmentUrls.filter(Boolean) : []
			if (urls.length) uni.previewImage({ current: urls[0], urls })
		},
		async loadRecords() {
			if (!this.plotId) {
				this.loading = false
				this.loadFailed = true
				uni.showToast({ title: '地块ID不能为空', icon: 'none' })
				return
			}
			this.loading = true
			this.loadFailed = false
			try {
				this.records = await getPlotFarmTaskTimeline(this.plotId)
			} catch (error) {
				this.records = []
				this.loadFailed = true
			} finally {
				this.loading = false
			}
		},
		handleBack() {
			uni.navigateBack()
		},
		statusClass(status) {
			return `status-${Number(status || 0)}`
		},
		actionClass(actionType) {
			return `action-${Number(actionType || 0)}`
		},
		recordDescription(record) {
			return record.feedbackDetail || record.actionContent || '已记录本次农事操作'
		}
	}
}
</script>

<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100vh;
	overflow: hidden;
	background-color: #f4f7f6;
}

.records-page {
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

.nav-action { justify-content: flex-start; border-radius: 44rpx; }
.nav-action-pressed { background-color: rgba(255, 255, 255, 0.16); }
.nav-icon { font-size: 36rpx; color: #ffffff; }
.nav-title {
	position: absolute;
	left: 100rpx;
	right: 100rpx;
	text-align: center;
	font-size: 18px;
	font-weight: 500;
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
	padding: 0 28rpx;
}

.section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 100rpx;
	padding: 0 4rpx;
}

.section-title-wrap { display: flex; align-items: baseline; }
.section-title { font-size: 18px; font-weight: 600; color: #17211f; }
.record-count { margin-left: 14rpx; font-size: 12px; color: #5f6e69; }
.sort-action {
	display: flex;
	align-items: center;
	min-height: 88rpx;
	margin: -20rpx -12rpx;
	padding: 0 12rpx;
	border-radius: 44rpx;
}
.sort-action-pressed { background-color: #e7f5f2; }
.section-hint { font-size: 12px; color: #6f7e79; }

.timeline-card,
.state-card {
	box-sizing: border-box;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 12rpx 32rpx rgba(28, 94, 86, 0.09);
}

.timeline-card { padding: 30rpx 28rpx 12rpx; }
.timeline-item { display: flex; min-height: 180rpx; }
.timeline-axis { position: relative; flex-shrink: 0; width: 34rpx; margin-right: 18rpx; }
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

.record-body { flex: 1; min-width: 0; padding-bottom: 32rpx; }
.record-title-row,
.record-meta { display: flex; align-items: center; justify-content: space-between; }
.record-action { font-size: 15px; font-weight: 600; color: #263431; }
.record-badges { display: flex; align-items: center; gap: 10rpx; }
.progress-badge,
.status-badge { padding: 5rpx 12rpx; border-radius: 14rpx; font-size: 11px; line-height: 1.2; }
.progress-badge { color: #147a6d; background-color: #e0f5f1; }
.status-badge { color: #64748b; background-color: #eef2f4; }
.status-1 { color: #7c6514; background-color: #fff7d6; }
.status-2 { color: #147a6d; background-color: #dcf6f1; }
.status-3 { color: #256d3b; background-color: #e1f5e7; }
.status-4 { color: #a34a13; background-color: #fff0d7; }
.status-5 { color: #64748b; background-color: #eef2f4; }
.task-title {
	display: block;
	margin-top: 10rpx;
	font-size: 12px;
	font-weight: 500;
	color: #317f75;
}
.record-content {
	display: block;
	margin-top: 10rpx;
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
.record-evidence-pressed { opacity: 0.82; }
.record-meta { margin-top: 14rpx; font-size: 12px; line-height: 1.3; }
.operator-name { color: #52615c; }
.execute-time { color: #74827e; font-variant-numeric: tabular-nums; }

.state-card {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-height: 360rpx;
	padding: 52rpx 32rpx;
	text-align: center;
}
.state-title { font-size: 16px; font-weight: 600; color: #35433f; }
.state-text { margin-top: 14rpx; font-size: 13px; line-height: 1.5; color: #687772; }
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
@keyframes loading-rotate { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .loading-dot { animation: none; } }
@media screen and (min-width: 768px) { .records-page { width: 750rpx; margin: 0 auto; } }
</style>
