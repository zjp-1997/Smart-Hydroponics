<template>
	<view class="plot-detail-page">
		<view class="detail-hero">
			<view class="navbar">
				<text class="iconfont icon-fanhui nav-icon" @tap="handleBack"></text>
				<text class="nav-title">地块详情</text>
				<text v-if="canManageFarm" class="iconfont icon-gengduobianji nav-icon" role="button" aria-label="编辑地块信息" @tap="handleEdit"></text>
				<view v-else class="nav-icon"></view>
			</view>

			<view class="crop-panel">
				<view class="crop-info">
					<text class="crop-name">{{ plot.name }}</text>
					<view class="crop-meta">
						<text>地块名称：{{ plot.farm }}</text>
						<text>种植面积：{{ plot.area }}</text>
					</view>
					<view class="crop-meta">
						<text>种植时间：{{ plot.rack }}</text>
						<text>预计采收时间：{{ plot.crop }}</text>
					</view>
				</view>
				<view class="crop-image-wrap" @tap="handleMonitor">
					<!-- 空闲地块不展示上一批次图片或监控入口。 -->
					<text v-if="plot.idle" class="idle-image-placeholder">-</text>
					<protected-image v-else class="crop-image" :src="plot.image" mode="aspectFill"></protected-image>
					<view v-if="!plot.idle" class="monitor-badge">
						<text class="iconfont icon-shishijiankong monitor-icon"></text>
						<text>查看监控</text>
					</view>
				</view>
			</view>
		</view>

		<scroll-view class="detail-content" scroll-y>
			<view class="env-card">
				<view class="card-head">
					<text class="env-title">环境监测</text>
						<text v-if="!plot.idle" class="env-more" @tap="handleMoreEnv">更多</text>
				</view>
				<view class="env-list">
					<view class="env-item" v-for="item in envItems" :key="item.label">
						<text class="env-value">{{ item.value }}</text>
						<text class="env-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<view class="plant-card">
				<view class="plant-head">
					<view class="plant-days">
						<text>已经种植 </text>
						<text class="plant-day-number">{{ plot.plantDays }}</text>
						<text v-if="!plot.idle"> 天</text>
					</view>
					<!-- 成熟提醒开关属于农场管理操作，普通用户的只读地块页不显示。 -->
					<view v-if="canManageFarm && !plot.idle" class="plant-switch-wrap">
						<text>成熟提醒</text>
						<view class="switch-control" :class="{ active: matureNotice }" @tap="toggleMatureNotice">
							<view class="switch-dot"></view>
						</view>
					</view>
				</view>
				<view v-if="!plot.idle" class="growth-line">
					<view
						class="growth-segment"
						v-for="stage in stages"
						:key="stage.name"
						:class="{ active: stage.active }"
					></view>
				</view>
				<view v-if="plot.idle" class="idle-growth-placeholder">-</view>
				<view v-else class="growth-labels">
					<text
						v-for="stage in stages"
						:key="stage.name"
						:class="{ active: stage.active }"
					>
						{{ stage.name }}
					</text>
				</view>
			</view>

			<view class="white-card">
				<view class="card-head">
					<text class="section-title">农事任务</text>
					<view class="task-head-actions">
						<view
							v-if="tasks.length > 4"
							class="auto-scroll-toggle"
							hover-class="card-link-pressed"
							:hover-stay-time="80"
							@tap="toggleTaskAutoScroll"
						>
							{{ taskAutoScrollEnabled ? '暂停' : '自动' }}
						</view>
						<!-- 扩大箭头触控区域，点击后进入当前地块的完整农事时间序列。 -->
						<view
							v-if="!plot.idle"
							class="card-link"
							hover-class="card-link-pressed"
							:hover-stay-time="80"
							@tap="openPlotTaskTimeline"
						>
							<text class="iconfont icon-youjiantou card-arrow"></text>
						</view>
					</view>
				</view>
				<!-- 自动滚动与手动上滑共用原生纵向滚动区，用户触摸期间暂停自动移动。 -->
				<scroll-view
					class="task-list"
					scroll-y
					scroll-with-animation
					:scroll-top="taskScrollTop"
					:show-scrollbar="false"
					@scroll="handleTaskScroll"
					@touchstart="pauseTaskAutoScroll"
					@touchend="resumeTaskAutoScroll"
					@touchcancel="resumeTaskAutoScroll"
				>
						<text v-if="!tasks.length" class="task-empty">{{ plot.idle ? '-' : '暂无农事任务' }}</text>
					<template v-else>
						<view
							class="task-row"
							v-for="item in tasks"
							:key="item.id"
							hover-class="task-row-pressed"
							:hover-stay-time="80"
							@tap="openTaskRecord(item)"
						>
							<text class="task-index">{{ item.sequence }}</text>
							<text class="task-title">{{ item.title }}</text>
							<text class="task-status">{{ item.status }}</text>
							<text class="task-deadline">{{ item.deadline }}</text>
						</view>
					</template>
				</scroll-view>
			</view>

			<view class="white-card device-card">
				<view class="card-head">
						<text class="section-title">{{ canManageFarm && !plot.idle ? '设备控制' : '设备状态' }}</text>
					<text class="iconfont icon-youjiantou card-arrow"></text>
				</view>
				<view class="device-list">
					<view class="device-row" v-for="item in devices" :key="item.name">
						<view class="device-icon-box">
							<text class="iconfont device-icon" :class="item.icon"></text>
						</view>
						<view class="device-info">
							<text class="device-name">{{ item.name }}</text>
							<view class="device-meta">
								
								<text class="device-state" :class="item.stateClass">{{ item.state }}</text>
							</view>
						</view>
						<view v-if="canManageFarm && !plot.idle"
							class="switch-control"
							:class="{ active: item.enabled }"
							@tap="toggleDevice(item)"
						>
							<view class="switch-dot"></view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<button v-if="canManageFarm && !plot.idle"
			class="harvest-button"
			:disabled="!plot.harvestable || harvestSubmitting"
			hover-class="harvest-button-pressed"
			:hover-stay-time="80"
			@tap="openHarvestDialog"
		>
			{{ harvestButtonText }}
		</button>

		<!-- 农场主编辑弹框仅接收五项基本信息，作物名称通过已有作物选择器确定。 -->
		<view v-if="canManageFarm && editDialogVisible" class="dialog-mask" @tap="closeEditDialog">
			<view class="harvest-dialog edit-dialog" @tap.stop>
				<text class="dialog-title">编辑地块信息</text>
				<scroll-view class="edit-form-scroll" scroll-y>
					<view class="form-item">
						<text class="form-label">作物名称</text>
						<picker :range="editableCrops" range-key="cropName" :value="editCropIndex" @change="handleEditCropChange">
							<view class="date-picker" :class="{ 'picker-placeholder': !editForm.cropId }">{{ selectedCropName }}</view>
						</picker>
					</view>
					<view class="form-item">
						<text class="form-label">地块名称</text>
						<input v-model.trim="editForm.plotName" class="form-input" maxlength="100" placeholder="请输入地块名称" />
					</view>
					<view class="form-item">
						<text class="form-label">种植面积（{{ plot.areaUnit }}）</text>
						<input v-model="editForm.plantingArea" class="form-input" type="digit" placeholder="请输入种植面积" />
					</view>
					<view class="form-item">
						<text class="form-label">种植时间</text>
						<picker mode="date" :value="editForm.plantedAt" @change="editForm.plantedAt = $event.detail.value">
							<view class="date-picker" :class="{ 'picker-placeholder': !editForm.plantedAt }">{{ editForm.plantedAt || '请选择种植日期' }}</view>
						</picker>
					</view>
					<view class="form-item">
						<text class="form-label">预计采收时间</text>
						<picker mode="date" :value="editForm.expectedHarvestAt" :start="editForm.plantedAt || undefined" @change="editForm.expectedHarvestAt = $event.detail.value">
							<view class="date-picker" :class="{ 'picker-placeholder': !editForm.expectedHarvestAt }">{{ editForm.expectedHarvestAt || '请选择预计采收日期' }}</view>
						</picker>
					</view>
				</scroll-view>
				<view class="dialog-actions">
					<button class="dialog-button cancel-button" :disabled="editSubmitting" @tap="closeEditDialog">取消</button>
					<button class="dialog-button confirm-button" :disabled="editSubmitting" @tap="confirmEdit">{{ editSubmitting ? '保存中...' : '保存修改' }}</button>
				</view>
			</view>
		</view>

		<view v-if="harvestDialogVisible" class="dialog-mask">
			<view class="harvest-dialog" @tap.stop>
				<text class="dialog-title">确认采收</text>
				<text class="dialog-tip">提交后将结束当前种植批次，请核对采收信息。</text>

				<view class="form-item">
					<text class="form-label">采收人</text>
					<input v-model.trim="harvestForm.harvester" class="form-input" name="harvest-person" maxlength="50" placeholder="请输入采收人" />
				</view>
				<view class="form-item">
					<text class="form-label">实际产量</text>
					<view class="yield-input-row">
						<input v-model="harvestForm.yieldAmount" class="form-input yield-input" name="harvest-yield-amount" type="digit" placeholder="请输入产量" />
						<input v-model.trim="harvestForm.yieldUnit" class="form-input unit-input" name="harvest-yield-unit" maxlength="20" placeholder="kg" />
					</view>
				</view>
				<view class="form-item">
					<text class="form-label">采收日期</text>
					<picker mode="date" :value="harvestForm.actualHarvestAt" :end="today" @change="handleHarvestDateChange">
						<view class="date-picker">{{ harvestForm.actualHarvestAt }}</view>
					</picker>
				</view>

				<view class="dialog-actions">
					<button class="dialog-button cancel-button" :disabled="harvestSubmitting" @tap="closeHarvestDialog">取消</button>
					<button class="dialog-button confirm-button" :disabled="harvestSubmitting" @tap="confirmHarvest">
						{{ harvestSubmitting ? '提交中...' : '确认采收' }}
					</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getPlotDetail, getEditableCrops, updatePlotBasicInfo, harvestPlot } from '@/api/plotList.js'
