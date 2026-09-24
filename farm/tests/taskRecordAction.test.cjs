const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/service/task_record.vue', 'utf8')

// 截止时间文案保持相邻，右侧按钮随任务状态执行“开始/完成”动作。
assert.match(page, /class="deadline-info"[\s\S]*?截止时间[\s\S]*?task\.deadlineText/)
assert.doesNotMatch(page, /\.deadline-row[\s\S]{0,160}?justify-content:\s*space-between/)
assert.match(page, /v-if="task\.canExecute \|\| task\.canComplete"[\s\S]*?@tap="handlePrimaryAction"/)
assert.match(page, /actionText\(\)[\s\S]*?完成任务/)
assert.match(page, /executeFarmTask\([\s\S]*?applyTaskUpdate\(task\)/)
assert.match(page, /\.execute-button\s*\{[\s\S]*?height:\s*88rpx/)

// 完成任务必须先上传凭证，确认后调用完成接口并同步刷新顶部快照和操作记录。
assert.match(page, /v-if="completionVisible"[\s\S]*?请上传一张现场图片作为完成凭证/)
assert.match(page, /chooseCompletionImage\(\)[\s\S]*?uni\.chooseImage/)
assert.match(page, /请先上传完成图片[\s\S]*?uploadFarmTaskCompletionImage\([\s\S]*?completeFarmTask\(/)
assert.match(page, /completeFarmTask\([\s\S]*?await this\.applyTaskUpdate\(task\)/)
assert.match(page, /\.completion-confirm[\s\S]*?height:\s*88rpx/)

// 任务摘要与底部操作区分层，长标题不会再被状态标签挤压。
assert.match(page, /class="task-summary"[\s\S]*?class="task-title-row"[\s\S]*?class="task-tags"[\s\S]*?class="status-badge"/)
assert.match(page, /class="task-summary"[\s\S]*?<\/view>\s*<view class="deadline-row">/)
assert.match(page, /\.deadline-row\s*\{[\s\S]*?border-top:/)

// 摘要卡不重复展示冗长任务详情，按钮缩小视觉尺寸但保留 88rpx 触控高度。
assert.doesNotMatch(page, /v-if="task\.taskContent" class="task-description"/)
assert.doesNotMatch(page, /\.task-description\s*\{/)
assert.match(page, /\.execute-button-label\s*\{[\s\S]*?height:\s*72rpx/)
