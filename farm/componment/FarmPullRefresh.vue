<template>
	<view
		class="pull-refresh-feedback farm-pull-refresh"
		role="status"
		:aria-label="refreshing ? `正在刷新${label}` : `下拉刷新${label}`"
	>
		<view class="pull-refresh-feedback__spinner" aria-hidden="true"></view>
		<text class="pull-refresh-feedback__label">
			{{ refreshing ? `正在刷新${label}…` : `下拉刷新${label}` }}
		</text>
	</view>
</template>

<script>
export default {
	name: 'FarmPullRefresh',
	props: {
		refreshing: {
			type: Boolean,
			default: false
		},
		label: {
			type: String,
			default: '页面数据'
		}
	}
}
</script>

<style scoped>
.farm-pull-refresh {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-direction: column;
	height: 112rpx;
	background: linear-gradient(180deg, rgba(215, 243, 238, 0.96) 0%, rgba(245, 248, 247, 0.98) 100%);
}

.pull-refresh-feedback__spinner {
	box-sizing: border-box;
	width: 46rpx;
	height: 46rpx;
	border: 5rpx solid rgba(27, 162, 145, 0.2);
	border-top-color: #1ba291;
	border-radius: 50%;
	animation: pull-refresh-spin 0.8s linear infinite;
}

.pull-refresh-feedback__label {
	margin-top: 8rpx;
	font-size: 12px;
	line-height: 1.35;
	color: #607a75;
}

@keyframes pull-refresh-spin {
	to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
	.pull-refresh-feedback__spinner { animation-duration: 1.6s; }
}
</style>
