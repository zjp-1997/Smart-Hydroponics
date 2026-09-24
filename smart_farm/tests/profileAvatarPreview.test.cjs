const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { test } = require('node:test')

test('个人中心通过JWT下载受保护头像并释放本地预览地址', () => {
  const api = readFileSync('src/api/auth.ts', 'utf8')
  const profile = readFileSync('src/components/ProfileDialog.vue', 'utf8')

  assert.match(api, /downloadAdminAvatar[\s\S]*responseType:\s*'blob'/)
  assert.match(profile, /await downloadAdminAvatar\(profileForm\.avatar\)/)
  assert.match(profile, /URL\.createObjectURL\(file\)/)
  assert.match(profile, /URL\.revokeObjectURL\(avatarUrl\.value\)/)
  assert.match(profile, /setAvatarPreview\(options\.file\)|setAvatarPreview\(file\)/)
})

test('顶部用户栏在资料更新后加载并显示受保护头像', () => {
  const header = readFileSync('src/components/Header.vue', 'utf8')

  assert.match(header, /await downloadAdminAvatar\(avatar\)/)
  assert.match(header, /subscribeCurrentUser\([\s\S]*loadHeaderAvatar\(user\?\.avatar\)/)
  assert.match(header, /<img v-if="headerAvatarUrl"/)
  assert.match(header, /URL\.revokeObjectURL\(headerAvatarUrl\.value\)/)
})
