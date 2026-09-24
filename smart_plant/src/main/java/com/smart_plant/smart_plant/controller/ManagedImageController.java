package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.security.RequirePermission;
import com.smart_plant.smart_plant.service.ManagedImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Shared image-upload endpoint for management forms that persist an image URL. */
@RestController
@RequestMapping("/managed-image")
@RequiredArgsConstructor
public class ManagedImageController {

    private final ManagedImageService managedImageService;

    @PostMapping("/upload")
    @RequirePermission({
            "crop:add", "crop:update", "disease_pest:manage", "user:manage",
            "user_oauth:manage", "planting_batch:manage", "warehouse:manage"
    })
    public R<CropImageUploadResult> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam String category) {
        return R.success(managedImageService.upload(file, category));
    }
}
