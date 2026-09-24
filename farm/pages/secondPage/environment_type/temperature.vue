<template>
	<view class="analysis-page">
		<view class="page-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">数据分析</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="analysis-content" scroll-y>
			<view class="analysis-overview">
				<view class="section-title">
					<view class="section-title-bar"></view>
					<text class="section-title-text">实时数据</text>
				</view>
				<view class="current-data">
					<text class="current-value">{{ realtimeData.value }}°C</text>
					<text class="current-label">{{ realtimeData.label }}</text>
					<text class="current-time">{{ realtimeData.time }}</text>
				</view>
				<view class="section-title statistics-title">
					<view class="section-title-bar"></view>
					<text class="section-title-text">数据统计</text>
				</view>
			</view>

			<view class="period-tabs">
				<view
					class="period-tab"
					v-for="period in periods"
					:key="period.value"
					:class="{ active: activePeriod === period.value }"
					@tap="switchPeriod(period.value)"
				>
					<text>{{ period.label }}</text>
				</view>
			</view>

			<view class="stat-card">
				<view class="stat-item" v-for="(item, index) in statItems" :key="item.label">
					<view class="stat-value-row">
						<text class="stat-value">{{ item.value }} °C</text>
						<view v-if="item.trend" class="stat-trend">
							<text class="iconfont icon-shangjiantou trend-icon"></text>
							<text class="trend-value">{{ item.trend }}</text>
						</view>
					</view>
					<text class="stat-label">{{ item.label }}</text>
					<view v-if="index < statItems.length - 1" class="stat-divider"></view>
				</view>
			</view>

			<view class="chart-card">
				<view class="chart-title-wrap">
					<text class="chart-title">温度曲线图</text>
				</view>
				<view
					id="temperatureChart"
					class="echarts-box"
					:option="chartOption"
					:change:option="chartRender.updateChart"
				></view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
export default {
	data() {
		return {
			activePeriod: 'week',
			periods: [
				{ label: '周', value: 'week' },
				{ label: '月', value: 'month' }
			],
			realtimeData: {
				value: '16.6',
				label: '当前温度',
				time: '2026-08-29 17:07:56'
			},
			statItems: [
				{ label: '最低值', value: 27 },
				{ label: '平均值', value: 32 },
				{ label: '最高值', value: 37, trend: 4 }
			],
			// 静态温度曲线数据；后续接入接口时只需替换对应周期的数据，不需要调整 UI 结构。
			chartDataMap: {
				week: {
					xLabels: ['2', '4', '6', '8', '10', '12', '14', '16', '18', '20', '22', '24'],
					values: [10, 10, 7, 8, 15, 18, 24, 22, 18, 16, 14, 11]
				},
				month: {
					xLabels: ['1', '5', '10', '15', '20', '25', '30'],
					values: [12, 15, 18, 22, 20, 17, 24]
				}
			}
		}
	},
	computed: {
		chartOption() {
			const chartData = this.chartDataMap[this.activePeriod] || this.chartDataMap.week

			return {
				animation: false,
				grid: {
					left: 8,
					right: 8,
					top: 18,
					bottom: 22,
					containLabel: true
				},
				xAxis: {
					type: 'category',
					boundaryGap: false,
					data: chartData.xLabels,
					axisTick: {
						show: false
					},
					axisLine: {
						show: true,
						lineStyle: {
							color: '#4b4b4b'
						}
					},
					axisLabel: {
						color: '#000000',
						fontSize: 12
					}
				},
				yAxis: {
					type: 'value',
					min: 0,
					max: 30,
					interval: 5,
					axisLine: {
						show: true,
						lineStyle: {
							color: '#4b4b4b'
						}
					},
					axisTick: {
						show: false
					},
					axisLabel: {
						color: '#000000',
						fontSize: 12
					},
					splitLine: {
						lineStyle: {
							color: '#ededed'
						}
					}
				},
				series: [
					{
						type: 'line',
						data: chartData.values,
						showSymbol: true,
						symbol: 'circle',
						symbolSize: 6,
						smooth: true,
						itemStyle: {
							color: '#ffffff',
							borderColor: '#1BA291',
							borderWidth: 2
						},
						lineStyle: {
							width: 2,
							color: '#1BA291'
						}
					}
				]
			}
		}
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		switchPeriod(period) {
			// 当前页面为静态展示，切换按钮先保留选中态；后续可按 period 请求周/月统计数据。
			this.activePeriod = period
		}
	}
}
</script>

