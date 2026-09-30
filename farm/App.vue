<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/* 统一各端基础渲染，页面 class 样式仍可按设计覆盖这些低优先级规则。 */
	page {
		box-sizing: border-box;
		width: 100%;
		min-height: 100%;
		margin: 0;
		color: #263238;
		background-color: #f7f7f7;
		font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
		font-size: 16px;
		line-height: 1.5;
		-webkit-text-size-adjust: 100%;
	}

	view,
	scroll-view,
	swiper,
	swiper-item,
	form,
	label,
	button,
	input,
	textarea,
	image {
		box-sizing: border-box;
	}

	button,
	input,
	textarea {
		font-family: inherit;
	}

	/* 微信小程序 button 自带边距、背景、行高和伪元素边框，统一清零后与 H5/App 自定义按钮一致。 */
	button {
		margin: 0;
		padding: 0;
		border: 0;
		border-radius: 0;
		color: inherit;
		background: transparent;
		font-size: inherit;
		line-height: inherit;
	}

	button::after {
		display: none;
		border: 0;
	}

	input,
	textarea {
		min-width: 0;
		margin: 0;
		padding: 0;
		border: 0;
		color: inherit;
		background: transparent;
		font-size: inherit;
		line-height: inherit;
	}

	image {
		vertical-align: middle;
	}

	scroll-view {
		width: 100%;
	}

	/* 所有自定义导航复用消息页的薄荷渐变、标题尺寸与 58rpx 导航行。 */
	.app-nav-surface {
		border-radius: 0 !important;
		background: transparent !important;
	}

	/* 渐变只覆盖状态栏和导航行，避免搜索、筛选或摘要区被误染成导航背景。 */
	.app-nav-surface::before {
		position: absolute;
		z-index: 0;
		top: 0;
		left: 0;
		right: 0;
		height: calc(var(--status-bar-height) + 88rpx);
		background: linear-gradient(110deg, #60cbba 0%, #57c5b4 50%, #4ebfad 100%);
		pointer-events: none;
		content: '';
	}

	.app-nav-row {
		position: relative !important;
		z-index: 1;
		display: flex !important;
		align-items: center !important;
		justify-content: space-between !important;
		height: 58rpx !important;
	}

	.app-nav-title {
		position: absolute !important;
		left: 90rpx !important;
		right: 90rpx !important;
		overflow: hidden;
		text-align: center !important;
		text-overflow: ellipsis;
		white-space: nowrap;
		font-size: 16px !important;
		font-weight: normal !important;
		line-height: 58rpx !important;
		color: #ffffff !important;
	}

	/* 少量页面原先把安全区与导航行合并成一个节点，使用紧凑类保持相同视觉。 */
	.app-nav-compact {
		box-sizing: border-box !important;
		height: calc(var(--status-bar-height) + 90rpx) !important;
		padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0 !important;
	}

	.app-nav-compact > .app-nav-title {
		bottom: 16rpx;
	}

	.app-nav-surface .app-nav-row .iconfont,
	.app-nav-compact > .back-button,
	.app-nav-compact > .back-button .iconfont {
		color: #ffffff !important;
	}

	/* H5 原生 TabBar 由框架渲染，显式清除圆角以保持四端导航外观一致。 */
	/* #ifdef H5 */
	uni-tabbar,
	.uni-tabbar {
		border-radius: 0 !important;
	}

	/* H5 scroll-view 的默认刷新图标使用 currentColor，统一覆盖为 farm 品牌薄荷绿。 */
	uni-scroll-view .uni-scroll-view-refresh__spinner {
		color: #1ba291;
	}

	uni-scroll-view .uni-scroll-view-refresh-inner {
		background-color: #f8fffd;
		box-shadow: 0 2px 10px rgba(27, 162, 145, 0.16);
	}
	/* #endif */
</style>
