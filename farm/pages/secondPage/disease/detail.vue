<template>
	<view class="detail-page">
		<!-- 顶部栏严格采用参考图的青绿色背景、居中标题和三枚等宽操作区。 -->
		<view class="detail-header">
			<view class="detail-nav">
				<view class="nav-action nav-action-left" hover-class="nav-action-pressed" aria-label="返回病虫害列表" @tap="handleBack">
					<text class="iconfont icon-fanhui nav-back-icon" aria-hidden="true"></text>
				</view>
				<text class="detail-nav-title">{{ detail ? detail.name : '病虫害详情' }}</text>
				<view class="nav-actions">
					<view
						class="nav-action"
						:class="{ 'nav-action-favorite': isFavorite }"
						hover-class="nav-action-pressed"
						:aria-label="isFavorite ? '取消收藏' : '收藏病虫害'"
						@tap="toggleFavorite"
					>
						<image class="nav-svg" src="/static/disease-detail/heart.svg" mode="aspectFit" aria-hidden="true"></image>
					</view>
					<button class="nav-action nav-share" open-type="share" hover-class="nav-action-pressed" aria-label="分享病虫害详情">
						<image class="nav-svg" src="/static/disease-detail/share.svg" mode="aspectFit" aria-hidden="true"></image>
					</button>
				</view>
			</view>
		</view>

		<scroll-view class="detail-scroll" scroll-y>
			<!-- 骨架屏与最终内容使用相同高度，避免数据返回时发生明显跳动。 -->
			<view v-if="loading" class="detail-loading" aria-label="正在加载病虫害详情">
				<view class="skeleton skeleton-cover"></view>
				<view class="skeleton-card">
					<view class="skeleton skeleton-title"></view>
					<view class="skeleton skeleton-line"></view>
					<view class="skeleton skeleton-line skeleton-line-short"></view>
				</view>
			</view>

			<!-- 错误状态保留参考页面的简洁视觉，并提供明确恢复入口。 -->
			<view v-else-if="errorMessage" class="detail-state">
				<text class="state-title">暂时无法查看详情</text>
				<text class="state-message">{{ errorMessage }}</text>
				<button class="retry-button" hover-class="button-pressed" @tap="loadDetail">重新加载</button>
			</view>

			<view v-else-if="detail" class="detail-content">
				<!-- 参考图使用无蒙层的全宽头图，点击后仍可查看原图。 -->
				<view class="cover-wrap" hover-class="cover-pressed" @tap="previewCover">
					<protected-image class="detail-cover" :src="detail.image || fallbackImage" :alt="`${detail.name}封面图`" mode="aspectFill"></protected-image>
				</view>

				<!-- 摘要卡与封面重叠，按参考图展示名称、分类、作物、时期和症状。 -->
				<view class="summary-card">
					<view class="summary-heading">
						<text class="detail-name">{{ detail.name }}</text>
						<text class="type-badge">{{ typeText }}</text>
					</view>
					<view class="summary-meta">
						<view class="summary-meta-item">
							<text class="meta-label">危害作物</text>
							<text class="meta-value">{{ detail.affectedCrops }}</text>
						</view>
						<view class="summary-meta-item">
							<text class="meta-label">发生时期</text>
							<text class="meta-value">{{ detail.occurrencePeriod }}</text>
						</view>
					</view>
					<view class="summary-symptom">
						<text class="meta-label">常见症状</text>
						<text class="summary-symptom-text">{{ detail.symptom }}</text>
					</view>
				</view>

				<!-- 症状正文与数据库图片共同构成识别依据。 -->
				<section-card title="症状识别">
					<text class="section-content">{{ detail.symptom }}</text>
					<scroll-view v-if="galleryImages.length" class="symptom-gallery" scroll-x show-scrollbar="false">
						<view class="gallery-row">
							<protected-image
								v-for="(image, index) in galleryImages"
								:key="`${image}-${index}`"
								class="symptom-image"
								:src="image"
								mode="aspectFill"
								:aria-label="`症状图片${index + 1}`"
								hover-class="image-pressed"
								@tap="previewGallery(index)"
							></protected-image>
						</view>
					</scroll-view>
				</section-card>

				<!-- 发生规律使用参考图的彩色图标块与三行信息结构。 -->
				<section-card title="发生规律">
					<view v-if="occurrenceItems.length" class="occurrence-list">
						<view v-for="item in occurrenceItems" :key="item.key" class="occurrence-item">
							<view class="occurrence-icon" :class="`occurrence-icon-${item.key}`">
								<text class="iconfont" :class="item.icon" aria-hidden="true"></text>
							</view>
							<view class="occurrence-copy">
								<text class="occurrence-title">{{ item.title }}</text>
								<text class="occurrence-text">{{ item.content }}</text>
							</view>
						</view>
					</view>
					<text v-else class="empty-inline">暂无发生规律说明</text>
				</section-card>

				<!-- 四类措施各自成为独立白色卡片，避免当前实现中的“大卡套小卡”。 -->
				<view class="section-block control-section">
					<view class="section-heading">
						<view class="section-mark"></view>
						<text class="section-title">防治方法</text>
					</view>
					<view v-if="controlGroups.length" class="control-groups">
						<view v-for="group in controlGroups" :key="group.category" class="control-card">
							<view class="control-heading">
								<view class="control-icon" :class="`control-icon-${group.tone}`">
									<text class="iconfont" :class="group.icon" aria-hidden="true"></text>
								</view>
								<view class="control-title-copy">
									<text class="control-group-title">{{ group.title }}</text>
									<text class="control-group-subtitle">{{ group.subtitle }}</text>
								</view>
							</view>
							<view v-for="(control, index) in group.items" :key="control.id || index" class="control-item">
								<view v-for="(line, lineIndex) in controlLines(control)" :key="lineIndex" class="control-line">
									<view class="control-dot" :class="`control-dot-${group.tone}`"></view>
									<text class="control-line-text">{{ line }}</text>
								</view>
							</view>
						</view>
					</view>
					<view v-else class="empty-controls">
						<text class="empty-controls-title">暂无防治方案</text>
						<text class="empty-controls-text">可联系农业专家获取针对性建议</text>
					</view>
				</view>

				<!-- 推荐药剂使用参考图的三列表格和底部黄色安全提示。 -->
				<view v-if="recommendedDrugs.length" class="section-block medicine-section">
					<view class="section-heading">
						<view class="section-mark"></view>
						<text class="section-title">推荐药剂</text>
					</view>
					<view class="medicine-card">
						<view class="drug-row drug-header">
							<text>药剂名称</text>
							<text>使用浓度</text>
							<text>注意事项</text>
						</view>
						<view v-for="(control, index) in recommendedDrugs" :key="control.id || index" class="drug-row">
							<text class="drug-name">{{ control.drugName }}</text>
							<text class="drug-dosage">{{ control.dosageSpec || '按登记标签用量' }}</text>
							<text class="drug-warning">{{ drugPrecaution(control) }}</text>
						</view>
						<view class="medicine-notice">
							<text class="notice-icon">△</text>
							<text class="notice-text">请严格按照说明书使用，注意轮换用药，避免产生抗药性。采收前严格遵守安全间隔期。</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import { getDiseasePestDetail } from '@/api/diseasePestList.js'
