package com.smart_plant.smart_plant.controller;

import com.github.pagehelper.PageInfo;
import com.smart_plant.smart_plant.dto.CropImageDownload;
import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.CropImage;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.CropImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/crop-image")
@RequiredArgsConstructor
@RequirePermission("crop_image:manage")
public class CropImageController {

    private final CropImageService cropImageService;

    @PostMapping("/add")
    public R<CropImage> addCropImage(@RequestBody CropImage cropImage) {
        return R.success(cropImageService.addCropImage(cropImage));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteCropImage(@PathVariable Long id) {
        cropImageService.deleteCropImage(id);
        return R.success();
    }

    @DeleteMapping("/batch")
    public R<Integer> deleteCropImages(@RequestBody List<Long> ids) {
        return R.success(cropImageService.deleteCropImages(ids));
    }

    @PutMapping
    public R<CropImage> updateCropImage(@RequestBody CropImage cropImage) {
        return R.success(cropImageService.updateCropImage(cropImage));
    }

    @GetMapping("/{id}")
    public R<CropImage> getCropImageById(@PathVariable Long id) {
        return R.success(cropImageService.getCropImageById(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadCropImage(@PathVariable Long id) {
        CropImageDownload download = cropImageService.downloadCropImage(id);
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

    @PostMapping("/upload")
    public R<CropImageUploadResult> uploadCropImage(@RequestParam("file") MultipartFile file) {
        return R.success(cropImageService.uploadCropImage(file));
    }

    @GetMapping("/list")
    public R<PageInfo<CropImage>> listCropImages(@RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate startDate,
                                                 @RequestParam(required = false)
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                 LocalDate endDate,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.success(cropImageService.listCropImages(
                userId, startDate, endDate, pageNum, pageSize));
    }
}
