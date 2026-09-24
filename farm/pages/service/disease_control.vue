<template>
	<view class="disease-page">
		<view class="disease-hero">
			<view class="disease-nav">
				<text class="iconfont icon-fanhui disease-back" @tap="handleBack"></text>
				<text class="disease-title">病虫害防治</text>
				<view class="disease-actions">
					<text v-if="canManageKnowledge" class="iconfont icon-tianjia1 disease-action-icon" aria-label="上传病虫害信息" @tap="openUploadDialog"></text>
					<text class="iconfont icon-saoma recognition-link disease-action-icon" aria-label="病虫害识别" @tap="handleRecognition"></text>
				</view>
			</view>
		</view>

		<scroll-view class="disease-content" scroll-y>
			<view class="search-wrap">
				<view class="search-box">
					<text class="iconfont icon-icon_33 search-icon"></text>
					<input
						class="search-input"
						name="disease-search"
						v-model="keyword"
						placeholder="请输入病虫害名称"
						placeholder-style="color: #C8C8C8;"
					/>
					<button class="search-button" :disabled="loading" @tap="handleSearch">
						{{ isSearchActive ? '取消' : '搜索' }}
					</button>
				</view>
			</view>

			<view v-if="!loading" class="result-summary">
				<text class="result-title">病虫害知识</text>
				<text class="result-count">{{ resultSummary }}</text>
			</view>

			<!-- 数据加载期间预留卡片空间，避免接口返回后页面发生明显跳动。 -->
			<view v-if="loading" class="disease-grid">
				<view class="disease-card skeleton-card" v-for="index in 4" :key="index">
					<view class="skeleton-image"></view>
					<view class="disease-copy">
						<view class="skeleton-line skeleton-title"></view>
						<view class="skeleton-line skeleton-desc"></view>
					</view>
				</view>
			</view>

			<view v-else-if="diseases.length" class="disease-grid">
				<view class="disease-card" v-for="item in diseases" :key="item.id" @tap="handleDisease(item)">
					<protected-image class="disease-image" :src="item.image || fallbackImage" mode="aspectFill"></protected-image>
					<view class="disease-copy">
						<text class="disease-name">{{ item.name }}</text>
						<view class="disease-symptom">
							<text class="symptom-label">症状</text>
							<text class="disease-desc">{{ item.symptom || '暂无症状描述' }}</text>
						</view>
					</view>
				</view>
			</view>

			<view v-else class="empty-state">
				<text class="iconfont icon-icon_33 empty-icon"></text>
				<text class="empty-title">{{ isSearchActive ? '未找到相关病虫害' : '暂无病虫害信息' }}</text>
				<text class="empty-hint">{{ isSearchActive ? '可点击取消查看全部信息' : '请稍后再试' }}</text>
			</view>
		</scroll-view>

		<!-- 上报弹框复用项目现有白色圆角弹层风格，不改变列表和顶部导航的视觉结构。 -->
		<view v-if="uploadDialogVisible" class="disease-upload-mask" @tap="closeUploadDialog">
			<view class="disease-upload-dialog" role="dialog" aria-modal="true" @tap.stop>
				<view class="disease-upload-header">
					<text class="disease-upload-title">上传病虫害信息</text>
					<text class="iconfont icon-quxiao disease-upload-close" aria-label="关闭" @tap="closeUploadDialog"></text>
				</view>

				<scroll-view class="disease-upload-body" scroll-y>
					<view class="disease-upload-form">
						<view class="upload-field">
							<text class="upload-field-label">图片</text>
							<view class="disease-image-picker" @tap="chooseDiseaseImage">
								<protected-image v-if="diseaseImagePath" class="disease-image-preview" :src="diseaseImagePath" mode="aspectFill"></protected-image>
								<view v-else class="disease-image-placeholder">
									<text class="iconfont icon-tianjia1 upload-image-icon"></text>
									<text class="upload-image-text">选择图片</text>
								</view>
							</view>
						</view>

						<view class="upload-field">
							<text class="upload-field-label">病虫害名称</text>
							<input class="upload-input" name="disease-name" v-model="uploadForm.name" maxlength="100" placeholder="请输入病虫害名称" />
						</view>

						<view class="upload-field">
							<text class="upload-field-label">作物类型</text>
							<!-- 官方扩展组件在当前表单内展开选项，不会生成被蒙层遮挡的底部 picker。 -->
							<uni-data-select
								class="upload-data-select"
								v-model="uploadForm.cropTypeId"
								:localdata="cropTypeOptions"
								:clear="false"
								placeholder="请选择作物类型"
								empty-tips="暂无可选作物类型"
							/>
						</view>

						<view class="upload-field-row">
							<view class="upload-field upload-field-half">
								<text class="upload-field-label">类型</text>
								<uni-data-select class="upload-data-select" v-model="uploadForm.type" :localdata="diseaseTypeOptions" :clear="false" />
							</view>
							<view class="upload-field upload-field-half">
								<text class="upload-field-label">状态</text>
								<uni-data-select class="upload-data-select" v-model="uploadForm.status" :localdata="statusOptions" :clear="false" />
							</view>
						</view>

						<view class="upload-field">
							<text class="upload-field-label">易发阶段</text>
							<input class="upload-input" name="disease-suitable-stage" v-model="uploadForm.suitableStage" maxlength="100" placeholder="如：苗期、花期、成熟期" />
						</view>

						<view class="upload-field">
							<text class="upload-field-label">症状描述</text>
							<textarea class="upload-textarea" name="disease-symptom" v-model="uploadForm.symptom" maxlength="500" placeholder="请输入症状描述"></textarea>
						</view>
					</view>
				</scroll-view>

				<view class="disease-upload-actions">
					<button class="upload-cancel" :disabled="submitting" @tap="closeUploadDialog">取消</button>
					<button class="upload-confirm" :disabled="submitting" @tap="submitDiseasePest">
						{{ submitting ? '提交中...' : '确认上传' }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getDiseaseCropTypes, getDiseasePests, uploadDiseasePest } from '@/api/diseasePestList.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			keyword: '',
			// 标记当前是否正在展示搜索结果，用于切换按钮的“搜索/取消”文字和行为。
			isSearchActive: false,
			loading: false,
			uploadDialogVisible: false,
			submitting: false,
			diseaseImagePath: '',
			cropTypeOptions: [],
			// 类型与状态是稳定枚举，保留在前端用于下拉选项展示，提交值与 smart_plant 约定一致。
			diseaseTypeOptions: [
				{ text: '病害', value: 1 },
				{ text: '虫害', value: 2 },
				{ text: '生理性病害', value: 3 }
			],
			statusOptions: [
				{ text: '启用', value: 1 },
				{ text: '禁用', value: 0 }
			],
			uploadForm: {
				name: '',
				cropTypeId: '',
				type: 1,
				suitableStage: '',
				symptom: '',
				status: 1
			},
			// 列表改由 smart_plant 数据库接口提供，页面不再维护静态业务数据。
			diseases: [],
			fallbackImage: '/static/lecttue.png'
		}
	},
	computed: {
		canManageKnowledge() {
			// 普通用户仅查阅知识和使用识别，知识库上报由农场主维护。
			return String(getUserInfo()?.roleCode || '').toLowerCase() === 'farm_owner'
		},
		resultSummary() {
			// 搜索状态强调结果数量，默认状态展示当前数据库列表总数。
			return this.isSearchActive
				? `找到 ${this.diseases.length} 条结果`
				: `共 ${this.diseases.length} 条`
		}
	},
	onLoad() {
		// 首次进入页面时加载 smart_farm 病虫害信息管理中的全部数据。
		this.loadDiseases()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		openUploadDialog() {
			// 打开弹框时加载 smart_plant 中启用的作物类型，确保选项与管理端一致。
			this.resetUploadForm()
			this.uploadDialogVisible = true
			if (!this.cropTypeOptions.length) {
				getDiseaseCropTypes().then((items) => {
					// uni-data-select 约定 text/value 字段，接口结果只在入口处转换一次。
					this.cropTypeOptions = items.map((item) => ({ text: item.name, value: item.id }))
				})
			}
		},
		closeUploadDialog() {
			if (!this.submitting) {
				this.uploadDialogVisible = false
			}
		},
		resetUploadForm() {
			// 每次打开上报弹框都使用干净表单，避免残留上一次提交内容。
			this.diseaseImagePath = ''
			this.uploadForm = {
				name: '',
				cropTypeId: '',
				type: 1,
				suitableStage: '',
				symptom: '',
				status: 1
			}
		},
		chooseDiseaseImage() {
			uni.chooseImage({
				count: 1,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (result) => {
					this.diseaseImagePath = result.tempFilePaths[0] || ''
				}
			})
		},
		validateUploadForm() {
			// 按弹框字段顺序校验，并返回用户可以直接理解的首个错误。
			if (!this.diseaseImagePath) return '请选择病虫害图片'
			if (!this.uploadForm.name.trim()) return '请输入病虫害名称'
			if (!this.uploadForm.cropTypeId) return '请选择作物类型'
			if (!this.uploadForm.suitableStage.trim()) return '请输入易发阶段'
			if (!this.uploadForm.symptom.trim()) return '请输入症状描述'
			return ''
		},
		async submitDiseasePest() {
			const errorMessage = this.validateUploadForm()
			if (errorMessage) {
				uni.showToast({ title: errorMessage, icon: 'none' })
				return
			}
			this.submitting = true
			try {
				// 图片和字段一次提交，成功后刷新当前列表以展示刚上传的数据。
				await uploadDiseasePest({
					imagePath: this.diseaseImagePath,
					...this.uploadForm,
					name: this.uploadForm.name.trim(),
					suitableStage: this.uploadForm.suitableStage.trim(),
					symptom: this.uploadForm.symptom.trim()
				})
				this.uploadDialogVisible = false
				await this.loadDiseases(this.isSearchActive ? this.keyword : '')
				uni.showToast({ title: '上传成功', icon: 'success' })
			} catch (error) {
				// 请求层已统一展示后端校验或网络错误，此处只恢复按钮状态。
			} finally {
				this.submitting = false
			}
		},
		async handleSearch() {
			if (this.isSearchActive) {
				// 点击“取消”时清空关键词、恢复按钮文字，并重新查询全部病虫害。
				this.keyword = ''
				this.isSearchActive = false
				await this.loadDiseases()
				return
			}

			const name = this.keyword.trim()
			if (!name) {
				uni.showToast({ title: '请输入病虫害名称', icon: 'none' })
				return
			}

			// 只有搜索请求成功后才切换为“取消”，避免请求失败时按钮状态与列表不一致。
			if (await this.loadDiseases(name)) {
				this.isSearchActive = true
			}
		},
		loadDiseases(name = '') {
			this.loading = true
			return getDiseasePests(name)
				.then((diseases) => {
					this.diseases = diseases
					return true
				})
				.catch(() => {
					// 请求层已统一提示错误，页面清空旧数据以免误认为搜索结果仍有效。
					this.diseases = []
					return false
				})
				.finally(() => {
					this.loading = false
				})
		},
		handleRecognition() {
			uni.navigateTo({
				// 使用稳定编码传递预选类型，避免识别类型名称调整后跳转失效。
				url: '/pages/service/strategy?recognitionTypeCode=DISEASE_PEST_RECOGNITION'
			})
		},
		handleDisease(item) {
			// 详情页只传递稳定主键，其他数据由详情接口实时读取。
			uni.navigateTo({
				url: `/pages/secondPage/disease/detail?id=${encodeURIComponent(item.id)}`
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

.disease-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.disease-hero {
	box-sizing: border-box;
	height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.disease-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.disease-title {
	position: absolute;
	left: 120rpx;
	right: 120rpx;
	text-align: center;
	font-size: 16px;
	line-height: 58rpx;
	color: #ffffff;
}

.disease-back {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
	text-align: left;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.recognition-link {
	position: relative;
	z-index: 2;
	font-size: 22px;
	line-height: 58rpx;
	color: #ffffff;
}

/* 顶部操作区仅增加上传入口，图标颜色和原扫码入口保持一致。 */
.disease-actions {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	height: 58rpx;
}

.disease-action-icon {
	width: 58rpx;
	height: 58rpx;
	text-align: center;
	font-size: 22px;
	line-height: 58rpx;
	color: #ffffff;
}

.disease-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 36rpx 46rpx;
	border-top-left-radius: 54rpx;
	border-top-right-radius: 54rpx;
	background-color: #f8fbfa;
	overflow: hidden;
}

.search-wrap {
	box-sizing: border-box;
	padding: 32rpx 0 24rpx;
}

.search-box {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	/* 搜索框适度收紧至 88rpx，兼顾页面密度和移动端触控空间。 */
	height: 40px;
	line-height: 40px;
	padding-left: 28rpx;
	border-radius: 44rpx;
	background-color: #ffffff;
	border: 1rpx solid rgba(111, 196, 186, 0.18);
	box-shadow: 0 8rpx 24rpx rgba(42, 115, 106, 0.08);
}

.search-icon {
	flex-shrink: 0;
	margin-right: 16rpx;
	font-size: 24rpx;
	line-height: 28rpx;
	color: #C8C8C8;
}

.search-input {
	flex: 1;
	min-width: 0;
	height: 40px;
	padding: 0;
	background-color: transparent;
	/* 14px 与页面正文比例一致，让输入内容和占位文字更轻巧。 */
	font-size: 12px;
	line-height: 40px;
	color: #000000;
}

.search-button {
	flex-shrink: 0;
	width: 70px;
	height: 40px;
	margin: 0 0 0 18rpx;
	padding: 0;
	border-radius: 40rpx;
	background-color: #6FC4BA;
	font-size: 12px;
	line-height: 40px;
	color: #ffffff;
}

.search-button::after {
	border: none;
}

.search-button[disabled] {
	opacity: 0.6;
}

.result-summary {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 22rpx;
}

.result-title {
	font-size: 16px;
	font-weight: 600;
	line-height: 44rpx;
	color: #263633;
}

.result-count {
	font-size: 12px;
	line-height: 36rpx;
	color: #7b8b88;
}

.disease-grid {
	display: grid;
	grid-template-columns: repeat(2, minmax(0, 1fr));
	column-gap: 24rpx;
	row-gap: 26rpx;
}

.disease-card {
	overflow: hidden;
	border: 1rpx solid rgba(111, 196, 186, 0.12);
	border-radius: 20rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(42, 115, 106, 0.07);
}

.disease-image {
	display: block;
	width: 100%;
	height: 188rpx;
	background-color: #f7f7f7;
}

.disease-copy {
	box-sizing: border-box;
	padding: 20rpx;
}

.disease-name {
	display: block;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	font-size: 15px;
	font-weight: 600;
	line-height: 1.35;
	color: #263633;
}

.disease-symptom {
	display: flex;
	align-items: flex-start;
	margin-top: 14rpx;
}

.symptom-label {
	flex-shrink: 0;
	margin-right: 10rpx;
	padding: 2rpx 8rpx;
	border-radius: 8rpx;
	background-color: rgba(111, 196, 186, 0.14);
	font-size: 10px;
	line-height: 28rpx;
	color: #338f84;
}

.disease-desc {
	display: -webkit-box;
	flex: 1;
	min-width: 0;
	overflow: hidden;
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
	font-size: 12px;
	line-height: 1.5;
	color: #70807d;
}

.skeleton-card {
	box-shadow: none;
}

.skeleton-image,
.skeleton-line {
	background-color: #eaf1ef;
}

.skeleton-image {
	height: 188rpx;
}

.skeleton-line {
	height: 24rpx;
	border-radius: 12rpx;
}

.skeleton-title {
	width: 68%;
}

.skeleton-desc {
	width: 92%;
	margin-top: 20rpx;
}

.empty-state {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-direction: column;
	min-height: 420rpx;
	color: #8b9996;
}

.empty-icon {
	font-size: 72rpx;
	line-height: 88rpx;
	color: #a9cbc7;
}

.empty-title {
	margin-top: 18rpx;
	font-size: 15px;
	font-weight: 500;
	line-height: 42rpx;
	color: #4d605c;
}

.empty-hint {
	margin-top: 6rpx;
	font-size: 12px;
	line-height: 36rpx;
	color: #93a19e;
}

/* 上报弹框沿用图片管理页面的蒙层、白色圆角面板和青绿色主按钮风格。 */
.disease-upload-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 1200;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 36rpx;
	padding-top: calc(36rpx + env(safe-area-inset-top));
	padding-bottom: calc(36rpx + env(safe-area-inset-bottom));
	background-color: rgba(20, 34, 31, 0.48);
}

.disease-upload-dialog {
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	width: 100%;
	max-width: 640rpx;
	height: 100%;
	max-height: 100%;
	padding: 36rpx;
	overflow: hidden;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 20rpx 56rpx rgba(0, 0, 0, 0.16);
}

.disease-upload-header,
.disease-upload-actions,
.upload-field-row {
	display: flex;
	align-items: center;
}

.disease-upload-header {
	flex-shrink: 0;
	justify-content: space-between;
}

.disease-upload-title {
	font-size: 18px;
	font-weight: 600;
	color: #1f2b28;
}

.disease-upload-close {
	width: 64rpx;
	height: 64rpx;
	margin: -14rpx -14rpx -14rpx 0;
	text-align: center;
	font-size: 32rpx;
	line-height: 64rpx;
	color: #6a7673;
}

.disease-upload-body {
	flex: 1 1 0;
	height: 0;
	min-height: 0;
	margin-top: 12rpx;
}

.disease-upload-form {
	padding-bottom: 24rpx;
}

.upload-field {
	margin-top: 24rpx;
}

.upload-field-row {
	align-items: flex-start;
	gap: 20rpx;
}

.upload-field-half {
	flex: 1;
	min-width: 0;
}

.upload-field-label {
	display: block;
	margin-bottom: 12rpx;
	font-size: 14px;
	font-weight: 500;
	color: #26332f;
}

.disease-image-picker {
	width: 150rpx;
	height: 150rpx;
	overflow: hidden;
	border: 2rpx dashed #8ecfc7;
	border-radius: 12rpx;
	background-color: #f3fbf9;
}

.disease-image-preview {
	display: block;
	width: 100%;
	height: 100%;
}

.disease-image-placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	flex-direction: column;
	width: 100%;
	height: 100%;
}

.upload-image-icon {
	font-size: 42rpx;
	color: #45ab9f;
}

.upload-image-text {
	margin-top: 8rpx;
	font-size: 12px;
	color: #277e73;
}

.upload-input,
.upload-textarea {
	box-sizing: border-box;
	width: 100%;
	border: 1rpx solid #dfe8e6;
	border-radius: 10rpx;
	background-color: #ffffff;
	font-size: 14px;
	color: #26332f;
}

.upload-input {
	height: 76rpx;
	padding: 0 20rpx;
	line-height: 76rpx;
}

/* 仅校准 uni-data-select 的外层尺寸，选项展开、遮罩和状态交由官方组件维护。 */
.upload-data-select {
	width: 100%;
}

.upload-data-select .uni-select {
	min-height: 76rpx;
	border-color: #dfe8e6;
	border-radius: 10rpx;
}

.upload-textarea {
	height: 150rpx;
	padding: 16rpx 20rpx;
	line-height: 1.5;
}

.disease-upload-actions {
	flex-shrink: 0;
	gap: 20rpx;
	padding-top: 28rpx;
	background-color: #ffffff;
}

.upload-cancel,
.upload-confirm {
	flex: 1;
	height: 88rpx;
	margin: 0;
	padding: 0;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 88rpx;
}

.upload-cancel {
	border: 1px solid #9bb0aa;
	background-color: #ffffff;
	color: #52645f;
}

.upload-confirm {
	border: 1px solid #5ab8ad;
	background-color: #5ab8ad;
	color: #ffffff;
}

.upload-cancel::after,
.upload-confirm::after {
	border: none;
}

.upload-cancel[disabled],
.upload-confirm[disabled] {
	opacity: 0.55;
}

@media screen and (min-width: 768px) {
	.disease-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
