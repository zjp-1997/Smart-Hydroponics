<template>
  <view class="maintenance-page">
    <!-- 标题旁直接展示全量未读数，数据来自维护消息统计接口。 -->
    <view class="page-hero">
      <view class="navbar">
        <view class="nav-action" aria-label="返回" @tap="handleBack">
          <text class="iconfont icon-fanhui nav-icon"></text>
        </view>
        <text class="nav-title">维护消息({{ unreadCount }})</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view class="maintenance-content" scroll-y>
      <!-- 首次请求保留固定高度，避免异步数据加载造成页面跳动。 -->
      <view v-if="loading && !messages.length" class="state-card loading-state">
        <view class="loading-line wide"></view>
        <view class="loading-line"></view>
        <view class="loading-line short"></view>
      </view>

      <view v-else-if="!messages.length" class="state-card empty-state">
        <view class="empty-icon" aria-hidden="true">
          <text class="iconfont icon-weihu"></text>
        </view>
        <text class="empty-title">暂无维护消息</text>
        <text class="empty-tip">设备产生维护提醒后会展示在这里</text>
      </view>

      <view v-else class="message-list">
        <!-- 列表只展示识别消息所需的核心摘要，完整信息在点击后的弹窗中呈现。 -->
        <view
          v-for="(item, index) in messages"
          :key="item.id"
          class="message-row"
          :class="{ unread: !item.readCount }"
          role="button"
          :aria-label="`${item.title || '设备维护提醒'}，${item.readCount ? '已读' : '未读'}，点击查看详情`"
          @tap="openMessage(item)"
        >
          <view class="message-icon" aria-hidden="true">
            <text class="iconfont icon-weihu"></text>
          </view>

          <view class="message-main">
            <text class="message-title">{{ item.title || '设备维护提醒' }}</text>
            <text class="message-summary">{{ item.content || '暂无详细内容' }}</text>
          </view>

          <view class="message-side">
            <text class="read-state" :class="{ read: item.readCount }">
              {{ item.readCount ? '已读' : '未读' }}
            </text>
            <text class="message-time">{{ formatListTime(item.sendTime) }}</text>
          </view>

          <!-- 显式插入相邻消息分隔线，避免不同运行端对结构伪类支持不一致。 -->
          <view v-if="index < messages.length - 1" class="message-divider" aria-hidden="true"></view>
        </view>
      </view>

      <!-- 保留分页和上一轮已实现的一键已读能力。 -->
      <view v-if="messages.length" class="load-actions">
        <button
          v-if="messages.length < total"
          class="action-button secondary"
          :disabled="loading || markingAllRead"
          @tap="load(false)"
        >
          {{ loading ? '加载中...' : '加载更多' }}
        </button>
        <button
          class="action-button primary"
          :disabled="loading || markingAllRead || unreadCount === 0"
          @tap="markAllRead"
        >
          {{ markingAllRead ? '处理中...' : (unreadCount === 0 ? '已全部阅读' : '一键已读') }}
        </button>
      </view>
    </scroll-view>

    <!-- 自定义详情弹窗保持 farm 的卡片风格，并支持点击遮罩或按钮关闭。 -->
    <view v-if="detailVisible" class="detail-mask" role="dialog" aria-modal="true" @tap="closeDetail">
      <view class="detail-dialog" @tap.stop>
        <view class="detail-head">
          <view class="detail-icon" aria-hidden="true">
            <text class="iconfont icon-weihu"></text>
          </view>
          <view class="detail-heading">
            <text class="detail-title">{{ selectedMessage.title || '设备维护提醒' }}</text>
            <text class="detail-time">{{ formatTime(selectedMessage.sendTime) }}</text>
          </view>
        </view>

        <scroll-view class="detail-body" scroll-y>
          <text class="detail-content">{{ selectedMessage.content || '暂无详细内容' }}</text>
          <view class="detail-meta">
            <view class="meta-row">
              <text class="meta-label">地块</text>
              <text class="meta-value">{{ selectedMessage.plotName || '未关联地块' }}</text>
            </view>
            <view class="meta-row">
              <text class="meta-label">设备</text>
              <text class="meta-value">{{ selectedMessage.deviceName || '未关联设备' }}</text>
            </view>
            <view class="meta-row">
              <text class="meta-label">故障编号</text>
              <text class="meta-value">{{ selectedMessage.faultId || '人工发布' }}</text>
            </view>
            <view class="meta-row">
              <text class="meta-label">发布人</text>
              <text class="meta-value">{{ selectedMessage.publisherName || '系统自动通知' }}</text>
            </view>
          </view>
        </scroll-view>

        <button class="detail-close" @tap="closeDetail">我知道了</button>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getMaintenanceStatistics,
  listMaintenanceMessages,
  markMaintenanceRead,
} from '@/api/maintenanceMsg.js'
import { getUserInfo } from '@/utils/auth.js'

