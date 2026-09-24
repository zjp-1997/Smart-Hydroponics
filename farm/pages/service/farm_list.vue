<template>
	<view class="farm-list-page">
		<view class="page-hero">
			<view class="navbar">
				<button class="nav-back" role="button" aria-label="返回上一页" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-icon" aria-hidden="true"></text>
				</button>
				<text class="nav-title">{{ isOrdinaryUser ? '农场信息' : '农场管理' }}</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="farm-content" scroll-y>
			<view v-if="loading && !farms.length" class="state-panel" role="status">正在加载农场信息...</view>
			<view v-else-if="errorMessage && !farms.length" class="state-panel error-state" role="alert">
				<text class="state-title">{{ offline ? '当前处于离线状态' : '农场信息加载失败' }}</text>
				<text class="state-detail">{{ errorMessage }}</text>
				<button class="retry-button" :disabled="loading" @tap="fetchFarmList">重新加载</button>
			</view>
			<view v-else-if="!farms.length" class="state-panel empty-state">
				<text class="state-title">暂无农场信息</text>
				<text class="state-detail">当前账号还没有可查看的农场</text>
			</view>
			<block v-else>
				<view v-if="errorMessage" class="stale-banner" role="alert">
					<text>{{ offline ? '网络已断开，正在显示上次加载的数据' : '刷新失败，正在显示上次加载的数据' }}</text>
					<button class="inline-retry" :disabled="loading" @tap="fetchFarmList">重试</button>
				</view>
				<!-- 卡片整体进入地块列表，图片区域单独保留预览能力。 -->
				<view
					class="farm-card"
					v-for="farm in farms"
					:key="farm.id"
					hover-class="farm-card-pressed"
					:hover-stay-time="80"
					role="button"
					:aria-label="`查看${farm.farmName}的地块，${farm.plotCount}个地块`"
					tabindex="0"
					@tap="handleFarmDetail(farm)"
					@keyup.enter="handleFarmDetail(farm)"
				>
					<view
						class="farm-image-action"
						hover-class="farm-image-pressed"
						:hover-stay-time="80"
						role="button"
						:aria-label="`预览${farm.farmName}的农场图片`"
						tabindex="0"
						@tap.stop="previewFarmImage(farm)"
						@keyup.enter.stop="previewFarmImage(farm)"
					>
						<protected-image class="farm-image" :src="farm.image || fallbackImage" :alt="`${farm.farmName}农场图片`" mode="aspectFill"></protected-image>
					</view>
					<view class="farm-info">
						<view class="farm-title-row">
							<text class="farm-name">{{ farm.farmName }}</text>
							<text class="plot-count-badge">{{ farm.plotCount }} 个地块</text>
						</view>
						<view class="farm-detail-row">
							<text class="detail-label">种植面积:</text>
							<text class="detail-value">{{ farm.plantArea || '-' }}</text>
						</view>
						<view class="farm-detail-row address-row">
							<text class="detail-label">种植地址:</text>
							<text class="detail-value address-value">{{ farm.address || '-' }}</text>
						</view>
					</view>
					<text class="iconfont icon-youjiantou farm-arrow" aria-hidden="true"></text>
				</view>
			</block>
		</scroll-view>
	</view>
</template>

<script>
import { getFarmList } from '@/api/farmList.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	computed: {
		isOrdinaryUser() {
			// 普通用户只能查看绑定农场主的农场，不显示“管理”字样。
			return String(getUserInfo()?.roleCode || '').toLowerCase() === 'user'
		}
	},
	data() {
		return {
			farms: [],
			loading: false,
			errorMessage: '',
			offline: false,
			fallbackImage: '/static/shanghai.jpg'
		}
	},
	onLoad() {
		this.fetchFarmList()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		async fetchFarmList() {
			if (this.loading) return
			this.loading = true
			this.errorMessage = ''
			this.offline = false
			// 页面只负责加载和展示；字段清洗、图片地址转换集中在 api/farmList.js 中。
			try {
				this.farms = await getFarmList()
			} catch (error) {
				this.offline = await this.isOffline()
				this.errorMessage = this.offline ? '请检查网络连接后重试' : (error?.message || '请稍后重试')
			} finally {
				this.loading = false
			}
		},
		isOffline() {
			return new Promise(resolve => {
				uni.getNetworkType({ success: result => resolve(result.networkType === 'none'), fail: () => resolve(false) })
			})
		},
		handleFarmDetail(farm) {
			uni.navigateTo({
				url: `/pages/secondPage/plot/plot_list?farmId=${encodeURIComponent(farm.id)}`
			})
		},
		previewFarmImage(farm) {
			const image = farm.image || this.fallbackImage
			uni.previewImage({ current: image, urls: [image] })
		}
	}
}
</script>

