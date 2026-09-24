<template>
	<view class="agreement-page">
		<view class="navbar">
			<button class="back-button" role="button" aria-label="返回登录页" @tap="goBack">
				<text class="iconfont icon-fanhui" aria-hidden="true"></text>
			</button>
			<text class="nav-title">{{ document.title }}</text>
			<view class="nav-placeholder"></view>
		</view>
		<scroll-view class="document" scroll-y>
			<text class="updated">更新日期：2026年9月23日</text>
			<view v-for="section in document.sections" :key="section.title" class="section">
				<text class="section-title">{{ section.title }}</text>
				<text class="section-content">{{ section.content }}</text>
			</view>
		</scroll-view>
	</view>
</template>

<script>
const DOCUMENTS = {
	user: {
		title: '用户协议',
		sections: [
			{ title: '一、服务说明', content: '本应用为智慧农业生产、设备、农场与咨询协作提供信息化服务。具体可用功能以您当前账号角色和页面实际展示为准。' },
			{ title: '二、账号使用', content: '您应提供真实、合法的信息并妥善保管账号凭证。请勿转借账号、绕过权限控制，或利用本服务从事违法、侵权及干扰系统运行的活动。' },
			{ title: '三、内容与操作', content: '您对通过账号提交的作业记录、图片、文件和消息负责。涉及设备控制或农业生产决策时，请结合现场情况复核，避免仅依赖自动化结果。' },
			{ title: '四、服务变更', content: '我们可能因维护、安全或产品升级调整服务，并在合理范围内提供提示。网络、终端或第三方服务异常可能造成暂时不可用。' },
			{ title: '五、联系我们', content: '如对本协议或账号使用有疑问，请通过应用内“帮助与反馈”联系系统运营方。' }
		]
	},
	privacy: {
		title: '隐私政策',
		sections: [
			{ title: '一、收集的信息', content: '为完成注册、登录和业务协作，我们可能处理账号、手机号、昵称与角色信息，以及您主动提交的农场、地块、作业、图片、文件和咨询消息。为保障运行安全，还可能记录必要的设备、网络和操作日志。' },
			{ title: '二、使用目的', content: '相关信息仅用于身份认证、权限控制、智慧农业功能交付、故障排查、安全审计和服务改进，不用于与上述目的无关的处理。' },
			{ title: '三、权限与第三方', content: '拍照、相册和文件权限仅在您主动使用对应功能时申请。未在登录页展示的第三方登录能力不会收集相应平台账号信息。' },
			{ title: '四、保存与保护', content: '我们在实现业务目的所需期限内保存信息，并采取访问控制、传输保护和日志审计等措施。请避免在咨询消息中提交无关的敏感个人信息。' },
			{ title: '五、您的权利', content: '您可以在个人中心查看或修改可编辑资料，并可通过应用内“帮助与反馈”申请更正、删除相关信息或注销账号。' }
		]
	}
}

export default {
	data() { return { type: 'user' } },
	computed: { document() { return DOCUMENTS[this.type] || DOCUMENTS.user } },
	onLoad(options) { this.type = options.type === 'privacy' ? 'privacy' : 'user' },
	methods: { goBack() { uni.navigateBack() } }
}
</script>

<style>
@import url("@/static/iconfont/iconfont.css");
page { background: #ffffff; }
.agreement-page { position: fixed; inset: 0; background: #ffffff; color: #26332f; }
.navbar { display: flex; align-items: center; justify-content: space-between; box-sizing: border-box; height: calc(var(--status-bar-height) + 104rpx); padding: var(--status-bar-height) 28rpx 0; border-bottom: 1rpx solid #e7ecea; }
.back-button, .nav-placeholder { width: 88rpx; height: 88rpx; }
.back-button { display: flex; align-items: center; justify-content: center; margin: 0; padding: 0; color: #26332f; background: transparent; font-size: 34rpx; }
.back-button::after { border: 0; }
.nav-title { font-size: 34rpx; font-weight: 600; }
.document { box-sizing: border-box; height: calc(100% - var(--status-bar-height) - 104rpx); padding: 36rpx 40rpx calc(48rpx + env(safe-area-inset-bottom)); }
.updated { display: block; margin-bottom: 36rpx; color: #66736f; font-size: 24rpx; line-height: 1.5; }
.section { margin-bottom: 36rpx; }
.section-title { display: block; margin-bottom: 14rpx; font-size: 30rpx; font-weight: 600; line-height: 1.5; }
.section-content { display: block; color: #46534f; font-size: 28rpx; line-height: 1.75; word-break: break-word; }
@media screen and (min-width: 768px) { .agreement-page { width: 750rpx; margin: 0 auto; } }
</style>
