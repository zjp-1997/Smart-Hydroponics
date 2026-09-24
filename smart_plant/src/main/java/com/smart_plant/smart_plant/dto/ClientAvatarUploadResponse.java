package com.smart_plant.smart_plant.dto;

/** farm 头像上传结果，url 保存到 user.avatar，size 用于客户端展示或审计。 */
public record ClientAvatarUploadResponse(String url, long size) {
}
