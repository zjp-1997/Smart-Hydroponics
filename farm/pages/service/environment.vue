<template>
	<view class="environment-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">环境监测</text>
				<picker
					class="plot-picker"
					mode="multiSelector"
					:range="selectorRange"
					:value="selectorValue"
					@columnchange="handleSelectorColumnChange"
					@change="handleSelectorChange"
					@cancel="syncSelectorToCurrent"
				>
					<view class="plot-select">
						<text class="plot-select-text">{{ selectedPlotName }}</text>
						<text class="iconfont icon-xuanzeqishouqi_o plot-select-icon"></text>
					</view>
				</picker>
			</view>
			<view class="device-status">
				<view class="status-bar"></view>
				<text class="status-name">气象监测</text>
				<view class="status-dot"></view>
				<text class="status-text">在线</text>
			</view>
		</view>

		<scroll-view class="environment-content" scroll-y>
			<view class="monitor-section">
				<view class="monitor-grid">
					<view class="monitor-card" v-for="item in weatherItems" :key="item.label" @tap="handleMonitorTap(item)">
						<view class="monitor-info">
							<text class="monitor-label">{{ item.label }}</text>
							<view class="value-row">
								<text class="monitor-value">{{ item.value }}</text>
								<view v-if="item.trend" class="trend-wrap">
									<text class="iconfont icon-shangjiantou trend-icon"></text>
									<text class="trend-text">{{ item.trend }}</text>
								</view>
							</view>
						</view>
						<text class="iconfont monitor-icon" :class="item.icon"></text>
					</view>
				</view>
			</view>

			<view class="monitor-section water-section">
				<view class="device-status section-status">
					<view class="status-bar"></view>
					<text class="status-name">水质监测</text>
					<view class="status-dot"></view>
					<text class="status-text">在线</text>
				</view>
				<view class="monitor-grid">
					<view class="monitor-card" v-for="item in waterQualityItems" :key="item.label" @tap="handleMonitorTap(item)">
						<view class="monitor-info">
							<text class="monitor-label">{{ item.label }}</text>
							<view class="value-row">
								<text class="monitor-value">{{ item.value }}</text>
								<view v-if="item.trend" class="trend-wrap">
									<text class="iconfont icon-shangjiantou trend-icon"></text>
									<text class="trend-text">{{ item.trend }}</text>
								</view>
							</view>
						</view>
						<text class="iconfont monitor-icon" :class="item.icon"></text>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getEnvironmentListByPlotId } from '@/api/environmentList.js'
import { getFarmList } from '@/api/farmList.js'
import { getAllPlotList, getPlotListByFarmId } from '@/api/plotList.js'