import { nextTaskScrollTop } from '@/utils/taskScroll.js'
import { getUserInfo } from '@/utils/auth.js'

const TASK_ROW_HEIGHT_RPX = 88
const TASK_LIST_HEIGHT_RPX = 352
const TASK_AUTO_SCROLL_INTERVAL = 3000

function createEmptyPlot() {
	return {
		id: null,
		idle: false,
		cropId: null,
		areaValue: null,
		areaUnit: '亩',
		expectedHarvestAt: '',
		name: '-',
		farm: '-',
		crop: '-',
		area: '-',
		rack: '-',
		plantDays: 0,
		growthStageName: '暂无阶段',
		harvestable: false,
		image: '/static/lecttue.png'
	}
}

function formatLocalDate(date) {
	const year = date.getFullYear()
	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')
	return `${year}-${month}-${day}`
}

export default {
	data() {
		const today = formatLocalDate(new Date())
		return {
			plot: createEmptyPlot(),
			matureNotice: true,
			envItems: [],
			stages: [
				{ name: '发芽期', active: false },
				{ name: '幼苗期', active: false },
				{ name: '莲座期', active: false },
				{ name: '成熟期', active: false }
			],
			// 地块详情展示任务快照，不再把执行记录误当成农事任务。
			tasks: [],
			taskScrollTop: 0,
			taskAutoScrollEnabled: true,
			taskAutoScrollTimer: null,
			devices: [],
			// 每次打开弹框从最新详情回填，取消后不修改页面展示数据。
			editDialogVisible: false,
			editSubmitting: false,
			editableCrops: [],
			editForm: { cropId: null, plotName: '', plantingArea: '', plantedAt: '', expectedHarvestAt: '' },
			today,
			harvestDialogVisible: false,
			harvestSubmitting: false,
			harvestForm: {
				harvester: '',
				yieldAmount: '',
				yieldUnit: 'kg',
				actualHarvestAt: today
			}
		}
	},
	computed: {
		editCropIndex() {
			return Math.max(0, this.editableCrops.findIndex((crop) => crop.id === this.editForm.cropId))
		},
		selectedCropName() {
			return this.editableCrops.find((crop) => crop.id === this.editForm.cropId)?.cropName || '请选择作物'
		},
		canManageFarm() {
			// 普通用户仅查看地块和设备状态；编辑、控制及采收保留给农场主。
			return String(getUserInfo()?.roleCode || '').toLowerCase() === 'farm_owner'
		},
		harvestButtonText() {
			if (this.harvestSubmitting) {
				return '正在采收...'
			}
			if (this.plot.harvestable) {
				return '确认采收'
			}
			if (this.plot.growthStageName && this.plot.growthStageName !== '暂无阶段') {
				const harvestDate = /^\d{4}-\d{2}-\d{2}$/.test(this.plot.crop)
					? new Date(`${this.plot.crop}T00:00:00`)
					: null
				const currentDate = new Date(`${this.today}T00:00:00`)
				const remainingDays = harvestDate
					? Math.ceil((harvestDate.getTime() - currentDate.getTime()) / 86400000)
					: 0
				if (remainingDays > 0) {
					return `预计 ${remainingDays} 天后可采收`
				}
				return '未到成熟期，暂不可采收'
			}
			return '暂无可采收批次'
		}
	},
	onLoad(options) {
		const plotId = options && options.id ? options.id : null
		this.fetchPlotDetail(plotId)
	},
	onShow() {
		this.startTaskAutoScroll()
	},
	onHide() {
		this.stopTaskAutoScroll()
	},
	onUnload() {
		this.stopTaskAutoScroll()
	},
	methods: {
		fetchPlotDetail(plotId) {
			if (!plotId) {
				uni.showToast({
					title: '地块ID不能为空',
					icon: 'none'
				})
				return
			}
			// 详情页只负责接收已归一化的数据，接口路径和字段转换统一维护在 api/plotList.js。
			return getPlotDetail(plotId).then((detail) => {
				this.plot = detail.plot
				this.envItems = detail.envItems
				this.stages = detail.stages
				this.tasks = detail.tasks
				this.taskScrollTop = 0
				this.devices = detail.devices
				this.$nextTick(() => this.startTaskAutoScroll())
			})
		},
		startTaskAutoScroll() {
			this.stopTaskAutoScroll()
			if (!this.taskAutoScrollEnabled || this.tasks.length <= 4) {
				return
			}
			this.taskAutoScrollTimer = setInterval(() => {
				this.taskScrollTop = nextTaskScrollTop(
					this.taskScrollTop,
					this.tasks.length,
					uni.upx2px(TASK_ROW_HEIGHT_RPX),
					uni.upx2px(TASK_LIST_HEIGHT_RPX)
				)
			}, TASK_AUTO_SCROLL_INTERVAL)
		},
		stopTaskAutoScroll() {
			if (this.taskAutoScrollTimer) {
				clearInterval(this.taskAutoScrollTimer)
				this.taskAutoScrollTimer = null
			}
		},
		pauseTaskAutoScroll() {
			this.stopTaskAutoScroll()
		},
		resumeTaskAutoScroll() {
			this.startTaskAutoScroll()
		},
		toggleTaskAutoScroll() {
			this.taskAutoScrollEnabled = !this.taskAutoScrollEnabled
			this.startTaskAutoScroll()
		},
		handleTaskScroll(event) {
			this.taskScrollTop = Number(event && event.detail && event.detail.scrollTop || 0)
		},
		handleBack() {
			uni.navigateBack()
		},
		async handleEdit() {
			if (!this.canManageFarm || !this.plot.id) return
			// 服务端限制可选作物与保存权限，前端角色判断只负责隐藏入口。
			try {
				this.editableCrops = await getEditableCrops()
				this.editForm = {
					cropId: this.plot.cropId,
					plotName: this.plot.farm === '-' ? '' : this.plot.farm,
					plantingArea: this.plot.areaValue == null ? '' : String(this.plot.areaValue),
					plantedAt: this.plot.rack === '-' ? '' : this.plot.rack,
					expectedHarvestAt: this.plot.expectedHarvestAt
				}
				this.editDialogVisible = true
			} catch (_) { /* 请求封装已展示错误提示。 */ }
		},
		closeEditDialog() {
			if (!this.editSubmitting) this.editDialogVisible = false
		},
		handleEditCropChange(event) {
			this.editForm.cropId = this.editableCrops[Number(event.detail.value)]?.id || null
		},
		async confirmEdit() {
			if (!this.canManageFarm || this.editSubmitting) return
			const form = this.editForm
			const area = String(form.plantingArea).trim()
			if (!form.cropId || !form.plotName || !/^(?:\d+)(?:\.\d{1,2})?$/.test(area) || Number(area) <= 0
				|| !form.plantedAt || !form.expectedHarvestAt || form.expectedHarvestAt < form.plantedAt) {
				uni.showToast({ title: '请检查作物、名称、面积和日期', icon: 'none' })
				return
			}
			this.editSubmitting = true
			try {
				await updatePlotBasicInfo(this.plot.id, { ...form, plantingArea: area })
				this.editDialogVisible = false
				uni.showToast({ title: '修改成功', icon: 'success' })
				await this.fetchPlotDetail(this.plot.id)
			} catch (_) { /* 请求封装负责错误提示；保存失败时保留表单供修改重试。 */ }
			finally { this.editSubmitting = false }
		},
		handleMonitor() {
			if (this.plot.idle) return
			uni.showToast({
				title: '查看监控',
				icon: 'none'
			})
		},
		handleMoreEnv() {
			if (this.plot.idle) return
			const plotId = this.plot && this.plot.id
			if (!plotId) {
				uni.showToast({
					title: '地块信息尚未加载',
					icon: 'none'
				})
				return
			}

			// 将当前地块主键传给环境监测页，避免目标页回退到默认地块。
			uni.navigateTo({
				url: `/pages/service/environment?plotId=${encodeURIComponent(plotId)}`
			})
		},
		openTaskRecord(task) {
			if (!task || !task.id) {
				uni.showToast({ title: '农事任务信息不完整', icon: 'none' })
				return
			}
			// 传递真实任务主键，由记录页分别读取任务快照与执行时间线。
			uni.navigateTo({
				url: `/pages/service/task_record?id=${encodeURIComponent(task.id)}`
			})
		},
		openPlotTaskTimeline() {
			if (this.plot.idle) return
			const plotId = this.plot && this.plot.id
			if (!plotId) {
				uni.showToast({ title: '地块信息尚未加载', icon: 'none' })
				return
			}

			// 仅传递地块主键，目标页通过 smart_plant 接口读取最新任务数据。
			uni.navigateTo({
				url: `/pages/secondPage/plot/task_timeline?plotId=${encodeURIComponent(plotId)}`
			})
		},
		openHarvestDialog() {
			if (!this.plot.harvestable || this.harvestSubmitting) {
				return
			}
			this.harvestForm = {
				harvester: '',
				yieldAmount: '',
				yieldUnit: 'kg',
				actualHarvestAt: this.today
			}
			this.harvestDialogVisible = true
		},
		closeHarvestDialog() {
			if (!this.harvestSubmitting) {
				this.harvestDialogVisible = false
			}
		},
		handleHarvestDateChange(event) {
			this.harvestForm.actualHarvestAt = event.detail.value
		},
		async confirmHarvest() {
			if (this.harvestSubmitting) {
				return
			}
			const yieldAmount = String(this.harvestForm.yieldAmount).trim()
			if (!this.harvestForm.harvester) {
				uni.showToast({ title: '请输入采收人', icon: 'none' })
				return
			}
			if (!yieldAmount || !Number.isFinite(Number(yieldAmount)) || Number(yieldAmount) < 0) {
				uni.showToast({ title: '请输入正确的实际产量', icon: 'none' })
				return
			}

			this.harvestSubmitting = true
			try {
				await harvestPlot(this.plot.id, {
					harvester: this.harvestForm.harvester,
					yieldAmount,
					yieldUnit: this.harvestForm.yieldUnit || 'kg',
					actualHarvestAt: this.harvestForm.actualHarvestAt
				})
				this.harvestDialogVisible = false
				uni.showToast({ title: '采收成功', icon: 'success' })
				await this.fetchPlotDetail(this.plot.id)
			} finally {
				this.harvestSubmitting = false
			}
		},
		toggleMatureNotice() {
			this.matureNotice = !this.matureNotice
		},
		toggleDevice(device) {
			if (this.plot.idle) return
			device.enabled = !device.enabled
		}
	}
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");

page {
	background-color: #f7f7f7;
	height: 100vh;
	overflow: hidden;
}

.plot-detail-page {
	position: relative;
	height: 100vh;
	overflow: hidden;
	background-color: #f7f7f7;
	font-size: 14px;
	font-weight: normal;
	color: #000000;
}

.detail-hero {
	box-sizing: border-box;
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	z-index: auto;
	height: calc(var(--status-bar-height) + 386rpx);
	padding: calc(var(--status-bar-height) + 16rpx) 36rpx 34rpx;
	/* 固定区高度需要叠加真机状态栏，避免环境监测模块顶到地块基础信息。 */
	background: linear-gradient(180deg, rgba(27, 162, 145, 0.7) 0%, rgba(90, 184, 173, 0) 100%);
}

.navbar {
	position: relative;
	z-index: 5;
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 56rpx;
	color: #ffffff;
}

.nav-title {
	position: absolute;
	left: 96rpx;
	right: 96rpx;
	text-align: center;
	font-size: 18px;
	line-height: 56rpx;
	color: #ffffff;
}

.nav-icon {
	position: relative;
	z-index: 2;
	width: 36rpx;
	height: 56rpx;
	font-size: 42rpx;
	line-height: 56rpx;
	color: #ffffff;
}

.navbar .nav-icon:first-child {
	text-align: left;
}

.navbar .nav-icon:last-child {
	text-align: right;
}

.icon-fanhui.nav-icon {
	font-size: 18px;
}

.crop-panel {
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	margin-top: 76rpx;
}

.crop-info {
	flex: 1;
	min-width: 0;
	padding-right: 24rpx;
}

.crop-name {
	display: block;
	font-size: 18px;
	line-height: 1.3;
	color: #1BA291;
}

.crop-meta {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin-top: 20rpx;
	font-size: 14px;
	line-height: 1.2;
	color: #C8C8C8;
}

.crop-meta text {
	margin-right: 28rpx;
	margin-bottom: 4rpx;
}

.crop-image-wrap {
	position: relative;
	display: flex;
	flex-direction: column;
	flex-shrink: 0;
	width: 136rpx;
	height: 148rpx;
	overflow: hidden;
	border-radius: 14rpx;
	background-color: #ffffff;
	box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.08);
}

