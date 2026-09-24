<template>
  <!-- 专家页面不使用农场主的固定四栏 TabBar，只保留咨询消息和个人中心。 -->
  <view class="expert-tabbar" role="navigation" aria-label="专家工作台导航">
    <view class="expert-tab" :class="{ active: active === 'news' }" role="button" aria-label="咨询消息" @tap="navigate('news')">
      <text class="iconfont icon-icon_msg tab-icon"></text><text>消息</text>
    </view>
    <view class="expert-tab" :class="{ active: active === 'mine' }" role="button" aria-label="个人中心" @tap="navigate('mine')">
      <text class="iconfont icon-gerenzhongxin2 tab-icon"></text><text>我的</text>
    </view>
  </view>
</template>

<script>
export default {
  props: { active: { type: String, required: true } },
  methods: {
    navigate(page) {
      // reLaunch 避免消息和我的互相压栈，始终保持专家双入口。
      if (page !== this.active) uni.reLaunch({ url: `/pages/expert/${page}` })
    },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
.expert-tabbar { position:fixed; z-index:10; left:0; right:0; bottom:0; display:flex; box-sizing:border-box; height:calc(58px + env(safe-area-inset-bottom)); padding-bottom:env(safe-area-inset-bottom); background:#fff; border-top:1px solid #eef0f0; }
.expert-tab { display:flex; flex:1; flex-direction:column; align-items:center; justify-content:center; gap:3px; color:#c8c8c8; font-size:14px; }
.expert-tab.active { color:#6fc4ba; }
.tab-icon { font-size:24px; line-height:1; }
.expert-tab:active { opacity:.7; }
</style>
