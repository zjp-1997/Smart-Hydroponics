<template>
	<view class="expert-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">专家咨询</text>
				<view class="nav-placeholder"></view>
			</view>

			<view class="search-box">
				<text class="iconfont icon-icon_33 search-icon"></text>
				<input
					class="search-input"
					name="expert-search"
					v-model="keyword"
					placeholder="请输入专家姓名"
					placeholder-style="color: #C8C8C8;"
				/>
			</view>
		</view>

		<scroll-view class="expert-content" scroll-y>
			<view class="expert-card" v-for="expert in filteredExperts" :key="expert.id">
				<view class="expert-main">
					<protected-image class="expert-avatar" :src="expert.avatar" mode="aspectFill"></protected-image>
					<view class="expert-info">
						<view class="expert-head">
							<text class="expert-name">{{ expert.name }}</text>
							<text class="expert-unit">{{ expert.unit }}</text>
						</view>
						<view class="tag-row">
							<text class="expert-tag" v-for="tag in expert.tags" :key="tag">{{ tag }}</text>
						</view>
					</view>
				</view>

				<text class="expert-desc">“{{ expert.desc }}”</text>

				<view class="expert-footer">
					<view class="score-wrap">
						<text class="score-label">官方推荐分</text>
						<text class="score-value">{{ expert.score }}</text>
					</view>
					<button
						class="consult-button"
						:class="{ 'consult-button--disabled': !expert.consultable }"
						@tap="handleConsult(expert)"
					>
						<text class="iconfont icon-icon_msg consult-icon"></text>
						<text class="consult-text">免费咨询</text>
					</button>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getExpertList } from '@/api/expertList.js'

export default {
	data() {
		return {
			keyword: '',
			// 专家列表由后端接口返回，不再保留页面静态数据。
			experts: [],
			// 加载状态用于防止用户在弱网情况下重复触发请求。
			loading: false
		}
	},
	onLoad() {
		this.fetchExperts()
	},
	computed: {
		filteredExperts() {
			const keyword = this.keyword.trim()
			if (!keyword) {
				return this.experts
			}
			return this.experts.filter((expert) => {
				return [expert.name, expert.unit, expert.desc, ...expert.tags].some((text) => text.indexOf(keyword) > -1)
			})
		}
	},
	methods: {
		fetchExperts() {
			if (this.loading) {
				return
			}
			this.loading = true
			// 请求封装层会统一处理 token、错误提示和后端 R<T> 数据解包。
			getExpertList()
				.then((experts) => {
					this.experts = experts
				})
				.catch(() => {
					this.experts = []
				})
				.finally(() => {
					this.loading = false
				})
		},
		handleBack() {
			uni.navigateBack()
		},
		handleConsult(expert) {
			if (!expert.consultable) {
				uni.showToast({ title: '该专家当前不可咨询', icon: 'none' })
				return
			}
			uni.navigateTo({
				// 进入聊天页时携带专家ID，后续发送消息需要用该ID创建或复用咨询会话。
				url: `/pages/secondPage/chat/index?id=${encodeURIComponent(expert.id)}&name=${encodeURIComponent(expert.name)}&avatar=${encodeURIComponent(expert.avatar)}`
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

.expert-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.page-hero {
	box-sizing: border-box;
	min-height: 220rpx;
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

.search-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	height: 78rpx;
	margin-top: 36rpx;
	padding: 0 28rpx;
	border-radius: 40rpx;
	background-color: #ffffff;
}

.search-icon {
	flex-shrink: 0;
	margin-right: 18rpx;
	font-size: 36rpx;
	line-height: 36rpx;
	color: #C8C8C8;
}

.search-input {
	flex: 1;
	height: 78rpx;
	min-width: 0;
	padding: 0;
	background-color: transparent;
	font-size: 14px;
	line-height: 78rpx;
	color: #000000;
}

.expert-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 230rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 44rpx;
}

.expert-card {
	box-sizing: border-box;
	margin-bottom: 28rpx;
	padding: 34rpx 30rpx 30rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.04);
}

.expert-main {
	display: flex;
	align-items: center;
}

.expert-avatar {
	flex-shrink: 0;
	width: 104rpx;
	height: 104rpx;
	border-radius: 50%;
	background-color: #f7f7f7;
}

.expert-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	margin-left: 24rpx;
}

.expert-head {
	display: flex;
	align-items: center;
	min-width: 0;
}

.expert-name {
	flex-shrink: 0;
	font-size: 16px;
	line-height: 1.3;
	color: #000000;
}

.expert-unit {
	min-width: 0;
	margin-left: 20rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 12px;
	line-height: 1.3;
	color: #C8C8C8;
}

.tag-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin-top: 10rpx;
}

.expert-tag {
	box-sizing: border-box;
	height: 34rpx;
	margin-right: 10rpx;
	margin-bottom: 8rpx;
	padding: 0 14rpx;
	border-radius: 8rpx;
	background-color: #F3ECCC;
	font-size: 12px;
	line-height: 34rpx;
	color: #ffffff;
}

.expert-desc {
	display: block;
	margin-top: 24rpx;
	font-size: 12px;
	line-height: 1.8;
	color: #000000;
}

.expert-footer {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 26rpx;
}

.score-wrap {
	display: flex;
	align-items: baseline;
	min-width: 0;
}

.score-label {
	font-size: 12px;
	line-height: 1.3;
	color: #C8C8C8;
}

.score-value {
	margin-left: 16rpx;
	font-size: 14px;
	line-height: 1.3;
	color: #1BA291;
}

.consult-button {
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
	min-width: 174rpx;
	height: 64rpx;
	margin: 0;
	padding: 0 22rpx;
	border: 2rpx solid #e5e5e5;
	border-radius: 34rpx;
	background-color: #ffffff;
	font-size: 14px;
	line-height: 64rpx;
	color: #000000;
}

.consult-button::after {
	border: none;
}

.consult-button--disabled {
	border-color: #dddddd;
	background-color: #eeeeee;
	opacity: 0.65;
}

.consult-button--disabled .consult-icon,
.consult-button--disabled .consult-text {
	color: #999999;
}

.consult-icon {
	margin-right: 12rpx;
	font-size: 34rpx;
	line-height: 34rpx;
	color: #1BA291;
}

.consult-text {
	font-size: 14px;
	line-height: 1.3;
	color: #000000;
}

@media screen and (min-width: 768px) {
	.expert-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
