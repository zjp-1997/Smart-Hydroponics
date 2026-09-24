package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.CropImageUploadResult;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.CropImageService;
import com.smart_plant.smart_plant.service.DiseasePestService;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.response.ResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

/** 验证 farm 上报接口会把图片地址和表单字段交给病虫害领域服务。 */
class ClientDiseasePestControllerTest {

    private final CropImageService cropImageService = mock(CropImageService.class);
    private final DiseasePestService diseasePestService = mock(DiseasePestService.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ClientDiseasePestController controller = new ClientDiseasePestController(
            cropImageService, diseasePestService, dataPermissionService);

    /** multipart 图片保存成功后，应使用返回地址新增病虫害主数据。 */
    @Test
    void addsDiseasePestWithUploadedImage() {
        User owner = new User();
        owner.setId(8L);
        when(dataPermissionService.currentUser()).thenReturn(owner);
        MultipartFile image = mock(MultipartFile.class);
        when(cropImageService.uploadCropImage(image))
                .thenReturn(new CropImageUploadResult("/uploads/crop-images/test.jpg", 128L));
        when(diseasePestService.addDiseasePest(any(DiseasePest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        R<DiseasePest> response = controller.addDiseasePest(
                image, "番茄晚疫病", 2L, 1, "结果期", "叶片出现水渍状病斑", 1);

        assertEquals("/uploads/crop-images/test.jpg", response.getData().getCoverImage());
        assertEquals("番茄晚疫病", response.getData().getName());
        assertEquals(2L, response.getData().getCropTypeId());
        verify(diseasePestService).addDiseasePest(response.getData());
        verify(dataPermissionService).requireFarmManager(8L);
    }

    @Test
    void ordinaryUserCannotModifyDiseaseKnowledge() {
        User worker = new User();
        worker.setId(22L);
        when(dataPermissionService.currentUser()).thenReturn(worker);
        doThrow(new BusinessException(ResponseCode.FORBIDDEN)).when(dataPermissionService).requireFarmManager(22L);
        MultipartFile image = mock(MultipartFile.class);

        assertThrows(BusinessException.class, () -> controller.addDiseasePest(
                image, "番茄晚疫病", 2L, 1, "结果期", "叶片出现水渍状病斑", 1));
        // 权限校验先于文件保存，拒绝请求时不产生孤立附件。
        verify(cropImageService, never()).uploadCropImage(image);
    }

    /** 用户端详情接口应调用仅返回启用内容的领域服务方法。 */
    @Test
    void getsEnabledDiseasePestDetail() {
        DiseasePest diseasePest = new DiseasePest();
        diseasePest.setId(9L);
        diseasePest.setName("黄瓜霉病");
        when(diseasePestService.getEnabledDiseasePestById(9L)).thenReturn(diseasePest);

        R<DiseasePest> response = controller.getDiseasePestDetail(9L);

        assertEquals(9L, response.getData().getId());
        assertEquals("黄瓜霉病", response.getData().getName());
        verify(diseasePestService).getEnabledDiseasePestById(9L);
    }
}
