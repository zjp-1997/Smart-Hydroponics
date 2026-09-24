<template>
	<view class="news-page">
		<view class="page-hero">
			<view class="navbar">
				<view class="nav-placeholder"></view>
				<text class="nav-title">消息</text>
				<!-- <view class="unread-summary">
					<text class="unread-label">未读（</text>
					<text class="unread-count">{{ totalUnread }}</text>
					<text class="unread-label">）</text>
				</view> -->
			</view>
		</view>

		<scroll-view class="news-content" scroll-y>
            <!-- 系统公告与维护消息使用同一入口行样式，以公告图标和未读数区分。 -->
            <view class="message-item" role="button" aria-label="查看系统消息" @tap="openAnnouncement">
                <view class="message-avatar maintenance-avatar" aria-hidden="true">
                    <text class="iconfont icon-xitonggonggao maintenance-icon"></text>
                </view>
                <view class="message-main"><text class="message-title">系统消息</text><text class="message-desc">查看系统公告</text></view>
                <view class="message-extra">
                    <text class="message-time">系统</text>
                    <text v-if="announcementUnread > 0" class="unread-badge">{{ announcementUnread }}</text>
                </view>
            </view>
            <!-- 维护消息复用专家会话的列表结构，仅用维护图标区分消息类型。 -->
            <view v-if="roleCode !== 'user'" class="message-item" @tap="openMaintenance">
                <view class="message-avatar maintenance-avatar" aria-hidden="true">
                    <text class="iconfont icon-weihu maintenance-icon"></text>
                </view>
                <view class="message-main"><text class="message-title">维护消息</text><text class="message-desc">设备故障自动通知，点击查看</text></view>
                <view class="message-extra">
                    <text class="message-time">系统</text>
                    <text v-if="maintenanceUnread > 0" class="unread-badge">{{ maintenanceUnread }}</text>
                </view>
            </view>
			<!-- 绑定成员即使尚无历史消息也会显示，点击后可直接发起沟通。 -->
			<text v-if="memberMessages.length" class="section-title">成员沟通</text>
			<view class="message-item" v-for="item in memberMessages" :key="`member-${item.peerUserId}`" @tap="openMemberChat(item)">
				<protected-image class="message-avatar" :src="item.avatar" mode="aspectFill"></protected-image>
				<view class="message-main">
					<view class="message-name-line">
						<text class="message-title member-title">{{ item.name }}</text>
						<text class="role-tag">{{ roleLabel(item.roleCode) }}</text>
					</view>
					<text class="message-desc">{{ item.content }}</text>
				</view>
				<view class="message-extra">
					<text class="message-time">{{ item.time }}</text>
					<text v-if="item.unread > 0" class="unread-badge">{{ item.unread }}</text>
				</view>
			</view>
			<text v-if="messages.length" class="section-title">专家咨询</text>
			<view class="message-item" v-for="item in messages" :key="item.id" @tap="handleMessage(item)">
				<protected-image class="message-avatar" :src="item.avatar" mode="aspectFill"></protected-image>
				<view class="message-main">
					<text class="message-title">{{ item.title }}</text>
					<text class="message-desc">{{ item.content }}</text>
				</view>
				<view class="message-extra">
					<text class="message-time">{{ item.time }}</text>
					<text v-if="item.unread > 0" class="unread-badge">{{ item.unread }}</text>
				</view>
			</view>
		</scroll-view>

	</view>
</template>

