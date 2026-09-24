<template>
	<view class="picture-page">
		<view class="picture-hero">
			<view class="picture-nav" :class="{ 'picture-nav-select': selectionMode }">
				<text v-if="selectionMode" class="iconfont icon-quxiao select-close" @tap="exitSelectionMode"></text>
				<text v-else class="iconfont icon-fanhui picture-back" @tap="handleBack"></text>
				<text class="picture-title">{{ selectionMode ? `已选择${selectedCount}项` : '图片管理' }}</text>
				<text v-if="selectionMode" class="iconfont icon-quanxuan select-all-icon" @tap="handleSelectAll"></text>
				<view v-else class="picture-actions">
					<text class="iconfont icon-icon_33 nav-action-icon" aria-label="搜索图片" @tap="handleSearch"></text>
					<text class="iconfont icon-tianjia1 nav-action-icon add-icon" aria-label="上传手机图片" @tap="openUploadDialog"></text>
				</view>
			</view>
		</view>

		<scroll-view class="picture-content" scroll-y>
			<view v-if="activeMode === 'photo'" class="photo-view">
				<text class="main-title">照片</text>
				<view v-if="loading" class="state-text">正在加载图片...</view>
				<view v-else-if="!groupedPictures.length" class="state-text">暂无图片</view>
				<block v-else>
					<view class="date-section" v-for="group in groupedPictures" :key="group.key">
						<text class="date-title">{{ group.title }}</text>
						<view class="image-grid">
							<view
								class="image-cell"
								v-for="photo in group.items"
								:key="photo.key"
								@tap="handlePhotoTap(photo)"
								@longpress="handlePhotoLongPress(photo)"
							>
								<protected-image class="picture-image" :src="photo.imageUrl" mode="aspectFill"></protected-image>
								<view
									class="picture-select-hit"
									:class="{ 'picture-select-hit-active': selectionMode, checked: isPhotoSelected(photo) }"
									@tap.stop="togglePhotoSelection(photo)"
								></view>
							</view>
						</view>
					</view>
				</block>
			</view>

			<view v-else class="album-view">
				<view class="album-card" v-for="album in albums" :key="album.id" @tap="handleAlbumTap(album)">
					<protected-image class="album-cover" :src="album.cover" mode="aspectFill"></protected-image>
					<view class="album-mask">
						<text>{{ album.name }}</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<view v-if="!selectionMode" class="floating-switch">
			<view class="switch-item" :class="{ active: activeMode === 'photo' }" @tap="switchMode('photo')">
				<text class="iconfont icon-morentupian-80pt switch-icon"></text>
			</view>
			<view class="switch-item" :class="{ active: activeMode === 'album' }" @tap="switchMode('album')">
				<text class="iconfont icon-jurassic_label switch-icon"></text>
			</view>
		</view>

		<view v-if="selectionMode" class="selection-toolbar">
			<button class="selection-action" @tap="handleDownloadSelected">
				<text class="iconfont icon-xiazai selection-action-icon"></text>
				<text class="selection-action-text">下载</text>
			</button>
			<button class="selection-action selection-delete" @tap="handleDeleteSelected">
				<text class="iconfont icon-shanchu selection-action-icon"></text>
				<text class="selection-action-text">删除</text>
			</button>
		</view>

		<view v-if="uploadDialogVisible" class="upload-mask" @tap="closeUploadDialog">
			<view class="upload-dialog" @tap.stop>
				<view class="upload-dialog-header">
					<text class="upload-dialog-title">上传手机图片</text>
					<text class="iconfont icon-quxiao upload-dialog-close" aria-label="关闭" @tap="closeUploadDialog"></text>
				</view>

				<view class="upload-user-row">
					<text class="upload-field-label">上传用户</text>
					<text class="upload-user-name">{{ uploadUserName }}</text>
				</view>

				<text class="upload-field-label preview-label">图片预览</text>
				<view class="upload-preview-grid">
					<view class="upload-preview-item" v-for="(imagePath, index) in uploadImagePaths" :key="imagePath">
						<protected-image class="upload-preview-image" :src="imagePath" mode="aspectFill" @tap="previewUploadImage(imagePath)"></protected-image>
						<text
							v-if="!uploading"
							class="upload-remove"
							aria-label="移除图片"
							@tap.stop="removeUploadImage(index)"
						>×</text>
					</view>
					<view
						v-if="uploadImagePaths.length < 9 && !uploading"
						class="upload-add-tile"
						hover-class="upload-add-tile-pressed"
						@tap="chooseUploadImages"
					>
						<text class="iconfont icon-tianjia1 upload-add-icon"></text>
						<text class="upload-add-text">选择图片</text>
					</view>
				</view>
				<text class="upload-hint">支持 JPG、PNG、WEBP，单张最大 5MB，最多选择 9 张</text>

				<view class="upload-dialog-actions">
					<button class="upload-cancel" :disabled="uploading" @tap="closeUploadDialog">取消</button>
					<button class="upload-confirm" :disabled="uploading" @tap="confirmUpload">
						{{ uploadButtonText }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { deletePictures, downloadPicturesToAlbum, getPictureList, uploadPhonePicture } from '@/api/pictureList.js'
import { getUserInfo } from '@/utils/auth.js'

export default {
	data() {
		return {
			activeMode: 'photo',
			loading: false,
			photos: [],
			albums: [],
			uploadDialogVisible: false,
			uploadImagePaths: [],
			uploadUserName: '当前登录用户',
			uploading: false,
			uploadedCount: 0,
			uploadTotal: 0,
			// selectionMode 表示图片网格进入多选状态，长按任意图片后开启。
			selectionMode: false,
			// selectedPictureKeys 保存 sourceType-id，避免不同来源图片 id 相同时相互覆盖。
			selectedPictureKeys: []
		}
	},
	onLoad() {
		this.fetchPictureList()
	},
	computed: {
		selectedCount() {
			return this.selectedPictureKeys.length
		},
		isAllSelected() {
			return this.photos.length > 0 && this.selectedPictureKeys.length === this.photos.length
		},
		selectedPictures() {
			return this.photos.filter((photo) => this.selectedPictureKeys.indexOf(photo.key) > -1)
		},
		groupedPictures() {
			const groups = []
			const groupIndex = {}

			this.photos.forEach((photo) => {
				const group = this.buildDateGroup(photo.imageTime)
				if (groupIndex[group.key] === undefined) {
					groupIndex[group.key] = groups.length
					groups.push({
						key: group.key,
						title: group.title,
						items: []
					})
				}
				groups[groupIndex[group.key]].items.push(photo)
			})

			return groups
		},
		uploadButtonText() {
			return this.uploading
				? `上传中 ${this.uploadedCount}/${this.uploadTotal}`
				: '确认上传'
		}
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		fetchPictureList() {
			this.loading = true
			// 页面 UI 继续沿用原来的 photos/albums 数据结构，接口适配逻辑统一放在 api/pictureList.js 中维护。
			return getPictureList()
				.then((data) => {
					this.photos = data.pictures
					this.albums = data.albums
				})
				.catch(() => {
					uni.showToast({
						title: '图片加载失败',
						icon: 'none'
					})
				})
				.finally(() => {
					this.loading = false
				})
		},
		handleSearch() {
			uni.showToast({
				title: '搜索图片',
				icon: 'none'
			})
		},
		openUploadDialog() {
			const user = getUserInfo() || {}
			this.uploadUserName = user.nickname || user.username || '当前登录用户'
			this.uploadImagePaths = []
			this.uploadedCount = 0
			this.uploadTotal = 0
			this.uploadDialogVisible = true
		},
		closeUploadDialog() {
			if (this.uploading) {
				return
			}
			this.resetUploadDialog()
		},
		resetUploadDialog() {
			this.uploadDialogVisible = false
			this.uploadImagePaths = []
			this.uploadedCount = 0
			this.uploadTotal = 0
		},
		chooseUploadImages() {
			const remainingCount = 9 - this.uploadImagePaths.length
			if (remainingCount <= 0) {
				uni.showToast({ title: '最多选择9张图片', icon: 'none' })
				return
			}
			uni.chooseImage({
				count: remainingCount,
				sizeType: ['compressed'],
				sourceType: ['album', 'camera'],
				success: (result) => {
					const selectedPaths = Array.isArray(result.tempFilePaths) ? result.tempFilePaths : []
					this.uploadImagePaths = [...new Set([...this.uploadImagePaths, ...selectedPaths])].slice(0, 9)
				}
			})
		},
		previewUploadImage(imagePath) {
			uni.previewImage({
				current: imagePath,
				urls: this.uploadImagePaths
			})
		},
		removeUploadImage(index) {
			this.uploadImagePaths = this.uploadImagePaths.filter((item, itemIndex) => itemIndex !== index)
		},
		async confirmUpload() {
			if (this.uploading) {
				return
			}
			if (!this.uploadImagePaths.length) {
				uni.showToast({ title: '请先选择图片', icon: 'none' })
				return
			}

			this.uploading = true
			this.uploadedCount = 0
			this.uploadTotal = this.uploadImagePaths.length
			const pendingPaths = [...this.uploadImagePaths]
			try {
				for (const imagePath of pendingPaths) {
					await uploadPhonePicture(imagePath)
					this.uploadedCount += 1
					this.uploadImagePaths = this.uploadImagePaths.filter((item) => item !== imagePath)
				}
				await this.fetchPictureList()
				this.uploading = false
				this.resetUploadDialog()
				uni.showToast({ title: `成功上传${pendingPaths.length}张图片`, icon: 'success' })
			} catch (error) {
				await this.fetchPictureList()
				if (this.uploadedCount > 0) {
					uni.showToast({ title: `已上传${this.uploadedCount}张，剩余图片可重试`, icon: 'none' })
				}
			} finally {
				this.uploading = false
			}
		},
		handlePhotoTap(photo) {
			if (this.selectionMode) {
				this.togglePhotoSelection(photo)
				return
			}
			const currentIndex = this.photos.findIndex((item) => item.key === photo.key)
			// 非选择模式下点击图片直接调用系统预览能力，实现图片放大查看。
			uni.previewImage({
				current: currentIndex > -1 ? this.photos[currentIndex].imageUrl : photo.imageUrl,
				urls: this.photos.map((item) => item.imageUrl)
			})
		},
		handlePhotoLongPress(photo) {
			this.selectionMode = true
			this.togglePhotoSelection(photo, true)
		},
		togglePhotoSelection(photo, forceSelected = false) {
			if (!photo || !photo.key) {
				return
			}
			const exists = this.selectedPictureKeys.indexOf(photo.key) > -1
			if (exists && !forceSelected) {
				this.selectedPictureKeys = this.selectedPictureKeys.filter((key) => key !== photo.key)
			} else if (!exists) {
				this.selectedPictureKeys = [...this.selectedPictureKeys, photo.key]
			}
			if (this.selectedPictureKeys.length === 0) {
				this.selectionMode = false
			}
		},
		isPhotoSelected(photo) {
			return !!photo && this.selectedPictureKeys.indexOf(photo.key) > -1
		},
		handleSelectAll() {
			// 顶部“全选”只作用于当前照片网格，不影响相册模式。
			this.selectedPictureKeys = this.isAllSelected ? [] : this.photos.map((photo) => photo.key)
			if (this.selectedPictureKeys.length === 0) {
				this.selectionMode = false
			}
		},
		exitSelectionMode() {
			this.selectionMode = false
			this.selectedPictureKeys = []
		},
		handleDeleteSelected() {
			if (!this.selectedPictures.length) {
				uni.showToast({
					title: '请选择图片',
					icon: 'none'
				})
				return
			}
			uni.showModal({
				title: '删除图片',
				content: `确定删除已选择的${this.selectedPictures.length}张图片吗？`,
				success: (result) => {
					if (!result.confirm) {
						return
					}
					this.deleteSelectedPictures()
				}
			})
		},
		async deleteSelectedPictures() {
			try {
				// 删除接口由后端再次校验图片归属，前端只负责传递用户选中的图片引用。
				await deletePictures(this.selectedPictures)
				this.exitSelectionMode()
				await this.fetchPictureList()
				uni.showToast({
					title: '删除成功',
					icon: 'success'
				})
			} catch (error) {
				// 请求层统一提示失败，避免平台级 Loading 动画触发运行端计时器异常。
			}
		},
		async handleDownloadSelected() {
			if (!this.selectedPictures.length) {
				uni.showToast({
					title: '请选择图片',
					icon: 'none'
				})
				return
			}
			try {
				// 下载能力统一收敛到 api/pictureList.js，页面只传递当前选中的图片引用。
				await downloadPicturesToAlbum(this.selectedPictures)
				uni.showToast({
					title: '下载完成',
					icon: 'success'
				})
			} catch (error) {
				uni.showToast({
					title: '下载失败',
					icon: 'none'
				})
			}
		},
		buildDateGroup(value) {
			const date = this.parseDate(value)
			if (!date) {
				return {
					key: 'unknown',
					title: '未知时间'
				}
			}

			return {
				key: `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`,
				title: `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
			}
		},
		parseDate(value) {
			if (!value) {
				return null
			}
			const normalizedValue = String(value).replace('T', ' ').replace(/-/g, '/')
			const date = new Date(normalizedValue)
			return Number.isNaN(date.getTime()) ? null : date
		},
		switchMode(mode) {
			this.activeMode = mode
			if (mode !== 'photo') {
				this.exitSelectionMode()
			}
		},
		handleAlbumTap(album) {
			// 相册入口只负责路由分发，具体列表数据由 phone.vue / camera.vue 分别调用专用接口。
			const url = album.id === 1
				? '/pages/secondPage/picture_type/phone'
				: '/pages/secondPage/picture_type/camera'
			uni.navigateTo({
				url
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

.picture-page {
	position: relative;
	min-height: 100vh;
	background-color: #f7f7f7;
	color: #000000;
	font-size: 14px;
	font-weight: normal;
}

.picture-hero {
	box-sizing: border-box;
	min-height: 228rpx;
	padding: calc(var(--status-bar-height) + 14rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.picture-nav {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.picture-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.picture-back {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
	text-align: left;
}

.picture-actions {
	position: relative;
	z-index: 2;
	display: flex;
	align-items: center;
	height: 68rpx;
}

.nav-action-icon {
	display: block;
	width: 68rpx;
	height: 68rpx;
	text-align: center;
	font-size: 34rpx;
	line-height: 68rpx;
	color: #ffffff;
}

.add-icon {
	font-size: 36rpx;
}

.select-close {
	position: relative;
	z-index: 2;
	width: 60rpx;
	height: 58rpx;
	text-align: left;
	font-size: 36rpx;
	line-height: 58rpx;
	color: #ffffff;
}

.select-all-icon {
	position: relative;
	z-index: 2;
	width: 60rpx;
	height: 58rpx;
	text-align: right;
	font-size: 40rpx;
	line-height: 58rpx;
	color: #ffffff;
}

.picture-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 108rpx);
	bottom: 0;
	box-sizing: border-box;
	padding: 0 0 160rpx;
}

.photo-view {
	box-sizing: border-box;
	padding-top: 26rpx;
}

.main-title {
	display: block;
	margin-left: 62rpx;
	font-size: 28px;
	line-height: 1.25;
	color: #000000;
}

.date-section {
	margin-bottom: 24rpx;
}

.date-section:first-of-type {
	margin-top: 24rpx;
}

.date-title {
	display: block;
	padding: 0 36rpx 20rpx;
	font-size: 16px;
	line-height: 1.3;
	color: #000000;
}

.image-grid {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 4rpx;
}

.image-cell {
	position: relative;
	box-sizing: border-box;
	width: 100%;
	overflow: hidden;
}

.picture-image {
	display: block;
	width: 100%;
	height: 183rpx;
	background-color: #ffffff;
}

.picture-select-hit {
	display: none;
	position: absolute;
	right: 14rpx;
	bottom: 14rpx;
	z-index: 3;
	box-sizing: border-box;
	width: 38rpx;
	height: 38rpx;
	border-radius: 50%;
	background-color: #efefef;
}

.picture-select-hit-active {
	display: block;
}

.picture-select-hit.checked {
	position: absolute;
	background-color: #1ba291;
}

.picture-select-hit.checked::after {
	position: absolute;
	left: 12rpx;
	top: 6rpx;
	width: 10rpx;
	height: 18rpx;
	border-right: 4rpx solid #ffffff;
	border-bottom: 4rpx solid #ffffff;
	content: "";
	transform: rotate(45deg);
}

.state-text {
	box-sizing: border-box;
	margin-top: 90rpx;
	padding: 40rpx 20rpx;
	text-align: center;
	font-size: 14px;
	line-height: 1.4;
	color: #C8C8C8;
}

.album-view {
	box-sizing: border-box;
	padding: 18rpx 52rpx 0;
}

.album-card {
	position: relative;
	height: 300rpx;
	margin-bottom: 44rpx;
	overflow: hidden;
	border-radius: 12rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.05);
}

.album-cover {
	display: block;
	width: 100%;
	height: 300rpx;
}

.album-mask {
	position: absolute;
	left: 0;
	right: 0;
	bottom: 0;
	box-sizing: border-box;
	height: 70rpx;
	padding-left: 36rpx;
	display: flex;
	align-items: center;
	background-color: rgba(0, 0, 0, 0.56);
	font-size: 16px;
	line-height: 1.25;
	color: #ffffff;
}

.floating-switch {
	position: fixed;
	left: 50%;
	bottom: 124rpx;
	z-index: 10;
	display: flex;
	align-items: center;
	width: 260rpx;
	height: 40px;
	padding: 0 18rpx;
	border-radius: 48rpx;
	background-color: #ffffff;
	box-shadow: 0 12rpx 30rpx rgba(0, 0, 0, 0.08);
	transform: translateX(-50%);
}

.switch-item {
	display: flex;
	flex: 1;
	align-items: center;
	justify-content: center;
	height: 24px;
	color: #C8C8C8;
}

.switch-icon {
	font-size: 20px;
	line-height: 20px;
	color: #C8C8C8;
}

.switch-item.active,
.switch-item.active .switch-icon {
	color: #1BA291;
}

.selection-toolbar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 20;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	height: calc(112rpx + env(safe-area-inset-bottom));
	padding: 10rpx 60rpx calc(10rpx + env(safe-area-inset-bottom));
	background-color: #ffffff;
	box-shadow: 0 -8rpx 22rpx rgba(0, 0, 0, 0.06);
}

.selection-action {
	box-sizing: border-box;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	width: 180rpx;
	height: 82rpx;
	margin: 0 42rpx;
	padding: 0;
	border-radius: 0;
	background-color: transparent;
	text-align: center;
	color: #000000;
}

.selection-action::after {
	border: none;
}

.selection-delete {
	color: #ff4d4f;
}

.selection-action-icon {
	display: block;
	font-size: 34rpx;
	line-height: 36rpx;
	color: #000000;
}

.selection-delete .selection-action-icon,
.selection-delete .selection-action-text {
	color: #ff4d4f;
}

.selection-action-text {
	display: block;
	margin-top: 6rpx;
	font-size: 12px;
	line-height: 1.2;
	color: #000000;
}

.upload-mask {
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
	background-color: rgba(20, 34, 31, 0.48);
}

.upload-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 640rpx;
	max-height: calc(100vh - 72rpx);
	padding: 36rpx;
	overflow-y: auto;
	border-radius: 24rpx;
	background-color: #ffffff;
	box-shadow: 0 20rpx 56rpx rgba(0, 0, 0, 0.16);
}

.upload-dialog-header,
.upload-user-row,
.upload-dialog-actions {
	display: flex;
	align-items: center;
}

.upload-dialog-header,
.upload-user-row {
	justify-content: space-between;
}

.upload-dialog-title {
	font-size: 18px;
	font-weight: 600;
	color: #1f2b28;
}

.upload-dialog-close {
	width: 64rpx;
	height: 64rpx;
	margin: -14rpx -14rpx -14rpx 0;
	text-align: center;
	font-size: 32rpx;
	line-height: 64rpx;
	color: #6a7673;
}

.upload-user-row {
	min-height: 80rpx;
	margin-top: 24rpx;
	padding: 0 20rpx;
	border-radius: 12rpx;
	background-color: #f3f7f6;
}

.upload-field-label {
	font-size: 14px;
	font-weight: 500;
	color: #26332f;
}

.upload-user-name {
	font-size: 14px;
	color: #65736f;
}

.preview-label {
	display: block;
	margin-top: 28rpx;
}

.upload-preview-grid {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 12rpx;
	margin-top: 16rpx;
}

.upload-preview-item,
.upload-add-tile {
	position: relative;
	box-sizing: border-box;
	height: 150rpx;
	overflow: hidden;
	border-radius: 12rpx;
}

.upload-preview-image {
	display: block;
	width: 100%;
	height: 100%;
}

.upload-remove {
	position: absolute;
	right: 0;
	top: 0;
	width: 52rpx;
	height: 52rpx;
	border-radius: 0 12rpx 0 28rpx;
	background-color: rgba(0, 0, 0, 0.62);
	text-align: center;
	font-size: 32rpx;
	line-height: 48rpx;
	color: #ffffff;
}

.upload-add-tile {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	border: 2rpx dashed #8ecfc7;
	background-color: #f3fbf9;
}

.upload-add-tile-pressed {
	background-color: #e8f7f4;
}

.upload-add-icon {
	font-size: 42rpx;
	color: #45ab9f;
}

.upload-add-text {
	margin-top: 8rpx;
	font-size: 12px;
	color: #277e73;
}

.upload-hint {
	display: block;
	margin-top: 14rpx;
	font-size: 11px;
	line-height: 1.5;
	color: #7f918c;
}

.upload-dialog-actions {
	gap: 20rpx;
	margin-top: 28rpx;
}

.upload-cancel,
.upload-confirm {
	flex: 1;
	height: 88rpx;
	margin: 0;
	border-radius: 12rpx;
	font-size: 14px;
	line-height: 88rpx;
}

.upload-cancel {
	border: 1px solid #9bb0aa;
	color: #52645f;
	background-color: #ffffff;
}

.upload-confirm {
	border: 1px solid #5ab8ad;
	color: #ffffff;
	background-color: #5ab8ad;
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
	.picture-page {
		width: 750rpx;
		margin: 0 auto;
	}

	.floating-switch,
	.selection-toolbar {
		left: 50%;
		right: auto;
		width: 750rpx;
		transform: translateX(-50%);
	}
}
</style>
