<template>
	<view class="chat-page">
		<view class="page-hero">
			<view class="navbar">
				<button class="back" aria-label="返回消息列表" @tap="handleBack"><text class="iconfont icon-fanhui"></text></button>
				<view class="title-wrap">
					<text class="nav-title">{{ peerName }}</text>
					<!-- 角色文字帮助农场主区分同名的普通用户和技术人员。 -->
					<text v-if="peerRoleLabel" class="role-label">{{ peerRoleLabel }}</text>
				</view>
				<view class="placeholder"></view>
			</view>
		</view>

		<scroll-view class="chat-content" :class="{ 'chat-content-expanded': showChatTools }" scroll-y :scroll-top="scrollTop">
			<button v-if="hasMoreBefore" class="load-earlier" :loading="loadingOlder" @tap="loadEarlier">
				{{ loadingOlder ? '正在加载...' : '加载更早消息' }}
			</button>
			<view v-if="!detailLoading && !messages.length" class="empty-tip">暂无聊天内容，发送一条消息开始沟通</view>
			<view v-for="message in displayMessages" :key="message.id">
				<text v-if="message.timeLabel" class="chat-time">{{ message.timeLabel }}</text>
				<view class="message-row" :class="{ 'message-row-self': message.role === 'self' }">
					<protected-image class="avatar" :src="message.role === 'self' ? selfAvatar : peerAvatar" mode="aspectFill"></protected-image>
					<view class="message-bubble" :class="{ 'message-bubble-self': message.role === 'self' }">
						<ChatMessageBody :message="message" />
					</view>
				</view>
			</view>
		</scroll-view>

		<ChatComposer v-model="inputValue" :sending="sending" :show-tools="showChatTools" :tools="chatTools"
			placeholder="发消息..." @update:modelValue="handleInput" @send="handleSend"
			@toggle-tools="showChatTools = !showChatTools" @tool="handleTool" />
	</view>
</template>

<script>
import {
	buildFarmChatSocketUrl,
	getFarmChatDetail,
	normalizeFarmChatSocketMessage,
	sendFarmChatMessage,
	uploadFarmChatAttachment,
} from '@/api/farmChat.js'
import ChatComposer from '@/componment/ChatComposer.vue'
import ChatMessageBody from '@/componment/ChatMessageBody.vue'
import { chooseChatAttachment } from '@/utils/chatAttachment.js'
import { chatTimeLabel } from '@/utils/chatTime.js'
import { getUserInfo } from '@/utils/auth.js'
import { resolveFileUrl } from '@/utils/request.js'

