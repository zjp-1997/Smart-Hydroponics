<template>
	<view class="plot-list-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">地块列表</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="plot-content" scroll-y>
			<!-- 搜索和状态筛选只作用于接口返回的当前地块集合，不改变顶部渐变区域。 -->
			<view class="search-box">
				<text class="iconfont icon-icon_33 search-icon" aria-hidden="true"></text>
				<input
					class="search-input"
					name="plot-search"
					:value="keyword"
					placeholder="搜索作物名称、地块编号..."
					placeholder-class="search-placeholder"
					aria-label="搜索作物名称或地块编号"
					@input="handleKeywordInput"
				/>
			</view>

			<view class="status-filter">
				<view
					class="status-chip"
					:class="{ active: activeStatus === status.value }"
					v-for="status in statusOptions"
					:key="status.label"
					role="button"
					:aria-label="`筛选${status.label}地块`"
					:aria-pressed="activeStatus === status.value"
					hover-class="status-chip-pressed"
					:hover-stay-time="80"
					@tap="selectStatus(status.value)"
				>
					{{ status.label }}
				</view>
			</view>

			<view v-if="loading" class="state-text">正在加载地块信息...</view>
			<view v-else-if="!visiblePlots.length" class="state-text">暂无符合条件的地块信息</view>
			<block v-else>
				<view class="plot-card" v-for="plot in visiblePlots" :key="plot.id">
					<view
						class="plot-image-wrap"
						hover-class="plot-image-pressed"
						:hover-stay-time="80"
						@tap="plot.plantingStatus === 'IDLE' ? handlePlotDetail(plot) : previewPlotImage(plot)"
					>
						<!-- 空闲地块不显示作物旧图，图片区域保留占位以维持卡片布局。 -->
						<text v-if="plot.plantingStatus === 'IDLE'" class="idle-image-placeholder">-</text>
						<protected-image v-else
							class="plot-image"
							:src="plot.image || fallbackImage"
							:alt="`${plot.cropName}地块图片`"
							mode="aspectFill"
						></protected-image>
						<view v-if="plot.plantingStatus !== 'IDLE'"
							class="monitor-mask"
							hover-class="monitor-mask-pressed"
							:hover-stay-time="80"
							@tap.stop="handleMonitor(plot)"
						>
							<text class="iconfont icon-shishijiankong monitor-icon"></text>
							<text class="monitor-text">查看监控</text>
						</view>
					</view>

					<view
						class="plot-info"
						hover-class="plot-info-pressed"
						:hover-stay-time="80"
						@tap="handlePlotDetail(plot)"
					>
						<view class="plot-title-row">
							<!-- 空闲地块没有作物，以地块名称作为卡片标题。 -->
							<text class="crop-name" :class="{ idle: plot.plantingStatus === 'IDLE' }">{{ plot.plantingStatus === 'IDLE' ? plot.plotName : plot.cropName }}</text>
							<text class="status-badge" :class="`status-${plot.plantingStatus.toLowerCase()}`">{{ plot.plantingStatusName }}</text>
						</view>
						<view class="info-row">
							<text class="iconfont icon-dingwei1 info-icon" aria-hidden="true"></text>
							<text class="info-label">地块面积：</text><text class="info-value">{{ plot.plotArea }}</text>
						</view>
						<view class="info-row">
							<text class="iconfont icon-rili info-icon" aria-hidden="true"></text>
							<text class="info-label">种植时间：</text><text class="info-value">{{ plot.plantingTime }}</text>
						</view>
						<view class="info-row">
							<text class="iconfont icon-a-44tubiao-41 info-icon" aria-hidden="true"></text>
							<text class="info-label">预采时间：</text><text class="info-value">{{ plot.harvestTime }}</text>
						</view>
						<view class="info-row yield-row">
							<text class="iconfont icon--ss-yezi info-icon" aria-hidden="true"></text>
							<text class="info-label">预估产量：</text><text class="info-value">{{ plot.estimatedYield }}</text>
						</view>
					</view>
				</view>
			</block>
		</scroll-view>
	</view>