// 后端分页接口固定按 20 条返回；批量已读每批处理 5 条，控制瞬时请求量。
const PAGE_SIZE = 20
const MARK_READ_BATCH_SIZE = 5

export default {
  data() {
    return {
      messages: [],
      total: 0,
      unreadCount: 0,
      page: 0,
      loading: false,
      markingAllRead: false,
      detailVisible: false,
      selectedMessage: {},
      refreshTimer: null,
    }
  },
  onShow() {
    this.load(true)
    clearInterval(this.refreshTimer)
    // 未浏览历史分页时定时同步首屏及全量未读数。
    this.refreshTimer = setInterval(() => {
      if (this.page <= 1 && !this.markingAllRead && !this.detailVisible) this.load(true)
    }, 15000)
  },
  onHide() {
    clearInterval(this.refreshTimer)
    this.refreshTimer = null
  },
  onUnload() {
    clearInterval(this.refreshTimer)
  },
  methods: {
    formatTime(value) {
      return String(value || '暂无时间').replace('T', ' ')
    },
    // 列表右侧采用精简时间，完整时间仍在详情弹窗中展示。
    formatListTime(value) {
      const time = this.formatTime(value)
      const match = time.match(/(\d{2}-\d{2})\s+(\d{2}:\d{2})/)
      return match ? `${match[1]}\n${match[2]}` : time
    },
    handleBack() {
      const pages = getCurrentPages()
      if (pages.length > 1) {
        uni.navigateBack()
        return
      }
      // 技术人员直接打开维护消息页时回到自己的三入口工作台。
      if (String(getUserInfo()?.roleCode || '').toLowerCase() === 'technician') {
        uni.reLaunch({ url: '/pages/technician/news' })
        return
      }
      uni.switchTab({ url: '/pages/index/news' })
    },
    async refreshUnreadCount() {
      try {
        const statistics = await getMaintenanceStatistics()
        this.unreadCount = Number(statistics && statistics.unreadDeliveryCount) || 0
      } catch {
        // 统计请求失败时保留当前数字，统一请求层负责提示网络错误。
      }
    },
    async load(reset) {
      if (this.loading) return
      this.loading = true
      const next = reset ? 1 : this.page + 1
      try {
        // 列表与统计并行获取，保证标题未读数不受当前分页范围影响。
        const [result] = await Promise.all([
          listMaintenanceMessages(next),
          this.refreshUnreadCount(),
        ])
        const list = result && Array.isArray(result.list) ? result.list : []
        this.messages = reset ? list : this.messages.concat(list)
        this.total = Number(result && result.total) || 0
        this.page = next
      } catch {
        // 保留已加载的数据，用户仍可查看现有内容并稍后重试。
      } finally {
        this.loading = false
      }
    },
    async openMessage(item) {
      if (!item || item.id == null) return
      // 先展示完整快照，让弹窗反馈不被网络请求阻塞。
      this.selectedMessage = { ...item }
      this.detailVisible = true

      if (item.readCount) return
      try {
        await markMaintenanceRead(item.id)
        item.readCount = 1
        this.selectedMessage.readCount = 1
        this.unreadCount = Math.max(0, this.unreadCount - 1)
      } catch {
        // 标记失败时维持未读状态，避免客户端与服务端产生假一致。
      }
    },
    closeDetail() {
      this.detailVisible = false
      this.selectedMessage = {}
    },
    async markAllRead() {
      if (this.markingAllRead || this.unreadCount === 0) return
      const modal = await uni.showModal({
        title: '一键已读',
        content: '确认将全部维护消息标记为已读吗？',
        confirmText: '确认已读',
        confirmColor: '#5AB8AD',
      })
      if (!modal.confirm) return

      this.markingAllRead = true
      try {
        // 后端暂无维护消息批量接口，因此获取全部分页后复用单条已读接口。
        const firstResult = await listMaintenanceMessages(1)
        const firstPage = firstResult && Array.isArray(firstResult.list) ? firstResult.list : []
        const total = Number(firstResult && firstResult.total) || 0
        const pageCount = Math.ceil(total / PAGE_SIZE)
        let allMessages = firstPage

        if (pageCount > 1) {
          const requests = []
          for (let page = 2; page <= pageCount; page += 1) requests.push(listMaintenanceMessages(page))
          const results = await Promise.all(requests)
          results.forEach(result => {
            if (result && Array.isArray(result.list)) allMessages = allMessages.concat(result.list)
          })
        }

        const unreadIds = [...new Set(
          allMessages.filter(item => !item.readCount && item.id != null).map(item => item.id),
        )]
        for (let index = 0; index < unreadIds.length; index += MARK_READ_BATCH_SIZE) {
          const batch = unreadIds.slice(index, index + MARK_READ_BATCH_SIZE)
          await Promise.all(batch.map(id => markMaintenanceRead(id)))
        }

        this.messages = allMessages.map(item => ({ ...item, readCount: 1 }))
        this.total = total
        this.page = Math.max(pageCount, 1)
        this.unreadCount = 0
        uni.showToast({ title: '已全部标记为已读', icon: 'success' })
      } catch {
        // 部分请求可能已成功，重新读取列表和统计，以服务端最终状态为准。
        await this.load(true)
      } finally {
        this.markingAllRead = false
      }
    },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page { background:#f7f7f7; }
.maintenance-page { min-height:100vh; color:#20262d; background:#f7f7f7; }
.page-hero {
  min-height:300rpx;
  padding:calc(var(--status-bar-height) + 16rpx) 36rpx 0;
  box-sizing:border-box;
  background:linear-gradient(180deg, rgba(27,162,145,.7) 0%, rgba(90,184,173,.24) 62%, rgba(247,247,247,0) 100%);
}
.navbar { display:flex; align-items:center; justify-content:space-between; height:58rpx; }
.nav-action, .nav-placeholder { display:flex; align-items:center; width:72rpx; height:72rpx; }
.nav-action { justify-content:flex-start; }
.nav-placeholder { visibility:hidden; }
.nav-icon { color:#fff; font-size:38rpx; line-height:1; }
.nav-title { color:#fff; font-size:34rpx; font-weight:600; line-height:1.4; font-variant-numeric:tabular-nums; }
.maintenance-content {
  position:absolute;
  /* 与消息首页一致，列表紧接导航栏，不额外保留卡片顶部空白。 */
  top:calc(var(--status-bar-height) + 88rpx);
  right:0;
  bottom:0;
  left:0;
  box-sizing:border-box;
  padding:0 0 calc(32rpx + env(safe-area-inset-bottom));
}

/* 完整复用消息首页示例的扁平白色消息行，不使用圆角卡片或外部阴影。 */
.message-list { overflow:hidden; background:#fff; }
.message-row {
  position:relative;
  display:flex;
  align-items:center;
  width:100%;
  box-sizing:border-box;
  min-height:140rpx;
  padding:24rpx 44rpx;
  background:#fff;
  transition:background-color .16s ease, opacity .16s ease;
}
.message-divider {
  position:absolute;
  right:0;
  bottom:0;
  left:170rpx;
  /* 使用实体 1px 线，避免 1rpx 再缩放后落在半像素网格而偶发消失。 */
  height:1px;
  background:#e3e8e7;
}
.message-row.unread { background:#fff; }
.message-icon, .empty-icon, .detail-icon {
  display:flex;
  flex-shrink:0;
  align-items:center;
  justify-content:center;
  color:#228f83;
  background:#e7f7f3;
}
.message-icon { width:92rpx; height:92rpx; border-radius:50%; font-size:48rpx; }
.message-main { flex:1; min-width:0; margin-left:34rpx; }
.message-title {
  display:block;
  overflow:hidden;
  color:#5d6772;
  font-size:28rpx;
  font-weight:400;
  line-height:1.3;
  text-overflow:ellipsis;
  white-space:nowrap;
}
.message-summary {
  display:block;
  overflow:hidden;
  margin-top:18rpx;
  color:#697480;
  font-size:24rpx;
  line-height:1.3;
  text-overflow:ellipsis;
  white-space:nowrap;
}
.message-side { display:flex; flex:0 0 116rpx; flex-direction:column; align-items:flex-end; justify-content:center; margin-left:16rpx; }
.read-state { color:#ff5b63; font-size:22rpx; font-weight:500; line-height:1.3; }
.read-state.read { color:#7b8590; font-weight:400; }
.message-time { margin-top:18rpx; color:#7b8590; font-size:20rpx; line-height:1.3; text-align:right; white-space:pre-line; font-variant-numeric:tabular-nums; }

.state-card { background:#fff; }
.empty-state { display:flex; flex-direction:column; align-items:center; padding:100rpx 32rpx; }
.empty-icon { width:104rpx; height:104rpx; border-radius:32rpx; font-size:52rpx; }
.empty-title { margin-top:24rpx; color:#39424c; font-size:28rpx; font-weight:600; }
.empty-tip { margin-top:10rpx; color:#697480; font-size:23rpx; }
.loading-state { padding:38rpx 30rpx; }
.loading-line { width:72%; height:22rpx; margin-top:22rpx; border-radius:11rpx; background:#e9efee; animation:loading-pulse 1.3s ease-in-out infinite; }
.loading-line:first-child { margin-top:0; }
.loading-line.wide { width:92%; }
.loading-line.short { width:48%; }
.load-actions { display:flex; gap:20rpx; padding:28rpx 36rpx 12rpx; }
.action-button { flex:1; height:88rpx; margin:0; border-radius:44rpx; font-size:26rpx; font-weight:500; line-height:88rpx; }
.action-button::after { border:0; }
.action-button.primary { color:#fff; background:#5ab8ad; box-shadow:0 8rpx 20rpx rgba(90,184,173,.2); }
.action-button.secondary { color:#228f83; background:#e7f7f3; }
.action-button[disabled] { color:#7d8790; background:#e4e8e9; box-shadow:none; opacity:1; }

/* 弹窗使用独立遮罩和受限高度，较长正文可滚动且不会超出安全区域。 */
.detail-mask {
  position:fixed;
  z-index:1000;
  top:0;
  right:0;
  bottom:0;
  left:0;
  display:flex;
  align-items:center;
  justify-content:center;
  box-sizing:border-box;
  padding:calc(var(--status-bar-height) + 40rpx) 40rpx calc(40rpx + env(safe-area-inset-bottom));
  background:rgba(22,32,31,.56);
}
.detail-dialog { width:100%; max-width:680rpx; overflow:hidden; border-radius:24rpx; background:#fff; box-shadow:0 24rpx 70rpx rgba(14,35,32,.22); animation:dialog-enter .2s ease-out; }
.detail-head { display:flex; align-items:center; padding:32rpx 32rpx 26rpx; border-bottom:1rpx solid #e8ecec; }
.detail-icon { width:76rpx; height:76rpx; border-radius:22rpx; font-size:40rpx; }
.detail-heading { flex:1; min-width:0; margin-left:22rpx; }
.detail-title { display:block; color:#20262d; font-size:30rpx; font-weight:600; line-height:1.45; overflow-wrap:anywhere; }
.detail-time { display:block; margin-top:6rpx; color:#697480; font-size:22rpx; line-height:1.45; }
.detail-body { box-sizing:border-box; max-height:56vh; padding:28rpx 32rpx; }
.detail-content { display:block; color:#3f4954; font-size:26rpx; line-height:1.75; white-space:pre-wrap; overflow-wrap:anywhere; }
.detail-meta { margin-top:28rpx; padding:8rpx 22rpx; border-radius:14rpx; background:#f4f8f7; }
.meta-row { display:flex; align-items:flex-start; gap:24rpx; padding:16rpx 0; }
.meta-row:not(:last-child) { border-bottom:1rpx solid #e1e9e7; }
.meta-label { flex:0 0 116rpx; color:#697480; font-size:23rpx; line-height:1.5; }
.meta-value { flex:1; min-width:0; color:#303943; font-size:23rpx; line-height:1.5; text-align:right; overflow-wrap:anywhere; }
.detail-close { height:88rpx; margin:0 32rpx 32rpx; border-radius:44rpx; color:#fff; background:#5ab8ad; font-size:26rpx; font-weight:500; line-height:88rpx; }
.detail-close::after { border:0; }

.message-row:active, .nav-action:active, .action-button:active, .detail-close:active { opacity:.72; }
@keyframes loading-pulse { 0%, 100% { opacity:.55; } 50% { opacity:1; } }
@keyframes dialog-enter { from { opacity:0; transform:scale(.96); } to { opacity:1; transform:scale(1); } }
@media (prefers-reduced-motion: reduce) {
  .message-row { transition:none; }
  .loading-line, .detail-dialog { animation:none; }
}
@media screen and (min-width: 768px) {
  .maintenance-page { width:750rpx; margin:0 auto; }
}
</style>
