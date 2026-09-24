<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { loadPublicSystemSetting } from '@/stores/systemSetting'

// 应用启动时读取公开站点配置，使登录页和后台页共享同一套品牌信息。
onMounted(() => {
  void loadPublicSystemSetting()
})
</script>

<template>
  <el-config-provider :locale="zhCn">
    <RouterView />
  </el-config-provider>
</template>