</template>

<script>
import { getAllPlotList, getPlotListByFarmId } from '@/api/plotList.js'

export default {
	data() {
		return {
			plots: [],
			farmId: null,
			keyword: '',
			activeStatus: null,
			// 顺序与产品要求一致，value 直接对应 smart_plant 返回的状态编码。
			statusOptions: [
				{ label: '全部', value: null },
				{ label: '种植中', value: 'PLANTING' },
				{ label: '待采收', value: 'HARVEST_READY' },
				{ label: '空闲中', value: 'IDLE' }
			],
			loading: false,
			fallbackImage: '/static/lecttue.png'
		}
	},
	computed: {
		visiblePlots() {
			const keyword = this.keyword.trim().toLowerCase()
			// 作物名称、地块编号和地块名称统一支持模糊搜索，并与状态条件组合过滤。
			return this.plots.filter((plot) => {
				const matchesStatus = this.activeStatus === null || plot.plantingStatus === this.activeStatus
				const searchText = `${plot.cropName} ${plot.plotCode} ${plot.plotName}`.toLowerCase()
				return matchesStatus && (!keyword || searchText.indexOf(keyword) > -1)
			})
		}
	},
	onLoad(options) {
		this.farmId = options && options.farmId ? options.farmId : null
		this.fetchPlotList()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		fetchPlotList() {
			this.loading = true
			// 是否携带 farmId 决定查询全部地块还是指定农场下的地块，接口层会统一处理响应结构。
			const requestTask = this.farmId ? getPlotListByFarmId(this.farmId) : getAllPlotList()
			requestTask
				.then((plots) => {
					this.plots = plots
				})
				.finally(() => {
					this.loading = false
				})
		},
		handleKeywordInput(event) {
			this.keyword = event.detail.value || ''
		},
		selectStatus(status) {
			this.activeStatus = status
		},
		handleMonitor(plot) {
			if (plot.plantingStatus === 'IDLE') return
			uni.navigateTo({
				url: `/pages/service/monitor_list?plotId=${encodeURIComponent(plot.id)}`
			})
		},
		previewPlotImage(plot) {
			if (plot.plantingStatus === 'IDLE') return
			const image = plot.image || this.fallbackImage
			uni.previewImage({ current: image, urls: [image] })
		},
		handlePlotDetail(plot) {
			uni.navigateTo({
				url: `/pages/secondPage/plot/detail?id=${encodeURIComponent(plot.id)}`
			})
		}
	}
}
</script>

<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.plot-list-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 14px;
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

.nav-icon,
.nav-placeholder {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
}

.nav-icon {
	text-align: left;
	font-size: 42rpx;
	line-height: 58rpx;
	color: #ffffff;
}

.icon-fanhui.nav-icon {
	font-size: 18px;
}

.plot-list-page .plot-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 34rpx;
}

.plot-list-page .search-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	height: 88rpx;
	padding: 0 34rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(31, 78, 71, 0.05);
}

.plot-list-page .search-icon {
	flex-shrink: 0;
	margin-right: 20rpx;
	font-size: 18px;
	color: #9aa8b5;
}

.plot-list-page .search-input {
	flex: 1;
	min-width: 0;
	height: 88rpx;
	font-size: 14px;
	color: #263238;
}

.plot-list-page .search-placeholder { color: #9ca7b3; }

.plot-list-page .status-filter {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	gap: 16rpx;
	margin-top: 28rpx;
}

.plot-list-page .status-chip {
	box-sizing: border-box;
	height: 72rpx;
	border-radius: 36rpx;
	text-align: center;
	font-size: 13px;
	line-height: 72rpx;
	color: #53616d;
	background-color: #ffffff;
	box-shadow: 0 6rpx 18rpx rgba(31, 78, 71, 0.06);
}

.plot-list-page .status-chip.active {
	font-weight: 600;
	color: #ffffff;
	/* 复用 farm 全局常用的柔和青绿色，避免局部高饱和绿色破坏视觉一致性。 */
	background-color: #6FC4BA;
}

.plot-list-page .status-chip-pressed { opacity: 0.78; }

.plot-list-page .plot-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	width: 100%;
	min-height: 318rpx;
	margin-top: 36rpx;
	margin-bottom: 0;
	padding: 36rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.04);
}

