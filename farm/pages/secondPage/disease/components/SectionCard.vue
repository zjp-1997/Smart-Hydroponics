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
