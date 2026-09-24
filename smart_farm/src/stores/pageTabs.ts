import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface PageTab {
  key: string
  title: string
  fullPath: string
  closable: boolean
}

const HOME_TAB: PageTab = {
  key: 'home',
  title: '首页工作台',
  fullPath: '/home',
  closable: false,
}

export const usePageTabsStore = defineStore('pageTabs', () => {
  const tabs = ref<PageTab[]>([])

  const ensureHomeTab = () => {
    if (!tabs.value.some((tab) => tab.key === HOME_TAB.key)) {
      tabs.value.unshift({ ...HOME_TAB })
    }
  }

  const openTab = (tab: PageTab) => {
    ensureHomeTab()
    const existingTab = tabs.value.find((item) => item.key === tab.key)

    if (existingTab) {
      existingTab.title = tab.title
      existingTab.fullPath = tab.fullPath
      existingTab.closable = tab.closable
      return
    }

    tabs.value.push(tab)
  }

  const removeTab = (key: string) => {
    const index = tabs.value.findIndex((tab) => tab.key === key)

    if (index >= 0 && tabs.value[index]?.closable) {
      tabs.value.splice(index, 1)
    }
  }

  const resetTabs = () => {
    tabs.value = []
  }

  return {
    tabs,
    ensureHomeTab,
    openTab,
    removeTab,
    resetTabs,
  }
})
