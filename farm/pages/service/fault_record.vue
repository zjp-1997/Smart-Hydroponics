<template>
	<view class="maintenance-page">
		<view class="page-hero">
			<view class="navbar">
				<!-- 返回按钮保留 44px 以上触控区域，与农事记录页的导航方式一致。 -->
				<view class="nav-action" hover-class="pressed" aria-label="返回故障列表" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon"></text>
				</view>
				<text class="nav-title">维护记录</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="record-content" scroll-y :show-scrollbar="false">
			<view v-if="loading" class="state-card">正在加载维护记录...</view>
			<template v-else-if="fault">
				<!-- 故障快照与下方生命周期记录分开呈现，便于先确认设备和故障。 -->
				<view class="fault-summary">
					<view class="device-icon"><text class="iconfont icon-guzhangchuli"></text></view>
					<view class="summary-main">
						<view class="summary-title-row">
							<text class="summary-title">{{ fault.faultName }}</text>
							<text class="status-badge" :class="`status-${fault.status}`">{{ fault.statusName }}</text>
						</view>
						<view class="summary-tags">
							<text class="plot-tag">{{ fault.plotName }}</text>
							<text class="device-name">{{ fault.deviceName }}</text>
						</view>
						<text v-if="fault.faultDesc" class="summary-description">{{ fault.faultDesc }}</text>
						<view class="summary-meta"><text>发生时间</text><text>{{ fault.startTimeText }}</text></view>
					</view>
				</view>

				<view class="section-head">
					<view><text class="section-title">操作记录</text><text class="record-count">{{ records.length }} 条</text></view>
					<text class="section-hint">按发生时间排序</text>
				</view>

				<view class="timeline-card">
					<!-- 后端按故障生命周期返回节点，历史缺少接单时间时显示“时间未记录”。 -->
					<view v-for="(record, index) in records" :key="record.key" class="timeline-item">
						<view class="timeline-axis">
							<view class="timeline-dot" :class="`dot-${record.status}`"></view>
							<view v-if="index < records.length - 1" class="timeline-line"></view>
						</view>
						<view class="record-body">
							<view class="record-title-row">
								<text class="record-action">{{ record.actionName }}</text>
								<text class="record-status" :class="`status-${record.status}`">{{ statusName(record.status) }}</text>
							</view>
							<text class="record-description">{{ record.content }}</text>
							<!-- 完成凭证可点开查看原图；上传地址由 API 统一解析。 -->
							<protected-image v-if="record.imageUrl" class="record-evidence" :src="record.imageUrl" mode="aspectFill" aria-label="查看完成凭证" @tap="previewEvidence(record.imageUrl)"></protected-image>
							<view class="record-meta">
								<text>{{ record.operatorName || '系统记录' }}</text>
								<text>{{ record.executeTimeText === '-' ? '时间未记录' : record.executeTimeText }}</text>
							</view>
						</view>
					</view>
				</view>
			</template>
			<view v-else class="state-card">
				<text>维护记录加载失败</text>
				<view class="retry-button" hover-class="pressed" @tap="loadRecord">重新加载</view>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getDeviceFaultRecord } from '@/api/deviceFault.js'

