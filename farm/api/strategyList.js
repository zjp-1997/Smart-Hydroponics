import { get, getBaseUrl, resolveFileUrl } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

const SUCCESS_CODE = 200
const RECOGNITION_RESULT_CACHE_KEY = 'farm_manual_recognition_result'

function normalizeRecognitionType(type) {
	return {
		// id 是提交识别时传给后端的稳定主键，typeName 只用于页面展示。
		id: type.id,
		typeCode: type.typeCode || '',
		typeName: type.typeName || '',
		description: type.description || ''
	}
}

function normalizeRecognitionResult(result) {
	const precautions = Array.isArray(result.precautions)
		? result.precautions
		: String(result.precautions || '')
			.split(/\r?\n/)
			.map((item) => item.trim())
			.filter(Boolean)

	return {
		// 后端返回相对路径时在这里补齐域名，页面只负责渲染可访问的图片地址。
		recordId: result.recordId,
		resultId: result.resultId,
		sourceType: result.sourceType,
		recognitionType: result.recognitionType || '',
		recognitionImage: resolveFileUrl(result.recognitionImage),
		recognitionResult: result.recognitionResult || '',
		resultIntroduction: result.resultIntroduction || '',
		precautions
	}
}

function normalizeUploadResponse(rawData) {
	const response = typeof rawData === 'string' ? JSON.parse(rawData) : rawData
	if (!response || response.code !== SUCCESS_CODE) {
		throw {
			message: (response && response.message) || '识别失败，请稍后重试',
			response
		}
	}
	return normalizeRecognitionResult(response.data || {})
}

function buildUploadUrl(url) {
	const normalizedUrl = url.startsWith('/') ? url : `/${url}`
	return `${getBaseUrl()}${normalizedUrl}`
}

// 查询后端已启用的识别类型，删除页面静态类型列表后统一从接口驱动下拉选项。
export function getRecognitionTypes() {
	return get('/smart_plant/client/recognition-types/list').then((data) => {
		const types = Array.isArray(data) ? data : []
		return types.map(normalizeRecognitionType).filter((type) => type.id && type.typeName)
	})
}

// 发起手工识别：使用 multipart 同时上传 recognitionType 和 image 字段。
export function recognizeByManualUpload({ recognitionType, imagePath }) {
	return new Promise((resolve, reject) => {
		const token = getToken()
		const header = token ? { Authorization: `Bearer ${token}` } : {}

		uni.uploadFile({
			url: buildUploadUrl('/smart_plant/client/recognitions/manual'),
			filePath: imagePath,
			name: 'image',
			header,
			formData: {
				recognitionType
			},
			success: (res) => {
				try {
					resolve(normalizeUploadResponse(res.data))
				} catch (error) {
					uni.showToast({
						title: error.message || '识别失败，请稍后重试',
						icon: 'none'
					})
					reject(error)
				}
			},
			fail: (error) => {
				const requestError = {
					message: '网络异常，请检查连接',
					error
				}
				uni.showToast({
					title: requestError.message,
					icon: 'none'
				})
				reject(requestError)
			}
		})
	})
}

// 将识别结果临时放入本地缓存，结果页按 recordId 读取，避免长文本和图片地址塞进页面 URL。
export function cacheRecognitionResult(result) {
	uni.setStorageSync(RECOGNITION_RESULT_CACHE_KEY, result)
}

// 读取最近一次识别结果；recordId 不匹配时返回空，防止结果页展示旧缓存。
export function getCachedRecognitionResult(recordId) {
	const result = uni.getStorageSync(RECOGNITION_RESULT_CACHE_KEY)
	if (!result) {
		return null
	}
	if (recordId && String(result.recordId) !== String(recordId)) {
		return null
	}
	// 读取缓存时重新规范化图片地址，使旧版本缓存中的后端 IP 自动迁移。
	return normalizeRecognitionResult(result)
}

function formatRecognitionTime(time) {
	// 后端 LocalDateTime 可能返回 yyyy-MM-ddTHH:mm:ss，这里统一转成页面原本展示的空格格式。
	return String(time || '').replace('T', ' ')
}

function normalizeRecognitionRecord(record) {
	return {
		// record.vue 原本使用 id/type/result/time 字段，这里保持字段形状，避免调整页面 UI。
		id: record.recordId,
		recordId: record.recordId,
		resultId: record.resultId,
		sourceType: record.sourceType,
		image: resolveFileUrl(record.recognitionImage),
		type: record.recognitionType || '-',
		result: record.recognitionResult || '-',
		time: formatRecognitionTime(record.recognitionTime),
		recognitionType: record.recognitionType || '',
		recognitionImage: resolveFileUrl(record.recognitionImage),
		recognitionResult: record.recognitionResult || '',
		resultIntroduction: record.resultIntroduction || '',
		precautions: Array.isArray(record.precautions) ? record.precautions : []
	}
}

// 查询当前登录用户的手工识别记录列表，列表字段统一在这里清洗后交给 record.vue 展示。
export function getRecognitionRecords(params = {}) {
	return get('/smart_plant/client/recognitions/records', {
		pageNum: params.pageNum || 1,
		pageSize: params.pageSize || 50
	}).then((data) => {
		const records = Array.isArray(data) ? data : []
		return records.map(normalizeRecognitionRecord)
	})
}
