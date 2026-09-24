<template>
  <!-- 技术人员使用三入口导航，避开农场主的固定四栏 TabBar。 -->
  <view class="technician-tabbar" role="navigation" aria-label="技术人员工作台导航">
    <view v-for="item in items" :key="item.key" class="technician-tab"
      :class="{ active: active === item.key }" role="button" :aria-label="item.label"
      @tap="navigate(item)">
      <text class="iconfont tab-icon" :class="item.icon"></text>
      <text>{{ item.label }}</text>
    </view>
  </view>
</template>

<script>
export default {
  props: { active: { type: String, required: true } },
  data() {
    return {
      items: [
        { key: 'home', label: '故障处理', icon: 'icon-guzhangchuli', path: '/pages/service/fault_handling' },
        { key: 'news', label: '消息', icon: 'icon-icon_msg', path: '/pages/technician/news' },
        { key: 'mine', label: '我的', icon: 'icon-gerenzhongxin2', path: '/pages/technician/mine' }
      ]
    }
  },
  methods: {
    navigate(item) {
      // 非原生 TabBar 页面用 reLaunch 切换，避免三入口反复压栈。
      if (item.key !== this.active) uni.reLaunch({ url: item.path })
    }
  }
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
.technician-tabbar { position:fixed; z-index:10; left:0; right:0; bottom:0; display:flex; box-sizing:border-box; height:calc(58px + env(safe-area-inset-bottom)); padding-bottom:env(safe-area-inset-bottom); background:#fff; border-top:1px solid #eef0f0; }
.technician-tab { display:flex; flex:1; flex-direction:column; align-items:center; justify-content:center; gap:3px; color:#8b9995; font-size:13px; }
.technician-tab.active { color:#267f74; }
.tab-icon { font-size:23px; line-height:1; }
.technician-tab:active { opacity:.7; }
</style>
