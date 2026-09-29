import { waitForRequestsToSettle } from './request.js'

const MIN_FEEDBACK_MS = 450

function asHooks(value) {
	if (!value) return []
	return Array.isArray(value) ? value : [value]
}

function rememberPageOptions(options) {
	this.$pullRefreshOptions = options || {}
}

async function runPageRefresh(vm) {
	const pageRefresh = asHooks(vm.$options && vm.$options.onPageRefresh)
	if (pageRefresh.length) {
		await Promise.all(pageRefresh.map((hook) => Promise.resolve(hook.call(vm))))
		return
	}

	const showHooks = asHooks(vm.$options && vm.$options.onShow)
	if (showHooks.length) {
		await Promise.all(showHooks.map((hook) => Promise.resolve(hook.call(vm))))
		return
	}

	const loadHooks = asHooks(vm.$options && vm.$options.onLoad)
		.filter((hook) => hook !== rememberPageOptions)
	await Promise.all(loadHooks.map((hook) => Promise.resolve(hook.call(vm, vm.$pullRefreshOptions || {}))))
}

export default {
	data() {
		return { pullRefreshing: false }
	},
	onLoad: rememberPageOptions,
	onPullDownRefresh() {
		return this.$handlePullDownRefresh()
	},
	methods: {
		async $handlePullDownRefresh() {
			if (this.pullRefreshing) return
			this.pullRefreshing = true
			const startedAt = Date.now()
			try {
				await runPageRefresh(this)
				await waitForRequestsToSettle()
			} catch {
				// 页面请求层已经负责错误提示；刷新控件只保证状态能够复位。
			} finally {
				const remaining = MIN_FEEDBACK_MS - (Date.now() - startedAt)
				if (remaining > 0) await new Promise((resolve) => setTimeout(resolve, remaining))
				this.pullRefreshing = false
				uni.stopPullDownRefresh()
			}
		}
	}
}
