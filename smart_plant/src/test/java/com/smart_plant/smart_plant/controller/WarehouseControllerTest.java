package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.ManagedImageService;
import com.smart_plant.smart_plant.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证仓库控制器通过仓库业务类型完成物资图片上传。 */
class WarehouseControllerTest {

    @Test
    void uploadsItemImageThroughWarehouseCategory() {
        WarehouseService warehouseService = mock(WarehouseService.class);
        ManagedImageService imageService = mock(ManagedImageService.class);
        WarehouseController controller = new WarehouseController(warehouseService, imageService);
        MockMultipartFile image = new MockMultipartFile(
                "file", "device.png", "image/png", new byte[]{1, 2, 3});
        CropImageUploadResult uploaded = new CropImageUploadResult(
                "/uploads/warehouse-images/device.png", 3L);
        when(imageService.upload(image, "warehouse")).thenReturn(uploaded);

        R<CropImageUploadResult> result = controller.uploadItemImage(image);

        assertEquals(uploaded, result.getData());
    }
}
