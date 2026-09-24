<template>
  <view class="chat-media-body">
    <!-- 图片直接显示缩略图，点击使用系统预览器查看原图。 -->
    <image v-if="type === 2 && imageUrl" class="chat-image" :src="imageUrl" mode="widthFix" @tap="previewImage"></image>
    <!-- 文件以名称展示，点击后打开或下载，不在气泡中显示原始 URL。 -->
    <view v-else-if="type === 4 && url" class="chat-file" role="button" :aria-label="`打开文件${message.content || '附件'}`" @tap="openFile">
      <text class="iconfont icon-wenjian file-icon"></text>
      <text class="file-name">{{ message.content || '聊天文件' }}</text>
    </view>
    <text v-else class="chat-text">{{ message.content || (type === 3 ? '[语音]' : '[消息]') }}</text>
  </view>
</template>

<script>
import { downloadProtectedFile, resolveFileUrl } from '@/utils/request.js'

export default {
  props: { message: { type: Object, required: true } },
  data() { return { localImageUrl: '' } },
  computed: {
    type() { return Number(this.message.messageType) },
    url() { return resolveFileUrl(this.message.mediaUrl) },
    imageUrl() { return this.localImageUrl },
  },
  watch: {
    url: {
      immediate: true,
      handler(url) {
        this.localImageUrl = ''
        if (this.type !== 2 || !url) return
        downloadProtectedFile(url)
          .then(path => { if (this.url === url) this.localImageUrl = path })
          .catch(() => {})
      },
    },
  },
  methods: {
    previewImage() { uni.previewImage({ current: this.imageUrl, urls: [this.imageUrl] }) },
    openFile() {
      downloadProtectedFile(this.url)
        .then(filePath => {
          // #ifdef H5
          const link = document.createElement('a')
          link.href = filePath
          link.download = this.message.content || '聊天附件'
          link.rel = 'noopener'
          link.click()
          // #endif
          // #ifndef H5
            uni.openDocument({
              filePath,
              showMenu: true,
              fail: () => {
                // ZIP 等类型交给 App 系统文件程序处理；没有可用程序时给出明确反馈。
                // #ifdef APP-PLUS
                plus.runtime.openFile(filePath, {}, () => uni.showToast({ title: '请安装可打开此文件的应用', icon: 'none' }))
                // #endif
                // #ifndef APP-PLUS
                uni.showToast({ title: '当前设备无法打开此文件', icon: 'none' })
                // #endif
              },
            })
          // #endif
        })
        .catch(() => uni.showToast({ title: '文件下载失败', icon: 'none' }))
    },
  },
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
.chat-media-body { max-width:100%; }
.chat-image { display:block; width:320rpx; max-width:100%; border-radius:10rpx; }
.chat-file { display:flex; align-items:center; gap:14rpx; min-width:200rpx; min-height:72rpx; max-width:100%; }
.file-icon { flex-shrink:0; font-size:42rpx; }
.file-name { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:26rpx; }
.chat-text { word-break:break-all; white-space:normal; }
</style>