.crop-image {
	display: block;
	width: 136rpx;
	height: 112rpx;
	flex-shrink: 0;
}

/* 空闲地块用占位符代替作物图片，避免沿用上一批次的视觉信息。 */
.idle-image-placeholder {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 100%;
	height: 100%;
	font-size: 26px;
	color: #7b8b88;
}

.monitor-badge {
	display: flex;
	align-items: center;
	justify-content: center;
	height: 36rpx;
	flex-shrink: 0;
	/* 监控入口放在图片下方独立显示，不再覆盖到作物图片上。 */
	background-color: rgba(0, 0, 0, 0.72);
	font-size: 12px;
	line-height: 36rpx;
	color: #ffffff;
}

.monitor-icon {
	margin-right: 4rpx;
	font-size: 22rpx;
	line-height: 22rpx;
	color: #ffffff;
}

.detail-content {
	position: absolute;
	left: 0;
	right: 0;
	top: calc(var(--status-bar-height) + 386rpx);
	bottom: 0;
	box-sizing: border-box;
	height: calc(100vh - var(--status-bar-height) - 386rpx);
	padding: 0 36rpx calc(28rpx + env(safe-area-inset-bottom));
	overflow: hidden;
}

.harvest-button {
	position: fixed;
	left: 50%;
	bottom: calc(32rpx + env(safe-area-inset-bottom));
	z-index: 4;
	box-sizing: border-box;
	width: 390rpx;
	height: 88rpx;
	margin: 0;
	border-radius: 44rpx;
	background-color: #1BA291;
	font-size: 16px;
	font-weight: 500;
	line-height: 88rpx;
	color: #ffffff;
	box-shadow: 0 10rpx 28rpx rgba(27, 162, 145, 0.24);
	transform: translateX(-50%);
}

