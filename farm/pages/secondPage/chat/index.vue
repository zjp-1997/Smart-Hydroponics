<template>
	<view class="chat-page">
		<view class="page-hero">
			<view class="navbar">
				<!-- 返回按钮尺寸与专家聊天页一致，保留可点击区域。 -->
				<button class="back" aria-label="返回消息" @tap="handleBack"><text class="iconfont icon-fanhui"></text></button>
				<text class="nav-title">{{ expertName }}</text>
				<view class="placeholder"></view>
			</view>
		</view>

		<scroll-view class="chat-content" :class="{ 'chat-content-expanded': showChatTools }" scroll-y :scroll-top="scrollTop">
			<view
				v-for="message in displayMessages"
				:key="message.id"
			>
				<!-- 仅在首条或与上一条相隔五分钟以上时显示微信式时间标签。 -->
				<text v-if="message.timeLabel" class="chat-time">{{ message.timeLabel }}</text>
				<view class="message-row" :class="{ 'message-row-user': message.role === 'user' }">
					<protected-image class="avatar" :src="getMessageAvatar(message)" mode="aspectFill"></protected-image>
					<view class="message-bubble" :class="{ 'message-bubble-user': message.role === 'user' }">
						<ChatMessageBody :message="message" />
					</view>
				</view>
			</view>
		</scroll-view>

		<ChatComposer v-model="inputValue" :sending="sending" :show-tools="showChatTools" :tools="chatTools"
			placeholder="发消息..." @update:modelValue="handleInput" @send="handleSend"
			@toggle-tools="toggleChatTools" @tool="handleTool" />
	</view>
</template>

<script>
import {
	buildExpertChatSocketUrl,
	getExpertChatDetail,
	normalizeSocketChatMessage,
	sendExpertChatMessage,
	uploadExpertChatAttachment
} from '@/api/expertChatList.js'
import ChatComposer from '@/componment/ChatComposer.vue'
import ChatMessageBody from '@/componment/ChatMessageBody.vue'
import { chooseChatAttachment } from '@/utils/chatAttachment.js'
import { chatTimeLabel } from '@/utils/chatTime.js'

