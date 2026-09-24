<template>
	<view class="strategy-page">
		<view class="strategy-hero">
			<view class="strategy-nav">
				<text class="iconfont icon-fanhui strategy-back" @tap="handleBack"></text>
				<text class="strategy-title">智能策略</text>
				<text class="record-link" @tap="handleRecord">识别记录</text>
			</view>
		</view>

		<view class="strategy-body">
			<view class="tab-wrap">
				<view
					class="tab-item"
					v-for="tab in tabs"
					:key="tab.value"
					:class="{ active: activeTab === tab.value }"
					@tap="switchTab(tab.value)"
				>
					<text>{{ tab.label }}</text>
				</view>
			</view>

			<view v-if="activeTab === 'recognition'" class="form-area">
				<view class="field-title">
					<view class="title-bar"></view>
					<text class="field-title-text">识别类型</text>
				</view>

				<picker mode="selector" :range="recognitionTypeNames" @change="handleTypeChange">
					<view class="select-box">
						<text class="select-text" :class="{ placeholder: !selectedType }">
							{{ selectedType || '请选择' }}
						</text>
						<text class="iconfont icon-xuanzeqizhankai_o select-icon"></text>
					</view>
				</picker>

				<view class="field-title image-title">
					<view class="title-bar"></view>
					<text class="field-title-text">识别图片</text>
				</view>

				<view class="upload-box" @tap="chooseImage">
					<protected-image v-if="imagePath" class="preview-image" :src="imagePath" mode="aspectFill"></protected-image>
					<text v-else class="upload-plus">＋</text>
				</view>
			</view>

			<!-- AI 消息由内部列表滚动，顶部导航与专家聊天同款输入区保持固定。 -->
			<scroll-view v-else class="consult-area" :class="{ expanded: showAiTools }" scroll-y
				:scroll-top="scrollTop" :scroll-into-view="scrollIntoView" @scrolltoupper="loadOlderAiHistory">
				<view v-if="historyLoading && !historyLoaded" class="consult-empty">正在加载历史对话...</view>
				<view v-else-if="!aiMessages.length" class="consult-empty">向 DeepSeek 咨询种植、病虫害或设备问题</view>
				<view v-if="historyLoaded && hasMoreHistory" class="history-more" @tap="loadOlderAiHistory">
					{{ historyLoading ? '加载中...' : '查看更早的对话' }}
				</view>
				<view
					v-for="message in aiMessages"
					:key="message.id"
					:id="`ai-message-${message.id}`"
					class="consult-row"
					:class="{ 'consult-row-user': message.role === 'user' }"
				>
					<view
						class="consult-message"
						:class="message.role === 'user' ? 'user-message' : 'ai-message'"
					>
						<ChatMessageBody :message="message" />
					</view>
				</view>
				<view v-if="sending" class="consult-thinking">DeepSeek 正在回复...</view>
			</scroll-view>
		</view>

		<button v-if="activeTab === 'recognition'" class="recognize-button" @tap="handleRecognize">识别</button>
		<!-- 复用专家聊天输入组件，拍照、图片和文件三个入口的尺寸与交互保持一致。 -->
		<ChatComposer v-else v-model="aiInput" :sending="sending || historyLoading" :show-tools="showAiTools" :tools="aiTools"
			placeholder="向 DeepSeek 提问..." @update:modelValue="handleAiInput" @send="handleAiSend"
			@toggle-tools="toggleAiTools" @tool="handleAiTool"
			@keyboard-height-change="scrollAiToLatest" />
	</view>
</template>

<script>
import {
	cacheRecognitionResult,
	getRecognitionTypes,
	recognizeByManualUpload
} from '@/api/strategyList.js'
import { getModelHistory, sendModelMessage, uploadModelAttachment } from '@/api/modelList.js'
import ChatComposer from '@/componment/ChatComposer.vue'
import ChatMessageBody from '@/componment/ChatMessageBody.vue'
import { chooseChatAttachment } from '@/utils/chatAttachment.js'

