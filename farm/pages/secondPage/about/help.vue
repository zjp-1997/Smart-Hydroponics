<template>
	<view class="help-page">
		<!-- 页面头部使用自定义导航，保证 H5 与 App 视觉一致。 -->
		<view class="page-header">
			<button class="back-button" hover-class="button-pressed" aria-label="返回" @tap="goBack">
				<text class="iconfont icon-fanhui"></text>
			</button>
			<text class="page-title">帮助与反馈</text>
			<view class="header-placeholder"></view>
		</view>

		<scroll-view class="page-content" scroll-y>
			<!-- 搜索区仅复现静态界面，不发起查询请求。 -->
			<view class="search-row">
				<view class="search-field">
					<view class="search-symbol"></view>
					<text class="search-placeholder">请输入问题关键词</text>
				</view>
				<view class="search-button">搜索</view>
			</view>

			<view class="faq-card">
				<view class="section-head">
					<text class="section-title">常见问题</text>
					<view class="more-link">
						<text>更多</text>
						<text class="iconfont icon-youjiantou"></text>
					</view>
				</view>
				<view v-for="question in questions" :key="question" class="question-row">
					<text class="question-text">{{ question }}</text>
					<text class="iconfont icon-youjiantou row-arrow"></text>
				</view>
			</view>

			<!-- 两张辅助入口卡片使用需求指定图标，仅作为静态展示。 -->
			<view class="action-grid">
				<view class="action-card help-card">
					<text class="iconfont icon-changjianwentixiangguanwenti2 action-icon"></text>
					<text class="action-title">使用帮助</text>
					<text class="action-description">查看操作指南</text>
				</view>
				<view class="action-card feedback-card">
					<text class="iconfont icon-fankui-tianchong action-icon"></text>
					<text class="action-title">我要反馈</text>
					<text class="action-description">提交问题或建议</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			// 常见问题为静态文案，不依赖后端接口。
			questions: [
				'如何添加设备？',
				'设备无法连接怎么办？',
				'如何查看历史数据？',
				'如何设置告警通知？',
				'账号登录失败怎么办？'
			]
		}
	},
	methods: {
		// 返回进入本页前的页面。
		goBack() {
			uni.navigateBack()
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100%;
	background-color: #ffffff;
}

.help-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #ffffff;
	color: #202625;
}

.page-header {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: calc(var(--status-bar-height) + 96rpx);
	padding: var(--status-bar-height) 20rpx 0;
}

.back-button,
.header-placeholder {
	box-sizing: border-box;
	width: 88rpx;
	height: 88rpx;
}

.back-button {
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0;
	padding: 0;
	border-radius: 50%;
	background: transparent;
}

.back-button::after {
	border: 0;
}

.back-button .iconfont {
	font-size: 36rpx;
}

.button-pressed {
	background-color: #eef7f4;
}

.page-title {
	font-size: 36rpx;
	font-weight: 600;
}

.page-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 96rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 40rpx 30rpx calc(40rpx + env(safe-area-inset-bottom));
}

.search-row {
	display: flex;
	align-items: center;
	height: 88rpx;
	border-radius: 22rpx;
	background-color: #f5f7fa;
}

.search-field {
	display: flex;
	flex: 1;
	align-items: center;
	min-width: 0;
	padding: 0 30rpx;
}

.search-symbol {
	position: relative;
	box-sizing: border-box;
	flex-shrink: 0;
	width: 30rpx;
	height: 30rpx;
	margin-right: 22rpx;
	border: 3rpx solid #9ca8b3;
	border-radius: 50%;
}

.search-symbol::after {
	position: absolute;
	right: -10rpx;
	bottom: -7rpx;
	width: 14rpx;
	height: 3rpx;
	border-radius: 3rpx;
	background-color: #9ca8b3;
	transform: rotate(45deg);
	content: '';
}

.search-placeholder {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 28rpx;
	color: #9da6ae;
}

.search-button {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 152rpx;
	height: 88rpx;
	border-radius: 22rpx;
	background: linear-gradient(135deg, #3b9e83, #28856e);
	font-size: 30rpx;
	color: #ffffff;
}

.faq-card {
	margin-top: 56rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 28rpx rgba(31, 67, 57, 0.035);
}

.section-head,
.question-row {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.section-head {
	height: 70rpx;
}

.section-title {
	font-size: 32rpx;
	font-weight: 600;
}

.more-link {
	display: flex;
	align-items: center;
	font-size: 27rpx;
	color: #6e7775;
}

.more-link .iconfont {
	margin-left: 10rpx;
	font-size: 24rpx;
	color: #9ea8a6;
}

.question-row {
	position: relative;
	min-height: 106rpx;
	padding: 0 18rpx;
}

.question-row::before {
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	height: 1rpx;
	background-color: #e8eceb;
	content: '';
}

.question-text {
	font-size: 28rpx;
	line-height: 1.4;
}

.row-arrow {
	font-size: 26rpx;
	color: #9ba6a3;
}

.action-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 28rpx;
	margin-top: 58rpx;
}

.action-card {
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 282rpx;
	border-radius: 24rpx;
}

.help-card {
	background: linear-gradient(145deg, #edf9f4, #e1f5ec);
	color: #24754f;
}

.feedback-card {
	background: linear-gradient(145deg, #eff6ff, #e7f1ff);
	color: #32588f;
}

.action-icon {
	font-size: 74rpx;
	line-height: 1;
}

.help-card .action-icon {
	color: #2daf58;
}

.feedback-card .action-icon {
	color: #438eea;
}

.action-title {
	margin-top: 24rpx;
	font-size: 32rpx;
	font-weight: 600;
}

.action-description {
	margin-top: 10rpx;
	font-size: 25rpx;
	color: #687472;
}

@media screen and (min-width: 768px) {
	.help-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
