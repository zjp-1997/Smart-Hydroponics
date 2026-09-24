<template>
	<view class="technical-page">
		<!-- 自定义导航栏与参考图一致，保留足够的返回触控区域。 -->
		<view class="page-header">
			<button class="back-button" hover-class="button-pressed" aria-label="返回" @tap="goBack">
				<text class="iconfont icon-fanhui"></text>
			</button>
			<text class="page-title">技术支持</text>
			<view class="header-placeholder"></view>
		</view>

		<!-- 页面内容固定在当前视口内，避免浏览器渲染纵向滚动条。 -->
		<view class="page-content">
			<!-- 顶部客服插画采用纯 CSS 构成，避免新增远程图片依赖。 -->
			<view class="support-hero">
				<view class="hero-copy">
					<text class="hero-title">专业支持\n用心为您服务</text>
					<text class="hero-subtitle">让农业更智能 · 让服务更贴心</text>
				</view>
				<view class="support-illustration" aria-hidden="true">
					<view class="leaf leaf-one"></view>
					<view class="leaf leaf-two"></view>
					<view class="agent-hair"></view>
					<view class="agent-face">
						<view class="agent-eye eye-left"></view>
						<view class="agent-eye eye-right"></view>
						<view class="agent-smile"></view>
					</view>
					<view class="headset-band"></view>
					<view class="headset-ear"></view>
					<view class="headset-mic"></view>
					<view class="agent-body"></view>
					<view class="agent-laptop"></view>
				</view>
			</view>

			<!-- 联系方式为静态信息，使用需求中指定的 iconfont 图标。 -->
			<view class="contact-card">
				<view v-for="item in contacts" :key="item.title" class="contact-row">
					<view class="contact-icon-wrap">
						<text class="iconfont contact-icon" :class="item.icon"></text>
					</view>
					<view class="contact-copy">
						<text class="contact-title">{{ item.title }}</text>
						<text class="contact-description">{{ item.description }}</text>
					</view>
					<text class="iconfont icon-youjiantou row-arrow"></text>
				</view>
			</view>

			<view class="service-card">
				<view class="contact-icon-wrap">
					<text class="iconfont icon-shijian contact-icon"></text>
				</view>
				<view class="contact-copy">
					<text class="contact-title">服务时间</text>
					<text class="contact-description">周一至周日&nbsp;&nbsp;8:00 - 22:00</text>
				</view>
			</view>

			<view class="support-slogan">
				<text class="slogan-title">专业 · 高效 · 贴心</text>
				<text class="slogan-description">为您的农业生产保驾护航</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			// 三项联系方式仅用于静态展示，不触发拨号、邮件或微信能力。
			contacts: [
				{ title: '服务热线', description: '400-123-4567', icon: 'icon-dianhua' },
				{ title: '服务邮箱', description: 'support@smart-plant.cn', icon: 'icon-fasongyoujian' },
				{ title: '微信支持', description: '扫码添加微信，获取技术支持', icon: 'icon-weixin' }
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
	background-color: #f7fbfa;
}

.technical-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7fbfa;
	color: #202625;
}

.page-header {
	position: relative;
	z-index: 3;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: calc(var(--status-bar-height) + 96rpx);
	padding: var(--status-bar-height) 20rpx 0;
	background-color: #ffffff;
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
	overflow: hidden;
	padding-bottom: calc(42rpx + env(safe-area-inset-bottom));
}