.harvest-button::after,
.dialog-button::after {
	border: 0;
}

.harvest-button[disabled] {
	background-color: #e4e8e9;
	color: #737b7b;
	box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.1);
	opacity: 1;
}

.harvest-button-pressed {
	opacity: 0.78;
}

.dialog-mask {
	position: fixed;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
	z-index: 20;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 36rpx;
	background-color: rgba(0, 0, 0, 0.48);
}

.harvest-dialog {
	box-sizing: border-box;
	width: 100%;
	max-width: 640rpx;
	padding: 36rpx;
	border-radius: 24rpx;
	background-color: #ffffff;
}

/* 五项表单在小屏上内部滚动，始终保留底部保存操作。 */
.edit-dialog {
	max-height: calc(100vh - 100rpx - env(safe-area-inset-bottom));
	display: flex;
	flex-direction: column;
}

.edit-form-scroll {
	flex: 1;
	min-height: 0;
	max-height: 750rpx;
}

.picker-placeholder {
	color: #798783;
}

.dialog-title {
	display: block;
	font-size: 18px;
	font-weight: 600;
	line-height: 1.4;
	color: #202525;
}

.dialog-tip {
	display: block;
	margin-top: 12rpx;
	font-size: 13px;
	line-height: 1.5;
	color: #697272;
}

