<template>
	<view class="home-page">
		<view class="hero">
			<view class="greeting">
				<view class="hello">早上好</view>
				<view class="welcome">欢迎登录智能化农业</view>
			</view>
			<view class="weather-art">
				<text
					v-for="layer in weatherArt.layers"
					:key="layer.key"
					class="iconfont weather-art-icon"
					:class="[layer.icon, layer.className]"
					:style="{ color: layer.color }"
				></text>
			</view>
		</view>

		<view class="weather-strip">
			<view class="weather-item" v-for="(item, index) in weatherItems" :key="item.label">
				<text class="iconfont weather-icon" :class="item.icon"></text>
				<text class="weather-value">{{ item.value }}</text>
				<view v-if="index < weatherItems.length - 1" class="weather-divider"></view>
			</view>
		</view>

		<scroll-view
			class="content"
			scroll-y
			:enhanced="true"
			:bounces="false"
			:show-scrollbar="false"
		>
			<view class="section-head">
				<text class="section-title">{{ isOrdinaryUser ? '所属农场地块' : '我的地块' }}</text>
				<view class="section-more" @tap="handleMorePlots">
					<text>全部地块</text>
					<text class="iconfont icon-youjiantou arrow-icon"></text>
				</view>
			</view>

			<scroll-view class="plot-list" scroll-x :show-scrollbar="false">
				<view class="plot-scroll-inner">
					<view class="plot-card" v-for="plot in plots" :key="plot.id" @tap="handlePlot(plot)">
						<protected-image class="plot-image" :src="plot.image || fallbackPlotImage" mode="aspectFill"></protected-image>
						<view class="plot-status">{{ plot.status }}</view>
						<view class="plot-name">{{ plot.name }}</view>
						<view class="plot-meta">
							<text>{{ plot.days }}</text>
							<text>{{ plot.landNo }}</text>
						</view>
					</view>
				</view>
			</scroll-view>

			<view class="task-card" @tap="handleTask">
				<view class="task-head">
					<view class="task-title-wrap">
						<text class="iconfont icon-renwu task-icon"></text>
						<text class="task-title">农事提醒</text>
					</view>
					<view class="task-more">
						<text>{{ isOrdinaryUser ? '我的任务' : '管理任务' }}</text>
						<text class="iconfont icon-youjiantou arrow-icon"></text>
					</view>
				</view>
				<view class="task-stats">
					<view class="task-stat" :class="{ 'task-stat-overdue': item.overdue }" v-for="item in taskStats" :key="item.label">
						<text class="task-count">{{ item.count }}</text>
						<text class="task-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="service-title">常用服务</view>
			<view class="service-grid">
				<view class="service-card" v-for="service in visibleServices" :key="service.title" @tap="handleService(service)">
					<view class="service-icon-wrap">
						<text class="iconfont service-icon" :class="[service.icon, service.iconClass]"></text>
					</view>
					<view class="service-copy">
						<text class="service-name">{{ service.title }}</text>
						<text class="service-desc">{{ service.desc }}</text>
					</view>
				</view>
			</view>
			<!-- 为固定底部导航保留滚动空间，确保常用服务最后一行可完整显示。 -->
			<!-- <view class="home-bottom-spacer"></view> -->
		</scroll-view>

	</view>
</template>

<script>
import { ensureNonExpertAccess } from '@/utils/expertAccess.js'
import { getUserInfo } from '@/utils/auth.js'
import { getCurrentWeather } from '@/api/homeWeather.js'
import { getAllPlotList } from '@/api/plotList.js'
import { getFarmTaskStatistics } from '@/api/taskList.js'

