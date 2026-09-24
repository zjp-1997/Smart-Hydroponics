<template>
  <view class="news-page">
    <view class="hero"><text class="title">消息</text></view>
    <scroll-view class="content" scroll-y>
      <!-- 消息入口沿用农场主消息页的头像、摘要和未读角标布局。 -->
      <view class="message-row" @tap="open('/pages/secondPage/message/announcement')">
        <view class="avatar"><text class="iconfont icon-xitonggonggao"></text></view>
        <view class="copy"><text class="name">系统消息</text><text class="description">查看系统公告</text></view>
        <text v-if="announcementUnread" class="badge">{{ announcementUnread }}</text>
        <text class="arrow iconfont icon-youjiantou"></text>
      </view>
      <view class="message-row" @tap="open('/pages/secondPage/message/maintenance')">
        <view class="avatar"><text class="iconfont icon-weihu"></text></view>
        <view class="copy"><text class="name">维护消息</text><text class="description">查看设备故障和任务通知</text></view>
        <text v-if="maintenanceUnread" class="badge">{{ maintenanceUnread }}</text>
        <text class="arrow iconfont icon-youjiantou"></text>
      </view>
      <text v-if="memberMessages.length" class="section-title">农场主沟通</text>
      <!-- 技术人员可能绑定多个农场主，因此逐个展示会话摘要与未读数。 -->
      <view v-for="item in memberMessages" :key="item.peerUserId" class="message-row" @tap="openMemberChat(item)">
        <protected-image class="member-avatar" :src="item.avatar" mode="aspectFill"></protected-image>
        <view class="copy"><text class="name">{{ item.name }}</text><text class="description">{{ item.content }}</text></view>
        <view class="message-extra"><text class="time">{{ item.time }}</text><text v-if="item.unread" class="badge">{{ item.unread }}</text></view>
        <text class="arrow iconfont icon-youjiantou"></text>
      </view>
    </scroll-view>
    <TechnicianTabBar active="news" />
  </view>
</template>

<script>
import { getMaintenanceStatistics } from '@/api/maintenanceMsg.js'
import { getSystemAnnouncementStatistics } from '@/api/systemAnnouncement.js'
import { getFarmChatSessions } from '@/api/farmChat.js'
import { ensureTechnicianAccess } from '@/utils/technicianAccess.js'
import TechnicianTabBar from '@/componment/TechnicianTabBar.vue'

export default {
  components: { TechnicianTabBar },
  data() { return { announcementUnread: 0, maintenanceUnread: 0, memberMessages: [], refreshTimer: null } },
  async onShow() {
    if (!await ensureTechnicianAccess()) return
    this.refreshUnread()
    // 页面可见时刷新未读数；切走后及时清除定时器。
    clearInterval(this.refreshTimer)
    this.refreshTimer = setInterval(() => this.refreshUnread(), 15000)
  },
  onHide() { clearInterval(this.refreshTimer); this.refreshTimer = null },
  onUnload() { clearInterval(this.refreshTimer) },
  methods: {
    async refreshUnread() {
      try {
        const [announcement, maintenance, memberMessages] = await Promise.all([
          getSystemAnnouncementStatistics(), getMaintenanceStatistics(), getFarmChatSessions()
        ])
        this.announcementUnread = Number(announcement?.unreadDeliveryCount || 0)
        this.maintenanceUnread = Number(maintenance?.unreadDeliveryCount || 0)
		this.memberMessages = memberMessages
      } catch { /* 请求层统一处理网络错误，保留上次未读数据。 */ }
    },
    open(path) { uni.navigateTo({ url: path }) },
    openMemberChat(item) {
      const query = [`peerUserId=${encodeURIComponent(item.peerUserId)}`, `name=${encodeURIComponent(item.name)}`,
        `avatar=${encodeURIComponent(item.avatar)}`, `role=${encodeURIComponent(item.roleCode)}`]
      if (item.sessionId) query.push(`sessionId=${encodeURIComponent(item.sessionId)}`)
      uni.navigateTo({ url: `/pages/secondPage/member_chat/index?${query.join('&')}` })
    }
  }
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background: #f7f7f7; }
.news-page { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: #f7f7f7; color: #26302f; }
.hero { box-sizing: border-box; height: 300rpx; padding-top: calc(var(--status-bar-height) + 16rpx); background: linear-gradient(180deg, rgba(27,162,145,.7), rgba(90,184,173,0)); text-align: center; }
.title { color: #fff; font-size: 16px; line-height: 58rpx; }
.content { position: absolute; top: calc(var(--status-bar-height) + 88rpx); bottom: calc(58px + env(safe-area-inset-bottom)); width: 100%; }
.message-row { display: flex; align-items: center; box-sizing: border-box; min-height: 126rpx; padding: 22rpx 44rpx; background: #fff; border-bottom: 1rpx solid #f2f5f4; }
.avatar { display: flex; align-items: center; justify-content: center; flex-shrink: 0; width: 92rpx; height: 92rpx; border-radius: 50%; background: #e7f7f3; color: #1b8f82; font-size: 48rpx; }
.member-avatar { flex-shrink:0; width:92rpx; height:92rpx; border-radius:50%; background:#fff; }
.copy { display: flex; flex: 1; flex-direction: column; min-width: 0; margin-left: 34rpx; }
.name { font-size: 14px; color: #404b49; }
.description { margin-top: 14rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #929d9a; font-size: 12px; }
.badge { min-width: 34rpx; height: 34rpx; padding: 0 8rpx; border-radius: 34rpx; background: #ff5b63; color: #fff; text-align: center; line-height: 34rpx; font-size: 12px; }
.message-extra { display:flex; flex-direction:column; align-items:flex-end; gap:12rpx; flex-shrink:0; }
.time { color:#a0aaa7; font-size:12px; }
.section-title { display:block; padding:20rpx 44rpx 12rpx; color:#75827f; background:#f7f7f7; font-size:12px; }
.arrow { margin-left: 18rpx; color: #c6cccc; font-size: 26rpx; }
@media screen and (min-width: 768px) { .news-page { width: 750rpx; margin: 0 auto; } }
</style>
