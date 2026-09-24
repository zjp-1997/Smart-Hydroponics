<template>
	<view class="recognition-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">识别结果</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="result-content" scroll-y>
			<view class="section-block">
				<view class="section-title">
					<view class="title-bar"></view>
					<text class="section-title-text">识别类型</text>
				</view>
				<text class="type-value">{{ recognitionType }}</text>
			</view>

			<view class="section-block image-section">
				<view class="section-title">
					<view class="title-bar"></view>
					<text class="section-title-text">识别图片</text>
				</view>
				<protected-image class="result-image" :src="recognitionImage" mode="aspectFill"></protected-image>
			</view>

			<view class="section-block">
				<view class="section-title">
					<view class="title-bar"></view>
					<text class="section-title-text">识别结果</text>
				</view>
				<text class="stage-value">{{ recognitionResult }}</text>
				<view class="result-card">
					<text class="result-desc">
						{{ resultIntroduction }}
					</text>
				</view>
			</view>

			<view class="section-block notice-section">
				<view class="section-title">
					<view class="title-bar"></view>
					<text class="section-title-text">注意事项</text>
				</view>
				<view class="notice-list">
					<view class="notice-item" v-for="notice in notices" :key="notice.index">
						<text class="notice-index">{{ notice.index }}</text>
						<text class="notice-text">{{ notice.text }}</text>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getCachedRecognitionResult } from '@/api/strategyList.js'

export default {
	data() {
		return {
			recognitionType: '',
			recognitionImage: '',
			recognitionResult: '',
			resultIntroduction: '',
			notices: []
		}
	},
	onLoad(options = {}) {
		// 结果页展示上一页接口返回的数据，避免继续使用页面内静态识别结果。
		const result = getCachedRecognitionResult(options.recordId)
		if (result) {
			this.fillRecognitionResult(result)
			return
		}
		uni.showToast({
			title: '识别结果不存在',
			icon: 'none'
		})
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		fillRecognitionResult(result) {
			// 将接口字段转换为当前页面既有展示字段，不调整页面 UI 结构。
			this.recognitionType = result.recognitionType || ''
			this.recognitionImage = result.recognitionImage || ''
			this.recognitionResult = result.recognitionResult || ''
			this.resultIntroduction = result.resultIntroduction || ''
			this.notices = (result.precautions || []).map((text, index) => ({
				index: index + 1,
				text
			}))
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.recognition-page {
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

.result-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 118rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 32rpx 36rpx 50rpx;
}

.section-block {
	margin-bottom: 38rpx;
}

.section-title {
	display: flex;
	align-items: center;
	height: 48rpx;
}

.title-bar {
	flex-shrink: 0;
	width: 14rpx;
	height: 48rpx;
	margin-right: 22rpx;
	border-radius: 8rpx;
	background-color: #6FC4BA;
}

.section-title-text {
	font-size: 18px;
	line-height: 48rpx;
	color: #000000;
}

.type-value,
.stage-value {
	display: block;
	margin-top: 34rpx;
	margin-left: 50rpx;
	font-size: 18px;
	line-height: 1.4;
	color: #6FC4BA;
}

.image-section {
	margin-bottom: 26rpx;
}

.result-image {
	display: block;
	width: 274rpx;
	height: 274rpx;
	margin-top: 34rpx;
	margin-left: 28rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
}

.result-card {
	box-sizing: border-box;
	margin-top: 28rpx;
	margin-left: 50rpx;
	margin-right: 50rpx;
	padding: 28rpx 34rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
}

.result-desc {
	display: block;
	font-size: 16px;
	line-height: 1.75;
	color: #000000;
	word-break: break-all;
	white-space: normal;
}

.notice-section {
	margin-top: 20rpx;
}

.notice-list {
	margin-top: 32rpx;
	margin-left: 50rpx;
}

.notice-item {
	display: flex;
	align-items: flex-start;
	margin-bottom: 16rpx;
}

.notice-index {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	width: 38rpx;
	height: 38rpx;
	margin-right: 20rpx;
	border-radius: 50%;
	background-color: #6FC4BA;
	font-size: 14px;
	line-height: 38rpx;
	color: #ffffff;
}

.notice-text {
	flex: 1;
	min-width: 0;
	font-size: 16px;
	line-height: 38rpx;
	color: #000000;
	word-break: break-all;
	white-space: normal;
}

@media screen and (min-width: 768px) {
	.recognition-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
