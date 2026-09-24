<template>
	<view class="monitor-page">
		<view class="monitor-hero">
			<view class="monitor-nav">
				<text class="iconfont icon-fanhui monitor-back" @tap="handleBack"></text>
				<text class="monitor-title">实时监控</text>
				<view class="monitor-nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="monitor-content" scroll-y>
			<view class="search-card">
				<view class="search-box">
					<text class="search-icon"></text>
					<text class="search-placeholder">搜索</text>
					<view class="search-btn" @tap="handleSearch">搜索</view>
				</view>
			</view>

			<view class="monitor-card" v-for="monitor in monitors" :key="monitor.id" @tap="openMonitor(monitor)">
				<protected-image class="monitor-cover" :src="monitor.cover" mode="aspectFill"></protected-image>
				<view class="play-btn" @tap.stop="openMonitor(monitor)">
					<text class="iconfont icon-bofang_o play-icon"></text>
				</view>
				<view class="cover-mask">
					<view class="monitor-name">
						<text>{{ monitor.cropName }}</text>
						<text class="camera-name">{{ monitor.cameraName }}</text>
						<text class="camera-status" :class="{ online: monitor.status === '在线' }">{{ monitor.status }}</text>
					</view>
					<text class="iconfont icon-bianji edit-icon" @tap.stop="openMonitor(monitor)"></text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getMonitorList } from '@/api/monitorList.js'

export default {
	data() {
		return {
			// 列表结构保持原 UI 所需字段不变，内容改由 smart_plant 接口动态填充。
			monitors: [],
			plotId: null
		}
	},
	onLoad(options) {
		this.plotId = options && options.plotId ? Number(options.plotId) : null
	},
	onShow() {
		this.loadMonitors()
	},
	methods: {
		async loadMonitors() {
			try {
				this.monitors = await getMonitorList({ plotId: this.plotId })
			} catch (error) {
				// 请求层已统一展示网络或业务错误，页面保留原有布局且不使用伪造静态数据。
				this.monitors = []
			}
		},
		handleBack() {
			uni.navigateBack()
		},
		handleSearch() {
			// 当前页面没有改变原搜索框 UI，点击搜索时重新获取后端最新监控数据。
			this.loadMonitors()
		},
		openMonitor(monitor) {
			uni.navigateTo({
				url: `/pages/secondPage/monitor/detail?id=${encodeURIComponent(monitor.id)}`
			})
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.monitor-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	color: #000000;
	font-size: 14px;
	font-weight: normal;
}

.monitor-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.monitor-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.monitor-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.monitor-back,
.monitor-nav-placeholder {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
}

.monitor-back {
	text-align: left;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.monitor-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 38rpx;
}

.search-card {
	box-sizing: border-box;
	margin-bottom: 36rpx;
}

.search-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	height: 80rpx;
	border-radius: 42rpx;
	background-color: #ffffff;
	overflow: hidden;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.04);
}

.search-icon {
	position: relative;
	width: 30rpx;
	height: 30rpx;
	margin-left: 34rpx;
	margin-right: 22rpx;
}

.search-icon::before {
	content: "";
	position: absolute;
	left: 1rpx;
	top: 1rpx;
	width: 20rpx;
	height: 20rpx;
	border: 4rpx solid #6f6f7c;
	border-radius: 50%;
}

.search-icon::after {
	content: "";
	position: absolute;
	right: 1rpx;
	bottom: 3rpx;
	width: 13rpx;
	height: 4rpx;
	border-radius: 4rpx;
	background-color: #6f6f7c;
	transform: rotate(45deg);
	transform-origin: center;
}

.search-placeholder {
	flex: 1;
	font-size: 14px;
	line-height: 1.25;
	color: #C8C8C8;
}

.search-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 124rpx;
	height: 80rpx;
	border-radius: 42rpx;
	font-size: 14px;
	line-height: 1.25;
	color: #ffffff;
	background-color: #1BA291;
}

.monitor-card {
	position: relative;
	width: 100%;
	height: 294rpx;
	margin-bottom: 34rpx;
	overflow: hidden;
	border-radius: 12rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.06);
}

.monitor-cover {
	display: block;
	width: 100%;
	height: 294rpx;
}

.play-btn {
	position: absolute;
	left: 50%;
	top: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 78rpx;
	height: 78rpx;
	transform: translate(-50%, -50%);
}

.play-icon {
	font-size: 40px;
	line-height: 58rpx;
	color: #ffffff;
}

.cover-mask {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 62rpx;
	padding: 0 26rpx 0 38rpx;
	background-color: rgba(0, 0, 0, 0.58);
	color: #ffffff;
}

.monitor-name {
	display: flex;
	align-items: center;
	min-width: 0;
	font-size: 16px;
	line-height: 1.25;
	color: #ffffff;
}

.camera-name {
	margin-left: 34rpx;
}

.camera-status {
	margin-left: 26rpx;
}

.camera-status.online {
	color: #1BA291;
}

.edit-icon {
	flex-shrink: 0;
	font-size: 38rpx;
	line-height: 38rpx;
	color: #ffffff;
}

@media screen and (min-width: 768px) {
	.monitor-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