.support-hero {
	position: relative;
	box-sizing: border-box;
	height: 320rpx;
	overflow: hidden;
	padding: 84rpx 30rpx 0;
	background: linear-gradient(135deg, #f0fbff 0%, #edf9f6 100%);
}

.hero-copy {
	position: relative;
	z-index: 2;
	display: flex;
	flex-direction: column;
}

.hero-title {
	white-space: pre-line;
	font-size: 44rpx;
	font-weight: 700;
	line-height: 1.38;
	color: #103f34;
}

.hero-subtitle {
	margin-top: 20rpx;
	font-size: 24rpx;
	line-height: 1.4;
	color: #6d7b78;
}

.support-illustration {
	position: absolute;
	right: 18rpx;
	bottom: 0;
	width: 286rpx;
	height: 330rpx;
}

.leaf {
	position: absolute;
	right: 2rpx;
	bottom: 60rpx;
	width: 42rpx;
	height: 92rpx;
	border-radius: 100% 0 100% 0;
	background-color: #bce8db;
	transform: rotate(24deg);
}

.leaf-two {
	right: 46rpx;
	bottom: 78rpx;
	width: 34rpx;
	height: 72rpx;
	transform: rotate(-18deg) scaleX(-1);
}

.agent-hair {
	position: absolute;
	left: 82rpx;
	top: 38rpx;
	z-index: 2;
	width: 128rpx;
	height: 150rpx;
	border-radius: 58% 58% 46% 46%;
	background-color: #294a59;
}

.agent-face {
	position: absolute;
	left: 105rpx;
	top: 65rpx;
	z-index: 3;
	width: 84rpx;
	height: 110rpx;
	border-radius: 46% 46% 48% 48%;
	background-color: #ffd7c0;
}

.agent-eye {
	position: absolute;
	top: 48rpx;
	width: 6rpx;
	height: 6rpx;
	border-radius: 50%;
	background-color: #324953;
}

.eye-left {
	left: 22rpx;
}

.eye-right {
	right: 22rpx;
}

.agent-smile {
	position: absolute;
	left: 34rpx;
	top: 70rpx;
	box-sizing: border-box;
	width: 18rpx;
	height: 9rpx;
	border-bottom: 3rpx solid #d98678;
	border-radius: 50%;
}

.headset-band {
	position: absolute;
	left: 89rpx;
	top: 56rpx;
	z-index: 4;
	box-sizing: border-box;
	width: 116rpx;
	height: 100rpx;
	border: 9rpx solid #547284;
	border-bottom: 0;
	border-radius: 60rpx 60rpx 0 0;
}

.headset-ear {
	position: absolute;
	left: 192rpx;
	top: 114rpx;
	z-index: 5;
	width: 20rpx;
	height: 42rpx;
	border-radius: 12rpx;
	background-color: #547284;
}

.headset-mic {
	position: absolute;
	left: 174rpx;
	top: 151rpx;
	z-index: 5;
	width: 34rpx;
	height: 5rpx;
	border-radius: 5rpx;
	background-color: #547284;
	transform: rotate(16deg);
}

.agent-body {
	position: absolute;
	left: 58rpx;
	bottom: -38rpx;
	width: 190rpx;
	height: 190rpx;
	border-radius: 46% 46% 12% 12%;
	background-color: #a7dccf;
}

.agent-laptop {
	position: absolute;
	left: 18rpx;
	bottom: -8rpx;
	z-index: 6;
	width: 190rpx;
	height: 88rpx;
	border-radius: 8rpx 8rpx 0 0;
	background-color: #aebdc4;
	transform: skew(10deg);
}

.contact-card,
.service-card {
	margin: 34rpx 30rpx 0;
	border-radius: 22rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 28rpx rgba(30, 89, 71, 0.04);
}

.contact-card {
	overflow: hidden;
}

.contact-row,
.service-card {
	position: relative;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 144rpx;
	padding: 22rpx 24rpx;
}

.contact-row + .contact-row::before {
	position: absolute;
	left: 120rpx;
	right: 0;
	top: 0;
	height: 1rpx;
	background-color: #e8eeec;
	content: '';
}

.contact-icon-wrap {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 82rpx;
	height: 82rpx;
	border-radius: 50%;
	background-color: #ecf9f4;
}

.contact-icon {
	font-size: 48rpx;
	line-height: 1;
	color: #28ad59;
}

.contact-copy {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	margin-left: 26rpx;
}

.contact-title {
	font-size: 30rpx;
	font-weight: 600;
	line-height: 1.35;
}

.contact-description {
	margin-top: 7rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 25rpx;
	line-height: 1.35;
	color: #697370;
}

.row-arrow {
	margin-left: 12rpx;
	font-size: 28rpx;
	color: #aeb7b5;
}

.service-card {
	margin-top: 34rpx;
}

.support-slogan {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin: clamp(24rpx, 4vh, 50rpx) 30rpx 0;
	padding: 52rpx 20rpx;
	border-radius: 24rpx;
	background: linear-gradient(135deg, #eaf8f3, #e2f5ee);
}

.slogan-title {
	font-size: 34rpx;
	font-weight: 600;
	color: #12684f;
}

.slogan-description {
	margin-top: 16rpx;
	font-size: 25rpx;
	color: #687673;
}

@media screen and (min-width: 768px) {
	.technical-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>

