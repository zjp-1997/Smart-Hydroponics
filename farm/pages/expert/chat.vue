<template>
  <view class="chat-page">
    <!-- 顶部导航与农场主聊天页共用同样的尺寸、渐变和返回按钮布局。 -->
    <view class="page-hero"><view class="navbar">
      <button class="back" aria-label="返回消息" @tap="goBack"><text class="iconfont icon-fanhui"></text></button>
      <text class="nav-title">{{ userName }}</text><view class="placeholder"></view>
    </view></view>
    <scroll-view class="chat-content" :class="{ expanded: showChatTools }" scroll-y :scroll-top="scrollTop">
      <button v-if="hasMoreBefore" class="load-earlier" :loading="loadingOlder" @tap="loadEarlier">
        {{ loadingOlder ? '正在加载...' : '加载更早消息' }}
      </button>
      <view v-if="!messages.length" class="empty">暂无聊天记录</view>
      <view v-for="message in displayMessages" :key="message.messageId">
        <!-- 微信式时间仅在首条、跨天或与上一条间隔较长时显示。 -->
        <text v-if="message.timeLabel" class="chat-time">{{ message.timeLabel }}</text>
        <view class="chat-row" :class="{ own: message.role === 'user' }">
          <view class="chat-avatar"><text class="iconfont" :class="message.role === 'user' ? 'icon-zhuanjia' : 'icon-gerenzhongxin2'"></text></view>
          <view class="bubble"><ChatMessageBody :message="message" /></view>
        </view>
      </view>
    </scroll-view>
    <ChatComposer v-model="draft" :sending="sending" :show-tools="showChatTools" :tools="chatTools"
                  placeholder="回复咨询用户..." @update:modelValue="onDraftChange" @send="sendReply"
                  @toggle-tools="toggleChatTools" @tool="handleTool" />
  </view>
</template>

<script>
import { getExpertSessionDetail, replyExpertSession, uploadExpertReplyAttachment } from '@/api/expertWorkspace.js'
import { buildExpertChatSocketUrl, normalizeSocketChatMessage } from '@/api/expertChatList.js'
import { ensureExpertAccess } from '@/utils/expertAccess.js'
import ChatComposer from '@/componment/ChatComposer.vue'
import ChatMessageBody from '@/componment/ChatMessageBody.vue'
import { chooseChatAttachment } from '@/utils/chatAttachment.js'
import { chatTimeLabel } from '@/utils/chatTime.js'

