package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.dto.ErrorLogHandleRequest;
import com.smart_plant.smart_plant.entity.ErrorLog;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ErrorLogMapper;
import com.smart_plant.smart_plant.service.DataPermissionService;
import com.smart_plant.smart_plant.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证错误采集的数据最小化策略、管理员权限和乐观锁冲突提示。 */
class ErrorLogServiceImplTest {

    private final ErrorLogMapper mapper = mock(ErrorLogMapper.class);
    private final ErrorLogWriter writer = mock(ErrorLogWriter.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final OperationLogService operationLogService = mock(OperationLogService.class);
    private final ErrorLogServiceImpl service = new ErrorLogServiceImpl(
            mapper, writer, dataPermissionService, operationLogService);

    @Test
    void recordDoesNotPersistQueryStringOrRequestBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/example");
        request.setQueryString("token=secret");
        request.setContent("password=secret".getBytes());

        String traceId = service.record(new IllegalStateException("test error"), request);

        var captor = org.mockito.ArgumentCaptor.forClass(ErrorLog.class);
        verify(writer).persist(captor.capture());
        ErrorLog saved = captor.getValue();
        assertNotNull(traceId);
        assertEquals("/admin/example", saved.getRequestUri());
        assertEquals("test error", saved.getErrorMessage());
        assertNull(saved.getOperatorName());
    }

    @Test
    void nonAdministratorCannotReadErrorDetails() {
        when(dataPermissionService.isAdmin()).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getDetail(1L));

        assertEquals("只有管理员可以访问错误日志", exception.getMessage());
        verify(mapper, never()).selectById(any());
    }

    @Test
    void updateConflictReturnsReadableMessage() {
        when(dataPermissionService.isAdmin()).thenReturn(true);
        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        when(dataPermissionService.currentUser()).thenReturn(admin);
        when(mapper.updateHandle(8L, 1, "fixed", 2L, "admin", 3)).thenReturn(0);
        when(mapper.selectById(8L)).thenReturn(new ErrorLog());
        ErrorLogHandleRequest request = new ErrorLogHandleRequest();
        request.setHandleStatus(1);
        request.setHandleRemark("fixed");
        request.setVersion(3);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.updateHandle(8L, request));

        assertEquals("错误日志已被其他管理员更新，请刷新后重试", exception.getMessage());
        verify(operationLogService, never()).record(any());
    }
}
