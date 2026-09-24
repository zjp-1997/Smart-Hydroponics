package com.smart_plant.smart_plant.dto;

/**
 * 专家资料图片上传结果。
 *
 * <p>返回可被管理端和 farm 用户端共同访问的 /uploads 相对路径，避免把浏览器本地缓存 key 写入数据库。</p>
 */
public record ExpertAssetUploadResult(String url, Long size) {
}
