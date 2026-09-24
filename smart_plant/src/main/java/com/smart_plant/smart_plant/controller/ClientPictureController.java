package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientPictureBatchRequest;
import com.smart_plant.smart_plant.dto.ClientPictureDownload;
import com.smart_plant.smart_plant.dto.ClientPictureListResponse;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ClientPictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * farm 用户端图片列表接口。
 *
 * <p>接口不接收 userId，后端统一从登录 token 中识别当前用户，
 * 只返回该用户自己的手机图片和摄像头采集图片。</p>
 */
@RestController
@RequestMapping({"/client", "/api/client"})
@RequiredArgsConstructor
public class ClientPictureController {

    /** 用户端图片服务，负责图片来源聚合和权限过滤。 */
    private final ClientPictureService clientPictureService;

    @GetMapping("/pictures/list")
    public R<ClientPictureListResponse> listCurrentClientPictures() {
        // 返回当前用户的手机图片和摄像头图片，供 farm 端图片管理页渲染。
        return R.success(clientPictureService.listCurrentClientPictures());
    }

    @GetMapping("/pictures/phone/list")
    public R<List<ClientPictureListResponse.PictureItem>> listCurrentClientPhonePictures() {
        // 单独返回当前用户的手机图片，用于 farm 端“手机图片”详情列表页。
        return R.success(clientPictureService.listCurrentClientPhonePictures());
    }

    @GetMapping("/pictures/camera/list")
    public R<List<ClientPictureListResponse.PictureItem>> listCurrentClientCameraPictures() {
        // 单独返回当前用户的摄像头采集图片，用于 farm 端“摄像头图片”详情列表页。
        return R.success(clientPictureService.listCurrentClientCameraPictures());
    }

    @PostMapping(value = "/pictures/phone/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ClientPictureListResponse.PictureItem> uploadCurrentClientPhonePicture(
            @RequestParam("image") MultipartFile image) {
        // 上传用户由登录令牌确定，接口不接收 userId，防止代替其他用户上传图片。
        return R.success(clientPictureService.uploadCurrentClientPhonePicture(image));
    }

    @PostMapping("/pictures/batch-delete")
    public R<Integer> deleteCurrentClientPictures(@RequestBody ClientPictureBatchRequest request) {
        // 混合图片批量删除：后端按 sourceType 分别处理手机图片和摄像头图片，并校验当前用户权限。
        return R.success(clientPictureService.deleteCurrentClientPictures(request == null ? null : request.getPictures()));
    }

    @PostMapping("/pictures/downloads")
    public R<List<ClientPictureListResponse.PictureItem>> prepareCurrentClientPictureDownloads(@RequestBody ClientPictureBatchRequest request) {
        // 下载前返回已授权图片地址，真正写入相册由 farm 端调用系统下载/保存能力完成。
        return R.success(clientPictureService.prepareCurrentClientPictureDownloads(request == null ? null : request.getPictures()));
    }

    @GetMapping("/pictures/{sourceType}/{id}/download")
    public ResponseEntity<byte[]> downloadCurrentClientPicture(@PathVariable String sourceType, @PathVariable Long id) {
        // 单张下载接口：后端根据 sourceType + id 校验当前用户权限后返回图片二进制。
        ClientPictureDownload download = clientPictureService.downloadCurrentClientPicture(
                new ClientPictureBatchRequest.PictureRef(id, sourceType)
        );
        return buildDownloadResponse(download);
    }

    @PostMapping("/pictures/batch-download")
    public ResponseEntity<byte[]> downloadCurrentClientPictures(@RequestBody ClientPictureBatchRequest request) {
        // 多张下载接口：后端先逐张校验归属，再将图片打包成 zip 返回。
        ClientPictureDownload download = clientPictureService.downloadCurrentClientPictures(
                request == null ? null : request.getPictures()
        );
        return buildDownloadResponse(download);
    }

    /** 构建附件下载响应，统一处理文件名编码、Content-Type 和二进制长度。 */
    private ResponseEntity<byte[]> buildDownloadResponse(ClientPictureDownload download) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.bytes().length)
                .body(download.bytes());
    }
}
