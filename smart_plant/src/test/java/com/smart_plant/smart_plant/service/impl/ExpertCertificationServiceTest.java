package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.ExpertCertificateMapper;
import com.smart_plant.smart_plant.mapper.ExpertDetailMapper;
import com.smart_plant.smart_plant.mapper.ExpertProfileMapper;
import com.smart_plant.smart_plant.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpertCertificationServiceTest {

    private final ExpertProfileMapper profiles = mock(ExpertProfileMapper.class);
    private final ExpertProfileServiceImpl service = new ExpertProfileServiceImpl(
            profiles, mock(ExpertDetailMapper.class), mock(ExpertCertificateMapper.class), mock(UserMapper.class));

    @Test
    void incompleteCertificationCannotBeSubmittedOrApproved() {
        ExpertProfile draft = profile(7L, 0);
        draft.setRealName("测试专家");
        when(profiles.selectById(7L)).thenReturn(draft);

        assertThrows(BusinessException.class, () -> service.submitExpertCertification(7L));
        assertThrows(BusinessException.class, () -> service.updateAuditStatus(7L, 2));
        verify(profiles, never()).updateAuditStatus(any(), any());
    }

    @Test
    void completeDraftEntersPendingAndKeepsServiceDisabled() {
        ExpertProfile draft = profile(7L, 0);
        draft.setRealName("测试专家");
        draft.setOrganization("智慧农业研究院");
        draft.setJobTitle("高级农艺师");
        draft.setCertificateUrl("/uploads/expert-assets/certificate/example.jpg");
        when(profiles.selectById(7L)).thenReturn(draft);
        when(profiles.updateById(any())).thenReturn(1);

        service.submitExpertCertification(7L);

        ArgumentCaptor<ExpertProfile> update = ArgumentCaptor.forClass(ExpertProfile.class);
        verify(profiles).updateById(update.capture());
        assertEquals(1, update.getValue().getAuditStatus());
        assertEquals(0, update.getValue().getStatus());
        assertEquals(0, update.getValue().getServiceStatus());
        assertEquals(0, update.getValue().getConsultationStatus());
    }

    private ExpertProfile profile(Long id, Integer auditStatus) {
        ExpertProfile profile = new ExpertProfile();
        profile.setId(id);
        profile.setAuditStatus(auditStatus);
        profile.setStatus(0);
        profile.setServiceStatus(0);
        profile.setConsultationStatus(0);
        return profile;
    }
}
