const assert = require('node:assert/strict')
const fs = require('node:fs')

const api = fs.readFileSync('api/modelList.js', 'utf8')

assert.match(api, /const params = \{ pageSize \}/)
assert.match(api, /beforeId != null && beforeId !== ''/)
assert.match(api, /ai-chat\/history', params/)
assert.doesNotMatch(api, /ai-chat\/history', \{ beforeId, pageSize \}/)
