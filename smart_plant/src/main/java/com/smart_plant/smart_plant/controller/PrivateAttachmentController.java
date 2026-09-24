package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.service.impl.ConsultAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Path;

/** 私有聊天附件下载入口；JWT 校验由全局拦截器处理，服务层核验资源归属。 */
@RestController
@RequestMapping("/uploads/consult-chat")
@RequiredArgsConstructor
public class PrivateAttachmentController {
    private final ConsultAttachmentService attachmentService;

    @GetMapping("/{ownerUserId}/{kind}/{fileName:.+}")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long ownerUserId,
            @PathVariable String kind, @PathVariable String fileName) throws IOException {
        Path path = attachmentService.resolveForDownload(ownerUserId, kind, fileName);
        FileSystemResource resource = new FileSystemResource(path);
        MediaType mediaType = MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ("image".equals(kind)
                ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
