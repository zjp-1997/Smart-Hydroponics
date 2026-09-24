<template>
	<view class="mine-page">
		<view class="page-hero">
			<!-- 点击专家头像或名称区域进入共用个人信息编辑页。 -->
			<view class="profile" hover-class="profile-pressed" :hover-stay-time="80" @tap="openProfile">
				<protected-image class="profile-avatar" :src="profile.avatar" mode="aspectFill" @error="useDefaultAvatar"></protected-image>
				<view class="profile-info">
					<text class="profile-name">{{ profile.name }}</text>
					<text class="profile-account">{{ profile.account }}</text>
				</view>
			</view>
		</view>

		<scroll-view class="mine-content" scroll-y>
			<!-- 专家中心沿用农场主菜单样式，不展示仓库记录与我的农事。 -->

			<view class="menu-group" v-for="(group, index) in menuGroups" :key="index">
				<button
					v-for="item in group"
					:key="item.title"
					class="menu-row"
					hover-class="entry-pressed"
					:hover-stay-time="80"
					@tap="handleEntry(item)"
				>
					<text class="iconfont menu-icon" :class="[item.icon, item.iconClass]"></text>
					<text class="menu-title">{{ item.title }}</text>
					<text v-if="menuBadge(item)" class="menu-badge" :class="`badge-${identityStatus}`">{{ menuBadge(item) }}</text>
					<text class="iconfont icon-youjiantou menu-arrow"></text>
				</button>
			</view>
			<view class="safe-bottom"></view>
		</scroll-view>
		<ExpertTabBar active="mine" />
	</view>
</template>

<script>
import { getUserInfo } from '@/utils/auth.js'
import { resolveFileUrl } from '@/utils/request.js'
import { getExpertProfile } from '@/api/expertWorkspace.js'
import { ensureExpertAccess } from '@/utils/expertAccess.js'
import ExpertTabBar from '@/componment/ExpertTabBar.vue'

const DEFAULT_AVATAR = '/static/avatar.png'

export default {
	components: { ExpertTabBar },
	data() {
		return {
			profile: {
				name: '用户',
				account: '-',
				avatar: DEFAULT_AVATAR
			},
			identityStatus: null,
			menuGroups: [
				[
					// 身份认证页展示专家档案及审核状态。
					{ title: '身份认证', icon: 'icon-a-typeyonghustyledunpai', path: '/pages/expert/identity' },
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
	async onShow() {
		if (!await ensureExpertAccess()) return
		this.loadProfile()
	},
	methods: {
		openProfile() {
			uni.navigateTo({ url: '/pages/secondPage/profile/index' })
		},
		loadProfile() {
			const user = getUserInfo() || {}
			const hasUserNickname = Boolean(user.nickname)
			const hasUserAvatar = Boolean(user.avatar)
			const realPhone = /^1[3-9]\d{9}$/.test(user.phone || '') ? user.phone : ''
			this.profile = {
				name: user.nickname || user.username || '用户',
				account: realPhone || user.username || '-',
				avatar: user.avatar ? resolveFileUrl(user.avatar) : DEFAULT_AVATAR
			}
			// 专家已维护 user 资料时优先显示用户资料，否则回退到专家认证档案。
			getExpertProfile().then(expert => {
				this.identityStatus = Number(expert.auditStatus ?? 0)
				if (!hasUserNickname) this.profile.name = expert.realName || this.profile.name
				if (!hasUserAvatar && expert.avatar) this.profile.avatar = resolveFileUrl(expert.avatar)
			}).catch(() => {})
		},
		useDefaultAvatar() {
			this.profile.avatar = DEFAULT_AVATAR
		},
		menuBadge(item) {
			if (item.path !== '/pages/expert/identity') return ''
			return ({ 0: '去认证', 1: '审核中', 2: '已认证', 3: '需修改' })[this.identityStatus] || ''
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
	background-color: #f7f7f7;
}

.mine-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 16px;
	font-weight: normal;
	color: #333333;
}

.page-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 64rpx) 44rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.profile {
	display: flex;
	align-items: center;
}

.profile-avatar {
	display: block;
	flex-shrink: 0;
	width: 112rpx;
	height: 112rpx;
	border: 6rpx solid rgba(255, 255, 255, 0.82);
	border-radius: 50%;
	background-color: #ffffff;
}

.profile-info {
	display: flex;
	flex-direction: column;
	min-width: 0;
	margin-left: 34rpx;
}

.profile-name,
.profile-account {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.profile-pressed { opacity: 0.72; }

.profile-name {
	font-size: 18px;
	font-weight: 500;
	line-height: 1.35;
	color: #26302f;
}

.profile-account {
	margin-top: 8rpx;
	font-size: 15px;
	line-height: 1.3;
	color: #52615f;
}

.mine-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 238rpx);
	bottom: calc(58px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	padding: 0 42rpx;
}

.quick-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 28rpx;
}

.quick-card {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 140rpx;
	margin: 0;
	padding: 24rpx 28rpx;
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.04);
	line-height: normal;
	text-align: left;
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
	font-size: 16px;
	line-height: 1.3;
	color: #343b3a;
}

.quick-desc {
	margin-top: 12rpx;
	font-size: 14px;
	line-height: 1.2;
	color: #c3c7c7;
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
	margin-top: 34rpx;
	padding: 10rpx 0;
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.035);
}

.menu-row {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	width: 100%;
	height: 88rpx;
	margin: 0;
	padding: 0 34rpx;
	border-radius: 0;
	background-color: transparent;
	line-height: normal;
	text-align: left;
}

.menu-icon {
	flex-shrink: 0;
	width: 42rpx;
	font-size: 30rpx;
	line-height: 42rpx;
	text-align: center;
	color: #737b7a;
}

.menu-title {
	flex: 1;
	margin-left: 20rpx;
	font-size: 16px;
	line-height: 1.3;
	color: #434948;
}

.menu-badge {
	flex-shrink: 0;
	margin-right: 12rpx;
	padding: 6rpx 14rpx;
	border-radius: 999rpx;
	background: #e9f6f3;
	color: #157e71;
	font-size: 12px;
	line-height: 1.4;
}

.badge-1 { background: #fff4df; color: #9a610b; }
.badge-2 { background: #e6f6ec; color: #167647; }
.badge-3 { background: #fff0ef; color: #b23d3d; }

.menu-arrow {
	font-size: 28rpx;
	line-height: 32rpx;
	color: #c6cccc;
}

.support-icon {
	color: #91b3cf;
}

.entry-pressed {
	background-color: #eef8f6;
}

.safe-bottom {
	height: calc(40rpx + env(safe-area-inset-bottom));
}

@media screen and (min-width: 768px) {
	.mine-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
