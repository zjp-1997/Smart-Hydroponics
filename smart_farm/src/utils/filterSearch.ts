const FILTER_FORM_SELECTOR = '.filter-form, .filters'
const SELECT_SELECTOR = '.el-select, .el-select__wrapper'
const CLEAR_TRIGGER_SELECTOR = [
  '.el-input__clear',
  '.el-select__clear',
  '.el-select__caret.is-show-close',
  '.el-range__close-icon',
  '.el-cascader__clearIcon',
].join(',')

const isHTMLElement = (value: EventTarget | null): value is HTMLElement => {
  return value instanceof HTMLElement
}

const isElement = (value: EventTarget | null): value is Element => {
  return value instanceof Element
}

const isVisibleButton = (button: HTMLButtonElement) => {
  return !button.disabled && button.offsetParent !== null
}

/**
 * 在当前筛选表单内寻找“查询”按钮。
 * 列表页已有查询按钮都承载了各自页面的分页重置与接口请求逻辑，复用按钮点击可以避免逐页重复写回车事件。
 */
const findSearchButton = (form: Element) => {
  const buttons = Array.from(form.querySelectorAll<HTMLButtonElement>('button'))

  return buttons.find((button) => {
    const buttonText = button.textContent?.trim() || ''
    return isVisibleButton(button) && buttonText.includes('查询')
  })
}

const triggerSearchAfterModelClear = (form: Element) => {
  const searchButton = findSearchButton(form)
  if (!searchButton) {
    return
  }

  // Element Plus 在自身 click 处理里同步清空 v-model，这里延后到模型更新后再查询。
  window.setTimeout(() => {
    searchButton.click()
  })
}

/**
 * 下拉选择框获得焦点时，Element Plus 默认会用 Enter 打开下拉面板。
 * 这里在触发查询前移出焦点，避免查询后下拉层继续展示在页面上。
 */
const blurFocusedSelect = (target: HTMLElement) => {
  if (!target.closest(SELECT_SELECTOR)) {
    return
  }

  ;(document.activeElement as HTMLElement | null)?.blur()
}

/**
 * 注册筛选表单的全局查询交互能力。
 * - 回车：复用当前筛选表单的“查询”按钮。
 * - 清除：点击输入框、下拉框、日期范围等控件的清除按钮后，按剩余条件重新查询。
 * 作用范围限定为列表页筛选表单，避免影响新增、编辑、登录等普通业务表单。
 */
export const setupFilterFormSearchInteractions = () => {
  document.addEventListener(
    'keydown',
    (event) => {
      if (event.key !== 'Enter' || event.isComposing || event.ctrlKey || event.metaKey || event.altKey) {
        return
      }

      if (!isHTMLElement(event.target) || event.target.closest('textarea')) {
        return
      }

      const form = event.target.closest(FILTER_FORM_SELECTOR)
      if (!form) {
        return
      }

      const searchButton = findSearchButton(form)
      if (!searchButton) {
        return
      }

      event.preventDefault()
      event.stopPropagation()
      event.stopImmediatePropagation()

      blurFocusedSelect(event.target)
      searchButton.click()
    },
    true,
  )

  document.addEventListener(
    'click',
    (event) => {
      if (!isElement(event.target) || !event.target.closest(CLEAR_TRIGGER_SELECTOR)) {
        return
      }

      const form = event.target.closest(FILTER_FORM_SELECTOR)
      if (!form) {
        return
      }

      triggerSearchAfterModelClear(form)
    },
    true,
  )
}