.plot-list-page .plot-image-wrap {
	position: relative;
	flex-shrink: 0;
	width: 250rpx;
	height: 250rpx;
	overflow: hidden;
	border-radius: 10rpx;
	background-color: #f7f7f7;
}

.plot-list-page .plot-image {
	display: block;
	width: 250rpx;
	height: 250rpx;
	margin-left: 0;
	border-radius: 0;
}

.plot-list-page .idle-image-placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 250rpx;
	height: 250rpx;
	font-size: 26px;
	color: #7b8b88;
}

.plot-list-page .plot-image-pressed { opacity: 0.82; }

.plot-list-page .monitor-mask {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	height: 84rpx;
	background-color: rgba(0, 0, 0, 0.66);
	color: #ffffff;
}

.plot-list-page .monitor-icon {
	margin-right: 10rpx;
	font-size: 36rpx;
	line-height: 36rpx;
	color: #ffffff;
}

.plot-list-page .monitor-text {
	font-size: 12px;
	line-height: 1.2;
	color: #ffffff;
}

.plot-list-page .monitor-mask-pressed { background-color: rgba(0, 0, 0, 0.8); }

.plot-list-page .plot-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	justify-content: center;
	min-width: 0;
	min-height: 250rpx;
	margin-left: 34rpx;
	padding: 4rpx 0;
	border-radius: 12rpx;
	box-sizing: border-box;
}

.plot-list-page .plot-info-pressed { background-color: #eef8f6; }

.plot-list-page .plot-title-row {
	display: flex;
	align-items: center;
	gap: 14rpx;
	min-width: 0;
	margin-bottom: 18rpx;
}

.plot-list-page .crop-name {
	max-width: 180rpx;
	overflow: hidden;
	font-size: 17px;
	font-weight: 700;
	line-height: 1.2;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: #1f2933;
}

.plot-list-page .crop-name.idle { color: #98a1ad; }

.plot-list-page .status-badge {
	flex-shrink: 0;
	padding: 6rpx 14rpx;
	border: 1rpx solid #c8e6e2;
	border-radius: 18rpx;
	font-size: 10px;
	line-height: 1.2;
	/* 状态徽标沿用农场卡片标签的低饱和主色组合。 */
	color: #267F74;
	background-color: #E7F5F2;
}

.plot-list-page .status-harvest_ready {
	border-color: #ffd7a3;
	color: #e27b08;
	background-color: #fff8eb;
}

.plot-list-page .status-idle {
	border-color: #d9dee5;
	color: #7c8794;
	background-color: #f3f5f7;
}

.plot-list-page .info-row {
	display: flex;
	align-items: center;
	width: 100%;
	margin-bottom: 12rpx;
	overflow: hidden;
	font-size: 12px;
	line-height: 1.25;
	white-space: nowrap;
	color: #6f7b87;
}

.plot-list-page .info-row:last-child { margin-bottom: 0; }

.plot-list-page .info-icon {
	flex-shrink: 0;
	width: 30rpx;
	margin-right: 8rpx;
	font-size: 14px;
	text-align: center;
	color: #c6ced6;
}

.plot-list-page .info-label { flex-shrink: 0; }

.plot-list-page .info-value {
	min-width: 0;
	overflow: hidden;
	font-weight: 500;
	text-overflow: ellipsis;
	color: #35404a;
}

.plot-list-page .yield-row .info-icon,
.plot-list-page .yield-row .info-value {
	font-weight: 600;
	color: #ef7d17;
}

.plot-list-page .state-text {
	box-sizing: border-box;
	margin-top: 80rpx;
	padding: 40rpx 20rpx;
	text-align: center;
	font-size: 14px;
	line-height: 1.4;
	color: #C8C8C8;
}

@media screen and (min-width: 768px) {
	.plot-list-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
