package com.smart_plant.smart_plant.dto;

/** 聊天附件上传结果；mediaUrl 原样用于发送消息，fileName 用于文件气泡。 */
public record ConsultAttachmentResponse(String mediaUrl, String fileName, long size) {
}