export default {
  components: { ChatComposer, ChatMessageBody },
  data() { return {
    sessionId: '', userName: '咨询用户', messages: [], draft: '', sending: false,
    showChatTools: false, scrollTop: 0, refreshTimer: null, hasMoreBefore: false, loadingOlder: false,
    socketTask: null, incrementalLoading: false,
    chatTools: [
      { label: '拍照', iconClass: 'icon-paizhao' },
      { label: '图片', iconClass: 'icon-morentupian-80pt' },
      { label: '文件', iconClass: 'icon-wenjian' },
    ],
  } },
  computed: {
    displayMessages() {
      // 专家侧和农场主侧都从每条消息的发送时间计算相同的分组标签。
      return this.messages.map((message, index) => ({
        ...message,
        timeLabel: chatTimeLabel(message.createTime, this.messages[index - 1]?.createTime),
      }))
    },
  },
  onLoad(options) { this.sessionId = options.sessionId || '' },
  async onShow() {
    if (!await ensureExpertAccess()) return
    if (!this.sessionId) { this.goBack(); return }
    this.loadDetail()
    this.connectSocket()
    clearInterval(this.refreshTimer)
    // 只按最后一条消息ID补拉，避免定时下载完整会话。
    this.refreshTimer = setInterval(() => this.loadNewMessages(), 10000)
  },
  onHide() { clearInterval(this.refreshTimer); this.refreshTimer = null; this.closeSocket() },
  onUnload() { clearInterval(this.refreshTimer); this.closeSocket() },
  methods: {
    goBack() { uni.reLaunch({ url: '/pages/expert/news' }) },
    onDraftChange(value) { if (value.trim()) this.showChatTools = false },
    toggleChatTools() { this.showChatTools = !this.showChatTools },
    connectSocket() {
      if (this.socketTask) return
      const url = buildExpertChatSocketUrl()
      if (!url) return
      // 推送负责实时到达，十秒游标补拉只用于弥补断线窗口。
      this.socketTask = uni.connectSocket({ url, complete: () => {} })
      this.socketTask.onMessage(event => {
        let payload
        try { payload = typeof event.data === 'string' ? JSON.parse(event.data) : event.data } catch { return }
        if (payload?.type !== 'CONSULT_MESSAGE' || String(payload.sessionId) !== String(this.sessionId)) return
        const message = normalizeSocketChatMessage(payload)
        if (!message.id || this.messages.some(item => String(item.messageId) === String(message.id))) return
        // 推送仅触发按 ID 补拉，确保断线窗口内更小 ID 的消息不会被较新推送越过。
        this.loadNewMessages().catch(() => {})
      })
      this.socketTask.onClose(() => { this.socketTask = null })
      this.socketTask.onError(() => { this.socketTask = null })
    },
    closeSocket() { if (this.socketTask) this.socketTask.close(); this.socketTask = null },
    async handleTool(tool) {
      if (this.sending) return
      const selected = await chooseChatAttachment(tool.label)
      if (!selected?.path) return
      this.sending = true
      this.showChatTools = false
      uni.showLoading({ title: '发送中...' })
      try {
        // 附件先上传到专家账号目录，再作为本人会话的图片或文件消息保存。
        const image = tool.label !== '文件'
        const attachment = await uploadExpertReplyAttachment(selected.path, image ? 'image' : 'file', selected.name)
        await replyExpertSession(this.sessionId, image ? '[图片]' : attachment.fileName,
          image ? 2 : 4, attachment.mediaUrl)
        await this.loadNewMessages()
      } catch { /* 失败由统一请求层提示，不插入未落库的临时消息。 */ }
      finally { this.sending = false; uni.hideLoading(); selected.cleanup?.() }
    },
    async loadDetail() {
      try {
        const detail = await getExpertSessionDetail(this.sessionId)
        this.userName = detail.userName || '咨询用户'
        this.messages = detail.messages || []
        this.hasMoreBefore = Boolean(detail.hasMoreBefore)
        // 更新 scrollTop 以便新回复后滚动到最新消息。
        this.$nextTick(() => { this.scrollTop = this.scrollTop === 1000000 ? 1000001 : 1000000 })
      } catch { /* 错误由请求层提示，保留已加载聊天内容。 */ }
    },
    async loadEarlier() {
      if (!this.hasMoreBefore || this.loadingOlder || !this.messages.length) return
      this.loadingOlder = true
      try {
        const detail = await getExpertSessionDetail(this.sessionId, { beforeId: this.messages[0].messageId })
        const existing = new Set(this.messages.map(item => String(item.messageId)))
        this.messages = [...(detail.messages || []).filter(item => !existing.has(String(item.messageId))), ...this.messages]
        this.hasMoreBefore = Boolean(detail.hasMoreBefore)
      } finally { this.loadingOlder = false }
    },
    async loadNewMessages() {
      if (this.incrementalLoading) return
      if (!this.messages.length) { await this.loadDetail(); return }
      this.incrementalLoading = true
      try {
        let hasMore = true
        while (hasMore) {
          const detail = await getExpertSessionDetail(this.sessionId, {
            afterId: this.messages[this.messages.length - 1].messageId
          })
          const existing = new Set(this.messages.map(item => String(item.messageId)))
          const additions = (detail.messages || []).filter(item => !existing.has(String(item.messageId)))
          if (additions.length) this.messages = [...this.messages, ...additions]
          hasMore = Boolean(detail.hasMoreAfter && additions.length)
        }
        this.$nextTick(() => { this.scrollTop = this.scrollTop === 1000000 ? 1000001 : 1000000 })
      } finally { this.incrementalLoading = false }
    },
    async sendReply() {
      const content = this.draft.trim()
      if (!content || this.sending) return
      this.sending = true
      this.showChatTools = false
      try {
        await replyExpertSession(this.sessionId, content)
        this.draft = ''
        await this.loadNewMessages()
      } catch { /* 发送失败保留草稿供用户重试。 */ }
      finally { this.sending = false }
    },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background:#f7f7f7; }
/* 固定聊天导航和底部输入框，只允许中间的消息列表滚动。 */
.chat-page { position:fixed; top:0; right:0; bottom:0; left:0; overflow:hidden; background:#f7f7f7; }
.page-hero { box-sizing:border-box; min-height:240rpx; padding:calc(var(--status-bar-height) + 16rpx) 36rpx 0; background:linear-gradient(180deg,rgba(27,162,145,.7),rgba(90,184,173,0)); }
.navbar { display:flex; align-items:center; justify-content:space-between; height:58rpx; }
.back,.placeholder { width:72rpx; height:72rpx; }
.back { display:flex; align-items:center; padding:0; margin:0; color:#fff; background:transparent; font-size:36rpx; }
.back::after { border:0; }
.nav-title { color:#fff; font-size:32rpx; font-weight:600; }
.chat-content { position:absolute; top:calc(var(--status-bar-height) + 88rpx); right:0; bottom:calc(108rpx + env(safe-area-inset-bottom)); left:0; box-sizing:border-box; padding:24rpx 30rpx; }
/* 展开操作项时为图标块与完整文字预留空间。 */
.chat-content.expanded { bottom:calc(284rpx + env(safe-area-inset-bottom)); }
.empty { padding:120rpx 0; color:#9ca5a3; text-align:center; }
.load-earlier { margin:0 auto 28rpx; padding:0 28rpx; border:0; color:#168d80; background:#e3f4f0; font-size:22rpx; }
.load-earlier::after { border:0; }
.chat-time { display:block; margin:18rpx 0 34rpx; color:#9da6a3; font-size:22rpx; line-height:1.4; text-align:center; }
.chat-row { display:flex; align-items:flex-start; margin-bottom:28rpx; }
.chat-row.own { flex-direction:row-reverse; }
.chat-avatar { display:flex; align-items:center; justify-content:center; flex-shrink:0; width:72rpx; height:72rpx; border-radius:50%; color:#1b8f82; background:#e7f7f3; font-size:36rpx; }
.bubble { display:flex; flex-direction:column; max-width:72%; margin-left:16rpx; padding:18rpx 24rpx; border-radius:20rpx; background:#fff; color:#38423f; font-size:28rpx; line-height:1.5; overflow-wrap:anywhere; }
.own .bubble { margin-left:0; margin-right:16rpx; background:#d9f2ed; }
</style>