export default {
	data() {
		return {
			fallbackPlotImage: '/static/lecttue.png',
			roleCode: '',
			currentWeatherName: '',
			weatherItems: [
				{ label: '位置', value: '定位中', icon: 'icon-dingwei' },
				{ label: '温度', value: '--', icon: 'icon-wendu' },
				{ label: '湿度', value: '--', icon: 'icon-shidu' },
				{ label: 'PM2.5', value: '--', icon: 'icon-yezi_leaf-01' }
			],
			plots: [],
			taskStats: [
				{ count: 0, label: '未开始' },
				{ count: 0, label: '进行中' },
				{ count: 0, label: '已完成' },
				{ count: 0, label: '已逾期', overdue: true }
			],
			services: [
				{ title: '农场管理', desc: '农场信息管理', icon: 'icon-ziyuan', path: '/pages/service/farm_list' },
				{ title: '设备管理', desc: '远程操控设备', icon: 'icon-shebeiguanli', path: '/pages/service/device_list' },
				{ title: '实时监控', desc: '实时查看状态', icon: 'icon-shishijiankong', path: '/pages/service/monitor_list' },
				{ title: '环境监测', desc: '监测环境数据', icon: 'icon-huanjingjiance', path: '/pages/service/environment' },
				{ title: '病害防治', desc: '病害防治知识', icon: 'icon-bingchonghai', path: '/pages/service/disease_control' },
				{ title: '智能方案', desc: '农作物未来预期', icon: 'icon-icon_haituntiaodu', path: '/pages/service/strategy' },
				{ title: '专家咨询', desc: '作物相关知识', icon: 'icon-zhuanjia', path: '/pages/service/expert_list' },
				{ title: '农事管理', desc: '执行农事任务', icon: 'icon-renwu-', path: '/pages/service/task_list' },
				{ title: '图片管理', desc: '管理采集图片', icon: 'icon-morentupian-80pt', path: '/pages/service/picture_list' },
				// 仓库卡片进入独立仓库管理页；仅补充路由，不改变首页卡片 UI。
				{ title: '仓库管理', desc: '仓库物资管理', icon: 'icon-cangkuguanli', iconClass: 'warehouse-icon', path: '/pages/service/wareHouse' }
			]
		}
	},
	computed: {
		isOrdinaryUser() {
			return this.roleCode === 'user'
		},
		visibleServices() {
			// 普通用户保留查看、本人农事、咨询和只读仓库入口，管理入口只给农场主。
			return this.isOrdinaryUser
				? this.services.filter((item) => !['设备管理', '图片管理'].includes(item.title))
					.map((item) => item.title === '农场管理'
						? { ...item, title: '农场信息', desc: '查看所属农场' }
						: item.title === '仓库管理' ? { ...item, title: '仓库物资', desc: '查看所属农场物资' } : item)
				: this.services
		},
		weatherArt() {
			// 顶部天气装饰图标跟随后端实时天气变化。
			return this.resolveWeatherArt(this.currentWeatherName)
		}
	},
	onLoad() {
		// 本地角色仅用于避免误入时发起农场主数据请求，onShow 仍由服务端核实。
		if (getUserInfo()?.roleCode !== 'expert') this.fetchHomePlots()
	},
	async onShow() {
		if (!await ensureNonExpertAccess()) return
		this.roleCode = String(getUserInfo()?.roleCode || '').toLowerCase()
		// 原生 tabBar 页面会被缓存，每次回到首页都按当前位置刷新天气和任务统计。
		this.fetchHomeWeather()
		this.fetchHomeTaskStatistics()
	},
	methods: {
		fetchHomeTaskStatistics() {
			return getFarmTaskStatistics()
				.then((statistics) => {
					this.taskStats = [
						{ count: statistics.pendingCount, label: '未开始' },
						{ count: statistics.runningCount, label: '进行中' },
						{ count: statistics.completedCount, label: '已完成' },
						{ count: statistics.overdueCount, label: '已逾期', overdue: true }
					]
				})
				.catch(() => {
					// 请求层已统一提示错误；首页保留上一次成功数据，避免短暂网络波动导致数字闪零。
				})
		},
		fetchHomeWeather() {
			// 首页天气为辅助数据，定位或第三方接口失败时只展示占位值，不再闪现演示静态数据。
			this.getHomeLocation()
				.then((location) => getCurrentWeather(location))
				.then((weather) => {
					this.currentWeatherName = weather.weather
					this.weatherItems = [
						{ label: '位置', value: weather.location, icon: 'icon-dingwei' },
						{ label: '温度', value: this.formatWeatherValue(weather.temperature, '°C'), icon: 'icon-wendu' },
						{ label: '湿度', value: this.formatWeatherValue(weather.humidity, '%'), icon: 'icon-shidu' },
						{ label: 'PM2.5', value: weather.pm25, icon: 'icon-yezi_leaf-01' }
					]
				})
				.catch(() => {
					this.currentWeatherName = ''
					this.weatherItems = [
						{ label: '位置', value: '定位失败', icon: 'icon-dingwei' },
						{ label: '温度', value: '--', icon: 'icon-wendu' },
						{ label: '湿度', value: '--', icon: 'icon-shidu' },
						{ label: 'PM2.5', value: '--', icon: 'icon-yezi_leaf-01' }
					]
				})
		},
		formatWeatherValue(value, unit) {
			// 第三方暂无数据时直接展示 --，避免拼成 --°C 或 --% 影响首页观感。
			return value && value !== '--' ? `${value}${unit}` : '--'
		},
		resolveWeatherArt(weatherName) {
			const sunColor = '#F8D448'
			const whiteColor = '#FFFFFF'
			const weather = weatherName || ''
			const singleLayer = (icon, color = whiteColor, className = '') => ({
				layers: [{ key: 'main', icon, color, className }]
			})
			const cloudyLayer = () => ({
				// 多云图标使用太阳层和云层叠加，确保太阳部分按需求显示为 #F8D448。
				layers: [
					{ key: 'sun', icon: 'icon-qing', color: sunColor, className: 'weather-art-sun' },
					{ key: 'cloud', icon: 'icon-yintian', color: whiteColor, className: 'weather-art-cloud' }
				]
			})
			const thunderLayer = () => ({
				// iconfont 合成图标无法拆分局部路径，这里使用裁剪强调层让雷电区域呈现指定黄色。
				layers: [
					{ key: 'rain', icon: 'icon-leizhenyu', color: '#EAF7FF', className: '' },
					{ key: 'lightning', icon: 'icon-leizhenyu', color: sunColor, className: 'weather-art-thunder-accent' }
				]
			})
			// 映射顺序按用户指定天气顺序维护，复杂天气优先匹配更具体的关键字。
			const weatherIconRules = [
				{ keywords: ['多云', '少云', '晴间多云'], art: cloudyLayer },
				{ keywords: ['晴'], art: () => singleLayer('icon-qing', sunColor) },
				{ keywords: ['阴天', '阴'], art: () => singleLayer('icon-yintian', '#F2F7F6') },
				{ keywords: ['雷阵雨', '雷暴'], art: thunderLayer },
				{ keywords: ['阵雨'], art: () => singleLayer('icon-zhenyu', '#EAF7FF') },
				{ keywords: ['暴雪'], art: () => singleLayer('icon-baoxue', '#F4FBFF') },
				{ keywords: ['大雪'], art: () => singleLayer('icon-daxue', '#F4FBFF') },
				{ keywords: ['中雪'], art: () => singleLayer('icon-zhongxue', '#F4FBFF') },
				{ keywords: ['小雪'], art: () => singleLayer('icon-xiaoxue', '#F4FBFF') },
				{ keywords: ['雨夹雪', '阵雪'], art: () => singleLayer('icon-yujiaxue', '#F4FBFF') },
				{ keywords: ['冰雹'], art: () => singleLayer('icon-bingbao', '#F4FBFF') },
				{ keywords: ['冻雨'], art: () => singleLayer('icon-yujiaxue', '#EAF7FF') },
				{ keywords: ['暴雨', '大暴雨', '特大暴雨'], art: () => singleLayer('icon-baoyu', '#EAF7FF') },
				{ keywords: ['大雨'], art: () => singleLayer('icon-dayu', '#EAF7FF') },
				{ keywords: ['中雨'], art: () => singleLayer('icon-C-zhongyu', '#EAF7FF') },
				{ keywords: ['小雨'], art: () => singleLayer('icon-xiaoyu', '#EAF7FF') },
				{ keywords: ['雾霾', '雾', '霾', '浮尘', '扬沙', '沙尘'], art: () => singleLayer('icon-wumai', '#EDF1EC') }
			]
			const matchedRule = weatherIconRules.find((rule) => rule.keywords.some((keyword) => weather.indexOf(keyword) > -1))
			return matchedRule ? matchedRule.art() : singleLayer('icon-yintian', '#F2F7F6')
		},
		getHomeLocation() {
			return new Promise((resolve, reject) => {
				uni.getLocation({
					// 首页天气按经纬度查询，统一传 wgs84 给后端，再由后端按服务商需要转换坐标系。
					type: this.getHomeLocationType(),
					isHighAccuracy: true,
					success: (res) => {
						const latitude = Number(res.latitude)
						const longitude = Number(res.longitude)
						if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
							reject(new Error('Invalid location response'))
							return
						}
						resolve({ longitude, latitude, coordinateSystem: this.getHomeLocationType() })
					},
					fail: reject
				})
			})
		},
		getHomeLocationType() {
			let locationType = 'wgs84'
			// #ifdef H5
			locationType = 'wgs84'
			// #endif
			// #ifdef APP-PLUS
			locationType = 'wgs84'
			// #endif
			return locationType
		},
		fetchHomePlots() {
			// 首页通过横向滑动展示当前用户全部地块，字段仍保持原卡片 UI 所需的精简摘要。
			getAllPlotList()
				.then((plots) => {
					this.plots = plots.map((plot) => ({
						id: plot.id,
						name: plot.cropName,
						status: plot.growthStageName,
						days: `已种植${plot.plantingDays || 0}天`,
						landNo: this.formatPlotNo(plot.plotName),
						image: plot.image || this.fallbackPlotImage
					}))
				})
				.catch(() => {
					this.plots = []
				})
		},
		formatPlotNo(plotName) {
			// 首页卡片空间较小，仅保留“几号地/几号地块”这类核心地块编号，避免长农场名前缀撑破旧版 UI。
			const name = plotName || '-'
			const matched = name.match(/[A-Za-z0-9一二三四五六七八九十百]+号(?:地块|地)?/)
			return matched ? matched[0].replace('地块', '地') : name
		},
		handleMorePlots() {
			uni.navigateTo({
				url: '/pages/secondPage/plot/plot_list'
			})
		},
		handlePlot(plot) {
			uni.navigateTo({
				url: `/pages/secondPage/plot/detail?id=${plot.id}`
			})
		},
		handleTask() {
			// 首页任务提醒卡片与常用服务中的农事管理入口统一跳转到任务列表页。
			uni.navigateTo({
				url: '/pages/service/task_list'
			})
		},
		handleService(service) {
			if (service.path) {
				uni.navigateTo({
					url: service.path
				})
				return
			}
			uni.showToast({
				title: service.title,
				icon: 'none'
			})
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100%;
	overflow: hidden;
	overscroll-behavior: none;
	background-color: #6FC4BA;
}

.home-page {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	box-sizing: border-box;
	width: 100%;
	height: 100vh;
	height: 100dvh;
	overflow: hidden;
	overscroll-behavior: none;
	background-color: #6FC4BA;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.hero {
	box-sizing: border-box;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	/* 增加首页头部高度，给状态栏、欢迎语和天气区域之间保留更舒展的纵向空间。 */
	height: 296rpx;
	padding: 70rpx 44rpx 72rpx;
	background-color: #6FC4BA;
}

.hello {
	font-size: 20px;
	font-weight: 700;
	line-height: 1.25;
	color: #ffffff;
}

.welcome {
	margin-top: 18rpx;
	font-size: 14px;
	font-weight: normal;
	line-height: 1.35;
	color: #ffffff;
}

.weather-art {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	/* 固定天气装饰图标容器尺寸，避免头部高度变化后真机端压缩图标显示。 */
	flex-shrink: 0;
	width: 170rpx;
	height: 118rpx;
	margin-right: 6rpx;
}

.weather-art-icon {
	display: block;
	position: absolute;
	left: 50%;
	top: 50%;
	width: 128rpx;
	height: 128rpx;
	text-align: center;
	font-size: 118rpx !important;
	line-height: 128rpx;
	text-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
	transform: translate(-50%, -50%);
}

.weather-art-sun {
	left: 65%;
	top: 34%;
	width: 82rpx;
	height: 82rpx;
	font-size: 78rpx !important;
	line-height: 82rpx;
}

.weather-art-cloud {
	left: 38%;
	top: 68%;
	width: 118rpx;
	height: 118rpx;
	font-size: 108rpx !important;
	line-height: 118rpx;
}

.weather-art-thunder-accent {
	clip-path: inset(44% 24% 0 28%);
}

.weather-strip {
	position: relative;
	z-index: 4;
	display: flex;
	align-items: center;
	height: 90px;
	margin-top: -2rpx;
	padding: 0 34rpx;
	border-top-left-radius: 64rpx;
	border-top-right-radius: 64rpx;
	border: 1px solid #6FC4BA;
	background-color: #6FC4BA;
	box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

.weather-item {
	position: relative;
	display: flex;
	flex: 1;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	height: 84rpx;
	color: #ffffff;
	margin-top: -30px;
}

.weather-icon {
	font-size: 38rpx;
	line-height: 38rpx;
	color: #ffffff;
}

.weather-value {
	margin-top: 10rpx;
	font-size: 12px;
	font-weight: normal;
	line-height: 1;
	color: #ffffff;
}

.weather-divider {
	position: absolute;
	right: 0;
	top: 16rpx;
	width: 4rpx;
	height: 44rpx;
	border-radius: 4rpx;
	background-color: rgba(255, 255, 255, 0.52);
}

.content {
	position: absolute;
	left: 0;
	right: 0;
	/* 头部增高后同步下移内容区，避免白色内容面板覆盖天气信息卡片。 */
	top: 410rpx;
	z-index: 5;
	/* H5 原生 tabBar 会占用 window-bottom；App 端该变量按原生窗口自动取值。 */
	bottom: var(--window-bottom, 0px);
	box-sizing: border-box;
	height: auto;
	overflow: hidden;
	overscroll-behavior-y: contain;
	padding: 38rpx 36rpx 28rpx;
	border-top-left-radius: 56rpx;
	border-top-right-radius: 56rpx;
	background-color: #f7f7f7;
	box-shadow: 0 -4px 10px rgba(0, 0, 0, 0.08);
}

.section-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.section-title {
	font-size: 18px;
	font-weight: normal;
	line-height: 1.3;
	color: #000000;
}

.section-more,
.task-more {
	display: flex;
	align-items: center;
	font-size: 14px;
	font-weight: normal;
	line-height: 1.3;
	color: #C8C8C8;
}

.arrow-icon {
	margin-left: 6rpx;
	font-size: 22rpx;
	color: #C8C8C8;
}

.plot-list {
	margin-top: 30rpx;
	width: 100%;
	white-space: nowrap;
}

.plot-scroll-inner {
	display: flex;
	align-items: stretch;
	width: max-content;
}

.plot-card {
	position: relative;
	box-sizing: border-box;
	flex-shrink: 0;
	width: 312rpx;
	min-height: 224rpx;
	margin-right: 38rpx;
	padding: 20rpx 20rpx 22rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.06);
}

.plot-card:last-child {
	margin-right: 0;
}

.plot-image {
	display: block;
	width: 140rpx;
	height: 88rpx;
	margin-left: 10rpx;
	border-radius: 6rpx;
	background-color: #f2f2f2;
}

.plot-status {
	position: absolute;
	right: 12rpx;
	top: 20rpx;
	box-sizing: border-box;
	min-width: 74rpx;
	height: 38rpx;
	padding: 0 10rpx;
	border: 2rpx solid #6FC4BA;
	border-radius: 8rpx;
	text-align: center;
	font-size: 12px;
	font-weight: normal;
	line-height: 34rpx;
	color: #ffffff;
	background-color: #6FC4BA;
}

.plot-card:nth-child(2) .plot-status {
	border-color: #C8C8C8;
	color: #C8C8C8;
	background-color: #ffffff;
}

.plot-name {
	margin-top: 22rpx;
	font-size: 14px;
	font-weight: normal;
	line-height: 1.3;
	color: #000000;
}

.plot-meta {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 10rpx;
	font-size: 12px;
	font-weight: normal;
	line-height: 1.3;
	color: #C8C8C8;
}

.task-card {
	box-sizing: border-box;
	margin-top: 28rpx;
	padding: 22rpx 24rpx 20rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.06);
}

