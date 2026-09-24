package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证管理端仓库图片会保存到独立、安全的业务目录。 */
class ManagedImageServiceImplTest {

    /** JUnit 临时目录确保测试不会向项目 uploads 目录写入文件。 */
    @TempDir
    Path tempDirectory;

    @Test
    void uploadsWarehouseImageIntoDedicatedDirectory() {
        String originalUserDirectory = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDirectory.toString());
        try {
            MockMultipartFile image = new MockMultipartFile(
                    "file", "device.png", "image/png", new byte[]{1, 2, 3});

            CropImageUploadResult result = new ManagedImageServiceImpl().upload(image, "warehouse");

            assertTrue(result.imageUrl().startsWith("/uploads/warehouse-images/"));
            Path storedFile = tempDirectory.resolve(result.imageUrl().substring(1));
            assertTrue(Files.exists(storedFile));
            assertEquals(3L, result.imageSize());
        } finally {
            // 恢复进程工作目录，避免影响同一 JVM 中的其他测试。
            System.setProperty("user.dir", originalUserDirectory);
        }
    }
}
