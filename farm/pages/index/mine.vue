<template>
	<view class="mine-page">
		<view class="page-hero">
			<!-- 农场主与普通用户点击头像或名称区域进入共用个人信息编辑页。 -->
			<view class="profile" hover-class="profile-pressed" :hover-stay-time="80"
				role="button" aria-label="编辑个人信息" @tap="openProfile">
				<protected-image class="profile-avatar" :src="profile.avatar" mode="aspectFill" @error="useDefaultAvatar"></protected-image>
				<view class="profile-info">
					<text class="profile-name">{{ profile.name }}</text>
					<text class="profile-account">{{ profile.account }}</text>
				</view>
			</view>
		</view>

		<scroll-view class="mine-content" scroll-y refresher-enabled
			refresher-default-style="none" :refresher-triggered="pullRefreshing"
			refresher-background="#f7f7f7" :refresher-threshold="56" @refresherrefresh="$handlePullDownRefresh"
			:show-scrollbar="false"
		>
			<farm-pull-refresh slot="refresher" :refreshing="pullRefreshing" label="页面数据" />
			<view class="quick-grid">
				<button
					v-for="item in quickActions"
					:key="item.title"
					class="quick-card"
					hover-class="entry-pressed"
					:hover-stay-time="80"
					:aria-label="`${item.title}，${item.desc}`"
					@tap="handleEntry(item)"
				>
					<view class="quick-copy">
						<text class="quick-title">{{ item.title }}</text>
						<text class="quick-desc">{{ item.desc }}</text>
					</view>
					<text class="iconfont quick-icon" :class="[item.icon, item.iconClass]" aria-hidden="true"></text>
				</button>
			</view>

			<view class="menu-group" v-for="(group, index) in visibleMenuGroups" :key="index">
				<button
					v-for="item in group"
					:key="item.title"
					class="menu-row"
					hover-class="entry-pressed"
					:hover-stay-time="80"
					:aria-label="item.title"
					@tap="handleEntry(item)"
				>
					<text class="iconfont menu-icon" :class="[item.icon, item.iconClass]" aria-hidden="true"></text>
					<text class="menu-title">{{ item.title }}</text>
					<text class="iconfont icon-youjiantou menu-arrow" aria-hidden="true"></text>
				</button>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
	</view>
</template>

<script>
import { getUserInfo } from '@/utils/auth.js'
import { resolveFileUrl } from '@/utils/request.js'
import { ensureNonExpertAccess } from '@/utils/expertAccess.js'

const DEFAULT_AVATAR = '/static/avatar.png'