.task-head,
.task-title-wrap {
	display: flex;
	align-items: center;
}

.task-head {
	justify-content: space-between;
}

.task-icon {
	margin-right: 16rpx;
	font-size: 34rpx;
	line-height: 34rpx;
	color: #6FC4BA;
}

.task-title {
	font-size: 18px;
	font-weight: normal;
	line-height: 1.3;
	color: #000000;
}

.task-stats {
	display: grid;
	grid-template-columns: repeat(4, minmax(0, 1fr));
	align-items: center;
	margin-top: 18rpx;
}

.task-stat {
	display: flex;
	flex-direction: column;
	align-items: center;
	min-width: 0;
	padding: 0 6rpx;
}

.task-stat + .task-stat {
	border-left: 1px solid #eeeeee;
}

.task-count {
	font-size: 14px;
	font-weight: normal;
	line-height: 1.2;
	color: #000000;
}

.task-label {
	margin-top: 6rpx;
	font-size: 12px;
	font-weight: normal;
	line-height: 1.2;
	color: #C8C8C8;
}

.task-stat-overdue .task-count,
.task-stat-overdue .task-label {
	color: #F56C6C;
}

.service-title {
	margin-top: 28rpx;
	font-size: 18px;
	font-weight: normal;
	line-height: 1.4;
	color: #000000;
}

