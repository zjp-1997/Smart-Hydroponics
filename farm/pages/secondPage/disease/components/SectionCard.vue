<template>
	<!-- 统一详情页分区标题与卡片容器，插槽承载各业务板块内容。 -->
	<view class="section-block">
		<view class="section-heading">
			<view class="section-mark"></view>
			<text class="section-title">{{ title }}</text>
		</view>
		<view class="info-card">
			<slot></slot>
		</view>
	</view>
</template>

<script>
export default {
	name: 'SectionCard',
	props: {
		// title 是每个详情分区对用户可见的标题，不能为空。
		title: { type: String, required: true }
	}
}
</script>

<style scoped>
/* 组件自有结构必须在组件内声明样式，避免 H5 的样式隔离使父页面规则失效。 */
.section-block {
	margin-top: 34rpx;
}

.section-heading {
	display: flex;
	align-items: center;
	margin: 0 24rpx 18rpx;
}

.section-mark {
	width: 7rpx;
	height: 34rpx;
	margin-right: 13rpx;
	border-radius: 999rpx;
	background-color: #1eb8ab;
}

.section-title {
	font-size: 17px;
	font-weight: 750;
	line-height: 44rpx;
	color: #26343d;
}

.info-card {
	box-sizing: border-box;
	margin: 0 24rpx;
	padding: 28rpx 30rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 28rpx rgba(30, 58, 64, 0.07);
}
</style>
