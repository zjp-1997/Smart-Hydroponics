import { get, getBaseUrl, post, resolveFileUrl, uploadFile } from '@/utils/request.js'
import { getToken } from '@/utils/auth.js'

const DEFAULT_PHONE_COVER = '/static/m2.jpg'
const DEFAULT_CAMERA_COVER = '/static/m1.png'

function normalizePicture(image) {
	return {
		id: image.id,
		sourceType: image.sourceType,
		// key 用于混合图片列表稳定标识，避免手机图片和摄像头图片 id 相同导致选择状态冲突。
		key: `${image.sourceType}-${image.id}`,
		imageTime: image.imageTime || '',
		imageUrl: image.imageUrl ? resolveFileUrl(image.imageUrl) : ''
	}
}

function firstImageOrDefault(images, defaultCover) {
	const firstImage = images.find((image) => image.imageUrl)
	return firstImage ? firstImage.imageUrl : defaultCover
}

function normalizePictureList(data) {
	const phoneImages = Array.isArray(data.phoneImages) ? data.phoneImages.map(normalizePicture) : []
	const cameraImages = Array.isArray(data.cameraImages) ? data.cameraImages.map(normalizePicture) : []
	const pictures = Array.isArray(data.pictures)
		? data.pictures.map(normalizePicture)
		: [...phoneImages, ...cameraImages]
	const allImages = pictures
		.sort((left, right) => String(right.imageTime).localeCompare(String(left.imageTime)))

	return {
		// 当前 picture_list.vue 的照片网格只接收图片地址数组，所以这里保持 UI 数据结构不变。
		photos: allImages.map((image) => image.imageUrl).filter(Boolean),
		pictures: allImages,
		phoneImages,
		cameraImages,
		albums: [
			{
				id: 1,
				name: '手机图片',
				cover: firstImageOrDefault(phoneImages, DEFAULT_PHONE_COVER)
			},
			{
				id: 2,
				name: '摄像头图片',
				cover: firstImageOrDefault(cameraImages, DEFAULT_CAMERA_COVER)
			}
		]
	}
}

// 查询当前登录用户可见的图片列表。用户身份由 token 决定，前端不传 userId。
export function getPictureList() {
	return get('/smart_plant/client/pictures/list').then((data) => normalizePictureList(data || {}))
}

// 查询当前登录用户的手机图片列表，供“手机图片”独立页面使用。
export function getPhonePictureList() {
	return get('/smart_plant/client/pictures/phone/list').then((data) => {
		const pictures = Array.isArray(data) ? data.map(normalizePicture) : []
		return pictures.sort((left, right) => String(right.imageTime).localeCompare(String(left.imageTime)))
	})
}

// 查询当前登录用户的摄像头采集图片列表，供“摄像头图片”独立页面使用。
export function getCameraPictureList() {
	return get('/smart_plant/client/pictures/camera/list').then((data) => {
		const pictures = Array.isArray(data) ? data.map(normalizePicture) : []
		return pictures.sort((left, right) => String(right.imageTime).localeCompare(String(left.imageTime)))
	})
}

// 单张上传由通用上传器处理；页面循环调用即可兼容 uni-app 各端的多图选择结果。
export function uploadPhonePicture(filePath) {
	return uploadFile({
		url: '/smart_plant/client/pictures/phone/upload',
		filePath,
		name: 'image'
	}).then(normalizePicture)
}

function buildPictureRefs(pictures) {
	return (Array.isArray(pictures) ? pictures : [])
		.filter((picture) => picture && picture.id && picture.sourceType)
		.map((picture) => ({
			// 后端根据 sourceType + id 校验当前用户权限并路由到对应图片表。
			id: picture.id,
			sourceType: picture.sourceType
		}))
}

function buildApiUrl(url) {
	// uni.downloadFile 不经过 request.js 拦截器，这里单独补齐后端 baseUrl。
	return `${getBaseUrl()}${url.startsWith('/') ? url : `/${url}`}`
}

function buildAuthHeader() {
	const token = getToken()
	// 下载接口同样需要 Bearer Token，防止绕过列表接口直接下载他人图片。
	return token ? { Authorization: `Bearer ${token}` } : {}
}

function isBrowserDownloadSupported() {
	// #ifdef H5
	// 仅 H5 使用浏览器下载，App/小程序端继续调用系统相册保存能力。
	return typeof window !== 'undefined' && typeof document !== 'undefined' && typeof Blob !== 'undefined'
	// #endif
	// #ifndef H5
	return false
	// #endif
}

function getResponseHeader(headers, name) {
	if (!headers || !name) {
		return ''
	}
	const targetName = name.toLowerCase()
	const headerKey = Object.keys(headers).find((key) => key.toLowerCase() === targetName)
	return headerKey ? headers[headerKey] : ''
}

function decodeFileName(value) {
	if (!value) {
		return ''
	}
	try {
		return decodeURIComponent(value.replace(/^UTF-8''/i, '').replace(/^"|"$/g, ''))
	} catch (error) {
		return value.replace(/^"|"$/g, '')
	}
}

function parseDownloadFileName(contentDisposition, defaultFileName) {
	if (!contentDisposition) {
		return defaultFileName
	}
	const utf8Match = contentDisposition.match(/filename\*=([^;]+)/i)
	if (utf8Match && utf8Match[1]) {
		return decodeFileName(utf8Match[1].trim())
	}
	const normalMatch = contentDisposition.match(/filename=([^;]+)/i)
	if (normalMatch && normalMatch[1]) {
		return decodeFileName(normalMatch[1].trim())
	}
	return defaultFileName
}