import SectionCard from './components/SectionCard.vue'

export default {
	components: { SectionCard },
	data() {
		return {
			diseaseId: null,
			loading: true,
			errorMessage: '',
			detail: null,
			isFavorite: false,
			fallbackImage: '/static/lecttue.png'
		}
	},
	computed: {
		/** 将数据库枚举转换成摘要卡中的红色分类标签。 */
		typeText() {
			const labels = { 1: '病害防治', 2: '虫害防治', 3: '生理障碍' }
			return labels[this.detail?.type] || '重点防治'
		},
		/** 详情图片由接口统一补齐服务地址，没有图片时不生成空白占位卡。 */
		galleryImages() {
			return Array.isArray(this.detail?.imageUrls) ? this.detail.imageUrls : []
		},
		/** 发生规律携带图标信息，模板只负责循环渲染。 */
		occurrenceItems() {
			if (!this.detail) return []
			return [
				{ key: 'habit', title: '生活习性', content: this.detail.livingHabits, icon: 'icon-shijian' },
				{ key: 'environment', title: '适宜环境', content: this.detail.suitableEnvironment, icon: 'icon-wendu' },
				{ key: 'route', title: '传播途径', content: this.detail.transmissionRoute, icon: 'icon-ziyuan' }
			].filter((item) => item.content)
		},
		/** 防治分类补充参考图所需的图标、色调和副标题。 */
		controlGroups() {
			const controls = Array.isArray(this.detail?.controls) ? this.detail.controls : []
			const categories = [
				{ category: 1, title: '农业防治', subtitle: '基础措施', icon: 'icon-shuangyezi', tone: 'green' },
				{ category: 2, title: '物理防治', subtitle: '绿色防控', icon: 'icon-dengpao', tone: 'yellow' },
				{ category: 3, title: '化学防治', subtitle: '应急控制', icon: 'icon-iconkaishixiaodu', tone: 'red' },
				{ category: 4, title: '生物防治', subtitle: '生态调控', icon: 'icon-jurassic_label', tone: 'teal' },
				{ category: 0, title: '综合防治', subtitle: '综合措施', icon: 'icon-bingchonghai', tone: 'blue' }
			]
			return categories
				.map((group) => ({
					...group,
					items: controls.filter((control) =>
						group.category === 0
							? ![1, 2, 3, 4].includes(control.controlCategory)
							: control.controlCategory === group.category
					)
				}))
				.filter((group) => group.items.length)
		},
		/** 只有配置了药剂名称的措施才进入推荐药剂表。 */
		recommendedDrugs() {
			return (this.detail?.controls || []).filter((control) => control.drugName)
		}
	},
	onLoad(options = {}) {
		const id = Number(options.id)
		if (!Number.isInteger(id) || id <= 0) {
			this.loading = false
			this.errorMessage = '病虫害参数无效'
			return
		}
		this.diseaseId = id
		this.loadDetail()
	},
	methods: {
		/** 优先返回来源页面，没有历史栈时回到病虫害列表。 */
		handleBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.reLaunch({ url: '/pages/service/disease_control' })
		},
		/** 收藏属于当前页面的轻量交互，不额外引入未要求的持久化接口。 */
		toggleFavorite() {
			this.isFavorite = !this.isFavorite
			uni.showToast({ title: this.isFavorite ? '已收藏' : '已取消收藏', icon: 'none' })
		},
		/** 请求一次详情接口，后端同时返回主数据和启用的防治措施。 */
		async loadDetail() {
			if (!this.diseaseId) return
			this.loading = true
			this.errorMessage = ''
			try {
				this.detail = await getDiseasePestDetail(this.diseaseId)
			} catch (error) {
				this.detail = null
				this.errorMessage = error?.message || '请检查网络后重试'
			} finally {
				this.loading = false
			}
		},
		/** 使用系统图片预览查看封面原图。 */
		previewCover() {
			const image = this.detail?.image || this.fallbackImage
			uni.previewImage({ current: image, urls: [image] })
		},
		/** 从点击位置打开详情图库，并支持系统手势切换。 */
		previewGallery(index) {
			uni.previewImage({ current: this.galleryImages[index], urls: this.galleryImages })
		},
		/** 将一条措施拆成参考图中的圆点列表，过滤空文本避免空行。 */
		controlLines(control) {
			return [control.method, control.usageMethod, control.precautions].filter(Boolean)
		},
		/** 优先展示安全间隔期；无固定天数时提示遵守登记标签。 */
		drugPrecaution(control) {
			if (control.safetyIntervalDays != null) return `安全间隔${control.safetyIntervalDays}天`
			return '按标签安全使用'
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f6f7f8;
}

.detail-page {
	position: relative;
	min-height: 100vh;
	background-color: #f6f7f8;
	color: #27343d;
}

.detail-header {
	box-sizing: border-box;
	height: calc(var(--status-bar-height) + 88rpx);
	padding-top: var(--status-bar-height);
	/* 与病虫害防治页 disease-hero 共用同一渐变及渐变高度，确保导航区域颜色完全一致。 */
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
	background-size: 100% 300rpx;
	background-repeat: no-repeat;
}

.detail-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 88rpx;
	padding: 0 16rpx;
}

