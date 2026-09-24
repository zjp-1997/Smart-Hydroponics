package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.CropImage;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.CameraImageMapper;
import com.smart_plant.smart_plant.mapper.CropImageMapper;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientPictureServiceImplTest {

    private final CropImageMapper cropImageMapper = mock(CropImageMapper.class);
    private final CameraImageMapper cameraImageMapper = mock(CameraImageMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final CropImageService cropImageService = mock(CropImageService.class);
    private final ClientPictureServiceImpl service = new ClientPictureServiceImpl(
            cropImageMapper,
            cameraImageMapper,
            dataPermissionService,
            cropImageService
    );

    @Test
    void uploadPhonePictureAlwaysUsesCurrentUser() {
        MultipartFile image = mock(MultipartFile.class);
        User currentUser = new User();
        currentUser.setId(18L);
        LocalDateTime createTime = LocalDateTime.of(2026, 9, 11, 10, 30);

        when(dataPermissionService.currentUser()).thenReturn(currentUser);
        when(cropImageService.uploadCropImage(image))
                .thenReturn(new CropImageUploadResult("/uploads/phone-images/test.jpg", 1024L));
        when(cropImageService.addCropImage(any(CropImage.class))).thenAnswer(invocation -> {
            CropImage savedImage = invocation.getArgument(0);
            savedImage.setId(7L);
            savedImage.setCreateTime(createTime);
            return savedImage;
        });

        var result = service.uploadCurrentClientPhonePicture(image);

        assertEquals(7L, result.getId());
        assertEquals("phone", result.getSourceType());
        assertEquals("/uploads/phone-images/test.jpg", result.getImageUrl());
        assertEquals(createTime, result.getImageTime());
        verify(cropImageService).addCropImage(argThat(savedImage ->
                Long.valueOf(18L).equals(savedImage.getUserId())
                        && "/uploads/phone-images/test.jpg".equals(savedImage.getImageUrl())
                        && Long.valueOf(1024L).equals(savedImage.getImageSize())
        ));
    }
}
