<template>
  <view class="news-page">
    <!-- 顶部未读数汇总本人咨询消息和系统公告。 -->
    <view class="page-hero"><view class="navbar"><text class="nav-title">消息{{ unreadCount ? `(${unreadCount})` : '' }}</text></view></view>
    <scroll-view class="news-content" scroll-y>
      <!-- 公告入口复用农场主消息页的图标、摘要和未读角标样式。 -->
      <view class="message-item" role="button" aria-label="查看系统消息" @tap="openAnnouncement">
        <view class="message-avatar announcement-avatar" aria-hidden="true">
          <text class="iconfont icon-xitonggonggao announcement-icon"></text>
        </view>
        <view class="message-main">
          <text class="message-title">系统消息</text>
          <text class="message-desc">查看系统公告</text>
        </view>
        <view class="message-extra">
          <text class="message-time">系统</text>
          <text v-if="announcementUnread > 0" class="unread-badge">{{ announcementUnread }}</text>
        </view>
      </view>
      <view v-if="loading && !sessions.length" class="state">加载咨询消息中...</view>
      <view v-else-if="!sessions.length" class="state">暂无咨询消息</view>
      <view v-for="session in sessions" :key="session.id" class="message-item" role="button"
            :aria-label="`与${displayName(session)}的咨询，${session.expertUnreadCount || 0}条未读`"
            @tap="openSession(session)">
        <protected-image class="message-avatar" :src="avatarFor(session)" mode="aspectFill"></protected-image>
        <view class="message-main">
          <text class="message-title">{{ displayName(session) }}</text>
          <text class="message-desc">{{ session.lastMessageContent || '暂无聊天内容' }}</text>
        </view>
        <view class="message-extra">
          <text class="message-time">{{ formatTime(session.lastMessageTime) }}</text>
          <text v-if="session.expertUnreadCount > 0" class="unread-badge">{{ session.expertUnreadCount }}</text>
        </view>
      </view>
    </scroll-view>
    <ExpertTabBar active="news" />
  </view>
</template>

<script>
import ExpertTabBar from '@/componment/ExpertTabBar.vue'
import { getExpertSessions } from '@/api/expertWorkspace.js'
import { getSystemAnnouncementStatistics } from '@/api/systemAnnouncement.js'
import { ensureExpertAccess } from '@/utils/expertAccess.js'
import { resolveFileUrl } from '@/utils/request.js'

export default {
  components: { ExpertTabBar },
  data() { return { sessions: [], announcementUnread: 0, loading: false, refreshTimer: null } },
  computed: {
    unreadCount() {
      // 标题数字包含两种消息的未读数，列表角标仍分别展示各自未读数。
      return this.announcementUnread + this.sessions.reduce((sum, item) => sum + Number(item.expertUnreadCount || 0), 0)
    },
  },
  async onShow() {
    if (!await ensureExpertAccess()) return
    this.loadSessions()
    this.loadAnnouncements()
    clearInterval(this.refreshTimer)
    // 页面可见时同步咨询与公告未读数；离开后立即停止轮询。
    this.refreshTimer = setInterval(() => {
      this.loadSessions()
      this.loadAnnouncements()
    }, 15000)
  },
  onHide() { clearInterval(this.refreshTimer); this.refreshTimer = null },
  onUnload() { clearInterval(this.refreshTimer) },
  methods: {
    async loadAnnouncements() {
      try {
        const statistics = await getSystemAnnouncementStatistics()
        this.announcementUnread = Number(statistics && statistics.unreadDeliveryCount) || 0
      } catch { /* 请求失败时保留已有未读数，由统一请求层提示错误。 */ }
    },
    openAnnouncement() {
      // 公告详情页按当前登录用户查询，专家只能阅读自己的接收记录。
      uni.navigateTo({ url: '/pages/secondPage/message/announcement' })
    },
    displayName(session) { return session.nickname || session.username || '咨询用户' },
    // 用户头像与农场主消息列表使用相同的资源解析规则。
    avatarFor(session) { return session.userAvatar ? resolveFileUrl(session.userAvatar) : '/static/avatar.png' },
    formatTime(value) { return value ? String(value).replace('T', ' ').slice(5, 16) : '' },
    async loadSessions() {
      if (this.loading) return
      this.loading = true
      try { this.sessions = await getExpertSessions() || [] }
      catch { /* 请求层负责错误提示，保留现有列表以便继续查看。 */ }
      finally { this.loading = false }
    },
    openSession(session) {
      // 会话 ID 只是定位参数，后端再次校验是否属于当前专家。
      uni.navigateTo({ url: `/pages/expert/chat?sessionId=${encodeURIComponent(session.id)}` })
    },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background:#f7f7f7; }
/* 固定消息页与顶部导航，仅让内部会话列表滚动。 */
.news-page { position:fixed; top:0; right:0; bottom:0; left:0; overflow:hidden; background:#f7f7f7; color:#333; }
.page-hero { box-sizing:border-box; min-height:300rpx; padding:calc(var(--status-bar-height) + 16rpx) 36rpx 0; background:linear-gradient(180deg,rgba(27,162,145,.7),rgba(90,184,173,0)); }
.navbar { display:flex; justify-content:center; align-items:center; height:58rpx; }
.nav-title { color:#fff; font-size:16px; line-height:58rpx; }
.news-content { position:absolute; top:calc(var(--status-bar-height) + 88rpx); bottom:calc(58px + env(safe-area-inset-bottom)); left:0; right:0; }
.message-item { display:flex; align-items:center; box-sizing:border-box; min-height:126rpx; padding:24rpx 44rpx; background:#fff; border-bottom:1px solid #f1f1f1; }
.message-item:active { opacity:.75; }
.message-avatar { display:block; flex-shrink:0; width:92rpx; height:92rpx; border-radius:50%; background:#e7f7f3; }
.announcement-avatar { display:flex; align-items:center; justify-content:center; }
.announcement-icon { color:#1b8f82; font-size:48rpx; line-height:1; }
.message-main { display:flex; flex:1; flex-direction:column; min-width:0; margin-left:34rpx; }
.message-title { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:14px; color:#596561; }
.message-desc { overflow:hidden; margin-top:18rpx; text-overflow:ellipsis; white-space:nowrap; font-size:12px; color:#929b99; }
.message-extra { display:flex; flex-shrink:0; flex-direction:column; align-items:flex-end; margin-left:16rpx; }
.message-time { color:#9ca5a3; font-size:12px; }
.unread-badge { box-sizing:border-box; min-width:34rpx; height:34rpx; margin-top:18rpx; padding:0 8rpx; border-radius:34rpx; color:#fff; background:#ff5b63; font-size:12px; line-height:34rpx; text-align:center; }
.state { padding:100rpx 32rpx; background:#fff; color:#8b9692; font-size:28rpx; text-align:center; }
@media screen and (min-width:768px) { .news-page { width:750rpx; margin:0 auto; } }
</style>
