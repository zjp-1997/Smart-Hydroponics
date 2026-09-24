package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.Crop;
import com.smart_plant.smart_plant.mapper.CropMapper;
import com.smart_plant.smart_plant.mapper.CropTypeMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CropServiceImplTest {

    private final CropMapper cropMapper = mock(CropMapper.class);

    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);

    private final CropServiceImpl cropService = new CropServiceImpl(
            cropMapper,
            mock(CropTypeMapper.class),
            dataPermissionService
    );

    @Test
    void addCropGeneratesCodeFromDatabaseId() {
        Crop crop = new Crop();
        crop.setCropName("生菜");
        crop.setCropCode("客户端编码");
        when(dataPermissionService.isAdmin()).thenReturn(true);
        doAnswer(invocation -> {
            Crop inserted = invocation.getArgument(0);
            inserted.setId(1L);
            return 1;
        }).when(cropMapper).insert(any(Crop.class));
        when(cropMapper.updateCropCode(1L, "C001")).thenReturn(1);
        when(cropMapper.selectById(1L)).thenAnswer(invocation -> crop);

        Crop result = cropService.addCrop(crop);

        assertEquals("C001", result.getCropCode());
        verify(cropMapper).updateCropCode(1L, "C001");
    }
}
