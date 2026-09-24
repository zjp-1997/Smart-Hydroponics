package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CameraImageDownload;
import com.smart_plant.smart_plant.entity.CameraImage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CameraImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/camera-image")
@RequiredArgsConstructor
@RequirePermission("camera_image:manage")
public class CameraImageController {

    private final CameraImageService cameraImageService;

    @DeleteMapping("/{id}")
    public R<Void> deleteCameraImage(@PathVariable Long id) {
        cameraImageService.deleteCameraImage(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteCameraImages(@RequestBody List<Long> ids) {
        return R.success(cameraImageService.deleteCameraImages(ids));
    }

    @GetMapping("/{id}")
    public R<CameraImage> getCameraImageById(@PathVariable Long id) {
        return R.success(cameraImageService.getCameraImageById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCameraImage(@PathVariable Long id) {
        CameraImageDownload download = cameraImageService.downloadCameraImage(id);
        MediaType mediaType = MediaType.parseMediaType(download.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(download.fileName(), java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(download.bytes());
    }

    @GetMapping("/list")
    public R<PageInfo<CameraImage>> listCameraImages(@RequestParam(required = false) Long cameraId,
                                                     @RequestParam(required = false) Long plotId,
                                                     @RequestParam(required = false) String cameraName,
                                                     @RequestParam(required = false) String plotName,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate startDate,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                     LocalDate endDate,
                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(cameraImageService.listCameraImages(
                cameraId, plotId, cameraName, plotName, startDate, endDate, pageNum, pageSize));
    }
}
