package com.smart_plant.smart_plant.dto;

/**
 * 农场图片上传结果。
 *
 * @param url  后端 /uploads 相对访问路径
 * @param size 文件大小，单位字节
 */
public record FarmImageUploadResult(String url, Long size) {
}