export default {
	components: { ChatComposer, ChatMessageBody },
	data() {
		return {
			activeTab: 'recognition',
			selectedType: '',
			selectedRecognitionTypeId: '',
			preferredRecognitionTypeCode: '',
			imagePath: '',
			aiInput: '',
			aiMessages: [],
			historyLoading: false,
			historyLoaded: false,
			hasMoreHistory: true,
			oldestChatId: null,
			sending: false,
			scrollTop: 0,
			scrollIntoView: '',
			showAiTools: false,
			// tabs 仅描述页面固定功能区，不属于识别业务数据，可继续在页面内维护。
			tabs: [
				{ label: '智能识别', value: 'recognition' },
				{ label: 'AI咨询', value: 'consult' }
			],
			recognitionTypes: [],
			aiTools: [
				{ label: '拍照', iconClass: 'icon-paizhao' },
				{ label: '图片', iconClass: 'icon-morentupian-80pt' },
				{ label: '文件', iconClass: 'icon-wenjian' }
			]
		}
	},
	computed: {
		recognitionTypeNames() {
			// picker 只接收字符串数组，真实的类型 ID 保留在 recognitionTypes 中提交给后端。
			return this.recognitionTypes.map((type) => type.typeName)
		}
	},
		onLoad(options) {
		// 病虫害防治页可通过类型编码要求智能策略自动填充指定识别类型。
		this.preferredRecognitionTypeCode = options && options.recognitionTypeCode
			? decodeURIComponent(options.recognitionTypeCode)
			: ''
		// 页面进入时从后端加载启用识别类型，删除本地静态业务数据。
		this.loadRecognitionTypes()
	},
		onShow() {
		// 返回已打开的 AI 咨询页时重新同步本人历史，防止缓存页面漏掉服务端记录。
		if (this.activeTab === 'consult') this.loadAiHistory(true)
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		switchTab(value) {
			this.activeTab = value
			// 切换 Tab 时收起 AI 工具面板，避免影响智能识别页面的底部按钮。
			this.showAiTools = false
			if (value === 'consult') {
				if (!this.historyLoaded) this.loadAiHistory(true)
				else this.scrollAiToLatest()
			}
		},
		handleTypeChange(event) {
			const index = Number(event.detail.value)
			const selectedType = this.recognitionTypes[index]
			this.selectedType = selectedType ? selectedType.typeName : ''
			this.selectedRecognitionTypeId = selectedType ? selectedType.id : ''
		},
		chooseImage() {
			// 先选择本地图片用于预览，点击“识别”时再和识别类型一起上传。
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (res) => {
					this.imagePath = res.tempFilePaths[0]
				}
			})
		},
		loadRecognitionTypes() {
			getRecognitionTypes()
				.then((types) => {
					this.recognitionTypes = types
					// 类型数据异步返回后再预选，确保 picker 展示名称与提交 ID 一致。
					const preferredType = types.find((type) => {
						return type.typeCode === this.preferredRecognitionTypeCode
					})
					if (preferredType) {
						this.selectedType = preferredType.typeName
						this.selectedRecognitionTypeId = preferredType.id
					}
				})
				.catch(() => {
					this.recognitionTypes = []
				})
		},
		async handleRecognize() {
			if (!this.selectedRecognitionTypeId) {
				uni.showToast({
					title: '请选择识别类型',
					icon: 'none'
				})
				return
			}
			if (!this.imagePath) {
				uni.showToast({
					title: '请上传识别图片',
					icon: 'none'
				})
				return
			}
			try {
				// 识别请求封装在 strategyList.js 中，页面只负责参数校验和跳转。
				const result = await recognizeByManualUpload({
					recognitionType: this.selectedRecognitionTypeId,
					imagePath: this.imagePath
				})
				cacheRecognitionResult(result)
				uni.navigateTo({
					url: `/pages/secondPage/AIRecognition/index?recordId=${encodeURIComponent(result.recordId)}`,
					success: () => {
						// navigateTo 会保留当前页面实例；跳转成功后主动清空表单，返回时不残留上次识别内容。
						this.resetRecognitionForm()
					}
				})
			} catch (error) {
				// 接口层已经完成错误提示，这里吞掉异常，避免事件回调产生未处理 Promise 警告。
			}
		},
		resetRecognitionForm() {
			// 清空智能识别表单的业务状态，不影响页面 UI 结构和样式。
			this.selectedType = ''
			this.selectedRecognitionTypeId = ''
			this.imagePath = ''
		},
		handleRecord() {
			uni.navigateTo({
				url: '/pages/secondPage/AIRecognition/record'
			})
		},
		toggleAiTools() {
			this.showAiTools = !this.showAiTools
		},
		handleAiInput(value) {
			if (value.trim()) {
				this.showAiTools = false
			}
		},
		scrollAiToLatest() {
			// 键盘改变可用高度后将已发送/已回复消息滚到末尾，顶部导航和 Tab 不参与滚动。
			this.scrollIntoView = ''
			this.$nextTick(() => { this.scrollTop = this.scrollTop === 1000000 ? 1000001 : 1000000 })
		},
		loadOlderAiHistory() {
			if (this.historyLoaded && this.hasMoreHistory) this.loadAiHistory(false)
		},
		async loadAiHistory(reset) {
			if (this.historyLoading || this.sending || (!reset && !this.hasMoreHistory)) return
			this.historyLoading = true
			const anchorId = !reset && this.aiMessages.length ? this.aiMessages[0].id : ''
			try {
				const history = await getModelHistory({ beforeId: reset ? undefined : this.oldestChatId, pageSize: 50 })
				// 每条 ai_chat 记录还原成一条用户消息和一条 DeepSeek 回复，并保留图片/文件气泡。
				const messages = history.flatMap((item) => {
					const file = !!item.fileUrl
					const image = !!item.imageUrl
					return [
						{ id: `user-${item.id}`, role: 'user', messageType: file ? 4 : image ? 2 : 1,
							content: file ? item.fileName || '文件附件' : image ? '[图片]' : item.userContent,
							mediaUrl: file ? item.fileUrl : item.imageUrl },
						{ id: `ai-${item.id}`, role: 'ai', messageType: 1, content: item.aiContent }
					]
				})
				if (reset) {
					this.aiMessages = messages
				} else {
					const existingIds = new Set(this.aiMessages.map((message) => message.id))
					this.aiMessages = [...messages.filter((message) => !existingIds.has(message.id)), ...this.aiMessages]
				}
				if (history.length) this.oldestChatId = history[0].id
				this.hasMoreHistory = history.length === 50
				this.historyLoaded = true
				if (reset) this.scrollAiToLatest()
				else if (anchorId) this.$nextTick(() => { this.scrollIntoView = `ai-message-${anchorId}` })
			} catch (error) {
				// 请求层负责错误提示；保留当前已展示消息供用户继续查看。
			} finally {
				this.historyLoading = false
			}
		},
		async handleAiSend() {
			const content = this.aiInput.trim()
			if (!content || this.sending || this.historyLoading) return
			this.sending = true
			this.showAiTools = false
			const pendingId = `user-${Date.now()}`
			this.aiMessages.push({
				id: pendingId,
				role: 'user',
				content,
				messageType: 1
			})
			this.scrollAiToLatest()
			try {
				// AI 消息只调用后端的 DeepSeek 代理，完全不创建专家咨询会话。
				const result = await sendModelMessage({ content })
				this.aiInput = ''
				const pendingIndex = this.aiMessages.findIndex((message) => message.id === pendingId)
				if (pendingIndex >= 0) this.aiMessages[pendingIndex].id = `user-${result.id}`
				this.aiMessages.push({
					id: `ai-${result.id || Date.now()}`,
					role: 'ai',
					content: result.aiContent,
					messageType: 1
				})
				this.scrollAiToLatest()
			} catch (error) {
				// 失败时保留输入并撤销未落库的气泡，用户可直接重试。
				this.aiMessages = this.aiMessages.filter((message) => message.id !== pendingId)
			} finally {
				this.sending = false
			}
		},
		async handleAiTool(tool) {
			if (this.sending || this.historyLoading) return
			const selected = await chooseChatAttachment(tool.label)
			if (!selected?.path) return
			this.sending = true
			this.showAiTools = false
			uni.showLoading({ title: '发送中...' })
			try {
				// 本人附件先上传，再由后端读取内容并调用 DeepSeek；文件名只用于气泡展示。
				const image = tool.label !== '文件'
				const attachment = await uploadModelAttachment(selected.path, image ? 'image' : 'file', selected.name)
				const result = await sendModelMessage({
					content: '',
					imageUrl: image ? attachment.mediaUrl : undefined,
					fileUrl: image ? undefined : attachment.mediaUrl,
					fileName: image ? undefined : attachment.fileName
				})
				this.aiMessages.push({
					id: `user-${result.id}`,
					role: 'user',
					content: image ? '[图片]' : attachment.fileName,
					messageType: image ? 2 : 4,
					mediaUrl: image ? result.imageUrl : result.fileUrl
				}, {
					id: `ai-${result.id}`,
					role: 'ai',
					content: result.aiContent,
					messageType: 1
				})
				this.scrollAiToLatest()
			} catch (error) {
				// 上传或模型调用失败由请求层提示，不展示未成功落库的附件。
			} finally {
				this.sending = false
				uni.hideLoading()
				selected.cleanup?.()
			}
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
}

