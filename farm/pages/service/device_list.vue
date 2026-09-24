<template>
	<view class="device-page">
		<view class="device-hero">
			<view class="device-nav">
				<text class="iconfont icon-fanhui device-back" @tap="handleBack"></text>
				<text class="device-title">设备管理</text>
				<text
					class="iconfont icon-guzhangchuli device-fault-entry"
					aria-label="进入故障处理"
					hover-class="device-control-pressed"
					@tap="openFaultHandling"
				></text>
			</view>
		</view>

		<scroll-view class="device-content" scroll-y>
			<view class="filter-card">
				<view class="filter-head">
					<view class="card-title-wrap">
						<view class="title-bar"></view>
						<text class="filter-title">地块选择</text>
					</view>
					<view class="manage-plots" hover-class="control-pressed" @tap="openPlotManagement">
						<text>管理地块</text>
						<text class="iconfont icon-youjiantou manage-arrow" aria-hidden="true"></text>
					</view>
				</view>
				<view class="filter-controls">
					<view class="search-box">
						<text class="iconfont icon-icon_33 search-icon" aria-hidden="true"></text>
						<input
							class="search-input"
							name="device-search"
							:value="keyword"
							aria-label="搜索设备或地块"
							confirm-type="search"
							placeholder="搜索设备或地块"
							placeholder-class="search-placeholder"
							@input="handleKeywordInput"
							@confirm="applyFilters"
						/>
					</view>
					<picker class="plot-picker" mode="selector" :range="plotNames" :value="selectedPlotIndex" aria-label="选择地块" @change="handlePlotChange">
						<view class="plot-select">
							<text class="plot-select-text">{{ currentPlot }}</text>
							<text class="iconfont icon-shaixuanxiajian select-icon"></text>
						</view>
					</picker>
				</view>
			</view>

			<view v-if="loading" class="state-text">正在加载设备...</view>
			<view v-else-if="!hasDevices" class="state-text">暂无符合条件的设备</view>
			<view class="device-card" v-for="group in visibleDeviceGroups" :key="group.title">
				<view class="group-head">
					<view class="device-icon-wrap" :class="groupTone(group.title)">
						<text class="iconfont device-type-icon" :class="groupIcon(group.title)" aria-hidden="true"></text>
					</view>
					<view class="group-title-wrap">
						<text class="group-title">{{ group.title }}</text>
						<text class="device-count">{{ onlineDeviceCount(group) }} 台在线</text>
					</view>
					<view class="group-status" :class="statusClass(groupStatus(group))">
						<view class="status-dot"></view>
						<text>{{ groupStatus(group) }}</text>
					</view>
					<text class="iconfont icon-youjiantou group-arrow" aria-hidden="true"></text>
				</view>
				<view class="device-row" v-for="device in group.devices" :key="`${device.source}-${device.id}`">
					<view class="device-copy">
						<text class="device-name">{{ device.cropName }}</text>
						<view class="device-meta">
							<text class="iconfont icon-dingwei1 location-icon" aria-hidden="true"></text>
							<text class="plot-name">{{ device.plotName }}</text>
							<text class="health-tag" :class="statusClass(device.status)">{{ deviceHealthLabel(device) }}</text>
						</view>
					</view>
					<view
						class="switch-hit"
						role="switch"
						:aria-label="`${device.cropName}${device.enabled ? '在线' : '离线'}`"
						:aria-checked="device.enabled"
						:aria-disabled="device.updating"
						:class="{ disabled: device.updating }"
						hover-class="control-pressed"
						@tap="toggleDevice(device)"
					>
						<view class="switch-control" :class="{ active: device.enabled }">
							<view class="switch-dot"></view>
						</view>
					</view>
				</view>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getDeviceList, updateDeviceOnlineStatus } from '@/api/deviceList.js'
import { getAllPlotList } from '@/api/plotList.js'
import { filterDeviceGroups } from '@/utils/deviceFilters.js'

