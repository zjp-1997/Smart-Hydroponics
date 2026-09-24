<template>
	<view class="live-page">
		<!-- 自定义导航栏遵循安全区，标题始终使用后端返回的摄像头名称。 -->
		<view class="live-navbar">
			<text class="iconfont icon-fanhui back-button" aria-label="返回" @tap="handleBack"></text>
			<text class="live-title">{{ monitor.cameraName || '实时监控' }}</text>
			<view class="nav-placeholder"></view>
		</view>

		<!-- HTTP/HLS 地址交给原生 video 播放，RTSP/GB28181 使用实时截图作为兼容兜底。 -->
		<view class="video-panel">
			<video
				v-if="playableStream"
				id="monitor-video"
				class="live-video"
				:src="monitor.streamUrl"
				:poster="monitor.cover"
				:controls="false"
				:autoplay="false"
				:muted="true"
				:show-center-play-btn="false"
				:show-play-btn="false"
				object-fit="cover"
				@play="isPlaying = true"
				@pause="isPlaying = false"
				@ended="isPlaying = false"
				@error="handleVideoError"
			></video>
			<protected-image v-else class="live-video" :src="monitor.cover" mode="aspectFill"></protected-image>

			<view v-if="!isPlaying" class="center-play" hover-class="control-pressed" aria-label="播放实时监控" @tap="handlePlay">
				<view class="play-triangle"></view>
			</view>

			<view class="video-caption">
				<view class="caption-text">
					<text>{{ monitor.cropName || '暂无作物' }}</text>
					<text class="caption-camera">{{ monitor.cameraName || '实时监控' }}</text>
				</view>
				<view class="fullscreen-button" hover-class="control-pressed" aria-label="全屏查看" @tap="handleFullscreen">
					<view class="corner corner-tl"></view>
					<view class="corner corner-tr"></view>
					<view class="corner corner-bl"></view>
					<view class="corner corner-br"></view>
				</view>
			</view>
		</view>

		<!-- 四个方向按钮使用项目 iconfont.css 中指定的图标类。 -->
		<view v-if="canControl" class="ptz-area">
			<view class="ptz-pad">
				<button class="ptz-button ptz-up" hover-class="ptz-pressed" aria-label="摄像头向上" @tap="sendPtzPulse('UP')">
					<text class="iconfont icon-shang ptz-icon"></text>
				</button>
				<button class="ptz-button ptz-left" hover-class="ptz-pressed" aria-label="摄像头向左" @tap="sendPtzPulse('LEFT')">
					<text class="iconfont icon-zuo ptz-icon"></text>
				</button>
				<view class="ptz-center" aria-hidden="true"></view>
				<button class="ptz-button ptz-right" hover-class="ptz-pressed" aria-label="摄像头向右" @tap="sendPtzPulse('RIGHT')">
					<text class="iconfont icon-icon-you ptz-icon"></text>
				</button>
				<button class="ptz-button ptz-down" hover-class="ptz-pressed" aria-label="摄像头向下" @tap="sendPtzPulse('DOWN')">
					<text class="iconfont icon-xiala ptz-icon"></text>
				</button>
			</view>
		</view>
	</view>
</template>

