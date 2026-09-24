export function filterDeviceGroups(groups, keyword = '', plotId = null) {
	const normalizedKeyword = String(keyword || '').trim().toLowerCase()
	return (Array.isArray(groups) ? groups : []).map((group) => ({
		...group,
		devices: (Array.isArray(group.devices) ? group.devices : []).filter((device) => {
			const matchesPlot = plotId === null || plotId === undefined || plotId === ''
				|| String(device.plotId) === String(plotId)
			const searchableText = `${device.cropName || ''} ${device.plotName || ''}`.toLowerCase()
			return matchesPlot && (!normalizedKeyword || searchableText.includes(normalizedKeyword))
		})
	}))
}
