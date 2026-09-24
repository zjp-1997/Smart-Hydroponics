package com.smart_plant.smart_plant.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_plant.smart_plant.config.WeatherProperties;
import com.smart_plant.smart_plant.entity.Farm;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.FarmMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FarmServiceImplTest {

    private final FarmMapper farmMapper = mock(FarmMapper.class);

    private final UserMapper userMapper = mock(UserMapper.class);

    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);

    private final WeatherProperties weatherProperties = new WeatherProperties();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FarmServiceImpl farmService = new FarmServiceImpl(
            farmMapper,
            userMapper,
            dataPermissionService,
            weatherProperties,
            objectMapper
    );

    @Test
    void addFarmGeneratesCodeAndParsesCoordinateForFarmOwner() {
        Long userId = 7L;
        Farm farm = new Farm();
        farm.setUserId(userId);
        farm.setFarmName("测试农场");
        farm.setCoordinate("118.123456,24.654321");

        User owner = new User();
        owner.setId(userId);
        owner.setRoleCode("farm_owner");
        owner.setStatus(1);

        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(userMapper.selectById(userId)).thenReturn(owner);
        when(farmMapper.countByFarmCode(any(String.class), isNull())).thenReturn(0);
        doAnswer(invocation -> {
            Farm insertedFarm = invocation.getArgument(0);
            insertedFarm.setId(100L);
            return 1;
        }).when(farmMapper).insert(any(Farm.class));
        when(farmMapper.selectById(100L)).thenReturn(farm);

        farmService.addFarm(farm);

        assertNotNull(farm.getFarmCode());
        assertTrue(farm.getFarmCode().matches("NC\\d{3}"));
        assertEquals(new BigDecimal("118.123456"), farm.getLongitude());
        assertEquals(new BigDecimal("24.654321"), farm.getLatitude());
        assertEquals("118.123456,24.654321", farm.getCoordinate());
        verify(farmMapper).insert(farm);
    }

    @Test
    void addFarmRejectsNonFarmOwnerUser() {
        Long userId = 8L;
        Farm farm = new Farm();
        farm.setUserId(userId);
        farm.setFarmName("测试农场");

        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.setRoleCode("user");
        normalUser.setStatus(1);

        when(dataPermissionService.isAdmin()).thenReturn(true);
        when(userMapper.selectById(userId)).thenReturn(normalUser);

        BusinessException exception = assertThrows(BusinessException.class, () -> farmService.addFarm(farm));

        assertEquals("所属用户必须为农场主", exception.getMessage());
        verify(farmMapper, never()).insert(any(Farm.class));
    }

    @Test
    void uploadFarmImageStoresFileWhenMimeTypeIsGenericButExtensionIsSafe() throws IOException {
        byte[] content = new byte[128];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "farm.jpg",
                "application/octet-stream",
                content);

        var result = farmService.uploadFarmImage(file);
        Path storedFile = Path.of(System.getProperty("user.dir"), result.url().substring(1));

        try {
            assertTrue(result.url().startsWith("/uploads/farm-images/"));
            assertTrue(result.url().endsWith(".jpg"));
            assertEquals(128L, result.size());
            assertTrue(storedFile.isAbsolute());
            assertTrue(Files.exists(storedFile));
            assertEquals(128L, Files.size(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }
}
