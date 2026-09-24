<template>
	<image
		:src="displaySrc"
		:mode="mode"
		:alt="alt"
		:lazy-load="lazyLoad"
		:fade-show="false"
		@error="handleImageError"
	></image>
</template>

<script>
import { downloadProtectedFile } from '@/utils/request.js'

const imageCache = new Map()

function loadDisplayImage(src) {
	if (!src || !/^https?:\/\//i.test(src)) return Promise.resolve(src || '')
	if (!imageCache.has(src)) {
		imageCache.set(src, downloadProtectedFile(src).catch((error) => {
			imageCache.delete(src)
			throw error
		}))
	}
	return imageCache.get(src)
}

export default {
	name: 'ProtectedImage',
	emits: ['error'],
	props: {
		src: { type: String, default: '' },
		mode: { type: String, default: 'scaleToFill' },
		alt: { type: String, default: '' },
		lazyLoad: { type: Boolean, default: false }
	},
	data() {
		return { displaySrc: '' }
	},
	watch: {
		src: {
			immediate: true,
			handler(src) {
				const expectedSrc = src || ''
				this.displaySrc = expectedSrc
				// #ifdef APP-PLUS
				if (/^https?:\/\//i.test(expectedSrc)) {
					this.displaySrc = ''
					loadDisplayImage(expectedSrc)
						.then((localPath) => {
							if (this.src === expectedSrc) this.displaySrc = localPath
						})
						.catch((error) => {
							if (this.src === expectedSrc) this.$emit('error', error)
						})
				}
				// #endif
			}
		}
	},
	methods: {
		handleImageError(error) {
			this.$emit('error', error)
		}
	}
}
</script>