export default {
	components: { ChatComposer, ChatMessageBody },
	data() {
		return {
			sessionId: '', peerUserId: '', peerName: '聊天', peerAvatar: '/static/avatar.png', peerRoleCode: '',
			selfAvatar: '/static/avatar.png', inputValue: '', sending: false, detailLoading: false,
			showChatTools: false, scrollTop: 0, socketTask: null, socketConnected: false, messages: [],
			hasMoreBefore: false, loadingOlder: false, incrementalLoading: false, syncTimer: null,
			chatTools: [
				{ label: '拍照', iconClass: 'icon-paizhao' },
				{ label: '图片', iconClass: 'icon-morentupian-80pt' },
				{ label: '文件', iconClass: 'icon-wenjian' },
			],
		}
	},
	computed: {
		peerRoleLabel() {
			return { farm_owner: '农场主', technician: '技术人员', user: '普通用户' }[this.peerRoleCode] || ''
		},
		displayMessages() {
			// 相邻消息间隔超过五分钟时再显示时间，减少重复信息。
			return this.messages.map((message, index) => ({
				...message,
				timeLabel: chatTimeLabel(message.createTime, this.messages[index - 1]?.createTime),
			}))
		},
	},
	onLoad(options = {}) {
		this.sessionId = options.sessionId ? decodeURIComponent(options.sessionId) : ''
		this.peerUserId = options.peerUserId ? decodeURIComponent(options.peerUserId) : ''
		this.peerName = options.name ? decodeURIComponent(options.name) : '聊天'
		this.peerAvatar = options.avatar ? decodeURIComponent(options.avatar) : '/static/avatar.png'
		this.peerRoleCode = options.role ? decodeURIComponent(options.role) : ''
		// 当前用户头像来自统一登录缓存，个人资料更新后无需额外接口。
		this.selfAvatar = resolveFileUrl(getUserInfo()?.avatar) || '/static/avatar.png'
		this.fetchDetail(true)
		this.connectSocket()
		this.startIncrementalSync()
	},
	onShow() { this.fetchDetail(true); this.connectSocket(); this.startIncrementalSync() },
	onHide() { clearInterval(this.syncTimer); this.syncTimer = null },
	onUnload() { clearInterval(this.syncTimer); this.closeSocket() },
	methods: {
		handleBack() { uni.navigateBack() },
		handleInput(value) { if (value.trim()) this.showChatTools = false },
		async fetchDetail(reset = false) {
			if ((!this.sessionId && !this.peerUserId) || this.detailLoading) return
			this.detailLoading = true
			try {
				const detail = await getFarmChatDetail({ sessionId: this.sessionId, peerUserId: this.peerUserId })
				this.sessionId = detail.sessionId || this.sessionId
				this.peerUserId = detail.peerUserId || this.peerUserId
				this.peerName = detail.peerName || this.peerName
				this.peerAvatar = detail.peerAvatar || this.peerAvatar
				this.peerRoleCode = detail.peerRoleCode || this.peerRoleCode
				this.messages = detail.messages || []
				this.hasMoreBefore = detail.hasMoreBefore
				this.scrollToLatest()
			} finally { this.detailLoading = false }
		},
		async loadEarlier() {
			if (!this.hasMoreBefore || this.loadingOlder || !this.messages.length) return
			this.loadingOlder = true
			try {
				const detail = await getFarmChatDetail({ sessionId: this.sessionId,
					beforeId: this.messages[0].id })
				const existing = new Set(this.messages.map(item => String(item.id)))
				this.messages = [...detail.messages.filter(item => !existing.has(String(item.id))), ...this.messages]
				this.hasMoreBefore = detail.hasMoreBefore
			} finally { this.loadingOlder = false }
		},
		async fetchNewMessages() {
			if (!this.sessionId || this.incrementalLoading) return
			if (!this.messages.length) { await this.fetchDetail(true); return }
			this.incrementalLoading = true
			try {
				let hasMore = true
				while (hasMore) {
					const detail = await getFarmChatDetail({ sessionId: this.sessionId,
						afterId: this.messages[this.messages.length - 1].id })
					const existing = new Set(this.messages.map(item => String(item.id)))
					const additions = detail.messages.filter(item => !existing.has(String(item.id)))
					if (additions.length) this.messages = [...this.messages, ...additions]
					hasMore = Boolean(detail.hasMoreAfter && additions.length)
				}
				this.scrollToLatest()
			} finally { this.incrementalLoading = false }
		},
		startIncrementalSync() {
			clearInterval(this.syncTimer)
			// WebSocket 提供实时推送，低频游标补拉负责网络抖动后的消息补偿。
			this.syncTimer = setInterval(() => this.fetchNewMessages().catch(() => {}), 15000)
		},
		async handleSend() {
			const content = this.inputValue.trim()
			if (!content || this.sending) return
			this.sending = true
			this.showChatTools = false
			try {
				const result = await sendFarmChatMessage({ sessionId: this.sessionId || null, peerUserId: this.peerUserId, content })
				this.sessionId = result.sessionId || this.sessionId
				this.inputValue = ''
				await this.fetchNewMessages()
			} catch { /* 请求层已经展示后端错误，保留输入内容方便重试。 */ }
			finally { this.sending = false }
		},
		async handleTool(tool) {
			if (this.sending) return
			const selected = await chooseChatAttachment(tool.label)
			if (!selected?.path) return
			this.sending = true
			this.showChatTools = false
			uni.showLoading({ title: '发送中...' })
			try {
				const image = tool.label !== '文件'
				const attachment = await uploadFarmChatAttachment(selected.path, image ? 'image' : 'file', selected.name)
				const result = await sendFarmChatMessage({
					sessionId: this.sessionId || null, peerUserId: this.peerUserId,
					messageType: image ? 2 : 4, content: image ? '[图片]' : attachment.fileName,
					mediaUrl: attachment.mediaUrl,
				})
				this.sessionId = result.sessionId || this.sessionId
				await this.fetchNewMessages()
			} catch { /* 上传或发送失败时保留数据库中的原聊天记录。 */ }
			finally { this.sending = false; uni.hideLoading(); selected.cleanup?.() }
		},
		connectSocket() {
			if (this.socketTask || this.socketConnected) return
			const url = buildFarmChatSocketUrl()
			if (!url) return
			// HTTP 负责可靠落库，WebSocket 只接收已保存消息的实时通知。
			this.socketTask = uni.connectSocket({ url, complete: () => {} })
			this.socketTask.onOpen(() => { this.socketConnected = true })
			this.socketTask.onMessage(event => this.handleSocketMessage(event.data))
			this.socketTask.onClose(() => { this.socketTask = null; this.socketConnected = false })
			this.socketTask.onError(() => { this.socketTask = null; this.socketConnected = false })
		},
		closeSocket() {
			if (this.socketTask) this.socketTask.close({ code: 1000, reason: 'page unload' })
			this.socketTask = null
			this.socketConnected = false
		},
		handleSocketMessage(raw) {
			let event
			try { event = typeof raw === 'string' ? JSON.parse(raw) : raw } catch { return }
			if (!event || event.type !== 'FARM_CHAT_MESSAGE') return
			const sameSession = this.sessionId && String(event.sessionId) === String(this.sessionId)
			const samePeer = !this.sessionId && String(event.peerUserId) === String(this.peerUserId)
			if (!sameSession && !samePeer) return
			this.sessionId = event.sessionId || this.sessionId
			const message = normalizeFarmChatSocketMessage(event)
			if (!message.id || this.messages.some(item => String(item.id) === String(message.id))) return
			// 推送只负责唤醒游标补拉，避免先插入较大 ID 后跳过断线期间的较小 ID 消息。
			this.fetchNewMessages().catch(() => {})
		},
		scrollToLatest() {
			this.$nextTick(() => { this.scrollTop = this.scrollTop === 1000000 ? 1000001 : 1000000 })
		},
	},
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background:#f7f7f7; }
.chat-page { position:fixed; inset:0; overflow:hidden; background:#f7f7f7; color:#26302f; font-size:12px; }
.page-hero { box-sizing:border-box; min-height:240rpx; padding:calc(var(--status-bar-height) + 16rpx) 36rpx 0; background:linear-gradient(180deg,rgba(27,162,145,.7),rgba(90,184,173,0)); }
.navbar { display:flex; align-items:center; justify-content:space-between; height:58rpx; }
.back,.placeholder { width:72rpx; height:72rpx; }
.back { display:flex; align-items:center; padding:0; margin:0; color:#fff; background:transparent; font-size:36rpx; }
.back::after { border:0; }
.title-wrap { display:flex; align-items:center; gap:10rpx; min-width:0; }
.nav-title { max-width:280rpx; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#fff; font-size:32rpx; font-weight:600; }
.role-label { padding:4rpx 10rpx; border:1rpx solid rgba(255,255,255,.72); border-radius:18rpx; color:#fff; font-size:20rpx; }
.chat-content { position:absolute; top:calc(var(--status-bar-height) + 88rpx); right:0; bottom:calc(108rpx + env(safe-area-inset-bottom)); left:0; box-sizing:border-box; padding:28rpx 36rpx 40rpx; }
.chat-content-expanded { bottom:calc(284rpx + env(safe-area-inset-bottom)); }
.empty-tip { margin-top:120rpx; text-align:center; color:#99a3a0; font-size:24rpx; }
.load-earlier { margin:0 auto 28rpx; padding:0 28rpx; border:0; color:#168d80; background:#e3f4f0; font-size:22rpx; }
.load-earlier::after { border:0; }
.chat-time { display:block; margin:18rpx 0 34rpx; text-align:center; color:#9da6a3; font-size:22rpx; line-height:1.4; }
.message-row { display:flex; align-items:flex-start; margin-bottom:34rpx; }
.message-row-self { flex-direction:row-reverse; }
.avatar { flex-shrink:0; width:96rpx; height:96rpx; border-radius:50%; background:#fff; }
.message-bubble { box-sizing:border-box; max-width:390rpx; margin-left:22rpx; padding:22rpx 24rpx; border-radius:8rpx; background:#fff; color:#26302f; line-height:1.6; }
.message-row-self .message-bubble { margin-right:22rpx; margin-left:0; }
.message-bubble-self { background:#1ba291; color:#fff; }
@media screen and (min-width:768px) { .chat-page { width:750rpx; margin:0 auto; } }
</style>
