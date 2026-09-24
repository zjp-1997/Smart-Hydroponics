package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.SystemSettingUpdateRequest;
import com.smart_plant.smart_plant.entity.SystemSetting;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.SystemSettingMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证系统设置的管理员权限、安全扩展名白名单和乐观锁行为。 */
class SystemSettingServiceImplTest {

    private final SystemSettingMapper mapper = mock(SystemSettingMapper.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final SystemSettingServiceImpl service = new SystemSettingServiceImpl(mapper, operationLogService);

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void nonAdministratorCannotUpdateGlobalSetting() {
        CurrentUserContext.set(currentUser("farm_owner"));
        SystemSettingUpdateRequest request = validRequest();

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateSetting(request));

        assertEquals("只有管理员可以修改系统设置", exception.getMessage());
        verify(mapper, never()).updateSingleton(request, 9L);
    }

    @Test
    void rejectsExecutableUploadExtension() {
        CurrentUserContext.set(currentUser("admin"));
        SystemSettingUpdateRequest request = validRequest();
        request.setAllowedUploadTypes("png,exe");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateSetting(request));

        assertEquals("包含不安全或不支持的上传文件类型：exe", exception.getMessage());
    }

    @Test
    void successfulUpdateNormalizesValuesAndRecordsAuditLog() {
        CurrentUserContext.set(currentUser("admin"));
        SystemSettingUpdateRequest request = validRequest();
        request.setAllowedUploadTypes(".PNG,png, PDF");
        SystemSetting saved = new SystemSetting();
        saved.setId(1);
        saved.setVersion(4);
        when(mapper.updateSingleton(request, 9L)).thenReturn(1);
        when(mapper.selectSingleton()).thenReturn(saved);

        SystemSetting result = service.updateSetting(request);

        assertEquals("png,pdf", request.getAllowedUploadTypes());
        assertEquals(4, result.getVersion());
        verify(operationLogService).record("修改系统设置");
    }

    /** 构造满足服务层格式校验的完整请求。 */
    private SystemSettingUpdateRequest validRequest() {
        SystemSettingUpdateRequest request = new SystemSettingUpdateRequest();
        request.setSystemName("Smart Plant");
        request.setHomeTitle("智慧农业工作台");
        request.setThemeColor("#239AAA");
        request.setTimezone("Asia/Shanghai");
        request.setDatetimeFormat("yyyy-MM-dd HH:mm:ss");
        request.setDefaultPageSize(10);
        request.setMaxUploadSizeMb(5);
        request.setAllowedUploadTypes("png,pdf");
        request.setVersion(3);
        return request;
    }

    /** 构造当前登录上下文使用的最小用户对象。 */
    private User currentUser(String roleCode) {
        User user = new User();
        user.setId(9L);
        user.setRoleCode(roleCode);
        user.setStatus(1);
        return user;
    }
}
