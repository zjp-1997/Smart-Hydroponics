<template>
  <view class="mine-page">
    <view class="hero">
      <!-- 点击技术人员头像或名称区域进入共用个人信息编辑页。 -->
      <view class="profile" hover-class="profile-pressed" @tap="openProfile">
        <protected-image class="avatar" :src="profile.avatar" mode="aspectFill" @error="profile.avatar = defaultAvatar"></protected-image>
        <view class="identity"><text class="nickname">{{ profile.name }}</text><text class="account">{{ profile.account }}</text></view>
      </view>
    </view>
    <scroll-view class="content" scroll-y>
      <!-- 技术人员个人中心沿用农场主菜单形态，只展示与当前身份有关的入口。 -->
      <view v-for="(group, index) in menuGroups" :key="index" class="menu-group">
        <view v-for="item in group" :key="item.title" class="menu-row" @tap="open(item.path)">
          <text class="iconfont menu-icon" :class="item.icon"></text>
          <text class="menu-title">{{ item.title }}</text>
          <text class="iconfont icon-youjiantou arrow"></text>
        </view>
      </view>
    </scroll-view>
    <TechnicianTabBar active="mine" />
  </view>
</template>

<script>
import { getUserInfo } from '@/utils/auth.js'
import { resolveFileUrl } from '@/utils/request.js'
import { ensureTechnicianAccess } from '@/utils/technicianAccess.js'
import TechnicianTabBar from '@/componment/TechnicianTabBar.vue'

const DEFAULT_AVATAR = '/static/avatar.png'
export default {
  components: { TechnicianTabBar },
  data() {
    return {
      defaultAvatar: DEFAULT_AVATAR,
      profile: { name: '技术人员', account: '-', avatar: DEFAULT_AVATAR },
      menuGroups: [
        [
          // “我的任务”替代农场主的添加设备；处理记录与首页任务分开呈现。
          { title: '我的任务', icon: 'icon-guzhangchuli', path: '/pages/service/fault_handling' },
          { title: '故障处理记录', icon: 'icon-weihu', path: '/pages/technician/records' },
          { title: '账户设置', icon: 'icon-shezhi', path: '/pages/secondPage/account_setting/index' }
        ],
        [
          { title: '关于我们', icon: 'icon-guanyuwomen', path: '/pages/secondPage/about/about_me' },
          { title: '帮助与反馈', icon: 'icon-bangzhu', path: '/pages/secondPage/about/help' },
          { title: '技术支持', icon: 'icon-a-kefukefuzhongxin', path: '/pages/secondPage/about/technical' }
        ]
      ]
    }
  },
  async onShow() {
    if (!await ensureTechnicianAccess()) return
    const user = getUserInfo() || {}
    // 未绑定真实手机号时不展示后端为数据库约束保存的内部占位值。
    const realPhone = /^1[3-9]\d{9}$/.test(user.phone || '') ? user.phone : ''
    this.profile = {
      name: user.nickname || user.username || '技术人员',
      account: realPhone || user.username || '-',
      avatar: user.avatar ? resolveFileUrl(user.avatar) : DEFAULT_AVATAR
    }
  },
  methods: {
    openProfile() {
      uni.navigateTo({ url: '/pages/secondPage/profile/index' })
    },
    open(path) {
      if (path === '/pages/service/fault_handling') uni.reLaunch({ url: path })
      else uni.navigateTo({ url: path })
    }
  }
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { height: 100%; background: #f7f7f7; }
.mine-page { position: relative; height: 100vh; overflow: hidden; background: #f7f7f7; color: #333; }
.hero { box-sizing: border-box; min-height: 300rpx; padding: calc(var(--status-bar-height) + 64rpx) 44rpx 0; background: linear-gradient(180deg, rgba(27,162,145,.7), rgba(90,184,173,0)); }
.profile { display: flex; align-items: center; }
.profile-pressed { opacity: .72; }
.avatar { width: 112rpx; height: 112rpx; border: 6rpx solid rgba(255,255,255,.82); border-radius: 50%; background: #fff; }
.identity { display: flex; flex-direction: column; min-width: 0; margin-left: 34rpx; }
.nickname { font-size: 18px; color: #26302f; }
.account { margin-top: 8rpx; color: #52615f; font-size: 15px; }
.content { position: absolute; top: calc(var(--status-bar-height) + 238rpx); bottom: calc(58px + env(safe-area-inset-bottom)); left: 0; right: 0; box-sizing: border-box; padding: 0 42rpx; }
.menu-group { margin-top: 34rpx; padding: 10rpx 0; border-radius: 20rpx; background: #fff; box-shadow: 0 8rpx 24rpx rgba(0,0,0,.035); }
.menu-row { display: flex; align-items: center; height: 88rpx; padding: 0 34rpx; }
.menu-row:active { background: #eef8f6; }
.menu-icon { width: 42rpx; color: #737b7a; font-size: 30rpx; text-align: center; }
.menu-title { flex: 1; margin-left: 20rpx; font-size: 16px; color: #434948; }
.arrow { color: #c6cccc; font-size: 28rpx; }
@media screen and (min-width: 768px) { .mine-page { width: 750rpx; margin: 0 auto; } }
</style>