.form-item {
	margin-top: 28rpx;
}

.form-label {
	display: block;
	margin-bottom: 12rpx;
	font-size: 14px;
	color: #303636;
}

.form-input,
.date-picker {
	box-sizing: border-box;
	height: 88rpx;
	padding: 0 24rpx;
	border: 2rpx solid #dfe7e6;
	border-radius: 12rpx;
	background-color: #f8faf9;
	font-size: 14px;
	line-height: 88rpx;
	color: #202525;
}

.yield-input-row {
	display: flex;
	gap: 16rpx;
}

.yield-input {
	flex: 1;
	min-width: 0;
}

.unit-input {
	width: 140rpx;
}

.dialog-actions {
	display: flex;
	gap: 20rpx;
	margin-top: 36rpx;
}

.dialog-button {
	flex: 1;
	height: 88rpx;
	margin: 0;
	border-radius: 44rpx;
	font-size: 15px;
	line-height: 88rpx;
}

.cancel-button {
	background-color: #edf4f3;
	color: #47716c;
}

.confirm-button {
	background-color: #1BA291;
	color: #ffffff;
}

.dialog-button[disabled] {
	opacity: 0.55;
}

.env-card {
	box-sizing: border-box;
	/* margin-top: -6rpx; */
	padding: 28rpx 34rpx 30rpx;
	border-radius: 14rpx;
	background-color: #6FC4BA;
	color: #ffffff;
	box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.06);
}

