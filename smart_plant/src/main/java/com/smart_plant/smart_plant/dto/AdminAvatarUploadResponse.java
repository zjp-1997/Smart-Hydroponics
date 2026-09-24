package com.smart_plant.smart_plant.dto;

/** 个人头像上传结果，URL 可直接保存到 user.avatar。 */
public record AdminAvatarUploadResponse(String url, Long size) {
}
