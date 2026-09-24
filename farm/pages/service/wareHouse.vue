<template>
	<view class="warehouse-page">
		<!-- 顶部只保留返回按钮和标题，按需求移除图片中的搜索、添加操作图标。 -->
		<view class="warehouse-header">
			<view class="warehouse-nav">
				<text class="iconfont icon-fanhui back-button" role="button" aria-label="返回上一页" @tap="handleBack"></text>
				<text class="page-title">{{ isOrdinaryUser ? '仓库物资' : '仓库管理' }}</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="warehouse-content" scroll-y :show-scrollbar="false" lower-threshold="120"
			@scrolltolower="loadMore">
			<!-- 三项统计由用户端聚合接口返回，保持与参考图一致的横向卡片布局。 -->
			<view class="stats-grid">
				<view class="stat-card" v-for="stat in stats" :key="stat.label">
					<text class="stat-label">{{ stat.label }}</text>
					<text class="stat-value">{{ stat.value }}</text>
					<text class="stat-unit">{{ stat.unit }}</text>
				</view>
			</view>

			<view class="search-box">
				<text class="iconfont icon-icon_33 search-icon" aria-hidden="true"></text>
				<input
					class="search-input"
					name="warehouse-search"
					:value="keyword"
					aria-label="搜索物品名称、编码或型号"
					placeholder="搜索物品名称、编码或型号..."
					placeholder-class="search-placeholder"
					@input="handleKeywordInput"
				/>
			</view>

			<!-- 分类栏允许横向滚动，避免小屏设备压缩或截断分类名称。 -->
			<scroll-view class="category-scroll" scroll-x :show-scrollbar="false">
				<view class="category-list">
					<view
						class="category-chip"
						:class="{ active: activeCategory === category.value }"
						v-for="category in categories"
						:key="category.label"
						role="button"
						:aria-label="`筛选${category.label}物资`"
						:aria-pressed="activeCategory === category.value"
						hover-class="category-chip-pressed"
						:hover-stay-time="80"
						@tap="selectCategory(category.value)"
					>
						{{ category.label }}
					</view>
				</view>
			</scroll-view>

			<view v-if="loading && !warehouse.items.length" class="state-card">正在加载仓库物资...</view>
			<view v-else-if="!warehouse.items.length" class="state-card">暂无符合条件的仓库物资</view>
			<view v-else class="item-grid">
				<view class="item-card" v-for="item in warehouse.items" :key="item.id">
					<view class="image-wrap">
						<!-- API 层已将上传图片地址统一解析到 image 字段，页面直接绑定该可展示地址。 -->
						<protected-image class="item-image" :src="item.image" :alt="`${item.name}图片`" mode="aspectFill"></protected-image>
						<text class="category-badge" :class="`category-${item.category}`">{{ item.categoryName }}</text>
						<text v-if="item.isWarning" class="warning-badge">库存紧张</text>
					</view>
					<view class="item-info">
						<view class="item-title-row">
							<text class="item-name">{{ item.name }}</text>
							<text class="stock-text" :class="{ warning: item.isWarning }">库存 {{ item.stockText }}{{ item.unit }}</text>
						</view>
						<text class="item-meta">型号：{{ item.specification }}</text>
						<text class="item-meta">入库：{{ item.inboundDate }}</text>
					</view>
				</view>
			</view>
			<button v-if="hasNextPage" class="load-more" :loading="loadingMore" @tap="loadMore">
				{{ loadingMore ? '正在加载...' : '加载更多' }}
			</button>
			<view v-else-if="warehouse.items.length" class="list-end">已加载全部物资</view>
		</scroll-view>
	</view>
</template>

