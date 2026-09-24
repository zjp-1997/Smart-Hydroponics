const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')
const { parse } = require('@vue/compiler-sfc')

test('登录字段具有可见名称、错误关联和清晰键盘焦点', () => {
  const source = readFileSync('src/views/LoginView.vue', 'utf8')
  const descriptor = parse(source).descriptor
  const template = descriptor.template.content
  const style = descriptor.styles.map((item) => item.content).join('\n')

  for (const [field, label] of [['username', '账号'], ['password', '密码'], ['captcha', '验证码']]) {
    assert.match(template, new RegExp(`label="${label}" prop="${field}"`))
    assert.match(template, new RegExp(`id="login-${field}"`))
    assert.match(template, new RegExp(`login-${field}-error`))
    assert.match(template, new RegExp(`fieldErrors\\.${field}`))
  }
  assert.match(template, /aria-label="passwordVisible \? '隐藏密码' : '显示密码'"/)
  assert.match(style, /:focus-visible/)
  assert.match(style, /\.el-input__wrapper\.is-focus/)
})