export default {
	data() {
		return {
			requestedPlotId: null,
			farms: [],
			plots: [],
			plotMap: {},
			selectorFarmIndex: 0,
			selectorPlotIndex: 0,
			selectorPlots: [],
			selectedFarm: null,
			selectedPlot: null,
			weatherItems: [],
			waterQualityItems: []
		}
	},
	computed: {
		selectedPlotName() {
			return this.formatPlotName(this.selectedPlot && this.selectedPlot.plotName)
		},
		selectorRange() {
			const farmNames = this.farms.map((farm) => farm.farmName || '未命名农场')
			const plotNames = this.selectorPlots.map((plot) => plot.plotName || '未命名地块')
			return [farmNames, plotNames]
		},
		selectorValue() {
			return [this.selectorFarmIndex, this.selectorPlotIndex]
		}
	},
	onLoad(options) {
		this.requestedPlotId = this.normalizeRouteId(options && options.plotId)
		this.initEnvironmentPage()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		initEnvironmentPage() {
			// 同时读取农场和全部地块；从详情页进入时优先定位路由中的 plotId。
			Promise.all([getFarmList(), getAllPlotList()])
				.then(([farms, plots]) => {
					this.farms = farms
					this.plots = plots
					this.plotMap = this.groupPlotsByFarm(plots)
					const requestedPlot = this.requestedPlotId
						? plots.find((plot) => this.isSameId(plot.id, this.requestedPlotId))
						: null

					if (this.requestedPlotId && !requestedPlot) {
						this.selectedFarm = null
						this.selectedPlot = null
						this.clearEnvironmentList()
						uni.showToast({
							title: '未找到指定地块',
							icon: 'none'
						})
						return
					}

					const defaultPlot = requestedPlot || plots.find((plot) => this.isDefaultPlot(plot)) || plots[0] || null
					const defaultFarm = defaultPlot
						? farms.find((farm) => this.isSameId(farm.id, defaultPlot.farmId))
						: farms[0] || null

					this.selectedFarm = defaultFarm || null
					if (defaultPlot) {
						this.selectedPlot = defaultPlot
						this.syncSelectorToCurrent()
						this.fetchEnvironmentList(defaultPlot.id)
						return
					}

					if (defaultFarm) {
						this.loadPlotsByFarm(defaultFarm, false)
						return
					}

					this.clearEnvironmentList()
				})
				.catch(() => {
					this.clearEnvironmentList()
					uni.showToast({
						title: '环境数据加载失败',
						icon: 'none'
					})
				})
		},
		loadPlotsByFarm(farm, showSelector) {
			if (!farm || !farm.id) {
				this.clearEnvironmentList()
				return
			}

			// 农场切换后重新读取该农场下的地块，确保用户只能选择自己名下农场的地块。
			getPlotListByFarmId(farm.id)
				.then((plots) => {
					this.plots = plots
					this.setFarmPlots(farm.id, plots)
					if (!plots.length) {
						this.selectedPlot = null
						this.selectorPlots = []
						this.selectorPlotIndex = 0
						this.clearEnvironmentList()
						uni.showToast({
							title: '该农场暂无地块',
							icon: 'none'
						})
						return
					}

					if (showSelector) {
						this.selectorPlots = plots
						this.selectorPlotIndex = 0
						return
					}

					const defaultPlot = plots.find((plot) => this.isDefaultPlot(plot)) || plots[0]
					this.selectedPlot = defaultPlot
					this.syncSelectorToCurrent()
					this.fetchEnvironmentList(defaultPlot.id)
				})
				.catch(() => {
					this.clearEnvironmentList()
					uni.showToast({
						title: '地块数据加载失败',
						icon: 'none'
					})
				})
		},
		handleSelectorColumnChange(event) {
			const { column, value } = event.detail
			if (column === 0) {
				this.selectorFarmIndex = value
				this.selectorPlotIndex = 0
				const farm = this.farms[value]
				const plots = this.getFarmPlots(farm)
				this.selectorPlots = plots
				if (!plots.length) {
					this.loadFarmPlotsForPicker(farm)
				}
				return
			}

			this.selectorPlotIndex = value
		},
		handleSelectorChange(event) {
			const [farmIndex, plotIndex] = event.detail.value
			const farm = this.farms[farmIndex]
			const plots = this.getFarmPlots(farm)
			const plot = plots[plotIndex]

			if (!farm || !plot) {
				uni.showToast({
					title: '请选择有效地块',
					icon: 'none'
				})
				this.syncSelectorToCurrent()
				return
			}

			this.selectorFarmIndex = farmIndex
			this.selectorPlotIndex = plotIndex
			this.selectorPlots = plots
			this.selectedFarm = farm
			this.selectedPlot = plot
			this.fetchEnvironmentList(plot.id)
		},
		fetchEnvironmentList(plotId) {
			if (!plotId) {
				this.clearEnvironmentList()
				return
			}

			// 页面只负责渲染卡片；接口字段过滤、单位拼接统一收敛在 api/environmentList.js。
			getEnvironmentListByPlotId(plotId)
				.then((data) => {
					this.weatherItems = data.weatherItems
					this.waterQualityItems = data.waterQualityItems
				})
				.catch(() => {
					this.clearEnvironmentList()
					uni.showToast({
						title: '监测数据加载失败',
						icon: 'none'
					})
				})
		},
		clearEnvironmentList() {
			// 清空列表而不是回退到静态数据，避免页面展示与当前用户无关的监测内容。
			this.weatherItems = []
			this.waterQualityItems = []
		},
		groupPlotsByFarm(plots) {
			return plots.reduce((result, plot) => {
				if (!plot || !plot.farmId) {
					return result
				}
				const key = String(plot.farmId)
				if (!result[key]) {
					result[key] = []
				}
				result[key].push(plot)
				return result
			}, {})
		},
		getFarmPlots(farm) {
			if (!farm || !farm.id) {
				return []
			}
			return this.plotMap[String(farm.id)] || []
		},
		setFarmPlots(farmId, plots) {
			const key = String(farmId)
			// Vue2 小程序端需要 $set 才能保证对象新增 key 后视图同步更新。
			if (this.$set) {
				this.$set(this.plotMap, key, plots)
				return
			}
			this.plotMap = {
				...this.plotMap,
				[key]: plots
			}
		},
		loadFarmPlotsForPicker(farm) {
			if (!farm || !farm.id) {
				return
			}
			// 当全部地块列表没有覆盖当前农场时，按农场补拉一次地块，保持选择器第二列可用。
			getPlotListByFarmId(farm.id)
				.then((plots) => {
					this.setFarmPlots(farm.id, plots)
					if (this.farms[this.selectorFarmIndex] && this.isSameId(this.farms[this.selectorFarmIndex].id, farm.id)) {
						this.selectorPlots = plots
						this.selectorPlotIndex = 0
					}
				})
				.catch(() => {
					uni.showToast({
						title: '地块数据加载失败',
						icon: 'none'
					})
				})
		},
		syncSelectorToCurrent() {
			const farmIndex = this.farms.findIndex((farm) => this.isSameId(farm.id, this.selectedFarm && this.selectedFarm.id))
			this.selectorFarmIndex = farmIndex > -1 ? farmIndex : 0
			const farm = this.farms[this.selectorFarmIndex]
			const plots = this.getFarmPlots(farm)
			this.selectorPlots = plots
			const plotIndex = plots.findIndex((plot) => this.isSameId(plot.id, this.selectedPlot && this.selectedPlot.id))
			this.selectorPlotIndex = plotIndex > -1 ? plotIndex : 0
		},
		formatPlotName(plotName) {
			if (!plotName) {
				return '地块03'
			}
			const matched = String(plotName).match(/\d+/)
			return matched ? `地块${matched[0].padStart(2, '0')}` : plotName
		},
		normalizeRouteId(value) {
			if (value === null || value === undefined || value === '') {
				return null
			}
			return decodeURIComponent(String(value)).trim() || null
		},
		isDefaultPlot(plot) {
			return /0?3号/.test(plot.plotName || '') || this.formatPlotName(plot.plotName) === '地块03'
		},
		isSameId(left, right) {
			return String(left) === String(right)
		},
		handleMonitorTap(item) {
			// 当前需求中任意环境指标都进入同一个温度图表静态分析页，后续可通过 query 扩展为不同指标图表。
			uni.navigateTo({
				url: `/pages/secondPage/environment_type/temperature?type=${encodeURIComponent(item.label)}`
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

.environment-page {
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

.nav-icon {
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

.environment-page .plot-select {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	justify-content: flex-end;
	height: 58rpx;
	color: #ffffff;
}

.environment-page .plot-picker {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	height: 58rpx;
	margin-top: 4px;
	padding: 0;
	border: 0;
	background: transparent;
}

.environment-page .plot-select-text {
	font-size: 16px;
	line-height: 1;
	color: #ffffff;
}

.environment-page .plot-select-icon {
	margin-left: 8rpx;
	font-size: 20rpx;
	line-height: 1;
	color: #ffffff;
}

.device-status {
	display: flex;
	align-items: center;
	margin-top: 58rpx;
	height: 48rpx;
	color: #1BA291;
}

.status-bar {
	width: 16rpx;
	height: 48rpx;
	margin-right: 20rpx;
	border-radius: 16rpx;
	background-color: #1BA291;
}

.status-name,
.status-text {
	font-size: 16px;
	line-height: 1.25;
	color: #1BA291;
}

.status-text {
	font-size: 12px;
}

.status-dot {
	width: 14rpx;
	height: 14rpx;
	margin: 0 14rpx 0 26rpx;
	border-radius: 50%;
	background-color: #1BA291;
}

.environment-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 212rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 44rpx;
}

.monitor-section {
	box-sizing: border-box;
}

.water-section {
	margin-top: 34rpx;
}

.section-status {
	margin-top: 0;
	margin-bottom: 22rpx;
}

.monitor-grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 22rpx 16rpx;
}

.monitor-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 150rpx;
	padding: 24rpx 20rpx 22rpx;
	border-radius: 12rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.04);
}

.monitor-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
}

.monitor-label {
	font-size: 14px;
	line-height: 1.25;
	color: #000000;
}

.value-row {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	margin-top: 20rpx;
}

.monitor-value {
	font-size: 12px;
	line-height: 1.25;
	color: #1BA291;
}

.trend-wrap {
	display: flex;
	align-items: center;
	margin-top: 4rpx;
	color: #1BA291;
}

.trend-icon {
	font-size: 14rpx;
	line-height: 14rpx;
	color: #1BA291;
}

.trend-text {
	margin-left: 4rpx;
	font-size: 12px;
	line-height: 1.25;
	color: #1BA291;
}

.monitor-icon {
	flex-shrink: 0;
	margin-left: 8rpx;
	font-size: 64rpx;
	line-height: 64rpx;
	color: #333333;
}

@media screen and (min-width: 768px) {
	.environment-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