<script>
import { getWarehouseOverview, WAREHOUSE_CATEGORIES } from '@/api/warehouse.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			keyword: '',
			activeCategory: null,
			categories: WAREHOUSE_CATEGORIES,
			warehouse: {
				totalStock: '0',
				itemTypes: 0,
				todayInbound: '0',
				items: []
			},
			loading: false,
			loadingMore: false,
			hasNextPage: false,
			pageNum: 1,
			pageSize: 20,
			searchTimer: null,
			requestVersion: 0
		}
	},
	computed: {
		isOrdinaryUser() {
			// 普通用户只能查看所属农场库存，标题与首页只读入口保持一致。
			return String(getUserInfo()?.roleCode || '').toLowerCase() === 'user'
		},
		stats() {
			// 将接口统计字段映射成参考图的三张概览卡片。
			return [
				{ label: '总库存', value: this.warehouse.totalStock, unit: '件' },
				{ label: '物品种类', value: this.warehouse.itemTypes, unit: '种' },
				{ label: '今日入库', value: this.warehouse.todayInbound, unit: '件' }
			]
		}
	},
	// 页面每次重新显示时都请求最新统计，确保后台入库或库存调整后无需重启应用即可更新。
	onShow() {
		this.fetchWarehouse(true)
	},
	onUnload() { clearTimeout(this.searchTimer) },
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		fetchWarehouse(reset = true) {
			const requestedPage = reset ? 1 : this.pageNum + 1
			const version = ++this.requestVersion
			this.loading = reset
			this.loadingMore = !reset
			// API 层负责字段清洗，页面只保存可直接渲染的数据。
			getWarehouseOverview({
				keyword: this.keyword.trim(), category: this.activeCategory,
				pageNum: requestedPage, pageSize: this.pageSize
			})
				.then((warehouse) => {
					if (version !== this.requestVersion) return
					this.warehouse = { ...warehouse, items: reset
						? warehouse.items
						: [...this.warehouse.items, ...warehouse.items] }
					this.pageNum = requestedPage
					this.hasNextPage = warehouse.hasNextPage
				})
				.finally(() => {
					if (version === this.requestVersion) { this.loading = false; this.loadingMore = false }
				})
		},
		handleKeywordInput(event) {
			this.keyword = event.detail.value || ''
			clearTimeout(this.searchTimer)
			// 输入停止后再请求服务端，避免每个按键都触发网络查询。
			this.searchTimer = setTimeout(() => this.fetchWarehouse(true), 300)
		},
		selectCategory(category) {
			this.activeCategory = category
			this.fetchWarehouse(true)
		},
		loadMore() {
			if (!this.hasNextPage || this.loading || this.loadingMore) return
			// 页码仅在请求成功后提交，失败时用户可以直接重试当前下一页。
			this.fetchWarehouse(false)
		}
	}
}
</script>

<style scoped>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f5f7f7;
}

.warehouse-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f5f7f7;
	color: #263238;
	font-size: 14px;
}

.warehouse-header {
	box-sizing: border-box;
	/* 与农场管理页共用相同的渐变方向、颜色、背景高度和安全区间距。 */
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.warehouse-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.page-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	font-weight: 700;
	line-height: 58rpx;
	color: #ffffff;
}

.back-button,
.nav-placeholder {
	position: relative;
	z-index: 2;
	/* 扩展返回按钮命中区域至 44pt，图标本身尺寸保持不变。 */
	width: 36rpx;
	height: 58rpx;
}

.back-button {
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.warehouse-content {
	position: absolute;
	left: 0;
	right: 0;
	/* 与农场管理页使用相同内容起点，使渐变背景的可见高度保持一致。 */
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 34rpx 30rpx 42rpx;
}

.stats-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 22rpx;
}

.stat-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	flex-direction: column;
	justify-content: center;
	min-width: 0;
	height: 176rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(31, 78, 71, 0.07);
}

.stat-label {
	font-size: 12px;
	color: #8c979e;
}

.stat-value {
	max-width: 100%;
	margin-top: 14rpx;
	overflow: hidden;
	font-size: 23px;
	font-weight: 700;
	line-height: 1;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: #0a9f90;
}