<script>
import { getMaintenanceStatistics } from '@/api/maintenanceMsg.js'
import { getSystemAnnouncementStatistics } from '@/api/systemAnnouncement.js'
import { getExpertChatSessions } from '@/api/expertChatList.js'
import { getFarmChatSessions } from '@/api/farmChat.js'
import { ensureNonExpertAccess } from '@/utils/expertAccess.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			roleCode: '',
            maintenanceUnread: 0,
            announcementUnread: 0,
            maintenanceTimer: null,
			// 消息列表由后端专家咨询会话接口驱动，不再维护本地静态聊天数据。
			messages: [],
			memberMessages: [],
			loading: false,
			memberLoading: false
		}
	},
	computed: {
		totalUnread() {
			// 顶部未读数由每个专家会话的 unread 汇总得到，避免额外请求未读数量接口。
			return [...this.messages, ...this.memberMessages].reduce((total, item) => total + Number(item.unread || 0), 0)
		}
	},
	onLoad() {
		if (getUserInfo()?.roleCode !== 'expert') this.fetchExpertChatSessions()
	},
    // 消息页可见时短轮询未读数，离开页面后停止，避免后台持续请求。
    onHide() { clearInterval(this.maintenanceTimer); this.maintenanceTimer = null },
    onUnload() { clearInterval(this.maintenanceTimer) },
	async onShow() {
        if (!await ensureNonExpertAccess()) return
		this.roleCode = String(getUserInfo()?.roleCode || '').toLowerCase()
        this.fetchAnnouncements()
		if (this.roleCode !== 'user') this.fetchMaintenance()
        clearInterval(this.maintenanceTimer)
        // 入口页可见时一并刷新两类系统消息的未读数。
        this.maintenanceTimer = setInterval(() => {
            if (this.roleCode !== 'user') this.fetchMaintenance()
            this.fetchAnnouncements()
			this.fetchMemberChatSessions()
        }, 15000)
		// 从专家聊天页返回消息页时刷新列表，确保最近消息和未读数及时更新。
		this.fetchExpertChatSessions()
		this.fetchMemberChatSessions()
	},
	methods: {
        async fetchAnnouncements() {
            try { this.announcementUnread = Number((await getSystemAnnouncementStatistics()).unreadDeliveryCount || 0) }
            catch { /* 请求层已提示错误，继续保留上次公告未读数。 */ }
        },
        openAnnouncement() {
            uni.navigateTo({ url: '/pages/secondPage/message/announcement' })
        },
        async fetchMaintenance() {
            try { this.maintenanceUnread = Number((await getMaintenanceStatistics()).unreadDeliveryCount || 0) }
            catch { /* 统一请求层提示错误，保留上次未读数。 */ }
        },
        openMaintenance() {
            // 所有页面均直接使用 uni-app 原生路由，不再经过全局 Loading 包装器。
            uni.navigateTo({ url: '/pages/secondPage/message/maintenance' })
        },
		fetchExpertChatSessions() {
			if (this.loading) {
				return
			}
			this.loading = true
			getExpertChatSessions()
				.then((messages) => {
					this.messages = messages
				})
				.catch(() => {
					this.messages = []
				})
				.finally(() => {
					this.loading = false
				})
		},
		fetchMemberChatSessions() {
			if (this.memberLoading) return
			this.memberLoading = true
			getFarmChatSessions()
				.then(items => { this.memberMessages = items })
				.catch(() => { this.memberMessages = [] })
				.finally(() => { this.memberLoading = false })
		},
		roleLabel(roleCode) {
			return { farm_owner: '农场主', technician: '技术人员', user: '普通用户' }[roleCode] || '成员'
		},
		openMemberChat(item) {
			const query = [
				`peerUserId=${encodeURIComponent(item.peerUserId)}`,
				`name=${encodeURIComponent(item.name)}`,
				`avatar=${encodeURIComponent(item.avatar)}`,
				`role=${encodeURIComponent(item.roleCode)}`
			]
			if (item.sessionId) query.push(`sessionId=${encodeURIComponent(item.sessionId)}`)
			uni.navigateTo({ url: `/pages/secondPage/member_chat/index?${query.join('&')}` })
		},
		handleMessage(item) {
			uni.navigateTo({
				// 点击专家会话进入对应专家聊天页，并携带 sessionId 继续复用已有会话。
				url: `/pages/secondPage/chat/index?id=${encodeURIComponent(item.expertId)}&sessionId=${encodeURIComponent(item.sessionId)}&name=${encodeURIComponent(item.title)}&avatar=${encodeURIComponent(item.avatar)}`
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

.news-page {
	/* 固定整个消息页，只有内部 news-content 响应纵向滚动。 */
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
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
	/* 与农场管理页 page-hero 完全一致，消息页顶部保持统一的渐变层级。 */
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
	font-size: 16px;
	line-height: 58rpx;
	color: #ffffff;
}

.nav-placeholder {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
}

.unread-summary {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	height: 58rpx;
	font-size: 14px;
	line-height: 58rpx;
}

.unread-label {
	font-size: 14px;
	line-height: 58rpx;
	color: #ffffff;
}

.unread-count {
	font-size: 14px;
	line-height: 58rpx;
	color: #ff2d2d;
}

.news-content {
	position: absolute;
	left: 0;
	right: 0;
	/* 列表紧跟顶部标题栏展示，减少标题栏与消息内容之间的空白间距。 */
	top: calc(var(--status-bar-height) + 88rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0;
}

.message-item {
	box-sizing: border-box;
	position: relative;
	display: flex;
	align-items: center;
	width: 100%;
	min-height: 126rpx;
	padding: 24rpx 44rpx;
	margin-bottom: 0;
	border-radius: 0;
	background-color: #ffffff;
}

.message-item:not(:last-child)::after {
	position: absolute;
	/* 分割线从头像右侧内容区开始，避免线条穿过头像区域。 */
	left: 170rpx;
	right: 0;
	bottom: 0;
	height: 1rpx;
	background-color: #f7f7f7;
	content: "";
	transform: scaleY(0.5);
	transform-origin: bottom;
}

.message-avatar {
	display: block;
	flex-shrink: 0;
	width: 92rpx;
	height: 92rpx;
	border-radius: 50%;
	background-color: #ffffff;
}

/* 维护消息以项目图标字体代替位图头像，尺寸和专家头像保持一致。 */
.maintenance-avatar {
	display: flex;
	align-items: center;
	justify-content: center;
	color: #1b8f82;
	background-color: #e7f7f3;
}

.maintenance-icon {
	font-size: 48rpx;
	line-height: 1;
}

.message-main {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	margin-left: 34rpx;
}

.message-title {
	font-size: 14px;
	line-height: 1.3;
	color: #C8C8C8;
}

/* 分组标题和角色标签沿用 farm 的薄荷绿色，帮助农场主区分专家咨询与成员沟通。 */
.section-title {
	display: block;
	box-sizing: border-box;
	padding: 20rpx 44rpx 12rpx;
	color: #75827f;
	background: #f7f7f7;
	font-size: 12px;
}

.message-name-line { display: flex; align-items: center; min-width: 0; }
.member-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #404b49; }
.role-tag { flex-shrink: 0; padding: 3rpx 10rpx; margin-left: 12rpx; border-radius: 16rpx; color: #1b8f82; background: #e7f7f3; font-size: 10px; }

.message-desc {
	margin-top: 18rpx;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 12px;
	line-height: 1.3;
	color: #C8C8C8;
}

.message-extra {
	display: flex;
	flex-shrink: 0;
	flex-direction: column;
	align-items: flex-end;
	justify-content: center;
	width: 86rpx;
	margin-left: 16rpx;
}

.message-time {
	font-size: 12px;
	line-height: 1.2;
	color: #C8C8C8;
}

.unread-badge {
	box-sizing: border-box;
	min-width: 34rpx;
	height: 34rpx;
	padding: 0 8rpx;
	margin-top: 22rpx;
	border-radius: 34rpx;
	text-align: center;
	font-size: 12px;
	line-height: 34rpx;
	color: #ffffff;
	background-color: #ff5b63;
}

@media screen and (min-width: 768px) {
	.news-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
