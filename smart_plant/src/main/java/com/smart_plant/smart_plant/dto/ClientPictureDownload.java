package com.smart_plant.smart_plant.dto;

/**
 * farm 用户端图片下载结果。
 *
 * <p>单张图片下载时 contentType 是 image/*，多张图片打包下载时 contentType 是 application/zip。</p>
 */
public record ClientPictureDownload(byte[] bytes, String fileName, String contentType) {
}