.detail-nav-title {
	position: absolute;
	left: 210rpx;
	right: 210rpx;
	overflow: hidden;
	text-align: center;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 17px;
	line-height: 88rpx;
	color: #ffffff;
}

.nav-actions {
	display: flex;
	align-items: center;
	gap: 16rpx;
}

.nav-action {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	width: 88rpx;
	height: 88rpx;
	margin: 0;
	padding: 0;
	border: 0;
	border-radius: 50%;
	background: transparent;
	line-height: 1;
}

.nav-action::after {
	border: 0;
}

.nav-action-left {
	width: 88rpx;
	justify-content: flex-start;
	padding-left: 8rpx;
}

.nav-action-pressed {
	background-color: rgba(255, 255, 255, 0.16);
}

.nav-action-favorite {
	background-color: rgba(255, 255, 255, 0.2);
}

.nav-back-icon {
	font-size: 20px;
	color: #ffffff;
}

.nav-svg {
	display: block;
	width: 38rpx;
	height: 38rpx;
}

.detail-scroll {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 88rpx);
	bottom: 0;
}

.detail-content,
.detail-loading {
	box-sizing: border-box;
	width: 100%;
	max-width: 750rpx;
	margin: 0 auto;
	padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.cover-wrap,
.skeleton-cover {
	height: 480rpx;
}

.cover-wrap {
	overflow: hidden;
	background-color: #dfe9e5;
}

.cover-pressed,
.image-pressed {
	opacity: 0.86;
}

.detail-cover {
	display: block;
	width: 100%;
	height: 100%;
}

.summary-card,
.info-card,
.control-card,
.medicine-card {
	box-sizing: border-box;
	margin-right: 24rpx;
	margin-left: 24rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 28rpx rgba(30, 58, 64, 0.07);
}

.summary-card {
	position: relative;
	z-index: 2;
	margin-top: -44rpx;
	padding: 30rpx 30rpx 28rpx;
}

.summary-heading,
.section-heading,
.control-heading {
	display: flex;
	align-items: center;
}

.summary-heading {
	justify-content: space-between;
}

.detail-name {
	flex: 1;
	min-width: 0;
	font-size: 21px;
	font-weight: 750;
	line-height: 1.4;
	color: #242f38;
}

.type-badge {
	flex-shrink: 0;
	margin-left: 18rpx;
	padding: 7rpx 18rpx;
	border-radius: 999rpx;
	background-color: #fff0ef;
	font-size: 12px;
	font-weight: 600;
	line-height: 30rpx;
	color: #ef5d62;
}

.summary-meta {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	gap: 26rpx;
	margin-top: 26rpx;
}

.summary-meta-item {
	min-width: 0;
}

.meta-label,
.meta-value,
.summary-symptom-text {
	display: block;
}

.meta-label {
	font-size: 12px;
	line-height: 30rpx;
	color: #a1acb4;
}

.meta-value {
	margin-top: 6rpx;
	font-size: 14px;
	font-weight: 600;
	line-height: 38rpx;
	color: #485760;
}

.summary-symptom {
	margin-top: 22rpx;
}

.summary-symptom-text {
	display: -webkit-box;
	overflow: hidden;
	margin-top: 5rpx;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 3;
	font-size: 14px;
	line-height: 1.65;
	color: #57666f;
}

.section-block {
	margin-top: 34rpx;
}

.section-heading {
	margin: 0 24rpx 18rpx;
}

.section-mark {
	width: 7rpx;
	height: 34rpx;
	margin-right: 13rpx;
	border-radius: 999rpx;
	background-color: #1eb8ab;
}

.section-title {
	font-size: 17px;
	font-weight: 750;
	line-height: 44rpx;
	color: #26343d;
}

.info-card {
	padding: 28rpx 30rpx;
}

.section-content,
.occurrence-text,
.control-line-text,
.empty-inline {
	display: block;
	font-size: 14px;
	line-height: 1.7;
	white-space: pre-wrap;
	word-break: break-word;
}

.section-content {
	color: #5a6972;
}

.symptom-gallery {
	width: 100%;
	margin-top: 24rpx;
	white-space: nowrap;
}

.gallery-row {
	display: inline-flex;
	gap: 14rpx;
	padding-right: 2rpx;
}

.symptom-image {
	width: 190rpx;
	height: 150rpx;
	border-radius: 2rpx;
	background-color: #e3ecea;
}

.occurrence-list {
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.occurrence-item {
	display: flex;
	align-items: flex-start;
}

.occurrence-icon,
.control-icon {
	display: flex;
	align-items: center;
	justify-content: center;
	flex: 0 0 auto;
	width: 54rpx;
	height: 54rpx;
	border-radius: 13rpx;
	font-size: 18px;
}

.occurrence-icon-habit {
	background-color: #ebfbf9;
	color: #31bcae;
}

.occurrence-icon-environment {
	background-color: #fff6e8;
	color: #ff8b3d;
}

.occurrence-icon-route {
	background-color: #edf5ff;
	color: #5b9bf0;
}

.occurrence-copy {
	flex: 1;
	min-width: 0;
	margin-left: 20rpx;
}

.occurrence-title {
	display: block;
	font-size: 14px;
	font-weight: 700;
	line-height: 36rpx;
	color: #34434c;
}

.occurrence-text {
	margin-top: 2rpx;
	color: #687780;
}

.control-groups {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}

.control-card {
	padding: 28rpx 30rpx;
}

.control-icon-green {
	background-color: #eafaf2;
	color: #2cbd79;
}

.control-icon-yellow {
	background-color: #fff9e6;
	color: #e7b322;
}

.control-icon-red {
	background-color: #fff0f0;
	color: #ef6268;
}

.control-icon-teal {
	background-color: #e9faf8;
	color: #20afa3;
}

.control-icon-blue {
	background-color: #edf5ff;
	color: #5b91df;
}

.control-title-copy {
	margin-left: 20rpx;
}

.control-group-title,
.control-group-subtitle {
	display: block;
}

.control-group-title {
	font-size: 15px;
	font-weight: 750;
	line-height: 36rpx;
	color: #33424b;
}

.control-group-subtitle {
	margin-top: 1rpx;
	font-size: 11px;
	line-height: 28rpx;
	color: #a0abb3;
}

.control-item {
	margin-top: 18rpx;
}

.control-line {
	display: flex;
	align-items: flex-start;
}

.control-line + .control-line {
	margin-top: 9rpx;
}

.control-dot {
	flex: 0 0 auto;
	width: 8rpx;
	height: 8rpx;
	margin: 15rpx 15rpx 0 4rpx;
	border-radius: 50%;
	background-color: #31bcae;
}

.control-dot-yellow {
	background-color: #e7b322;
}

.control-dot-red {
	background-color: #ef6268;
}

.control-dot-teal {
	background-color: #20afa3;
}

.control-dot-blue {
	background-color: #5b91df;
}

.control-line-text {
	flex: 1;
	min-width: 0;
	color: #5d6c75;
}

.medicine-card {
	overflow: hidden;
	border-radius: 2rpx;
}

.drug-row {
	display: grid;
	grid-template-columns: 0.95fr 1.2fr 1.1fr;
	align-items: center;
	min-height: 96rpx;
	padding: 14rpx 24rpx;
	font-size: 12px;
	line-height: 1.45;
	color: #64727b;
}

.drug-row + .drug-row {
	border-top: 1rpx solid #f0f1f2;
}

.drug-header {
	min-height: 64rpx;
	background-color: #fafbfb;
	font-size: 11px;
	color: #8e9aa2;
}

.drug-name {
	font-weight: 700;
	color: #3d4b54;
}

.drug-dosage {
	padding-right: 10rpx;
}

.drug-warning {
	justify-self: start;
	padding: 7rpx 12rpx;
	border-radius: 999rpx;
	background-color: #fff5e8;
	font-size: 11px;
	line-height: 26rpx;
	color: #ff9248;
}

.medicine-notice {
	display: flex;
	align-items: flex-start;
	padding: 20rpx 24rpx;
	background-color: #fffbe8;
}

.notice-icon {
	flex: 0 0 auto;
	margin-right: 12rpx;
	font-size: 15px;
	font-weight: 700;
	line-height: 34rpx;
	color: #e6ad2f;
}

.notice-text {
	font-size: 11px;
	line-height: 34rpx;
	color: #9b7d32;
}

.empty-inline,
.empty-controls {
	color: #87949c;
}

.empty-controls {
	margin: 0 24rpx;
	padding: 36rpx 24rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
	text-align: center;
	box-shadow: 0 8rpx 28rpx rgba(30, 58, 64, 0.07);
}

.empty-controls-title,
.empty-controls-text {
	display: block;
}

.empty-controls-title {
	font-size: 14px;
	font-weight: 650;
}

.empty-controls-text {
	margin-top: 8rpx;
	font-size: 12px;
}

.detail-state {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-direction: column;
	min-height: 720rpx;
	padding: 48rpx;
	text-align: center;
}

.state-title {
	font-size: 17px;
	font-weight: 650;
}

.state-message {
	margin-top: 10rpx;
	font-size: 13px;
	line-height: 40rpx;
	color: #718089;
}

.retry-button {
	width: 240rpx;
	height: 88rpx;
	margin-top: 30rpx;
	padding: 0;
	border-radius: 44rpx;
	background-color: #22b8ad;
	font-size: 14px;
	line-height: 88rpx;
	color: #ffffff;
}

.retry-button::after {
	border: 0;
}

.button-pressed {
	opacity: 0.78;
}

.skeleton {
	background-color: #e1e8e7;
}

.skeleton-card {
	margin: -44rpx 24rpx 0;
	padding: 30rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
}

.skeleton-title,
.skeleton-line {
	border-radius: 999rpx;
}

.skeleton-title {
	width: 52%;
	height: 38rpx;
}

.skeleton-line {
	width: 100%;
	height: 24rpx;
	margin-top: 28rpx;
}

.skeleton-line-short {
	width: 68%;
}

@media screen and (min-width: 768px) {
	.detail-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
