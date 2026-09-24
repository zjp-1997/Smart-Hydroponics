<template>
	<view class="record-page">
		<view class="page-hero">
			<view class="navbar">
				<view class="back-action" role="button" aria-label="返回我的页面" @tap="handleBack">
					<text class="iconfont icon-fanhui"></text>
				</view>
				<text class="nav-title">仓库记录</text>
				<view class="nav-spacer"></view>
			</view>
		</view>

		<view class="record-panel">
			<!-- 筛选栏固定在列表上方，切换类型时从第一页重新查询。 -->
			<view class="filter-tabs">
				<view v-for="tab in tabs" :key="tab.label" class="filter-tab"
					:class="{ active: recordType === tab.value }" role="button"
					:aria-label="`查看${tab.label}记录`" :aria-pressed="recordType === tab.value"
					@tap="selectType(tab.value)">{{ tab.label }}</view>
			</view>

			<scroll-view class="record-scroll" scroll-y :show-scrollbar="false" lower-threshold="80"
				@scrolltolower="loadMore">
				<view v-if="loading && !records.length" class="state-card">正在加载仓库记录...</view>
				<view v-else-if="error && !records.length" class="state-card">
					<text>记录加载失败，请检查网络后重试</text>
					<view class="retry-button" role="button" @tap="reload">重新加载</view>
				</view>
				<view v-else-if="!records.length" class="state-card">
					<text>暂无{{ activeLabel }}记录</text>
					<text class="state-hint">有新的仓库出入库操作后会显示在这里</text>
				</view>
				<view v-for="record in records" :key="record.id" class="record-card">
					<view class="card-top">
						<view class="type-mark" :class="record.recordType === 1 ? 'inbound' : 'outbound'">
							{{ record.recordType === 1 ? '入库' : '出库' }}
						</view>
						<text class="record-time">{{ record.recordTime || '-' }}</text>
					</view>
					<view class="item-line">
						<text class="item-name">{{ record.itemName }}</text>
						<text class="quantity" :class="record.recordType === 1 ? 'inbound-text' : 'outbound-text'">
							{{ record.recordType === 1 ? '+' : '-' }}{{ record.quantity }}{{ record.itemUnit }}
						</text>
					</view>
					<text v-if="record.itemCode" class="item-code">编号 {{ record.itemCode }}</text>
					<view class="detail-line">
						<text>操作人 {{ record.operator }}</text>
						<text>结存 {{ record.afterQty }}{{ record.itemUnit }}</text>
					</view>
					<text v-if="record.recipient" class="extra-line">领用人 {{ record.recipient }}</text>
					<text v-if="record.remark" class="extra-line">备注 {{ record.remark }}</text>
				</view>
				<view v-if="loading && records.length" class="list-footer">正在加载更多...</view>
					<view v-else-if="error && records.length" class="list-footer retry-footer" @tap="retryMore">加载失败，点击重试</view>
				<view v-else-if="records.length && !hasMore" class="list-footer">没有更多记录了</view>
				<view class="safe-bottom"></view>
			</scroll-view>
		</view>
	</view>
</template>

<script>
import { getWarehouseRecords } from '@/api/warehouse.js'
import { ensureNonExpertAccess } from '@/utils/expertAccess.js'

const PAGE_SIZE = 10

