package com.smart_plant.smart_plant.dto;

/** 系统Logo、网站图标或登录背景上传结果。 */
public record SystemAssetUploadResponse(String type, String url, Long size) {
}
