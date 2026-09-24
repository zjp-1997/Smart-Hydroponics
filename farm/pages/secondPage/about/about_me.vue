<template>
	<view class="about-page">
		<!-- 自定义导航栏：与项目其他二级页面保持一致，并避开系统状态栏。 -->
		<view class="page-header">
			<button class="back-button" hover-class="button-pressed" aria-label="返回" @tap="goBack">
				<text class="iconfont icon-fanhui"></text>
			</button>
			<text class="page-title">关于我们</text>
			<view class="header-placeholder"></view>
		</view>

		<!-- 页面内容固定在当前视口内，避免浏览器渲染纵向滚动条。 -->
		<view class="page-content">
			<!-- 品牌区使用指定双叶图标复现参考图中的幼苗标识。 -->
			<view class="brand-section">
				<text class="iconfont icon-shuangyezi brand-icon"></text>
				<text class="brand-name">智慧农业</text>
				<text class="brand-slogan">让科技助力每一份生长</text>
				<view class="brand-divider"></view>
			</view>

			<text class="brand-description">我们致力于通过物联网、大数据和人工智能技术，\n为现代农业提供智能化、数字化的解决方案，\n让农业生产更高效、更绿色、更可持续。</text>

			<!-- 愿景、使命与价值观为静态品牌信息，不触发业务请求。 -->
			<view class="value-card">
				<view v-for="item in values" :key="item.title" class="value-row">
					<view class="value-icon-wrap">
						<text class="iconfont value-icon" :class="item.icon"></text>
					</view>
					<view class="value-copy">
						<text class="value-title">{{ item.title }}</text>
						<text class="value-description">{{ item.description }}</text>
					</view>
					<text class="iconfont icon-youjiantou row-arrow"></text>
				</view>
			</view>

			<view class="footer-copy">
				<text>版本号 v1.0.0</text>
				<text>© 2024 智慧农业 版权所有</text>
			</view>
		</view>

		<!-- 底部柔和曲线仅承担装饰作用。 -->
		<view class="bottom-wave wave-left"></view>
		<view class="bottom-wave wave-right"></view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			// 三项静态品牌价值复用统一行结构，避免重复模板。
			values: [
				{ title: '我们的愿景', description: '用科技改善农业，让农业更美好', icon: 'icon-shuangyezi' },
				{ title: '我们的使命', description: '推动农业数字化，助力乡村振兴', icon: 'icon-bazi1' },
				{ title: '我们的价值观', description: '创新 · 专业 · 合作 · 共赢', icon: 'icon-guanyuwomen1' }
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

.about-page {
	position: relative;
	box-sizing: border-box;
	height: 100vh;
	overflow: hidden;
	background: linear-gradient(180deg, #ffffff 0%, #fbfefd 100%);
	color: #202625;
}

/* 导航栏左右占位等宽，保证标题始终视觉居中。 */
.page-header {
	position: relative;
	z-index: 3;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: calc(var(--status-bar-height) + 96rpx);
	padding: var(--status-bar-height) 20rpx 0;
	background-color: rgba(255, 255, 255, 0.96);
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
	color: #1f2524;
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
	line-height: 1.3;
}

.page-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 96rpx);
	bottom: 0;
	z-index: 2;
	box-sizing: border-box;
	overflow: hidden;
	padding: 32rpx 44rpx calc(52rpx + env(safe-area-inset-bottom));
}

.brand-section {
	display: flex;
	flex-direction: column;
	align-items: center;
}

.brand-icon {
	font-size: 142rpx;
	line-height: 142rpx;
	color: #2caf59;
}

.brand-name {
	margin-top: 18rpx;
	font-size: 42rpx;
	font-weight: 700;
	line-height: 1.3;
	color: #123f32;
}

.brand-slogan {
	margin-top: 14rpx;
	font-size: 28rpx;
	line-height: 1.5;
	color: #6b7573;
}

.brand-divider {
	width: 64rpx;
	height: 4rpx;
	margin-top: 42rpx;
	border-radius: 4rpx;
	background-color: #c4ded7;
}

.brand-description {
	display: block;
	margin-top: 48rpx;
	text-align: center;
	white-space: pre-line;
	font-size: 27rpx;
	line-height: 1.8;
	color: #515d5a;
}

.value-card {
	overflow: hidden;
	margin-top: 54rpx;
	border-radius: 22rpx;
	background-color: rgba(255, 255, 255, 0.96);
	box-shadow: 0 8rpx 32rpx rgba(41, 109, 87, 0.06);
}

.value-row {
	position: relative;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 142rpx;
	padding: 20rpx 18rpx;
}

.value-row + .value-row::before {
	position: absolute;
	left: 112rpx;
	right: 0;
	top: 0;
	height: 1rpx;
	background-color: #edf0ef;
	content: '';
}

.value-icon-wrap {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 92rpx;
	height: 92rpx;
	border-radius: 50%;
	background-color: #e8f7f1;
}

.value-icon {
	font-size: 50rpx;
	line-height: 1;
	color: #29ad59;
}

.value-copy {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	margin-left: 24rpx;
}

.value-title {
	font-size: 30rpx;
	font-weight: 600;
	line-height: 1.35;
}

.value-description {
	margin-top: 7rpx;
	font-size: 25rpx;
	line-height: 1.35;
	color: #697370;
}

.row-arrow {
	margin-left: 12rpx;
	font-size: 30rpx;
	color: #aeb7b5;
}

.footer-copy {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 18rpx;
	margin-top: clamp(30rpx, 6vh, 100rpx);
	padding-bottom: 38rpx;
	font-size: 24rpx;
	line-height: 1.3;
	color: #88918f;
}

.bottom-wave {
	position: absolute;
	z-index: 1;
	bottom: -120rpx;
	border-radius: 50%;
	background-color: rgba(212, 244, 235, 0.72);
}

.wave-left {
	left: -180rpx;
	width: 620rpx;
	height: 230rpx;
	transform: rotate(15deg);
}

.wave-right {
	right: -150rpx;
	width: 500rpx;
	height: 260rpx;
	background-color: rgba(227, 248, 242, 0.88);
	transform: rotate(-12deg);
}

@media screen and (min-width: 768px) {
	.about-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
