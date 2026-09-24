<template>
	<view class="tab-page">
		<view class="page-hero">
			<view class="navbar">
				<view class="nav-placeholder"></view>
				<text class="nav-title">地图</text>
				<view class="nav-placeholder"></view>
			</view>
		</view>

		<view class="page-content">
			<!-- #ifdef H5 -->
			<view
				class="h5-map-preview"
				@touchstart="startMapDrag"
				@touchmove.stop.prevent="moveMapDrag"
				@touchend="endMapDrag"
				@mousedown="startMapDrag"
				@mousemove="moveMapDrag"
				@mouseup="endMapDrag"
				@mouseleave="endMapDrag"
				@wheel.stop.prevent="handleMapWheel"
			>
				<protected-image
					class="h5-map-tile"
					v-for="tile in h5MapTiles"
					:key="tile.key"
					:src="tile.url"
					:style="tile.style"
					mode="scaleToFill"
				></protected-image>
				<view
					v-if="currentLocation"
					class="h5-location-marker"
					:style="h5LocationMarkerStyle"
				></view>
			</view>
			<!-- #endif -->
			<!-- #ifndef H5 -->
			<map
				id="farmLocationMap"
				class="farm-map"
				:latitude="center.latitude"
				:longitude="center.longitude"
				:scale="mapScale"
				:markers="locationMarkers"
				:enable-satellite="mapMode === 'satellite'"
				:enable-traffic="trafficEnabled"
			>
				<!-- App-vue 原生地图需用 cover-view 承载按钮；其原生点击事件不提供 stopPropagation，不能使用 .stop。 -->
				<!-- #ifdef APP-PLUS -->
				<cover-view class="native-map-control native-traffic" :class="{ active: trafficEnabled }" @click="toggleTraffic">路况</cover-view>
				<cover-view class="native-map-control native-satellite" :class="{ active: mapMode === 'satellite' }" @click="toggleSatellite">卫星图</cover-view>
				<cover-view class="native-map-control native-zoom-in" @click="zoomIn">＋</cover-view>
				<cover-view class="native-map-control native-zoom-out" @click="zoomOut">－</cover-view>
				<cover-view class="native-map-control native-locate" @click="locateCurrentPosition">⌖</cover-view>
				<!-- #endif -->
			</map>
			<!-- #endif -->

			<!-- #ifndef APP-PLUS -->
			<view class="map-layer-tools">
				<view class="layer-button" :class="{ active: trafficEnabled }" @tap.stop="toggleTraffic">
					<view class="road-icon">
						<view class="road-dot"></view>
						<view class="road-dot"></view>
						<view class="road-dot"></view>
					</view>
					<text class="layer-text">路况</text>
				</view>
				<view class="layer-button" :class="{ active: mapMode === 'satellite' }" @tap.stop="toggleSatellite">
					<view class="satellite-icon">
						<view class="satellite-line satellite-line-a"></view>
						<view class="satellite-line satellite-line-b"></view>
					</view>
					<text class="layer-text">卫星图</text>
				</view>
			</view>

			<view class="map-action-tools">
				<view class="zoom-card">
					<view class="zoom-button" @tap.stop="zoomIn">+</view>
					<view class="zoom-divider"></view>
					<view class="zoom-button zoom-minus" @tap.stop="zoomOut">-</view>
				</view>
				<view class="locate-card" @tap.stop="locateCurrentPosition">
					<text class="iconfont icon-dingwei locate-icon"></text>
				</view>
			</view>
			<!-- #endif -->

			<!-- <view class="search-button" @tap.stop="handleSearchPlace">
				<view class="search-symbol">
					<view class="search-circle"></view>
					<view class="search-handle"></view>
				</view>
				<text class="search-text">查找地点</text>
			</view> -->
		</view>

	</view>
</template>

