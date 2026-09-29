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
</style>