.card-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.env-title {
	font-size: 14px;
	line-height: 1.3;
	color: #ffffff;
}

.env-more {
	font-size: 12px;
	line-height: 1.3;
	color: #ffffff;
}

.env-list {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 34rpx;
}

.env-item {
	display: flex;
	flex: 1;
	flex-direction: column;
	align-items: center;
	font-size: 14px;
	line-height: 1.25;
	color: #ffffff;
}

.env-value {
	color: #ffffff;
}

.env-label {
	margin-top: 12rpx;
	color: #ffffff;
}

.plant-card,
.white-card {
	box-sizing: border-box;
	margin-top: 32rpx;
	padding: 28rpx 34rpx;
	border-radius: 14rpx;
	background-color: #ffffff;
	box-shadow: 0 10rpx 24rpx rgba(0, 0, 0, 0.04);
}

.plant-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.plant-days {
	font-size: 14px;
	line-height: 1.3;
	color: #000000;
}

.plant-day-number {
	color: #1BA291;
}

.plant-switch-wrap {
	display: flex;
	align-items: center;
	font-size: 12px;
	line-height: 1.2;
	color: #C8C8C8;
}

.switch-control {
	box-sizing: border-box;
	position: relative;
	width: 48rpx;
	height: 26rpx;
	margin-left: 10rpx;
	border-radius: 26rpx;
	background-color: #e2e2e2;
	transition: background-color 0.2s;
}

