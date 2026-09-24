package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import org.springframework.web.multipart.MultipartFile;

/** Stores images used by management forms and returns a durable public resource path. */
public interface ManagedImageService {

    CropImageUploadResult upload(MultipartFile file, String category);
}