export default {
	data() {
		return {
			keyword: '',
			selectedPlotIndex: 0,
			plotOptions: [{ id: null, name: '全部地块' }],
			allDeviceGroups: [],
			deviceGroups: [],
			loading: false
		}
	},
	computed: {
		plotNames() {
			return this.plotOptions.map((plot) => plot.name)
		},
		currentPlot() {
			const plot = this.plotOptions[this.selectedPlotIndex]
			return plot ? plot.name : '全部地块'
		},
		visibleDeviceGroups() {
			return this.deviceGroups.filter((group) => group.devices.length)
		},
		hasDevices() {
			return this.visibleDeviceGroups.length > 0
		}
	},
	onLoad() {
		this.initPage()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		// 从设备管理页顶部进入故障处理，保持业务入口与设备上下文相邻。
		openFaultHandling() {
			uni.navigateTo({ url: '/pages/service/fault_handling' })
		},
		openPlotManagement() {
			uni.navigateTo({ url: '/pages/secondPage/plot/plot_list' })
		},
		initPage() {
			this.loading = true
			Promise.all([getDeviceList(), getAllPlotList().catch(() => [])])
				.then(([groups, plots]) => {
					this.allDeviceGroups = groups
					this.plotOptions = [
						{ id: null, name: '全部地块' },
						...plots.map((plot) => ({ id: plot.id, name: plot.plotName }))
					]
					this.applyFilters()
				})
				.catch(() => {
					this.allDeviceGroups = []
					this.deviceGroups = []
				})
				.finally(() => {
					this.loading = false
				})
		},
		handleKeywordInput(event) {
			this.keyword = event.detail.value
			this.applyFilters()
		},
		handlePlotChange(event) {
			this.selectedPlotIndex = Number(event.detail.value || 0)
			this.applyFilters()
		},
		applyFilters() {
			const selectedPlot = this.plotOptions[this.selectedPlotIndex]
			this.deviceGroups = filterDeviceGroups(
				this.allDeviceGroups,
				this.keyword,
				selectedPlot && selectedPlot.id
			)
		},
		groupIcon(title) {
			return {
				'补光灯': 'icon-buguangdeng',
				'水泵': 'icon-icon-shebeishuliang-shuibengxitong',
				'摄像头': 'icon-shexiangtou',
				'环境传感器': 'icon-a-47-wendu',
				'水质传感器': 'icon-line-094'
			}[title] || 'icon-shebeiguanli'
		},
		groupTone(title) {
			return {
				'补光灯': 'tone-light',
				'水泵': 'tone-pump',
				'摄像头': 'tone-camera',
				'环境传感器': 'tone-environment',
				'水质传感器': 'tone-water'
			}[title] || 'tone-environment'
		},
		groupStatus(group) {
			if (group.devices.some((device) => device.status === '故障')) return '故障'
			if (group.devices.some((device) => device.status === '在线')) return '在线'
			return '离线'
		},
		onlineDeviceCount(group) {
			return group.devices.filter((device) => device.status === '在线').length
		},
		statusClass(status) {
			return `status-${status === '在线' ? 'online' : status === '故障' ? 'fault' : 'offline'}`
		},
		deviceHealthLabel(device) {
			return device.status === '在线' ? '正常' : device.status
		},
		async toggleDevice(device) {
			if (device.updating) return
			device.updating = true
			const targetEnabled = !device.enabled
			try {
				const updatedDevice = await updateDeviceOnlineStatus(device, targetEnabled)
				Object.assign(device, updatedDevice)
				uni.showToast({ title: targetEnabled ? '设备已上线' : '设备已离线', icon: 'success' })
			} finally {
				device.updating = false
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.device-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	color: #000000;
	font-size: 14px;
	font-weight: normal;
}

.device-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.device-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.device-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.device-back,
.device-fault-entry {
	position: relative;
	z-index: 2;
	width: 44px;
	height: 44px;
	line-height: 44px;
}

.device-back {
	margin-left: -12rpx;
	text-align: left;
	font-size: 18px;
	color: #ffffff;
}

.device-fault-entry {
	margin-right: -12rpx;
	text-align: right;
	font-size: 22px;
	color: #ffffff;
}

.device-control-pressed {
	opacity: 0.68;
}

.device-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 20rpx;
}

.filter-card,
.device-card {
	box-sizing: border-box;
	border: 1rpx solid rgba(78, 101, 117, 0.04);
	border-radius: 26rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 30rpx rgba(44, 76, 98, 0.07);
}

.filter-card {
	padding: 24rpx 30rpx 28rpx;
}

.device-card {
	margin-top: 20rpx;
	padding: 20rpx 28rpx 18rpx;
}

.filter-head,
.filter-controls,
.group-head,
.group-title-wrap,
.device-row,
.device-meta {
	display: flex;
	align-items: center;
}

.card-title-wrap {
	display: flex;
	align-items: center;
}

.title-bar {
	width: 8rpx;
	height: 34rpx;
	margin-right: 20rpx;
	border-radius: 12rpx;
	background-color: #12bba8;
}

.filter-title {
	font-size: 17px;
	font-weight: 600;
	line-height: 1.3;
	color: #18222d;
}

.filter-head {
	justify-content: space-between;
}

.manage-plots {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	min-width: 152rpx;
	height: 88rpx;
	margin: -18rpx -12rpx -18rpx 0;
	font-size: 12px;
	color: #7c8793;
}

.manage-arrow {
	margin-left: 12rpx;
	font-size: 22rpx;
	color: #9ba7b3;
}

.filter-controls {
	margin-top: 20rpx;
}

.search-box,
.plot-select {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	height: 76rpx;
	border: 1rpx solid #e3e9ef;
	border-radius: 14rpx;
	background-color: #f8fafc;
}

.search-box {
	flex: 1;
	min-width: 0;
	padding: 0 22rpx;
}

.search-icon {
	flex-shrink: 0;
	width: 32rpx;
	height: 32rpx;
	margin-right: 16rpx;
	text-align: center;
	font-size: 28rpx;
	line-height: 32rpx;
	color: #7f8b98;
}

.search-input {
	flex: 1;
	min-width: 0;
	height: 76rpx;
	font-size: 13px;
	line-height: 76rpx;
	color: #283747;
}

.search-placeholder {
	color: #98a2ad;
}

.device-page .plot-picker {
	flex-shrink: 0;
	width: 220rpx;
	height: 76rpx;
	margin-left: 18rpx;
}

.device-page .plot-select {
	justify-content: space-between;
	width: 100%;
	padding: 0 20rpx;
}

.device-page .plot-select-text {
	max-width: 158rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 13px;
	font-weight: 500;
	line-height: 1.25;
	color: #283747;
}

.device-page .select-icon {
	font-size: 20rpx;
	line-height: 1;
	color: #73808d;
}

.device-icon-wrap {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 80rpx;
	height: 80rpx;
	margin-right: 20rpx;
	border-radius: 50%;
}

.device-type-icon {
	font-size: 42rpx;
	line-height: 1;
}

.tone-light { color: #f2a900; background-color: #fff8df; }
.tone-pump { color: #1879d8; background-color: #eaf4ff; }
.tone-camera { color: #7040c8; background-color: #f1eaff; }
.tone-environment { color: #079d83; background-color: #ddf7f1; }
.tone-water { color: #1975dd; background-color: #e5f1ff; }

.group-title-wrap {
	flex: 1;
	min-width: 0;
}

.group-title {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 17px;
	font-weight: 600;
	line-height: 1.3;
	color: #182536;
}

.device-count {
	flex-shrink: 0;
	margin-left: 16rpx;
	font-size: 12px;
	color: #84909d;
}

.group-status {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	min-width: 92rpx;
	height: 42rpx;
	margin-left: 16rpx;
	padding: 0 14rpx;
	border-radius: 22rpx;
	font-size: 12px;
	color: #0a9b78;
	background-color: #e8f8f3;
}

.status-dot {
	width: 12rpx;
	height: 12rpx;
	margin-right: 10rpx;
	border-radius: 50%;
	background-color: currentColor;
}

.group-status.status-fault {
	color: #eb4d4b;
	background-color: #fff0ef;
}

.group-status.status-offline {
	color: #788490;
	background-color: #eef1f4;
}

.group-arrow {
	flex-shrink: 0;
	margin-left: 18rpx;
	font-size: 24rpx;
	color: #91a0ae;
}

.device-row {
	box-sizing: border-box;
	min-height: 92rpx;
	padding-left: 100rpx;
}

.device-row + .device-row {
	margin-top: 8rpx;
	padding-top: 12rpx;
	border-top: 1rpx solid #eef2f5;
}

.device-copy {
	flex: 1;
	min-width: 0;
}

.device-name {
	display: block;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 13px;
	font-weight: 500;
	line-height: 1.4;
	color: #263646;
}

.device-meta {
	min-width: 0;
	margin-top: 10rpx;
}

.location-icon {
	flex-shrink: 0;
	margin-right: 8rpx;
	font-size: 22rpx;
	color: #7d8c9c;
}

.plot-name {
	max-width: 170rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 11px;
	line-height: 1.3;
	color: #8793a0;
}

.health-tag {
	flex-shrink: 0;
	margin-left: 18rpx;
	padding: 5rpx 12rpx;
	border-radius: 7rpx;
	font-size: 11px;
	line-height: 1.2;
	color: #66717c;
	background-color: #f0f2f4;
}

.health-tag.status-online { color: #089c7b; background-color: #e4f7f2; }
.health-tag.status-fault { color: #e44543; background-color: #ffedec; }
.health-tag.status-offline { color: #6f7a85; background-color: #eef1f3; }

.control-pressed {
	opacity: 0.68;
}

.switch-hit.disabled {
	opacity: 0.55;
}

.switch-hit {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	flex-shrink: 0;
	width: 104rpx;
	height: 88rpx;
}

.switch-control {
	box-sizing: border-box;
	position: relative;
	width: 86rpx;
	height: 50rpx;
	border-radius: 28rpx;
	background-color: #dce2e8;
	transition: background-color 0.2s;
}

.state-text {
	box-sizing: border-box;
	margin-top: 20rpx;
	padding: 48rpx 24rpx;
	border-radius: 26rpx;
	background-color: #ffffff;
	text-align: center;
	font-size: 14px;
	line-height: 1.4;
	color: #737b7b;
	box-shadow: 0 10rpx 30rpx rgba(44, 76, 98, 0.07);
}

.switch-control.active {
	background-color: #08b8a5;
}

.switch-dot {
	position: absolute;
	left: 6rpx;
	top: 6rpx;
	width: 38rpx;
	height: 38rpx;
	border-radius: 50%;
	background-color: #ffffff;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.16);
	transition: transform 0.2s;
}

.switch-control.active .switch-dot {
	transform: translateX(36rpx);
}

.safe-bottom { height: calc(env(safe-area-inset-bottom) + 28rpx); }

@media (prefers-reduced-motion: reduce) {
	.switch-control,
	.switch-dot { transition: none; }
}

@media screen and (min-width: 768px) {
	.device-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
