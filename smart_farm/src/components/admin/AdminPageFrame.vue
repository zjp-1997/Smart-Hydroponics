<script setup lang="ts">
import { ref } from 'vue'
import Header from '@/components/Header.vue'
import LeftMenu from '@/components/LeftMenu.vue'

withDefaults(defineProps<{
  breadcrumbs: string[]
  pageClass?: string
  shellClass?: string
  contentClass?: string
}>(), {
  pageClass: '',
  shellClass: '',
  contentClass: '',
})

// 页面框架统一持有移动端侧栏状态，业务列表只关心自身内容。
const sidebarVisible = ref(false)
</script>

<template>
  <div class="admin-page" :class="pageClass">
    <LeftMenu :visible="sidebarVisible" @close="sidebarVisible = false" />
    <div class="admin-shell" :class="shellClass">
      <Header :breadcrumbs="breadcrumbs" @toggle-sidebar="sidebarVisible = true" />
      <main class="admin-content" :class="contentClass">
        <slot />
      </main>
    </div>
    <!-- 页面级弹窗通过overlay插槽保持在框架根节点，避免业务页面重复骨架。 -->
    <slot name="overlay" />
  </div>
</template>
