<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { type PageTab, usePageTabsStore } from '@/stores/pageTabs'

const route = useRoute()
const router = useRouter()
const pageTabsStore = usePageTabsStore()
const tabsScrollRef = ref<HTMLElement | null>(null)

const activeTabKey = computed(() => String(route.name || route.path))

const scrollActiveTabIntoView = () => {
  void nextTick(() => {
    const activeTab = tabsScrollRef.value?.querySelector<HTMLElement>('.page-tab.active')
    activeTab?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
  })
}

watch(
  () => [route.name, route.fullPath, route.meta.pageTitle, route.meta.hidePageTab],
  () => {
    if (route.meta.hidePageTab) {
      return
    }

    const title = String(route.meta.pageTitle || route.name || '未命名页面')
    pageTabsStore.openTab({
      key: activeTabKey.value,
      title,
      fullPath: route.fullPath,
      closable: !route.meta.fixedPageTab,
    })
    scrollActiveTabIntoView()
  },
  { immediate: true },
)

const activateTab = async (tab: PageTab) => {
  if (tab.key !== activeTabKey.value) {
    await router.push(tab.fullPath)
  }
}

const closeTab = async (tab: PageTab) => {
  if (!tab.closable) {
    return
  }

  const closingIndex = pageTabsStore.tabs.findIndex((item) => item.key === tab.key)
  const isClosingActiveTab = tab.key === activeTabKey.value
  pageTabsStore.removeTab(tab.key)

  if (!isClosingActiveTab) {
    return
  }

  const nextTab =
    pageTabsStore.tabs[Math.max(0, closingIndex - 1)] ||
    pageTabsStore.tabs[closingIndex] ||
    pageTabsStore.tabs[0]

  await router.push(nextTab?.fullPath || '/home')
}

const scrollTabs = (direction: -1 | 1) => {
  tabsScrollRef.value?.scrollBy({ left: direction * 240, behavior: 'smooth' })
}
</script>

<template>
  <nav class="page-tabs" aria-label="已打开页面">
    <el-tooltip content="向左滚动" placement="bottom">
      <el-button
        class="tabs-scroll-button"
        :icon="ArrowLeft"
        text
        aria-label="向左滚动标签页"
        @click="scrollTabs(-1)"
      />
    </el-tooltip>

    <div ref="tabsScrollRef" class="page-tabs-scroll">
      <div
        v-for="tab in pageTabsStore.tabs"
        :key="tab.key"
        class="page-tab"
        :class="{ active: tab.key === activeTabKey }"
      >
        <button class="page-tab-title" type="button" :title="tab.title" @click="activateTab(tab)">
          {{ tab.title }}
        </button>
        <el-tooltip v-if="tab.closable" content="关闭页面" placement="bottom">
          <el-button
            class="page-tab-close"
            :icon="Close"
            text
            circle
            :aria-label="`关闭${tab.title}`"
            @click="closeTab(tab)"
          />
        </el-tooltip>
      </div>
    </div>

    <el-tooltip content="向右滚动" placement="bottom">
      <el-button
        class="tabs-scroll-button"
        :icon="ArrowRight"
        text
        aria-label="向右滚动标签页"
        @click="scrollTabs(1)"
      />
    </el-tooltip>
  </nav>
</template>

<style scoped>
.page-tabs {
  height: 44px;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 38px;
  align-items: stretch;
  padding: 6px 14px;
  border-bottom: 1px solid #e2e8f0;
  background: #f6f8fb;
}

.page-tabs-scroll {
  min-width: 0;
  display: flex;
  align-items: stretch;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
}

.page-tabs-scroll::-webkit-scrollbar {
  display: none;
}

.page-tab {
  position: relative;
  height: 32px;
  min-width: 104px;
  max-width: 220px;
  display: flex;
  align-items: center;
  border: 1px solid #d8dee8;
  border-radius: 4px;
  color: #475569;
  background: #ffffff;
  flex: 0 0 auto;
  transition: border-color 0.16s ease, color 0.16s ease, background-color 0.16s ease;
}

.page-tab:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.page-tab.active {
  border-color: #409eff;
  color: #2563eb;
  background: #eff6ff;
}

.page-tab.active::after {
  position: absolute;
  right: 8px;
  bottom: -7px;
  left: 8px;
  height: 2px;
  content: '';
  background: #409eff;
}

.page-tab-title {
  min-width: 0;
  height: 100%;
  padding: 0 12px;
  border: 0;
  color: inherit;
  background: transparent;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  flex: 1 1 auto;
  font-size: 13px;
}

.page-tab:has(.page-tab-close) .page-tab-title {
  padding-right: 4px;
}

.page-tab-close {
  width: 26px;
  height: 26px;
  margin-right: 3px;
  color: #94a3b8;
  flex: 0 0 auto;
}

.page-tab-close:hover {
  color: #ffffff;
  background: #ef4444;
}

.tabs-scroll-button {
  width: 30px;
  height: 32px;
  align-self: center;
  justify-self: center;
  color: #64748b;
}

.tabs-scroll-button:hover {
  color: #2563eb;
  background: #eaf2ff;
}

@media (max-width: 640px) {
  .page-tabs {
    grid-template-columns: 32px minmax(0, 1fr) 32px;
    padding: 6px 8px;
  }

  .page-tab {
    min-width: 92px;
    max-width: 168px;
  }
}
</style>