export default {
	data() {
		return { faultId: null, fault: null, records: [], loading: true }
	},
	onLoad(options) {
		this.faultId = options && options.id ? options.id : null
		this.loadRecord()
	},
	methods: {
		async loadRecord() {
			if (!this.faultId) {
				this.loading = false
				uni.showToast({ title: '故障ID不能为空', icon: 'none' })
				return
			}
			this.loading = true
			try {
				// 每次进入页面读取服务端快照，确保状态和完成凭证与列表一致。
				const data = await getDeviceFaultRecord(this.faultId)
				this.fault = data.fault
				this.records = data.records
			} catch (error) {
				this.fault = null
				this.records = []
			} finally {
				this.loading = false
			}
		},
		handleBack() {
			uni.navigateBack()
		},
		statusName(status) {
			return { 0: '待处理', 1: '处理中', 2: '已处理', 3: '已关闭' }[status] || '未知状态'
		},
		previewEvidence(url) {
			uni.previewImage({ current: url, urls: [url] })
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page { height: 100vh; overflow: hidden; background-color: #f4f7f6; }
.maintenance-page { position: relative; height: 100vh; overflow: hidden; color: #1f2937; background: linear-gradient(180deg, #6fc4ba 0, #8ed2c9 210rpx, #f4f7f6 430rpx); }
.page-hero { box-sizing: border-box; height: calc(var(--status-bar-height) + 104rpx); padding: var(--status-bar-height) 24rpx 0; }
.navbar { position: relative; display: flex; align-items: center; justify-content: space-between; height: 88rpx; color: #fff; }
.nav-action, .nav-placeholder { display: flex; align-items: center; width: 88rpx; height: 88rpx; }
.nav-action { justify-content: flex-start; border-radius: 44rpx; }
.pressed { opacity: .72; }
.nav-icon { font-size: 36rpx; color: #fff; }
.nav-title { position: absolute; left: 100rpx; right: 100rpx; text-align: center; font-size: 18px; font-weight: 500; }
.record-content { position: absolute; top: calc(var(--status-bar-height) + 104rpx); bottom: 0; left: 0; right: 0; box-sizing: border-box; height: calc(100vh - var(--status-bar-height) - 104rpx); padding: 16rpx 28rpx 0; }
.fault-summary, .timeline-card, .state-card { box-sizing: border-box; border: 1rpx solid rgba(31, 41, 55, .04); border-radius: 24rpx; background: #fff; box-shadow: 0 12rpx 32rpx rgba(28, 94, 86, .09); }
.fault-summary { display: flex; align-items: flex-start; padding: 28rpx; }
.device-icon { display: flex; flex: 0 0 132rpx; align-items: center; justify-content: center; height: 132rpx; margin-right: 24rpx; border-radius: 18rpx; color: #53b7aa; background: #eef7f4; }
.device-icon .iconfont { font-size: 72rpx; }
.summary-main, .record-body { flex: 1; min-width: 0; }
.summary-title-row, .summary-meta, .record-title-row, .record-meta { display: flex; align-items: center; justify-content: space-between; }
.summary-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 17px; font-weight: 600; color: #17211f; }
.status-badge, .record-status { flex-shrink: 0; margin-left: 12rpx; padding: 6rpx 12rpx; border-radius: 18rpx; font-size: 11px; line-height: 1.2; }
.status-0 { color: #7c6514; background: #fff7d6; }
.status-1 { color: #147a6d; background: #dcf6f1; }
.status-2 { color: #256d3b; background: #e1f5e7; }
.status-3 { color: #64748b; background: #eef2f4; }
.summary-tags { display: flex; align-items: center; margin-top: 14rpx; }
.plot-tag { max-width: 180rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 6rpx 14rpx; border-radius: 8rpx; font-size: 12px; color: #fff; background: #57b8ac; }
.device-name { overflow: hidden; margin-left: 14rpx; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: #667570; }
.summary-description { display: -webkit-box; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; margin-top: 14rpx; font-size: 12px; line-height: 1.55; color: #667570; }
.summary-meta { margin-top: 16rpx; font-size: 12px; color: #6d7a76; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; margin: 38rpx 4rpx 20rpx; }
.section-title { font-size: 18px; font-weight: 600; color: #17211f; }
.record-count { margin-left: 14rpx; font-size: 12px; color: #6d7a76; }
.section-hint { font-size: 12px; color: #89938f; }
.timeline-card { padding: 30rpx 28rpx 12rpx; }
.timeline-item { display: flex; min-height: 166rpx; }
.timeline-axis { position: relative; flex: 0 0 34rpx; margin-right: 18rpx; }
.timeline-dot { position: relative; z-index: 2; box-sizing: border-box; width: 24rpx; height: 24rpx; margin-top: 5rpx; border: 6rpx solid #57b8ac; border-radius: 50%; background: #fff; }
.dot-0 { border-color: #d2a838; }
.dot-2 { border-color: #4f9d68; }
.timeline-line { position: absolute; top: 28rpx; bottom: -4rpx; left: 11rpx; width: 2rpx; background: #cfe7e3; }
.record-body { padding-bottom: 30rpx; }
.record-action { font-size: 15px; font-weight: 600; color: #263431; }
.record-description { display: block; margin-top: 12rpx; font-size: 13px; line-height: 1.55; color: #586661; }
.record-evidence { display: block; width: 180rpx; height: 132rpx; margin-top: 16rpx; border-radius: 12rpx; background: #edf3f1; }
.record-meta { margin-top: 14rpx; font-size: 12px; line-height: 1.3; color: #52615c; }
.record-meta text:last-child { color: #8a9692; }
.state-card { display: flex; align-items: center; justify-content: center; flex-direction: column; min-height: 300rpx; padding: 50rpx; font-size: 14px; color: #7a8883; }
.retry-button { margin-top: 24rpx; padding: 22rpx 40rpx; border-radius: 44rpx; color: #fff; background: #39a99b; }
.safe-bottom { height: calc(env(safe-area-inset-bottom) + 36rpx); }
@media screen and (min-width: 768px) { .maintenance-page { width: 750rpx; margin: 0 auto; } }
</style>