export default {
	components: { ChatComposer, ChatMessageBody },
	data() {
		return {
			expertId: '',
			sessionId: '',
			expertName: '张三',
			expertAvatar: '/static/avatar.png',
			userAvatar: '/static/avatar.png',
			inputValue: '',
			sending: false,
			scrollTop: 0,
			showChatTools: false,
			// 详情加载状态用于避免 onLoad 期间重复请求同一会话历史。
			detailLoading: false,
			// socketTask 保存当前页面 WebSocket 连接，页面卸载时需要主动关闭。
			socketTask: null,
			// WebSocket 连接状态用于避免重复建立连接。
			socketConnected: false,
			// 聊天内容由用户发送和后端返回状态驱动，不再使用静态演示消息。
			messages: [],
			chatTools: [
				{ label: '拍照', iconClass: 'icon-paizhao' },
				{ label: '图片', iconClass: 'icon-morentupian-80pt' },
				{ label: '文件', iconClass: 'icon-wenjian' }
			]
		}
	},
	computed: {
		displayMessages() {
			// 时间标签依赖前一条消息，统一由共享工具计算，实时消息追加后会自动更新。
			return this.messages.map((message, index) => ({
				...message,
				timeLabel: chatTimeLabel(message.createTime, this.messages[index - 1]?.createTime)
			}))
		}
	},
	onLoad(options = {}) {
		// 专家列表进入聊天页时会携带专家姓名；兜底默认值保证直接打开页面也能渲染。
		if (options.id) {
			this.expertId = decodeURIComponent(options.id)
		}
		if (options.sessionId) {
			// 从消息列表进入时复用已有会话，避免同一专家产生多个进行中会话。
			this.sessionId = decodeURIComponent(options.sessionId)
		}
		if (options.name) {
			this.expertName = decodeURIComponent(options.name)
		}
		if (options.avatar) {
			// 列表页先带入当前头像，详情接口返回后会再覆盖为数据库最新头像。
			this.expertAvatar = decodeURIComponent(options.avatar)
		}
		this.fetchChatDetail()
		this.connectConsultSocket()
	},
	onShow() {
		// 页面从其他入口返回栈顶时重新拉取数据库消息，确保同一专家会话在两个入口之间实时同步。
		this.fetchChatDetail()
		this.connectConsultSocket()
	},
	onUnload() {
		// 离开聊天页时关闭 WebSocket，避免页面销毁后仍继续接收推送。
		this.closeConsultSocket()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		toggleChatTools() {
			this.showChatTools = !this.showChatTools
		},
		handleInput(value) {
			if (value.trim()) {
				this.showChatTools = false
			}
		},
		connectConsultSocket() {
			if (this.socketTask || this.socketConnected) {
				return
			}
			const socketUrl = buildExpertChatSocketUrl()
			if (!socketUrl) {
				return
			}
			// 第一阶段 WebSocket 只负责实时接收推送，发送消息仍走 HTTP 接口完成落库。
			const socketTask = uni.connectSocket({
				url: socketUrl,
				complete: () => {}
			})
			this.socketTask = socketTask
			socketTask.onOpen(() => {
				this.socketConnected = true
			})
			socketTask.onMessage((event) => {
				this.handleConsultSocketMessage(event.data)
			})
			socketTask.onClose(() => {
				this.socketTask = null
				this.socketConnected = false
			})
			socketTask.onError(() => {
				this.socketTask = null
				this.socketConnected = false
			})
		},
		closeConsultSocket() {
			if (!this.socketTask) {
				return
			}
			this.socketTask.close({
				code: 1000,
				reason: 'page unload'
			})
			this.socketTask = null
			this.socketConnected = false
		},
		handleConsultSocketMessage(rawMessage) {
			let message = null
			try {
				message = typeof rawMessage === 'string' ? JSON.parse(rawMessage) : rawMessage
			} catch (error) {
				return
			}
			if (!message || message.type !== 'CONSULT_MESSAGE') {
				return
			}
			const sameSession = this.sessionId && String(message.sessionId) === String(this.sessionId)
			const sameExpertWithoutSession = !this.sessionId && this.expertId && String(message.expertId) === String(this.expertId)
			if (!sameSession && !sameExpertWithoutSession) {
				return
			}
			this.sessionId = message.sessionId || this.sessionId
			this.expertId = message.expertId || this.expertId
			this.expertName = message.expertName || this.expertName
			const normalizedMessage = normalizeSocketChatMessage(message)
			if (!normalizedMessage.id || this.messages.some((item) => String(item.id) === String(normalizedMessage.id))) {
				return
			}
			// WebSocket 推送只追加当前会话的新消息，历史全量仍由 HTTP 详情接口兜底同步。
			this.messages = [...this.messages, normalizedMessage]
			this.scrollToLatest()
		},
		async fetchChatDetail() {
			if (!this.expertId && !this.sessionId) {
				return
			}
			if (this.detailLoading) {
				return
			}
			this.detailLoading = true
			try {
				// 通过后端统一读取会话历史，保证专家列表入口和消息列表入口展示同一批聊天记录。
				const detail = await getExpertChatDetail({
					expertId: this.expertId,
					sessionId: this.sessionId
				})
				this.sessionId = detail.sessionId || this.sessionId
				this.expertId = detail.expertId || this.expertId
				this.expertName = detail.expertName || this.expertName
				// 专家头像来自后端 expert_profile.avatar，管理端修改后刷新聊天页即可同步展示。
				this.expertAvatar = detail.expertAvatar || this.expertAvatar
				this.messages = detail.messages || []
				this.scrollToLatest()
			} finally {
				this.detailLoading = false
			}
		},
		async handleSend() {
			const content = this.inputValue.trim()
			if (!content || this.sending) {
				return
			}
			this.sending = true
			this.showChatTools = false
			try {
				// 发送专家咨询消息时优先使用当前会话ID，首次发送则由后端创建会话。
				const result = await sendExpertChatMessage({
					expertId: this.expertId,
					sessionId: this.sessionId,
					content
				})
				this.sessionId = result.sessionId || this.sessionId
				this.inputValue = ''
				// 发送成功后重新加载数据库中的会话消息，避免不同入口进入聊天页时出现本地临时消息不同步。
				await this.fetchChatDetail().catch(() => {})
			} catch (error) {
				// 发送失败保留输入草稿，避免用户重新输入；请求层已提示错误。
			} finally { this.sending = false }
		},
		scrollToLatest() {
			// 变更滚动值使新消息到来后聊天列表保持在底部。
			this.$nextTick(() => { this.scrollTop = this.scrollTop === 1000000 ? 1000001 : 1000000 })
		},
		async handleTool(tool) {
			if (this.sending) return
			const selected = await chooseChatAttachment(tool.label)
			if (!selected?.path) return
			this.sending = true
			this.showChatTools = false
			uni.showLoading({ title: '发送中...' })
			try {
				// 拍照和相册都发送图片类型；文件名称随消息保存供双方查看。
				const image = tool.label !== '文件'
				const attachment = await uploadExpertChatAttachment(selected.path, image ? 'image' : 'file', selected.name)
				const result = await sendExpertChatMessage({
					expertId: this.expertId,
					sessionId: this.sessionId,
					messageType: image ? 2 : 4,
					content: image ? '[图片]' : attachment.fileName,
					mediaUrl: attachment.mediaUrl
				})
				this.sessionId = result.sessionId || this.sessionId
				await this.fetchChatDetail()
			} catch { /* 上传或发送失败时请求层提示错误，聊天历史保持不变。 */ }
			finally { this.sending = false; uni.hideLoading(); selected.cleanup?.() }
		},
		getMessageAvatar(message) {
			// 用户侧暂无个人头像接口时使用默认头像；专家消息统一使用最新专家头像。
			return message.role === 'user' ? this.userAvatar : this.expertAvatar
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.chat-page {
	/* 固定聊天导航和底部输入框，只允许中间的消息列表滚动。 */
	position: fixed;
	top: 0;
	right: 0;
	bottom: 0;
	left: 0;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 12px;
	font-weight: normal;
	color: #000000;
}

.page-hero {
	box-sizing: border-box;
	min-height: 240rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.navbar {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.nav-title {
	color: #fff;
	font-size: 32rpx;
	font-weight: 600;
}

.back,.placeholder { width:72rpx; height:72rpx; }
.back {
	display:flex;
	align-items:center;
	padding:0;
	margin:0;
	color:#fff;
	background:transparent;
	font-size:36rpx;
}
.back::after { border:0; }

.chat-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 88rpx);
	bottom: calc(108rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	padding: 28rpx 36rpx 40rpx;
}

.chat-content-expanded {
	/* 展开操作项时为图标块与完整文字预留空间。 */
	bottom: calc(284rpx + env(safe-area-inset-bottom));
}

.chat-time {
	display: block;
	margin: 18rpx 0 34rpx;
	text-align: center;
	font-size: 22rpx;
	line-height: 1.4;
	color: #9da6a3;
}

.message-row {
	display: flex;
	align-items: flex-start;
	margin-bottom: 34rpx;
}

.message-row-user {
	flex-direction: row-reverse;
}

.avatar {
	flex-shrink: 0;
	width: 96rpx;
	height: 96rpx;
	border-radius: 50%;
	background-color: #ffffff;
}

.message-bubble {
	box-sizing: border-box;
	max-width: 390rpx;
	margin-left: 22rpx;
	padding: 22rpx 24rpx;
	border-radius: 8rpx;
	background-color: #ffffff;
	font-size: 12px;
	line-height: 1.6;
	color: #000000;
}

.message-row-user .message-bubble {
	margin-right: 22rpx;
	margin-left: 0;
}

.message-bubble-user {
	background-color: #1BA291;
	color: #ffffff;
}

@media screen and (min-width: 768px) {
	.chat-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