.service-grid {
	display: flex;
	flex-wrap: wrap;
	justify-content: space-between;
	margin-top: 18rpx;
}

.service-card {
	box-sizing: border-box;
	display: grid;
	align-items: center;
	grid-template-columns: 72rpx 1fr;
	column-gap: 18rpx;
	width: 318rpx;
	min-height: 112rpx;
	margin-bottom: 20rpx;
	padding: 20rpx 18rpx 20rpx 22rpx;
	border-radius: 16rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.06);
}

.service-icon-wrap {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 72rpx;
	height: 72rpx;
	overflow: hidden;
}

.service-icon {
	display: block;
	flex-shrink: 0;
	width: 72rpx;
	height: 72rpx;
	text-align: center;
	font-size: 58rpx;
	line-height: 72rpx;
	color: #6FC4BA;
}

.warehouse-icon {
	font-size: 50rpx;
	transform: translateX(-6rpx);
}

.service-copy {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.service-name {
	font-size: 16px;
	font-weight: normal;
	line-height: 1.25;
	color: #000000;
}

.service-desc {
	margin-top: 8rpx;
	font-size: 12px;
	font-weight: normal;
	line-height: 1.25;
	color: #C8C8C8;
}

/* .home-bottom-spacer {
	height: 58px;
} */

@media screen and (min-width: 768px) {
	.home-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