.switch-control.active {
	background-color: #1BA291;
}

.switch-dot {
	position: absolute;
	left: 3rpx;
	top: 3rpx;
	width: 20rpx;
	height: 20rpx;
	border-radius: 50%;
	background-color: #ffffff;
	transition: transform 0.2s;
}

.switch-control.active .switch-dot {
	transform: translateX(22rpx);
}

.growth-line {
	display: flex;
	align-items: center;
	margin-top: 40rpx;
}

.idle-growth-placeholder {
	margin-top: 30rpx;
	color: #7b8b88;
}

.growth-segment {
	flex: 1;
	height: 8rpx;
	margin-right: 8rpx;
	border-radius: 8rpx;
	background-color: #e2e2e2;
}

.growth-segment:last-child {
	margin-right: 0;
}

.growth-segment.active {
	background-color: #1BA291;
}

.growth-labels {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-top: 18rpx;
	font-size: 12px;
	line-height: 1.2;
	color: #C8C8C8;
}

.growth-labels text.active {
	color: #1BA291;
}

.section-title {
	font-size: 14px;
	line-height: 1.3;
	color: #000000;
}

.card-arrow {
	font-size: 28rpx;
	line-height: 28rpx;
	color: #C8C8C8;
}

.card-link {
	display: flex;
	align-items: center;
	justify-content: flex-end;
	width: 88rpx;
	height: 88rpx;
	margin: -30rpx -20rpx -30rpx 0;
	padding-right: 20rpx;
	border-radius: 44rpx;
	box-sizing: border-box;
}