<script>
import { ensureNonExpertAccess } from '@/utils/expertAccess.js'
export default {
	data() {
		return {
			mapScale: 15,
			mapMode: 'standard',
			trafficEnabled: false,
			isLocating: false,
			locationFailed: false,
			mapDrag: {
				active: false,
				startX: 0,
				startY: 0,
				offsetX: 0,
				offsetY: 0
			},
			// 默认定位到漳州，防止用户拒绝授权或定位失败时地图出现空白。
			center: {
				latitude: 24.513245,
				longitude: 117.647822
			},
			// currentLocation 只保存设备真实定位点，拖动地图时不会修改它。
			currentLocation: null
		}
	},
	computed: {
		locationMarkers() {
			// App-vue 地图不支持 show-location，使用真实定位坐标绘制可见的蓝色位置点。
			if (!this.currentLocation) return []
			return [{
				id: 1,
				latitude: this.currentLocation.latitude,
				longitude: this.currentLocation.longitude,
				iconPath: '/static/map-current-location.png',
				width: 28,
				height: 28
			}]
		},
		h5MapTiles() {
			// H5 调试环境不使用高德整页 URI，避免出现高德自带的顶部栏、App 引导和底部面板。
			const zoom = this.mapScale
			const tileSize = 256
			const tilePoint = this.lngLatToTilePoint(this.center.latitude, this.center.longitude, zoom)
			const centerTileX = Math.floor(tilePoint.x)
			const centerTileY = Math.floor(tilePoint.y)
			const tiles = []
			for (let xOffset = -3; xOffset <= 3; xOffset += 1) {
				for (let yOffset = -4; yOffset <= 4; yOffset += 1) {
					const tileX = centerTileX + xOffset
					const tileY = centerTileY + yOffset
					const left = (tileX - tilePoint.x) * tileSize + this.mapDrag.offsetX
					const top = (tileY - tilePoint.y) * tileSize + this.mapDrag.offsetY
					tiles.push({
						key: `${zoom}-${tileX}-${tileY}`,
						url: this.buildAmapTileUrl(tileX, tileY, zoom),
						style: `left: ${this.formatCenterOffset(left)}; top: ${this.formatCenterOffset(top)};`
					})
				}
			}
			return tiles
		},
		h5LocationMarkerStyle() {
			if (!this.currentLocation) {
				return ''
			}
			const tileSize = 256
			const centerTilePoint = this.lngLatToTilePoint(this.center.latitude, this.center.longitude, this.mapScale)
			const locationTilePoint = this.lngLatToTilePoint(this.currentLocation.latitude, this.currentLocation.longitude, this.mapScale)
			const left = (locationTilePoint.x - centerTilePoint.x) * tileSize + this.mapDrag.offsetX
			const top = (locationTilePoint.y - centerTilePoint.y) * tileSize + this.mapDrag.offsetY
			// H5 定位点按真实定位坐标绘制，不再固定在屏幕中心，拖动地图时会停留在真实地图位置。
			return `left: ${this.formatCenterOffset(left)}; top: ${this.formatCenterOffset(top)};`
		}
	},
	onReady() {
		// 地图组件首次渲染完成后同步一次视角，避免 onShow 定位早于地图初始化。
		this.moveMapToCenter()
	},
	async onShow() {
		if (!await ensureNonExpertAccess()) return
		// tabBar 页面会被缓存，onReady 只触发一次；每次点击“地图”tab 回到页面时都要自动定位。
		this.$nextTick(() => {
			this.locateCurrentPosition()
		})
	},
	methods: {
		locateCurrentPosition() {
			if (this.isLocating) {
				return
			}
			this.isLocating = true
			this.locationFailed = false
			this.resetMapDrag()
			this.requestMapLocation(this.getLocationType(), true)
		},
		requestMapLocation(type, allowSystemFallback) {
			// 高德 gcj02 定位失败时改用系统 wgs84 定位，避免真机因地图 SDK 配置而停留在默认地点。
			uni.getLocation({
				type,
				isHighAccuracy: true,
				success: (res) => {
					const rawLatitude = Number(res.latitude)
					const rawLongitude = Number(res.longitude)
					if (!Number.isFinite(rawLatitude) || !Number.isFinite(rawLongitude)
						|| Math.abs(rawLatitude) > 90 || Math.abs(rawLongitude) > 180) {
						this.onMapLocationFailed(new Error('Invalid location response'), type, allowSystemFallback)
						return
					}
					const { latitude, longitude } = this.normalizeLocationForMap(rawLatitude, rawLongitude, type)
					this.center = {
						latitude,
						longitude
					}
					this.currentLocation = {
						latitude,
						longitude
					}
					this.$nextTick(() => {
						this.moveMapToCenter()
					})
					this.isLocating = false
				},
				fail: (error) => {
					this.onMapLocationFailed(error, type, allowSystemFallback)
				}
			})
		},
		onMapLocationFailed(error, type, allowSystemFallback) {
			if (allowSystemFallback && type === 'gcj02') {
				console.warn('Amap location failed, trying system location:', error)
				this.requestMapLocation('wgs84', false)
				return
			}
			this.isLocating = false
			this.handleLocationError(error)
		},
		getLocationType() {
			let locationType = 'gcj02'
			// #ifdef H5
			// Chrome/H5 端直接使用浏览器定位返回的 wgs84，避免触发 provider 坐标转换失败。
			locationType = 'wgs84'
			// #endif
			// #ifdef APP-PLUS
			// App 真机端继续使用 gcj02，与已配置的高德地图 SDK 坐标体系保持一致。
			locationType = 'gcj02'
			// #endif
			return locationType
		},
		normalizeLocationForMap(latitude, longitude, sourceType) {
			// H5 和 App 的系统定位均返回 wgs84，统一转换后才能与高德地图及定位点对齐。
			return sourceType === 'wgs84'
				? this.wgs84ToGcj02(latitude, longitude)
				: { latitude, longitude }
		},
		wgs84ToGcj02(latitude, longitude) {
			if (this.isOutOfChina(latitude, longitude)) {
				return { latitude, longitude }
			}
			const a = 6378245.0
			const ee = 0.00669342162296594323
			let dLat = this.transformLat(longitude - 105.0, latitude - 35.0)
			let dLng = this.transformLng(longitude - 105.0, latitude - 35.0)
			const radLat = latitude / 180.0 * Math.PI
			let magic = Math.sin(radLat)
			magic = 1 - ee * magic * magic
			const sqrtMagic = Math.sqrt(magic)
			dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI)
			dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI)
			// 国内地图服务采用 gcj02 坐标，高德瓦片与定位点必须使用同一坐标系。
			return {
				latitude: latitude + dLat,
				longitude: longitude + dLng
			}
		},
		isOutOfChina(latitude, longitude) {
			// 境外坐标不需要火星坐标转换，避免无意义偏移。
			return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
		},
		transformLat(x, y) {
			let result = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
			result += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
			result += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
			result += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0
			return result
		},
		transformLng(x, y) {
			let result = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
			result += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
			result += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
			result += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
			return result
		},
		buildAmapTileUrl(tileX, tileY, zoom) {
			const maxTileIndex = 2 ** zoom - 1
			if (tileX < 0 || tileY < 0 || tileX > maxTileIndex || tileY > maxTileIndex) {
				return ''
			}
			const serverIndex = Math.abs(tileX + tileY) % 4 + 1
			const server = `0${serverIndex}`
			if (this.mapMode === 'satellite') {
				// H5 卫星图使用高德卫星瓦片，颜色风格与高德地图保持一致。
				return `https://webst${server}.is.autonavi.com/appmaptile?style=6&x=${tileX}&y=${tileY}&z=${zoom}`
			}
			// H5 标准地图使用高德道路瓦片，避免 OpenStreetMap 与高德配色不一致。
			return `https://webrd${server}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x=${tileX}&y=${tileY}&z=${zoom}`
		},
		lngLatToTilePoint(latitude, longitude, zoom) {
			const maxLatitude = 85.05112878
			const normalizedLatitude = Math.max(Math.min(Number(latitude), maxLatitude), -maxLatitude)
			const normalizedLongitude = Math.max(Math.min(Number(longitude), 180), -180)
			const latRad = normalizedLatitude * Math.PI / 180
			const scale = 2 ** zoom
			// 将经纬度换算为 Web Mercator 瓦片坐标，用于 H5 无 Key 地图预览。
			return {
				x: (normalizedLongitude + 180) / 360 * scale,
				y: (1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2 * scale
			}
		},
		tilePointToLngLat(tilePoint, zoom) {
			const scale = 2 ** zoom
			const longitude = tilePoint.x / scale * 360 - 180
			const latitudeRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * tilePoint.y / scale)))
			// 拖动结束后把 Web Mercator 瓦片坐标还原成经纬度，作为新的地图中心点。
			return {
				latitude: latitudeRad * 180 / Math.PI,
				longitude
			}
		},
		formatCenterOffset(offset) {
			const absoluteOffset = Math.abs(Number(offset)).toFixed(2)
			// 生成标准 calc 表达式，避免出现 calc(50% + -20px) 这类兼容性较差的写法。
			return Number(offset) < 0 ? `calc(50% - ${absoluteOffset}px)` : `calc(50% + ${absoluteOffset}px)`
		},
		zoomIn() {
			// 统一控制 H5 瓦片缩放和 App 原生 map 组件缩放层级。
			this.resetMapDrag()
			this.mapScale = Math.min(this.mapScale + 1, 18)
		},
		zoomOut() {
			// 限制最小缩放层级，避免地图缩得过小后无法看清当前位置。
			this.resetMapDrag()
			this.mapScale = Math.max(this.mapScale - 1, 3)
		},
		handleMapWheel(event) {
			const deltaY = event && event.detail ? event.detail.deltaY : event.deltaY
			if (Number(deltaY) < 0) {
				this.zoomIn()
				return
			}
			this.zoomOut()
		},
		startMapDrag(event) {
			const point = this.getMapPointerPoint(event)
			if (!point) {
				return
			}
			this.mapDrag = {
				active: true,
				startX: point.x,
				startY: point.y,
				offsetX: 0,
				offsetY: 0
			}
		},
		moveMapDrag(event) {
			if (!this.mapDrag.active) {
				return
			}
			const point = this.getMapPointerPoint(event)
			if (!point) {
				return
			}
			// 拖动时先只移动瓦片偏移量，保证 H5 调试地图能像真实地图一样跟手拖动。
			this.mapDrag = {
				...this.mapDrag,
				offsetX: point.x - this.mapDrag.startX,
				offsetY: point.y - this.mapDrag.startY
			}
		},
		endMapDrag() {
			if (!this.mapDrag.active) {
				return
			}
			const tilePoint = this.lngLatToTilePoint(this.center.latitude, this.center.longitude, this.mapScale)
			const nextCenter = this.tilePointToLngLat({
				x: tilePoint.x - this.mapDrag.offsetX / 256,
				y: tilePoint.y - this.mapDrag.offsetY / 256
			}, this.mapScale)
			this.center = nextCenter
			this.resetMapDrag()
		},
		resetMapDrag() {
			this.mapDrag = {
				active: false,
				startX: 0,
				startY: 0,
				offsetX: 0,
				offsetY: 0
			}
		},
		getMapPointerPoint(event) {
			const touch = event && event.touches && event.touches[0]
			const changedTouch = event && event.changedTouches && event.changedTouches[0]
			const source = touch || changedTouch || event
			if (!source) {
				return null
			}
			const x = Number(source.clientX)
			const y = Number(source.clientY)
			if (!Number.isFinite(x) || !Number.isFinite(y)) {
				return null
			}
			return { x, y }
		},
		toggleTraffic() {
			// App 端透传给 map 的 enable-traffic，H5 端作为按钮状态展示。
			this.trafficEnabled = !this.trafficEnabled
		},
		toggleSatellite() {
			// 标准图和卫星图之间切换，H5 与 App 使用同一个状态字段。
			this.mapMode = this.mapMode === 'satellite' ? 'standard' : 'satellite'
		},
		handleSearchPlace() {
			uni.showToast({
				title: '地点搜索待接入',
				icon: 'none'
			})
		},
		moveMapToCenter() {
			// #ifdef APP-PLUS
			const mapContext = uni.createMapContext('farmLocationMap', this)
			if (mapContext && typeof mapContext.moveToLocation === 'function') {
				// 定位成功后主动移动地图视角，兼容部分端只更新经纬度但不立即居中的情况。
				mapContext.moveToLocation({
					latitude: this.center.latitude,
					longitude: this.center.longitude
				})
			}
			// #endif
		},
		handleLocationError(error) {
			this.locationFailed = true
			console.error('Get farm map location failed:', error)
			uni.showToast({
				title: '定位失败，请开启定位权限后重试',
				icon: 'none'
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

.tab-page {
	position: relative;
	/* TabBar 页面只占可用视口，避免页面滚动改变地图与底部导航的相对位置。 */
	height: 100vh;
	overflow: hidden;
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
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
}

.nav-title {
	font-size: 16px;
	font-weight: normal;
	line-height: 1.3;
	color: #ffffff;
}

.nav-placeholder {
	width: 96rpx;
	height: 1rpx;
}

.page-content {
	position: absolute;
	left: 0;
	right: 0;
	/* 与消息页内容起点保持一致，减少顶部标题栏与地图之间的空白。 */
	top: calc(var(--status-bar-height) + 88rpx);
	bottom: 0;
	box-sizing: border-box;
	overflow: hidden;
	background-color: #e9f3f1;
}

.farm-map {
	position: absolute;
	left: 0;
	top: 0;
	display: block;
	/* 原生地图铺满导航栏以下的视口；TabBar 已由页面视口扣除，不再重复减去底部安全区。 */
	width: 100vw;
	height: calc(100vh - var(--status-bar-height) - 88rpx);
}

/* App 原生地图上的按钮必须是互不嵌套的 cover-view，位置相对完整地图内容区。 */
.native-map-control {
	position: absolute;
	right: 24rpx;
	z-index: 5;
	box-sizing: border-box;
	width: 84rpx;
	height: 76rpx;
	border-radius: 8rpx;
	background-color: #ffffff;
	color: #333333;
	font-size: 12px;
	line-height: 76rpx;
	text-align: center;
}

.native-map-control.active {
	background-color: #eefaf8;
	color: #168b7d;
}

.native-traffic { top: 28rpx; }
.native-satellite { top: 112rpx; }
.native-zoom-in { bottom: 196rpx; font-size: 27px; }
.native-zoom-out { bottom: 112rpx; font-size: 27px; }
.native-locate { bottom: 24rpx; font-size: 32px; }

.h5-map-preview {
	position: absolute;
	left: 0;
	top: 0;
	overflow: hidden;
	width: 100%;
	height: 100%;
	background-color: #e9f3f1;
	cursor: grab;
	touch-action: none;
	user-select: none;
}

.h5-map-preview:active {
	cursor: grabbing;
}

.h5-map-tile {
	position: absolute;
	width: 256px;
	height: 256px;
	pointer-events: none;
	user-select: none;
}

.h5-location-marker {
	position: absolute;
	left: 50%;
	top: 50%;
	z-index: 2;
	width: 36rpx;
	height: 36rpx;
	border: 6rpx solid #ffffff;
	border-radius: 50%;
	background-color: #2f86ff;
	box-shadow: 0 4rpx 14rpx rgba(47, 134, 255, 0.36);
	transform: translate(-50%, -50%);
}

.map-layer-tools {
	position: absolute;
	right: 24rpx;
	top: 28rpx;
	z-index: 5;
	overflow: hidden;
	width: 76rpx;
	border-radius: 8rpx;
	background-color: #ffffff;
	box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.12);
}

.layer-button {
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	min-height: 88rpx;
	padding: 10rpx 0;
	color: #333333;
}

.layer-button.active {
	color: #6FC4BA;
	background-color: #eefaf8;
}

.layer-text {
	margin-top: 4rpx;
	font-size: 10px;
	line-height: 1.2;
	color: inherit;
}

.road-icon {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 42rpx;
	height: 28rpx;
	border: 4rpx solid currentColor;
	border-radius: 16rpx;
}

.road-dot {
	width: 6rpx;
	height: 6rpx;
	margin: 0 3rpx;
	border-radius: 50%;
	background-color: currentColor;
}

.satellite-icon {
	position: relative;
	width: 42rpx;
	height: 34rpx;
}

.satellite-line {
	position: absolute;
	left: 4rpx;
	top: 14rpx;
	width: 36rpx;
	height: 8rpx;
	border-radius: 8rpx;
	background-color: currentColor;
}

.satellite-line-a {
	transform: rotate(32deg);
}

.satellite-line-b {
	transform: rotate(-32deg);
}

.map-action-tools {
	position: absolute;
	right: 20rpx;
	bottom: 24rpx;
	z-index: 5;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.zoom-card,
.locate-card {
	overflow: hidden;
	width: 82rpx;
	border-radius: 8rpx;
	background-color: #ffffff;
	box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.12);
}

.zoom-card {
	margin-bottom: 16rpx;
}

.zoom-button {
	height: 70rpx;
	text-align: center;
	font-size: 30px;
	font-weight: normal;
	line-height: 70rpx;
	color: #000000;
}

.zoom-minus {
	font-size: 34px;
	line-height: 62rpx;
}

.zoom-divider {
	width: 56rpx;
	height: 1rpx;
	margin: 0 auto;
	background-color: #dddddd;
	transform: scaleY(0.5);
}

.locate-card {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 82rpx;
}

.locate-icon {
	font-size: 42rpx;
	line-height: 42rpx;
	color: #000000;
}

.search-button {
	position: absolute;
	left: 24rpx;
	bottom: 24rpx;
	z-index: 5;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-width: 190rpx;
	height: 70rpx;
	padding: 0 20rpx;
	border-radius: 8rpx;
	background-color: #ffffff;
	box-shadow: 0 6rpx 18rpx rgba(0, 0, 0, 0.12);
}

.search-symbol {
	position: relative;
	flex-shrink: 0;
	width: 36rpx;
	height: 36rpx;
	margin-right: 12rpx;
}

.search-circle {
	position: absolute;
	left: 0;
	top: 0;
	width: 26rpx;
	height: 26rpx;
	border: 4rpx solid #000000;
	border-radius: 50%;
}

.search-handle {
	position: absolute;
	right: 0;
	bottom: 2rpx;
	width: 18rpx;
	height: 4rpx;
	border-radius: 4rpx;
	background-color: #000000;
	transform: rotate(45deg);
	transform-origin: right center;
}

.search-text {
	font-size: 14px;
	line-height: 1.2;
	color: #666666;
}

@media screen and (min-width: 768px) {
	.tab-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