<script>
import { controlMonitorPtz, getMonitorDetail } from '@/api/monitorList.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			monitorId: null,
			monitor: {
				cameraName: '',
				cropName: '',
				cover: '/static/m1.png',
				streamUrl: '',
				streamProtocol: '',
				onlineStatus: 0
			},
			isPlaying: false,
			isControlling: false,
			stopTimer: null
		}
	},
	computed: {
		canControl() {
			// 普通用户可以观看视频，但云台属于农场主设备控制权限。
			return String(getUserInfo()?.roleCode || '').toLowerCase() === 'farm_owner'
		},
		// uni-app 的 video 可直接处理 HTTP(S) 媒体；RTSP/GB28181 需要流媒体网关转为 HLS/FLV。
		playableStream() {
			return /^https?:\/\//i.test(this.monitor.streamUrl || '')
		}
	},
	onLoad(options) {
		this.monitorId = Number(options && options.id)
		if (!Number.isFinite(this.monitorId) || this.monitorId <= 0) {
			uni.showToast({ title: '监控设备参数无效', icon: 'none' })
			return
		}
		this.loadMonitor()
	},
	onUnload() {
		if (this.stopTimer) {
			clearTimeout(this.stopTimer)
		}
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		async loadMonitor() {
			try {
				this.monitor = await getMonitorDetail(this.monitorId)
			} catch (error) {
				// 请求层已统一展示错误提示，页面保留稳定的占位画面。
			}
		},
		handlePlay() {
			if (this.monitor.onlineStatus !== 1) {
				uni.showToast({ title: '摄像头当前离线', icon: 'none' })
				return
			}
			if (!this.playableStream) {
				uni.showToast({ title: '当前视频协议需通过流媒体网关播放', icon: 'none' })
				return
			}
			uni.createVideoContext('monitor-video', this).play()
		},
		handleFullscreen() {
			if (this.playableStream) {
				uni.createVideoContext('monitor-video', this).requestFullScreen({ direction: 0 })
				return
			}
			// 无可播放流时全屏预览摄像头截图，保证全屏入口仍有明确结果。
			uni.previewImage({ current: this.monitor.cover, urls: [this.monitor.cover] })
		},
		handleVideoError() {
			this.isPlaying = false
			uni.showToast({ title: '实时视频暂时无法播放', icon: 'none' })
		},
		async sendPtzPulse(direction) {
			if (this.isControlling) {
				return
			}
			if (this.monitor.onlineStatus !== 1) {
				uni.showToast({ title: '摄像头当前离线', icon: 'none' })
				return
			}
			this.isControlling = true
			try {
				// 单次点击先下发方向，再短延迟发送 STOP，避免云台持续转动。
				await controlMonitorPtz(this.monitorId, direction)
				this.stopTimer = setTimeout(async () => {
					try {
						await controlMonitorPtz(this.monitorId, 'STOP')
					} catch (error) {
						// 请求层已经给出失败提示，这里只负责恢复按钮可操作状态。
					} finally {
						this.isControlling = false
					}
				}, 300)
			} catch (error) {
				this.isControlling = false
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #fafafa;
}

.live-page {
	min-height: 100vh;
	background-color: #fafafa;
	color: #353535;
}

.live-navbar {
	box-sizing: border-box;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	height: calc(var(--status-bar-height) + 88rpx);
	padding: 0 28rpx;
	border-bottom: 1rpx solid #eeeeee;
	background-color: #ffffff;
}

.back-button,
.nav-placeholder {
	display: flex;
	align-items: center;
	width: 72rpx;
	height: 88rpx;
}

.back-button {
	font-size: 40rpx;
	color: #4a4a4a;
}

.live-title {
	flex: 1;
	height: 88rpx;
	overflow: hidden;
	text-align: center;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 17px;
	line-height: 88rpx;
	color: #444444;
}

.video-panel {
	position: relative;
	width: 100%;
	height: 404rpx;
	overflow: hidden;
	background-color: #26312c;
}

.live-video {
	display: block;
	width: 100%;
	height: 100%;
}

.center-play {
	position: absolute;
	left: 50%;
	top: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 76rpx;
	height: 76rpx;
	border: 3rpx solid rgba(255, 255, 255, 0.92);
	border-radius: 50%;
	transform: translate(-50%, -50%);
	transition: opacity 100ms ease;
}

.play-triangle {
	width: 0;
	height: 0;
	margin-left: 7rpx;
	border-top: 14rpx solid transparent;
	border-bottom: 14rpx solid transparent;
	border-left: 21rpx solid #ffffff;
}

.video-caption {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 76rpx;
	padding: 0 26rpx 0 38rpx;
	background-color: rgba(18, 43, 24, 0.68);
	color: #ffffff;
}

.caption-text {
	display: flex;
	align-items: center;
	min-width: 0;
	font-size: 16px;
}

.caption-camera {
	margin-left: 40rpx;
}

.fullscreen-button {
	position: relative;
	width: 64rpx;
	height: 64rpx;
	transition: opacity 100ms ease;
}

.corner {
	position: absolute;
	width: 14rpx;
	height: 14rpx;
	border-color: #ffffff;
	border-style: solid;
}

.corner-tl { left: 10rpx; top: 10rpx; border-width: 3rpx 0 0 3rpx; }
.corner-tr { right: 10rpx; top: 10rpx; border-width: 3rpx 3rpx 0 0; }
.corner-bl { left: 10rpx; bottom: 10rpx; border-width: 0 0 3rpx 3rpx; }
.corner-br { right: 10rpx; bottom: 10rpx; border-width: 0 3rpx 3rpx 0; }

.control-pressed {
	opacity: 0.62;
}

.ptz-area {
	display: flex;
	justify-content: center;
	padding-top: 202rpx;
}

.ptz-pad {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 356rpx;
	height: 356rpx;
	border-radius: 50%;
	background-color: #ffffff;
}

.ptz-center {
	width: 116rpx;
	height: 116rpx;
	border-radius: 50%;
	background-color: #f0f0f0;
}

.ptz-button {
	position: absolute;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 96rpx;
	height: 96rpx;
	margin: 0;
	padding: 0;
	border: 0;
	border-radius: 50%;
	background-color: transparent;
	line-height: 96rpx;
	transition: background-color 100ms ease;
	touch-action: manipulation;
}

.ptz-button::after {
	border: 0;
}

.ptz-up { left: 130rpx; top: 13rpx; }
.ptz-down { left: 130rpx; bottom: 13rpx; }
.ptz-left { left: 13rpx; top: 130rpx; }
.ptz-right { right: 13rpx; top: 130rpx; }

.ptz-icon {
	font-size: 34rpx;
	line-height: 1;
	color: #050505;
}

.ptz-pressed {
	background-color: #f0f0f0;
}

@media screen and (min-width: 768px) {
	.live-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