.stat-unit {
	margin-top: 8rpx;
	font-size: 11px;
	color: #bdc6ca;
}

.search-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	height: 94rpx;
	margin-top: 24rpx;
	padding: 0 34rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(31, 78, 71, 0.06);
}

.search-icon {
	flex-shrink: 0;
	margin-right: 20rpx;
	font-size: 18px;
	color: #c5cdd2;
}

.search-input {
	flex: 1;
	min-width: 0;
	height: 94rpx;
	font-size: 14px;
	color: #37474f;
}

.search-placeholder {
	color: #c4cbd0;
}

.category-scroll {
	width: 100%;
	margin-top: 24rpx;
	white-space: nowrap;
}

.category-list {
	display: flex;
	width: max-content;
	padding-right: 30rpx;
}

.category-chip {
	box-sizing: border-box;
	min-width: 118rpx;
	height: 68rpx;
	margin-right: 16rpx;
	padding: 0 28rpx;
	border-radius: 34rpx;
	text-align: center;
	font-size: 13px;
	line-height: 68rpx;
	color: #64748b;
	background-color: #edf1f5;
}

.category-chip.active {
	font-weight: 600;
	color: #ffffff;
	background: linear-gradient(135deg, #27c9b8, #10a99c);
}

.category-chip-pressed {
	opacity: 0.78;
}

.item-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 24rpx;
	margin-top: 26rpx;
}

.item-card {
	min-width: 0;
	overflow: hidden;
	border-radius: 26rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(31, 78, 71, 0.08);
}

.image-wrap {
	position: relative;
	height: 290rpx;
	background: linear-gradient(180deg, #ffffff 0%, #eef4f5 100%);
}

.item-image {
	display: block;
	width: 100%;
	height: 100%;
}

.category-badge,
.warning-badge {
	position: absolute;
	top: 18rpx;
	box-sizing: border-box;
	height: 40rpx;
	padding: 0 16rpx;
	border-radius: 20rpx;
	font-size: 11px;
	line-height: 40rpx;
	color: #ffffff;
}

.category-badge {
	left: 18rpx;
	background-color: #16b9a9;
}

.category-2 { background-color: #46a5e5; }
.category-3 { background-color: #ef7c25; }
.category-4 { background-color: #7d8fa4; }
.category-5 { background-color: #526d82; }
.category-6 { background-color: #8b75c8; }

.warning-badge {
	right: 18rpx;
	background-color: #f04455;
}

.item-info {
	box-sizing: border-box;
	min-height: 174rpx;
	padding: 24rpx 24rpx 22rpx;
}

.item-title-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.item-name {
	flex: 1;
	min-width: 0;
	overflow: hidden;
	font-size: 15px;
	font-weight: 700;
	line-height: 1.3;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: #263238;
}

.stock-text {
	flex-shrink: 0;
	font-size: 12px;
	font-weight: 600;
	color: #0eb6a6;
}

.stock-text.warning {
	color: #ef3340;
}

.item-meta {
	display: block;
	margin-top: 12rpx;
	overflow: hidden;
	font-size: 11px;
	line-height: 1.25;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: #a9b3b9;
}

.state-card {
	box-sizing: border-box;
	margin-top: 30rpx;
	padding: 64rpx 24rpx;
	border-radius: 24rpx;
	text-align: center;
	font-size: 14px;
	color: #8c979e;
	background-color: #ffffff;
}

.load-more {
	margin: 28rpx auto 0;
	padding: 0 40rpx;
	border: 0;
	border-radius: 34rpx;
	background: #e1f5f1;
	color: #087f73;
	font-size: 13px;
}

.load-more::after { border: 0; }
.list-end { padding: 28rpx 0 8rpx; text-align: center; color: #a1aaa8; font-size: 12px; }

@media screen and (min-width: 768px) {
	.warehouse-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
