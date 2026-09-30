const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { join } = require('node:path')

const farmRoot = join(__dirname, '..')
const pages = [
	['pages/login/pwd_index.vue', /<form class="form"[\s\S]*:password="!passwordVisible"[\s\S]*<\/form>/],
	['pages/register/index.vue', /<form class="form"[\s\S]*name="register-password"[\s\S]*name="register-confirm-password"[\s\S]*<\/form>/],
	['pages/secondPage/account_setting/index.vue', /<form class="phone-dialog password-dialog"[\s\S]*name="password-reset-new-password"[\s\S]*name="password-reset-confirm-password"[\s\S]*<\/form>/]
]

for (const [file, formPattern] of pages) {
	assert.match(readFileSync(join(farmRoot, file), 'utf8'), formPattern, `${file} 的密码输入框必须位于 form 中`)
}
