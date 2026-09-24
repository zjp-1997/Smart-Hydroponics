const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')

const page = readFileSync('pages/service/disease_control.vue', 'utf8')
const strategyPage = readFileSync('pages/service/strategy.vue', 'utf8')
const api = readFileSync('api/diseasePestList.js', 'utf8')
const pagesConfig = readFileSync('pages.json', 'utf8')
const packageConfig = JSON.parse(readFileSync('package.json', 'utf8'))

// 顶部入口必须使用指定扫码图标，并把病虫害综合识别编码传给智能策略页。
assert.match(page, /iconfont icon-saoma recognition-link/)
assert.match(page, /icon-tianjia1 disease-action-icon[\s\S]*?icon-saoma recognition-link/)
assert.match(page, /recognitionTypeCode=DISEASE_PEST_RECOGNITION/)
assert.match(strategyPage, /type\.typeCode === this\.preferredRecognitionTypeCode/)

// 搜索必须通过 smart_plant 接口查询数据库，而不是过滤页面静态数组。
assert.match(api, /disease-pest\/list/)
assert.match(api, /Array\.isArray\(data\.list\)/)
assert.match(api, /resolveFileUrl\(item\.coverImage\)/)
assert.match(api, /client\/disease-pests/)
assert.match(api, /getDiseaseCropTypes/)
assert.match(page, /getDiseasePests\(name\)/)
assert.doesNotMatch(page, /filteredDiseases|胡豆赤斑病害/)

// 搜索成功后按钮切换为取消，取消时清空关键词并重新加载完整列表。
assert.match(page, /isSearchActive \? '取消' : '搜索'/)
assert.match(page, /if \(this\.isSearchActive\)[\s\S]*?this\.keyword = ''[\s\S]*?await this\.loadDiseases\(\)/)
assert.match(page, /if \(await this\.loadDiseases\(name\)\)[\s\S]*?this\.isSearchActive = true/)

// 卡片继续沿用原 UI，只把展示值替换为数据库图片、名称和症状字段。
assert.match(page, /:src="item\.image \|\| fallbackImage"/)
assert.match(page, /\{\{ item\.name \}\}/)
assert.match(page, /\{\{ item\.symptom \|\| '暂无症状描述' \}\}/)

// 内容区提供加载占位、结果数量和空结果反馈，顶部导航结构保持原样。
assert.match(page, /v-if="loading" class="disease-grid"/)
assert.match(page, /class="result-summary"/)
assert.match(page, /未找到相关病虫害/)
assert.match(page, /class="disease-symptom"/)

// 新增图标打开上报弹框，表单包含需求字段并通过 multipart 接口提交。
assert.match(page, /uploadDialogVisible[\s\S]*?上传病虫害信息/)
assert.match(page, /uploadForm\.name/)
assert.match(page, /uploadForm\.suitableStage/)
assert.match(page, /uploadForm\.symptom/)
assert.match(page, /uploadDiseasePest\(/)

// 弹框内选择项统一使用 uni-data-select，避免原生 picker 的底部弹层被上传弹框遮挡。
assert.doesNotMatch(page, /<picker\s+mode="selector"/)
assert.equal((page.match(/<uni-data-select/g) || []).length, 3)
assert.match(page, /:localdata="cropTypeOptions"/)
assert.match(page, /:localdata="diseaseTypeOptions"/)
assert.match(page, /:localdata="statusOptions"/)
assert.doesNotMatch(page, /activeDropdown|toggleDropdown|upload-dropdown-menu/)

// 小屏手机上表单区独立滚动，内容末尾与底部操作栏保持间距，避免症状描述被按钮遮挡。
assert.match(page, /class="disease-upload-form"/)
assert.match(page, /\.disease-upload-body\s*\{[^}]*flex:\s*1 1 0;[^}]*height:\s*0;/s)
assert.match(page, /\.disease-upload-dialog\s*\{[^}]*height:\s*100%;[^}]*max-height:\s*100%;[^}]*overflow:\s*hidden;/s)
assert.match(page, /safe-area-inset-bottom/)

// npm 版本的 uni-ui 必须通过 easycom 自动注册，否则 H5 运行时无法解析 uni-data-select。
assert.equal(packageConfig.dependencies['@dcloudio/uni-ui'], '^1.5.12')
assert.match(pagesConfig, /"\^uni-\(\.\*\)":\s*"@dcloudio\/uni-ui\/lib\/uni-\$1\/uni-\$1\.vue"/)