function requestDownloadBuffer({ url, method = 'GET', data, defaultFileName }) {
	return new Promise((resolve, reject) => {
		uni.request({
			url,
			method,
			data,
			header: {
				'Content-Type': 'application/json',
				...buildAuthHeader()
			},
			responseType: 'arraybuffer',
			success(response) {
				if (response.statusCode < 200 || response.statusCode >= 300) {
					reject(new Error('图片下载失败'))
					return
				}
				const contentType = getResponseHeader(response.header, 'Content-Type') || 'application/octet-stream'
				const contentDisposition = getResponseHeader(response.header, 'Content-Disposition')
				resolve({
					buffer: response.data,
					contentType,
					fileName: parseDownloadFileName(contentDisposition, defaultFileName)
				})
			},
			fail: reject
		})
	})
}

function triggerBrowserDownload({ buffer, contentType, fileName }) {
	// H5 端无法保存到系统相册，因此使用浏览器标准下载能力保存文件。
	const blob = new Blob([buffer], { type: contentType })
	const objectUrl = window.URL.createObjectURL(blob)
	const link = document.createElement('a')
	link.href = objectUrl
	link.download = fileName
	link.style.display = 'none'
	document.body.appendChild(link)
	link.click()
	document.body.removeChild(link)
	window.URL.revokeObjectURL(objectUrl)
}

function defaultPictureFileName(picture) {
	const extension = picture.sourceType === 'camera' ? 'jpg' : 'jpg'
	return `${picture.sourceType || 'picture'}-${picture.id}.${extension}`
}

// 批量删除当前登录用户选中的图片，支持手机图片和摄像头图片混合删除。
export function deletePictures(pictures) {
	return post('/smart_plant/client/pictures/batch-delete', {
		pictures: buildPictureRefs(pictures)
	})
}

// 获取选中图片的可下载地址；实际保存到相册由页面调用 uni.downloadFile / uni.saveImageToPhotosAlbum 完成。
export function getPictureDownloadList(pictures) {
	return post('/smart_plant/client/pictures/downloads', {
		pictures: buildPictureRefs(pictures)
	}).then((data) => {
		const list = Array.isArray(data) ? data : []
		return list.map(normalizePicture).filter((picture) => picture.imageUrl)
	})
}

// 构建单张图片下载接口地址，页面无需关心后端路由规则。
export function buildPictureDownloadUrl(picture) {
	return buildApiUrl(
		`/smart_plant/client/pictures/${encodeURIComponent(picture.sourceType)}/${encodeURIComponent(picture.id)}/download`
	)
}

// 调用后端单张图片下载接口，返回 uni-app 可保存到相册的临时文件路径。
export function downloadPictureFile(picture) {
	return new Promise((resolve, reject) => {
		uni.downloadFile({
			url: buildPictureDownloadUrl(picture),
			header: buildAuthHeader(),
			success(downloadResult) {
				const statusCode = downloadResult.statusCode || 0
				if (statusCode >= 200 && statusCode < 300 && downloadResult.tempFilePath) {
					resolve(downloadResult.tempFilePath)
					return
				}
				reject(new Error('图片下载失败'))
			},
			fail: reject
		})
	})
}

function saveImageToAlbum(filePath) {
	return new Promise((resolve, reject) => {
		if (typeof uni.saveImageToPhotosAlbum !== 'function') {
			reject(new Error('当前运行环境不支持保存到系统相册'))
			return
		}
		uni.saveImageToPhotosAlbum({
			filePath,
			success: resolve,
			fail: reject
		})
	})
}

// 多选下载时逐张调用后端受控下载接口并保存到系统相册，兼容手机图片和摄像头图片。
export async function downloadPicturesToAlbum(pictures) {
	const pictureRefs = buildPictureRefs(pictures)
	if (isBrowserDownloadSupported()) {
		if (pictureRefs.length === 1) {
			const picture = pictureRefs[0]
			const result = await requestDownloadBuffer({
				url: buildPictureDownloadUrl(picture),
				defaultFileName: defaultPictureFileName(picture)
			})
			triggerBrowserDownload(result)
			return pictureRefs.length
		}
		const result = await requestDownloadBuffer({
			url: buildApiUrl('/smart_plant/client/pictures/batch-download'),
			method: 'POST',
			data: {
				pictures: pictureRefs
			},
			defaultFileName: 'farm-pictures.zip'
		})
		triggerBrowserDownload(result)
		return pictureRefs.length
	}
	for (const picture of pictureRefs) {
		const tempFilePath = await downloadPictureFile(picture)
		await saveImageToAlbum(tempFilePath)
	}
	return pictureRefs.length
}

// 调用后端批量下载接口获取 zip 二进制，适合 H5/管理端按压缩包保存的场景。
export function downloadPictureBatchFile(pictures) {
	return requestDownloadBuffer({
		url: buildApiUrl('/smart_plant/client/pictures/batch-download'),
		method: 'POST',
		data: {
			pictures: buildPictureRefs(pictures)
		},
		defaultFileName: 'farm-pictures.zip'
	}).then((result) => {
		if (isBrowserDownloadSupported()) {
			triggerBrowserDownload(result)
			return result.buffer
		}
		return result.buffer
	})
}
