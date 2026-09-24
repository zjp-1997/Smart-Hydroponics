<template>
  <view class="record-page">
    <view class="hero"><view class="navbar">
      <text class="iconfont icon-fanhui back" @tap="goBack"></text>
      <text class="title">故障处理记录</text>
    </view></view>
    <scroll-view class="content" scroll-y>
      <view v-if="loading" class="empty">正在加载处理记录...</view>
      <view v-else-if="!records.length" class="empty">暂无已完成的故障记录</view>
      <!-- 记录卡片复用农事记录的摘要加详情入口布局，完整时间轴在故障记录页展示。 -->
      <view v-for="fault in records" :key="fault.id" class="card" @tap="openRecord(fault.id)">
        <view class="card-heading"><text class="fault-name">{{ fault.faultName }}</text><text class="done">已处理</text></view>
        <view class="meta"><text class="plot">{{ fault.plotName }}</text><text class="device">{{ fault.deviceName }}</text></view>
        <view class="detail"><text>发生时间</text><text>{{ fault.startTimeText }}</text></view>
        <view class="detail"><text>完成时间</text><text>{{ fault.endTimeText }}</text></view>
        <view v-if="fault.handleResult" class="result">{{ fault.handleResult }}</view>
        <view class="footer"><text>查看处理过程</text><text class="iconfont icon-youjiantou"></text></view>
      </view>
      <view class="safe-bottom"></view>
    </scroll-view>
  </view>
</template>

<script>
import { getTechnicianCompletedFaults } from '@/api/deviceFault.js'
import { ensureTechnicianAccess } from '@/utils/technicianAccess.js'

export default {
  data() { return { records: [], loading: false } },
  async onShow() {
    if (!await ensureTechnicianAccess()) return
    this.loading = true
    try { this.records = await getTechnicianCompletedFaults() }
    catch { this.records = [] }
    finally { this.loading = false }
  },
  methods: {
    // 仅传 ID；详情接口再次检查任务是否指派给当前技术人员。
    openRecord(id) { uni.navigateTo({ url: `/pages/service/fault_record?id=${encodeURIComponent(id)}` }) },
    goBack() {
      if (getCurrentPages().length > 1) uni.navigateBack()
      else uni.reLaunch({ url: '/pages/technician/mine' })
    }
  }
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background: #f7f7f7; }
.record-page { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: #f7f7f7; color: #26302f; }
.hero { box-sizing: border-box; height: 300rpx; padding: calc(var(--status-bar-height) + 16rpx) 36rpx 0; background: linear-gradient(180deg, rgba(27,162,145,.7), rgba(90,184,173,0)); }
.navbar { display: flex; align-items: center; height: 58rpx; position: relative; }
.back { z-index: 1; width: 58rpx; color: #fff; font-size: 18px; }
.title { position: absolute; left: 90rpx; right: 90rpx; text-align: center; color: #fff; font-size: 16px; }
.content { position: absolute; top: calc(var(--status-bar-height) + 110rpx); bottom: 0; left: 0; right: 0; box-sizing: border-box; padding: 0 36rpx; }
.empty { margin-top: 24rpx; padding: 70rpx 20rpx; border-radius: 20rpx; background: #fff; text-align: center; color: #89938f; }
.card { margin: 0 0 24rpx; padding: 26rpx 28rpx; border-radius: 20rpx; background: #fff; box-shadow: 0 8rpx 22rpx rgba(38,91,84,.05); }
.card-heading, .meta, .detail, .footer { display: flex; align-items: center; justify-content: space-between; }
.fault-name { font-size: 15px; font-weight: 500; }
.done { color: #168577; font-size: 12px; }
.meta { justify-content: flex-start; gap: 24rpx; margin: 20rpx 0; font-size: 12px; }
.plot { padding: 8rpx 12rpx; border-radius: 6rpx; background: #5ab8ad; color: #fff; }
.device { color: #56625f; }
.detail { min-height: 44rpx; color: #89938f; font-size: 12px; }
.detail text:last-child { color: #45514f; }
.result { margin-top: 14rpx; padding: 16rpx 18rpx; border-radius: 10rpx; background: #f7f9f8; color: #56625f; font-size: 12px; }
.footer { margin-top: 22rpx; padding-top: 18rpx; border-top: 1rpx solid #edf1f0; color: #168577; font-size: 12px; }
.safe-bottom { height: calc(40rpx + env(safe-area-inset-bottom)); }
@media screen and (min-width: 768px) { .record-page { width: 750rpx; margin: 0 auto; } }
</style>
