<template>
	<view class="picture-page">
		<view class="page-hero">
			<view class="navbar">
				<text v-if="selectionMode" class="iconfont icon-quxiao nav-icon select-close" @tap="exitSelectionMode"></text>
				<text v-else class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">{{ selectionMode ? `已选择${selectedCount}项` : '摄像头图片' }}</text>
				<text v-if="selectionMode" class="iconfont icon-quanxuan nav-placeholder select-all-icon" @tap="handleSelectAll"></text>
				<view v-else class="nav-placeholder"></view>
			</view>
		</view>

		<scroll-view class="picture-content" :class="{ 'picture-content-select': selectionMode }" scroll-y>
			<view v-if="loading" class="state-text">正在加载图片...</view>
			<view v-else-if="!groupedPictures.length" class="state-text">暂无摄像头图片</view>
			<block v-else>
				<view class="date-section" v-for="group in groupedPictures" :key="group.key">
					<text class="date-title">{{ group.title }}</text>
					<view class="image-grid">
						<view
							class="image-cell"
							v-for="picture in group.items"
							:key="picture.key"
							@tap="handlePictureTap(picture)"
							@longpress="handlePictureLongPress(picture)"
						>
							<protected-image class="picture-image" :src="picture.imageUrl" mode="aspectFill"></protected-image>
							<view
								class="picture-select-hit"
								:class="{ 'picture-select-hit-active': selectionMode, checked: isPictureSelected(picture) }"
								@tap.stop="togglePictureSelection(picture)"
							></view>
						</view>
					</view>
				</view>
			</block>
		</scroll-view>

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
	</view>
</template>

<script>
import { deletePictures, downloadPicturesToAlbum, getCameraPictureList } from '@/api/pictureList.js'

export default {
	data() {
		return {
			loading: false,
			pictures: [],
			// selectionMode 表示页面处于多选状态，用户长按图片后开启。
			selectionMode: false,
			// selectedPictureKeys 保存图片唯一 key，避免选择状态依赖数组下标导致刷新后错位。
			selectedPictureKeys: []
		}
	},
	computed: {
		selectedCount() {
			return this.selectedPictureKeys.length
		},
		isAllSelected() {
			return this.pictures.length > 0 && this.selectedPictureKeys.length === this.pictures.length
		},
		selectedPictures() {
			return this.pictures.filter((picture) => this.selectedPictureKeys.indexOf(picture.key) > -1)
		},
		groupedPictures() {
			const groups = []
			const groupIndex = {}

			this.pictures.forEach((picture) => {
				const group = this.buildDateGroup(picture.imageTime)
				if (groupIndex[group.key] === undefined) {
					groupIndex[group.key] = groups.length
					groups.push({
						key: group.key,
						title: group.title,
						items: []
					})
				}
				groups[groupIndex[group.key]].items.push(picture)
			})

			return groups
		}
	},
	onLoad() {
		this.fetchPictures()
	},
	methods: {
		handleBack() {
			uni.navigateBack()
		},
		fetchPictures() {
			this.loading = true
			// 当前页面只展示摄像头采集图片，后端根据 token 限定当前用户的数据范围。
			return getCameraPictureList()
				.then((pictures) => {
					this.pictures = pictures
				})
				.finally(() => {
					this.loading = false
				})
		},
		handlePictureTap(picture) {
			if (this.selectionMode) {
				this.togglePictureSelection(picture)
				return
			}
			const currentIndex = this.pictures.findIndex((item) => item.key === picture.key)
			// 普通模式点击图片使用系统预览能力查看大图。
			uni.previewImage({
				current: currentIndex > -1 ? this.pictures[currentIndex].imageUrl : picture.imageUrl,
				urls: this.pictures.map((item) => item.imageUrl)
			})
		},
		handlePictureLongPress(picture) {
			this.selectionMode = true
			this.togglePictureSelection(picture, true)
		},
		togglePictureSelection(picture, forceSelected = false) {
			if (!picture || !picture.key) {
				return
			}
			const exists = this.selectedPictureKeys.indexOf(picture.key) > -1
			if (exists && !forceSelected) {
				this.selectedPictureKeys = this.selectedPictureKeys.filter((key) => key !== picture.key)
			} else if (!exists) {
				this.selectedPictureKeys = [...this.selectedPictureKeys, picture.key]
			}
			if (this.selectedPictureKeys.length === 0) {
				this.selectionMode = false
			}
		},
		isPictureSelected(picture) {
			return !!picture && this.selectedPictureKeys.indexOf(picture.key) > -1
		},
		handleSelectAll() {
			// 全选只作用于当前摄像头图片页面的全部图片。
			this.selectedPictureKeys = this.isAllSelected ? [] : this.pictures.map((picture) => picture.key)
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
					if (result.confirm) {
						this.deleteSelectedPictures()
					}
				}
			})
		},
		async deleteSelectedPictures() {
			try {
				// 删除接口会在后端校验图片归属，防止越权删除。
				await deletePictures(this.selectedPictures)
				this.exitSelectionMode()
				await this.fetchPictures()
				uni.showToast({
					title: '删除成功',
					icon: 'success'
				})
			} catch (error) {
				// 请求层统一提示失败，不再调用存在兼容问题的平台级 Loading。
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
				// 下载能力统一收敛到 api/pictureList.js，页面只传递当前选中的摄像头图片引用。
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

			const now = new Date()
			const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
			const current = new Date(date.getFullYear(), date.getMonth(), date.getDate())
			const diffDays = Math.round((today.getTime() - current.getTime()) / 86400000)
			const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`

			if (diffDays === 0) {
				return { key, title: '今天' }
			}
			if (diffDays === 1) {
				return { key, title: '昨天' }
			}
			return {
				key,
				title: `${date.getMonth() + 1}月${date.getDate()}日`
			}
		},
		parseDate(value) {
			if (!value) {
				return null
			}
			// 兼容后端 LocalDateTime 的 ISO 字符串以及部分端上对横杠日期的解析差异。
			const normalizedValue = String(value).replace('T', ' ').replace(/-/g, '/')
			const date = new Date(normalizedValue)
			return Number.isNaN(date.getTime()) ? null : date
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
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.page-hero {
	box-sizing: border-box;
	min-height: 300rpx;
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0;
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.navbar {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 58rpx;
}

.nav-title {
	position: absolute;
	left: 90rpx;
	right: 90rpx;
	text-align: center;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.nav-icon,
.nav-placeholder {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 58rpx;
}

.nav-icon {
	text-align: left;
	font-size: 18px;
	line-height: 58rpx;
	color: #ffffff;
}

.select-close {
	font-size: 32rpx;
}

.select-all-icon {
	text-align: right;
	font-size: 34rpx;
	line-height: 58rpx;
	color: #ffffff;
}

.picture-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 136rpx);
	bottom: 0;
	box-sizing: border-box;
	padding-bottom: 40rpx;
}

.picture-content-select {
	padding-bottom: 142rpx;
}

.date-section {
	margin-bottom: 24rpx;
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

@media screen and (min-width: 768px) {
	.picture-page {
		width: 750rpx;
		margin: 0 auto;
	}

	.selection-toolbar {
		left: 50%;
		right: auto;
		width: 750rpx;
		transform: translateX(-50%);
	}
}
</style>