.card-link-pressed {
	background-color: #eef8f6;
}

.task-head-actions {
	display: flex;
	align-items: center;
	margin: -30rpx -20rpx -30rpx 0;
}

.auto-scroll-toggle {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 88rpx;
	height: 88rpx;
	border-radius: 44rpx;
	font-size: 12px;
	color: #1BA291;
}

.task-head-actions .card-link {
	margin: 0;
}

.task-list {
	height: 352rpx;
	margin-top: 26rpx;
}

.task-row {
	display: grid;
	grid-template-columns: 38rpx minmax(0, 1fr) 104rpx 164rpx;
	column-gap: 10rpx;
	align-items: center;
	min-height: 88rpx;
	padding: 0 8rpx;
	border-radius: 10rpx;
	font-size: 14px;
	line-height: 1.25;
	/* 新任务文本保持灰色层级，同时提高白色卡片上的阅读对比度。 */
	color: #737373;
}

.task-row-pressed {
	background-color: #eef8f6;
}

.task-index,
.task-title,
.task-status,
.task-deadline {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.task-status,
.task-deadline {
	text-align: right;
}

.task-empty {
	display: block;
	padding-top: 78rpx;
	text-align: center;
	font-size: 13px;
	color: #737373;
}

.device-card {
	margin-bottom: 28rpx;
}

.device-list {
	margin-top: 28rpx;
}

.device-row {
	box-sizing: border-box;
	display: flex;
	align-items: center;
	min-height: 118rpx;
	margin-bottom: 28rpx;
	padding: 20rpx 28rpx;
	border-radius: 14rpx;
	background-color: #f7f7f7;
}

.device-row:last-child {
	margin-bottom: 0;
}

.device-icon-box {
	display: flex;
	flex-shrink: 0;
	align-items: center;
	justify-content: center;
	width: 66rpx;
	height: 66rpx;
	margin-right: 24rpx;
}

.device-icon {
	font-size: 58rpx;
	line-height: 66rpx;
	color: #1BA291;
}

.device-info {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
}

.device-name {
	font-size: 14px;
	line-height: 1.25;
	color: #000000;
}

.device-meta {
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	margin-top: 12rpx;
	font-size: 12px;
	line-height: 1.2;
	color: #C8C8C8;
}

.device-state {
	margin-top: 8rpx;
}

.device-state.online {
	color: #1BA291;
}

.device-state.offline {
	color: #C8C8C8;
}

.device-state.error {
	color: #f04444;
}

@media screen and (min-width: 768px) {
	.plot-detail-page {
		width: 750rpx;
		margin: 0 auto;
	}
}
</style>