<script module="chartRender" lang="renderjs">
import * as echarts from 'echarts'

export default {
	data() {
		return {
			chart: null
		}
	},
	mounted() {
		this.updateChart(this.option)
	},
	beforeDestroy() {
		this.disposeChart()
	},
	beforeUnmount() {
		this.disposeChart()
	},
	methods: {
		initChart() {
			if (this.chart) {
				return
			}
			const chartDom = document.getElementById('temperatureChart')
			if (!chartDom) {
				return
			}
			this.chart = echarts.init(chartDom)
			window.addEventListener('resize', this.resizeChart)
		},
		updateChart(option) {
			// renderjs 运行在视图层，适合承载 ECharts 这类依赖 DOM/canvas 的图表实例。
			setTimeout(() => {
				this.initChart()
				if (this.chart && option) {
					this.chart.setOption(option, true)
				}
			}, 0)
		},
		resizeChart() {
			if (this.chart) {
				this.chart.resize()
			}
		},
		disposeChart() {
			window.removeEventListener('resize', this.resizeChart)
			if (this.chart) {
				this.chart.dispose()
				this.chart = null
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.analysis-page {
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
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.analysis-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 104rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 48rpx;
}

.analysis-overview {
	box-sizing: border-box;
	margin-bottom: 34rpx;
	padding-top: 0;
}

.section-title {
	display: flex;
	align-items: center;
	height: 50rpx;
}

.section-title-bar {
	width: 16rpx;
	height: 48rpx;
	margin-right: 16rpx;
	border-radius: 16rpx;
	background-color: #1BA291;
}

.section-title-text {
	font-size: 16px;
	line-height: 1.25;
	color: #1BA291;
}

.current-data {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 12rpx;
}

.current-value {
	font-size: 24px;
	line-height: 1.2;
	color: #1BA291;
}

.current-label {
	margin-top: 10rpx;
	font-size: 10px;
	line-height: 1.2;
	color: #1BA291;
}

.current-time {
	margin-top: 14rpx;
	font-size: 10px;
	line-height: 1.2;
	color: #C8C8C8;
}

.statistics-title {
	margin-top: 24rpx;
}

.period-tabs {
	display: flex;
	align-items: center;
	height: 84rpx;
	overflow: hidden;
	border-radius: 44rpx;
	background-color: #ffffff;
}

.period-tab {
	display: flex;
	flex: 1;
	align-items: center;
	justify-content: center;
	height: 84rpx;
	font-size: 14px;
	line-height: 1.2;
	color: #C8C8C8;
}

.period-tab.active {
	border-radius: 44rpx;
	background-color: #6FC4BA;
	color: #ffffff;
}

.stat-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 116rpx;
	margin-top: 48rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
}

.stat-item {
	position: relative;
	display: flex;
	flex: 1;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-width: 0;
}

.stat-value-row {
	position: relative;
	display: flex;
	align-items: flex-start;
	justify-content: center;
}

.stat-value {
	font-size: 14px;
	line-height: 1.2;
	color: #6FC4BA;
}

.stat-unit {
	margin-left: 4rpx;
	font-size: 14px;
	line-height: 1.3;
	color: #C8C8C8;
}

.stat-trend {
	position: absolute;
	left: calc(100% + 2rpx);
	top: -8rpx;
	display: flex;
	align-items: center;
	color: #CA765A;
}

.trend-icon {
	font-size: 10px;
	line-height: 1;
	color: #CA765A;
}

.trend-value {
	margin-left: 2rpx;
	font-size: 10px;
	line-height: 1;
	color: #CA765A;
}

.stat-label {
	margin-top: 12rpx;
	font-size: 14px;
	line-height: 1.2;
	color: #000000;
}

.stat-divider {
	position: absolute;
	right: 0;
	top: 22rpx;
	width: 4rpx;
	height: 64rpx;
	border-radius: 4rpx;
	background-color: #1BA291;
}

.chart-card {
	box-sizing: border-box;
	margin-top: 48rpx;
	padding: 54rpx 16rpx 42rpx;
	background-color: #ffffff;
}

.chart-title-wrap {
	display: flex;
	align-items: center;
	justify-content: center;
}

.chart-title {
	font-size: 14px;
	line-height: 1.25;
	color: #000000;
}

.echarts-box {
	width: 100%;
	height: 324rpx;
	margin-top: 26rpx;
}

@media screen and (min-width: 768px) {
	.analysis-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
