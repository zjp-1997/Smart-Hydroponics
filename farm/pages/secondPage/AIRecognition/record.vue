<template>
	<view class="record-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">识别记录</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="record-content" scroll-y>
			<view class="record-card" v-for="record in records" :key="record.id" @tap="handleRecordDetail(record)">
				<protected-image class="record-image" :src="record.image" mode="aspectFill"></protected-image>
				<view class="record-info">
					<view class="record-head">
						<text class="record-title">{{ record.type }}</text>
						<text class="record-time">{{ record.time }}</text>
					</view>
					<text class="record-result">{{ record.result }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import {
	cacheRecognitionResult,
	getRecognitionRecords
} from '@/api/strategyList.js'

export default {
	data() {
		return {
			records: []
		}
	},
	onLoad() {
		// 用户点击“识别记录”进入页面后立即加载后端数据。
		this.loadRecognitionRecords()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		loadRecognitionRecords() {
			getRecognitionRecords()
				.then((records) => {
					this.records = records
				})
				.catch(() => {
					this.records = []
				})
		},
		handleRecordDetail(record) {
			// 列表接口已返回结果页所需字段，点击记录时直接缓存并复用识别结果页展示逻辑。
			cacheRecognitionResult(record)
			uni.navigateTo({
				url: `/pages/secondPage/AIRecognition/index?recordId=${encodeURIComponent(record.recordId)}`
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

.record-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.page-hero {
	box-sizing: border-box;
	height: 300rpx;
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

.record-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 46rpx;
}

.record-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 190rpx;
	margin-bottom: 48rpx;
	padding: 24rpx 44rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
}

.record-image {
	flex-shrink: 0;
	width: 166rpx;
	height: 140rpx;
	border-radius: 8rpx;
	background-color: #f7f7f7;
}

.record-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	justify-content: center;
	min-width: 0;
	margin-left: 42rpx;
}

.record-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-width: 0;
}

.record-title {
	flex-shrink: 0;
	font-size: 16px;
	line-height: 1.4;
	color: #000000;
}

.record-time {
	min-width: 0;
	margin-left: 24rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 14px;
	line-height: 1.4;
	color: #C8C8C8;
}

.record-result {
	margin-top: 24rpx;
	font-size: 16px;
	line-height: 1.4;
	color: #C8C8C8;
}

@media screen and (min-width: 768px) {
	.record-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
