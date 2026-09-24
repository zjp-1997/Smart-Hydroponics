<template>
  <!-- 语音链路尚未接入，隐藏入口，避免把装饰按钮误认为已交付能力。 -->
  <view class="composer-panel">
    <view class="composer-row">
      <view class="composer-field">
        <input class="composer-input" :value="modelValue" :placeholder="placeholder"
               maxlength="2000" confirm-type="send" @input="onInput" @confirm="$emit('send')"
               @keyboardheightchange="$emit('keyboard-height-change')" />
        <button class="composer-icon" role="button" aria-label="更多操作" @tap="$emit('toggle-tools')">
          <text class="iconfont icon-tianjia" aria-hidden="true"></text>
        </button>
      </view>
      <button class="composer-send" role="button" :disabled="sending || !modelValue.trim()" @tap="$emit('send')">发送</button>
    </view>
    <view v-if="showTools" class="composer-tools">
      <button v-for="tool in tools" :key="tool.label" class="tool-card" role="button" :disabled="sending" @tap="$emit('tool', tool)">
        <!-- 图标块与名称分开占位，避免按钮默认行高把名称挤出可视区域。 -->
        <view class="tool-icon-box"><text class="iconfont tool-icon" :class="tool.iconClass"></text></view>
        <text class="tool-label">{{ tool.label }}</text>
      </button>
    </view>
  </view>
</template>

<script>
export default {
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '发消息...' },
    sending: { type: Boolean, default: false },
    showTools: { type: Boolean, default: false },
    tools: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue', 'send', 'toggle-tools', 'tool', 'keyboard-height-change'],
  methods: {
    // uni-app input 事件返回 detail.value，统一映射为组件 v-model 值。
    onInput(event) { this.$emit('update:modelValue', event.detail.value || '') },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
.composer-panel { position:fixed; z-index:10; right:0; bottom:0; left:0; box-sizing:border-box; padding:12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom)); background:#fff; }
.composer-row { display:flex; align-items:center; gap:16rpx; min-height:84rpx; }
.composer-field { display:flex; flex:1; align-items:center; min-width:0; height:72rpx; padding:0 10rpx 0 22rpx; border-radius:36rpx; background:#f5f7f6; }
.composer-input { flex:1; min-width:0; height:72rpx; padding:0; color:#303943; background:transparent; font-size:28rpx; }
.composer-icon { display:flex; flex-shrink:0; align-items:center; justify-content:center; width:48rpx; height:48rpx; padding:0; margin:0 0 0 8rpx; border-radius:50%; color:#555; background:transparent; font-size:38rpx; line-height:1; }
.composer-send { flex-shrink:0; width:112rpx; height:72rpx; padding:0; margin:0; border-radius:36rpx; color:#fff; background:#5ab8ad; font-size:26rpx; line-height:72rpx; }
.composer-send[disabled] { color:#879895; background:#d4e4e0; opacity:1; }
.composer-icon::after,.composer-send::after,.tool-card::after { border:0; }
.composer-icon:active,.composer-send:active,.tool-card:active { opacity:.72; }
.composer-tools { display:flex; justify-content:space-around; padding:22rpx 0 12rpx; }
/* 操作按钮自身不着色；图标块和文字各占一行，名称始终完整显示。 */
.tool-card { display:flex; flex-direction:column; align-items:center; gap:8rpx; width:142rpx; height:136rpx; padding:0; margin:0; color:#66736f; background:transparent; font-size:24rpx; line-height:1.2; }
.tool-icon-box { display:flex; align-items:center; justify-content:center; width:142rpx; height:96rpx; border-radius:14rpx; background:#f5f7f6; }
.tool-icon { color:#46524f; font-size:46rpx; line-height:1; }
.tool-label { display:block; height:32rpx; line-height:32rpx; white-space:nowrap; }
</style>