export default {
	data() {
		return {
			// 三个筛选共用同一分页接口，null 表示同时查询入库和出库。
			tabs: [{ label: '全部', value: null }, { label: '入库', value: 1 }, { label: '出库', value: 2 }],
			recordType: null,
			records: [],
			pageNum: 1,
			hasMore: true,
			loading: false,
			error: false,
			requestVersion: 0
		}
	},
	computed: {
		activeLabel() {
			return this.tabs.find((tab) => tab.value === this.recordType)?.label || '仓库'
		}
	},
	async onShow() {
		// 从其他页面返回时刷新流水；后端会再次校验角色和物资归属。
		if (!await ensureNonExpertAccess()) return
		this.reload()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		selectType(value) {
			if (this.recordType === value) return
			this.recordType = value
			this.reload()
		},
		reload() {
			// 递增版本号使旧筛选请求即使后返回，也不会覆盖当前列表。
			this.requestVersion += 1
			this.records = []
			this.pageNum = 1
			this.hasMore = true
			this.loading = false
			this.error = false
			this.loadMore()
		},
		retryMore() {
			// 分页失败仅在用户主动点击时重试，避免列表停在底部时反复自动请求。
			this.error = false
			this.loadMore()
		},
		async loadMore() {
			if (this.loading || !this.hasMore || this.error) return
			const version = this.requestVersion
			this.loading = true
			this.error = false
			try {
				const page = await getWarehouseRecords({
					recordType: this.recordType,
					pageNum: this.pageNum,
					pageSize: PAGE_SIZE
				})
				if (version !== this.requestVersion) return
				// 成功后才推进页码，网络失败时重试仍请求同一页。
				this.records = [...this.records, ...page.items]
				this.hasMore = page.hasNextPage
				this.pageNum += 1
			} catch (error) {
				if (version === this.requestVersion) this.error = true
			} finally {
				if (version === this.requestVersion) this.loading = false
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page { height: 100%; background: #f5f7f7; }
.record-page { position: fixed; inset: 0; overflow: hidden; color: #263238; font-size: 14px; background: #f5f7f7; }
.page-hero { box-sizing: border-box; height: 300rpx; padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0; background: linear-gradient(180deg, rgba(27, 162, 145, .7), rgba(90, 184, 173, 0)); }
.navbar { display: flex; align-items: center; justify-content: space-between; height: 88rpx; }
.back-action, .nav-spacer { display: flex; align-items: center; width: 88rpx; height: 88rpx; }
.back-action { color: white; font-size: 18px; }
.nav-title { color: white; font-size: 18px; font-weight: 700; }
.record-panel { position: absolute; top: calc(var(--status-bar-height) + 136rpx); right: 0; bottom: 0; left: 0; display: flex; flex-direction: column; overflow: hidden; }
.filter-tabs { display: flex; flex-shrink: 0; margin: 0 30rpx 20rpx; padding: 8rpx; border-radius: 20rpx; background: white; box-shadow: 0 8rpx 24rpx rgba(31, 78, 71, .07); }
.filter-tab { flex: 1; min-height: 72rpx; line-height: 72rpx; text-align: center; border-radius: 16rpx; color: #637572; }
.filter-tab.active { background: #6fc4ba; color: white; font-weight: 600; }
.record-scroll { flex: 1; min-height: 0; box-sizing: border-box; padding: 0 30rpx; }
.record-card { box-sizing: border-box; margin-bottom: 20rpx; padding: 28rpx 30rpx; border-radius: 22rpx; background: white; box-shadow: 0 8rpx 24rpx rgba(31, 78, 71, .06); }
.card-top, .item-line, .detail-line { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.type-mark { padding: 7rpx 18rpx; border-radius: 99rpx; font-size: 12px; font-weight: 600; }
.type-mark.inbound { color: #258d7f; background: #e3f5f1; }
.type-mark.outbound { color: #b77638; background: #fff0de; }
.record-time { font-size: 12px; color: #83918e; }
.item-line { align-items: baseline; margin-top: 22rpx; }
.item-name { min-width: 0; font-size: 16px; font-weight: 600; word-break: break-all; }
.quantity { flex-shrink: 0; font-size: 17px; font-weight: 700; }
.inbound-text { color: #258d7f; }
.outbound-text { color: #b77638; }
.item-code { display: block; margin-top: 9rpx; color: #899995; font-size: 12px; }
.detail-line { margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid #eef2f1; color: #60716d; font-size: 12px; }
.extra-line { display: block; margin-top: 12rpx; color: #60716d; font-size: 12px; word-break: break-all; }
.state-card { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 300rpx; margin-top: 18rpx; border-radius: 22rpx; background: white; color: #60716d; text-align: center; }
.state-hint { margin-top: 16rpx; color: #9aa8a4; font-size: 12px; }
.retry-button { min-width: 160rpx; min-height: 76rpx; margin-top: 20rpx; line-height: 76rpx; color: #258d7f; }
.list-footer { min-height: 84rpx; line-height: 84rpx; color: #9aa8a4; text-align: center; font-size: 12px; }
.retry-footer { color: #258d7f; }
.safe-bottom { height: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