export default {
	data() {
		return {
			profile: {
				name: '用户',
				account: '-',
				avatar: DEFAULT_AVATAR
			},
			roleCode: '',
			quickActions: [
				{
					title: '我的农事',
					desc: '加水、添肥',
					icon: 'icon-nongshijilu',
					iconClass: 'farm-task-icon',
					path: '/pages/service/myTask'
				},
				{
					title: '仓库记录',
					desc: '入库、出库',
					icon: 'icon-cangkuguanli',
					iconClass: 'warehouse-icon',
					// 独立只读流水页展示入库和出库记录。
					path: '/pages/service/warehouse_record'
				}
			],
			menuGroups: [
				[
					{ title: '添加设备', icon: 'icon-tianjia', path: '/pages/service/device_list' },
					// 账户设置使用独立二级页面，避免在“我的”页面内堆叠账号操作。
					{ title: '账户设置', icon: 'icon-shezhi', path: '/pages/secondPage/account_setting/index' }
				],
				[
					// 三个说明入口跳转到本次新增的 farm 静态页面，不改变“我的”页面原有视觉。
					{ title: '关于我们', icon: 'icon-guanyuwomen', path: '/pages/secondPage/about/about_me' },
					{ title: '帮助与反馈', icon: 'icon-bangzhu', path: '/pages/secondPage/about/help' },
					{ title: '技术支持', icon: 'icon-a-kefukefuzhongxin', iconClass: 'support-icon', path: '/pages/secondPage/about/technical' }
				]
			]
		}
	},
	computed: {
		visibleMenuGroups() {
			// 工作人员的个人中心仅保留本人账号与帮助入口，不展示添加设备。
			return this.roleCode === 'user'
				? this.menuGroups.map((group) => group.filter((item) => item.title !== '添加设备'))
				: this.menuGroups
		}
	},
	async onShow() {
		if (!await ensureNonExpertAccess()) return
		this.roleCode = String(getUserInfo()?.roleCode || '').toLowerCase()
		this.loadProfile()
	},
	methods: {
		openProfile() {
			// 本页面由农场主和普通用户复用，两种角色均可编辑自己的资料。
			if (['farm_owner', 'user'].includes(this.roleCode)) {
				uni.navigateTo({ url: '/pages/secondPage/profile/index' })
			}
		},
		loadProfile() {
			const user = getUserInfo() || {}
			const realPhone = /^1[3-9]\d{9}$/.test(user.phone || '') ? user.phone : ''
			this.profile = {
				name: user.nickname || user.username || '用户',
				account: realPhone || user.username || '-',
				avatar: user.avatar ? resolveFileUrl(user.avatar) : DEFAULT_AVATAR
			}
		},
		useDefaultAvatar() {
			this.profile.avatar = DEFAULT_AVATAR
		},
		handleEntry(item) {
			if (item.path) {
				uni.navigateTo({ url: item.path })
				return
			}
			uni.showToast({ title: `${item.title}功能建设中`, icon: 'none' })
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	height: 100%;
	background-color: #f3f8f6;
}

.mine-page {
	position: relative;
	height: 100vh;
	height: 100dvh;
	overflow: hidden;
	background:
		radial-gradient(ellipse at 84% 15%, rgba(78, 198, 181, 0.23) 0%, rgba(78, 198, 181, 0) 42%),
		radial-gradient(ellipse at 14% 12%, rgba(255, 255, 255, 0.48) 0%, rgba(255, 255, 255, 0) 38%),
		linear-gradient(180deg, #9edfd6 0%, #dff6f1 27%, #f0faf7 58%, #f5fdf9 100%);
	font-size: 16px;
	font-weight: normal;
	color: #233a40;
}

.mine-page::before,
.mine-page::after {
	position: absolute;
	z-index: 0;
	border-radius: 50%;
	pointer-events: none;
	content: "";
}

.mine-page::before {
	left: -220rpx;
	top: 74rpx;
	width: 980rpx;
	height: 240rpx;
	background-color: rgba(255, 255, 255, 0.38);
	transform: rotate(-7deg);
}

.mine-page::after {
	right: -260rpx;
	top: 96rpx;
	width: 760rpx;
	height: 250rpx;
	background-color: rgba(45, 181, 163, 0.12);
	transform: rotate(8deg);
}

.page-hero {
	position: relative;
	z-index: 1;
	box-sizing: border-box;
	min-height: 360rpx;
	padding: calc(var(--status-bar-height) + 108rpx) 44rpx 0;
	background: transparent;
}

.profile {
	position: relative;
	z-index: 1;
	display: flex;
	align-items: center;
}

.profile-avatar {
	display: block;
	flex-shrink: 0;
	width: 118rpx;
	height: 118rpx;
	border: 6rpx solid rgba(255, 255, 255, 0.82);
	border-radius: 50%;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(33, 104, 94, 0.12);
}

.profile-info {
	display: flex;
	flex-direction: column;
	min-width: 0;
	margin-left: 31rpx;
}

.profile-name,
.profile-account {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.profile-pressed { opacity: 0.72; }

.profile-name {
	font-size: 24px;
	font-weight: 600;
	line-height: 1.35;
	color: #17373d;
}

.profile-account {
	margin-top: 10rpx;
	font-size: 17px;
	line-height: 1.3;
	color: #4c666b;
}

.mine-content {
	position: absolute;
	z-index: 1;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 286rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 32rpx;
}

.quick-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 30rpx;
}

.quick-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 154rpx;
	margin: 0;
	padding: 28rpx 34rpx;
	border-radius: 26rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 30rpx rgba(35, 93, 84, 0.065);
	line-height: normal;
	text-align: left;
}

.quick-card:first-child {
	background: linear-gradient(135deg, #ffffff 0%, #f1fbf8 100%);
}

.quick-card:last-child {
	background: linear-gradient(135deg, #ffffff 0%, #fff8f2 100%);
}

.quick-card::after,
.menu-row::after {
	border: 0;
}

.quick-copy {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.quick-title,
.quick-desc {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.quick-title {
	font-size: 20px;
	font-weight: 600;
	line-height: 1.3;
	color: #1f3740;
}

.quick-desc {
	margin-top: 12rpx;
	font-size: 16px;
	line-height: 1.2;
	color: #879692;
}

.quick-icon {
	flex-shrink: 0;
	margin-left: 16rpx;
	font-size: 64rpx;
	line-height: 68rpx;
}

.farm-task-icon {
	color: #63c2b7;
}

.warehouse-icon {
	color: #ffac6a;
}

.menu-group {
	overflow: hidden;
	margin-top: 36rpx;
	padding: 0;
	border-radius: 22rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 30rpx rgba(35, 93, 84, 0.055);
}

.menu-row {
	position: relative;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	width: 100%;
	height: 115rpx;
	margin: 0;
	padding: 0 36rpx;
	border-radius: 0;
	background-color: transparent;
	line-height: normal;
	text-align: left;
}

.menu-row:not(:last-child)::before {
	position: absolute;
	left: 26rpx;
	right: 26rpx;
	bottom: 0;
	height: 1rpx;
	background-color: #edf1f0;
	content: "";
	transform: scaleY(0.5);
	transform-origin: bottom;
}

.menu-icon {
	flex-shrink: 0;
	width: 44rpx;
	font-size: 44rpx;
	line-height: 52rpx;
	text-align: center;
	color: #456f78;
}

.menu-title {
	flex: 1;
	margin-left: 30rpx;
	font-size: 18px;
	font-weight: 500;
	line-height: 1.3;
	color: #243b42;
}

.menu-arrow {
	font-size: 34rpx;
	line-height: 38rpx;
	color: #b6c1bf;
}

.support-icon {
	color: #456f78;
}

.entry-pressed {
	background-color: #eaf6f3;
}

.safe-bottom {
	height: calc(52rpx + env(safe-area-inset-bottom));
}

@media screen and (min-width: 768px) {
	.mine-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