.strategy-page {
	/* AI 咨询仅滚动消息区，底部输入框与顶部导航始终留在视口内。 */
	position: fixed;
	inset: 0;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.strategy-hero {
	box-sizing: border-box;
	height: 220rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.strategy-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.strategy-title {
	position: absolute;
	left: 120rpx;
	right: 120rpx;
	text-align: center;
	font-size: 16px;
	line-height: 58rpx;
	color: #ffffff;
}

.strategy-back {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
	text-align: left;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.record-link {
	position: relative;
	z-index: 2;
	font-size: 14px;
	line-height: 58rpx;
	color: #ffffff;
}

.strategy-body {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 150rpx);
	bottom: 0;
	box-sizing: border-box;
	overflow: hidden;
	border-top-left-radius: 54rpx;
	border-top-right-radius: 54rpx;
	background-color: #ffffff;
}

.tab-wrap {
	display: flex;
	align-items: center;
	height: 90rpx;
	background-color: #f7f7f7;
	border-top-left-radius: 54rpx;
	border-top-right-radius: 54rpx;
	overflow: hidden;
}

.tab-item {
	display: flex;
	align-items: center;
	justify-content: center;
	flex: 1;
	height: 90rpx;
	font-size: 14px;
	line-height: 90rpx;
	color: #C8C8C8;
}

.tab-item.active {
	background-color: #6FC4BA;
	color: #ffffff;
}

.tab-item:first-child {
	border-top-left-radius: 54rpx;
}

.tab-item:last-child {
	border-top-right-radius: 54rpx;
}

.form-area {
	box-sizing: border-box;
	height: calc(100% - 90rpx);
	overflow-y: auto;
	padding: 34rpx 36rpx 180rpx;
	background-color: #ffffff;
}

.field-title {
	display: flex;
	align-items: center;
	height: 42rpx;
}

.title-bar {
	flex-shrink: 0;
	width: 12rpx;
	height: 42rpx;
	margin-right: 18rpx;
	border-radius: 8rpx;
	background-color: #6FC4BA;
}

.field-title-text {
	font-size: 16px;
	line-height: 42rpx;
	color: #000000;
}

.select-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 72rpx;
	margin-top: 24rpx;
	padding: 0 22rpx;
	border: 2rpx solid #eeeeee;
	border-radius: 8rpx;
	background-color: #ffffff;
}

.select-text {
	flex: 1;
	min-width: 0;
	font-size: 14px;
	line-height: 72rpx;
	color: #000000;
}

.select-text.placeholder {
	color: #000000;
}

.select-icon {
	flex-shrink: 0;
	font-size: 22rpx;
	line-height: 22rpx;
	color: #000000;
}

.image-title {
	margin-top: 36rpx;
}

.upload-box {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 124rpx;
	height: 124rpx;
	margin-top: 34rpx;
	margin-left: 34rpx;
	background-color: #f4f4f4;
}

.upload-plus {
	font-size: 58rpx;
	line-height: 1;
	color: #C8C8C8;
}

.preview-image {
	width: 124rpx;
	height: 124rpx;
}

.recognize-button {
	position: fixed;
	left: 36rpx;
	right: 36rpx;
	bottom: calc(44rpx + env(safe-area-inset-bottom));
	z-index: 20;
	height: 86rpx;
	margin: 0;
	padding: 0;
	border-radius: 44rpx;
	background-color: #6FC4BA;
	font-size: 14px;
	line-height: 86rpx;
	color: #ffffff;
}

.recognize-button::after {
	border: none;
}

.consult-area {
	position: absolute;
	left: 0;
	right: 0;
	top: 90rpx;
	bottom: calc(108rpx + env(safe-area-inset-bottom));
	box-sizing: border-box;
	padding: 36rpx 36rpx 24rpx;
	background-color: #ffffff;
}

.consult-area.expanded {
	bottom: calc(284rpx + env(safe-area-inset-bottom));
}

.consult-empty,
.consult-thinking {
	padding: 48rpx 24rpx;
	color: #899995;
	font-size: 26rpx;
	text-align: center;
}

.history-more {
	/* 历史入口保留足够触控高度，向上翻页时固定在消息内容最前方。 */
	min-height: 72rpx;
	line-height: 72rpx;
	color: #6f9f99;
	font-size: 24rpx;
	text-align: center;
}

.consult-row {
	display: flex;
	align-items: flex-start;
	width: 100%;
	margin-bottom: 24rpx;
}

.consult-row-user {
	justify-content: flex-end;
}

.consult-message {
	box-sizing: border-box;
	font-size: 26rpx;
	line-height: 1.5;
	word-break: break-all;
	white-space: normal;
}

.user-message {
	max-width: 72%;
	padding: 18rpx 24rpx;
	border-radius: 20rpx;
	background-color: #d9f2ed;
	color: #38423f;
}

.ai-message {
	max-width: 72%;
	padding: 18rpx 24rpx;
	border-radius: 20rpx;
	background-color: #f5f7f6;
	color: #38423f;
}

@media screen and (min-width: 768px) {
	.strategy-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