<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.farm-list-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 16px;
	font-weight: normal;
	color: #000000;
}

.page-hero {
	box-sizing: border-box;
	min-height: 300rpx;
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
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.nav-back,
.nav-placeholder {
	position: relative;
	z-index: 2;
	width: 88rpx;
	height: 88rpx;
}

.nav-back {
	display: flex;
	align-items: center;
	justify-content: center;
	margin: 0 0 0 -26rpx;
	padding: 0;
	background: transparent;
	color: #ffffff;
}
.nav-back::after { border: 0; }
.nav-icon {
	font-size: 18px;
	line-height: 1;
	color: #ffffff;
}

.farm-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 34rpx;
}

.farm-card {
	position: relative;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 232rpx;
	margin-bottom: 28rpx;
	padding: 24rpx 52rpx 24rpx 24rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(31, 78, 71, 0.08);
}

.farm-card:first-child {
	margin-top: 0;
}

.state-panel {
	box-sizing: border-box;
	margin-top: 80rpx;
	padding: 40rpx 20rpx;
	text-align: center;
	font-size: 14px;
	line-height: 1.5;
	color: #66736f;
}
.state-title,
.state-detail { display: block; }
.state-title { color: #26332f; font-size: 16px; font-weight: 600; }
.state-detail { margin-top: 12rpx; }
.retry-button { min-width: 200rpx; height: 80rpx; margin-top: 28rpx; padding: 0 28rpx; border-radius: 40rpx; color: #ffffff; background: #1ba291; font-size: 14px; line-height: 80rpx; }
.retry-button::after,
.inline-retry::after { border: 0; }
.stale-banner { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; box-sizing: border-box; margin-bottom: 18rpx; padding: 18rpx 20rpx; border-radius: 12rpx; color: #7a4b00; background: #fff3d6; font-size: 13px; line-height: 1.4; }
.inline-retry { flex-shrink: 0; min-width: 88rpx; height: 64rpx; margin: 0; padding: 0 16rpx; border-radius: 32rpx; color: #176c62; background: transparent; font-size: 13px; line-height: 64rpx; }

.farm-image-action,
.farm-image {
	display: block;
	flex-shrink: 0;
	width: 184rpx;
	height: 184rpx;
	border-radius: 18rpx;
	background-color: #f7f7f7;
}

.farm-image-action { overflow: hidden; }
.farm-image-pressed { opacity: 0.82; }

.farm-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	justify-content: center;
	min-width: 0;
	min-height: 184rpx;
	margin-left: 28rpx;
	box-sizing: border-box;
}

.farm-card-pressed { background-color: #f2faf8; }

.farm-title-row,
.farm-detail-row {
	display: flex;
	align-items: center;
}

.farm-title-row { justify-content: space-between; gap: 16rpx; }

.farm-name {
	flex: 1;
	min-width: 0;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 17px;
	font-weight: 600;
	line-height: 1.35;
	color: #26332f;
}

.plot-count-badge {
	flex-shrink: 0;
	padding: 6rpx 12rpx;
	border-radius: 999rpx;
	background-color: #e7f5f2;
	font-size: 11px;
	color: #267f74;
}

.farm-detail-row { margin-top: 18rpx; font-size: 13px; line-height: 1.4; }
.detail-label { flex-shrink: 0; width: 112rpx; color: #66736f; }
.detail-value { min-width: 0; color: #46534f; }
.address-row { align-items: flex-start; }
.address-value {
	display: -webkit-box;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
}

.farm-arrow {
	position: absolute;
	right: 18rpx;
	top: 50%;
	transform: translateY(-50%);
	font-size: 24rpx;
	color: #9aaca7;
}

@media screen and (min-width: 768px) {
	.farm-list-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
