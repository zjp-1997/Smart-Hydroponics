package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.entity.ExpertProfile;
import com.smart_plant.smart_plant.dto.ClientExpertListResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatRequest;
import com.smart_plant.smart_plant.dto.ClientExpertChatResponse;
import com.smart_plant.smart_plant.dto.ClientExpertChatDetailResponse;
import com.smart_plant.smart_plant.entity.User;
import com.smart_plant.smart_plant.mapper.UserMapper;
import com.smart_plant.smart_plant.security.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ExpertProfileStandaloneIntegrationTest {

    @Autowired
    private ExpertProfileService expertProfileService;

    @Autowired
    private ClientExpertService clientExpertService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void createsExpertWithoutBoundUserAndPersistsRequiredFields() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ExpertProfile profile = new ExpertProfile();
        profile.setRealName("独立专家" + suffix);
        profile.setOrganization("测试机构" + suffix);
        profile.setJobTitle("高级农艺师");
        profile.setCertificateUrl("/uploads/test-certificate.png");
        profile.setCertificateAuditStatus(1);
        profile.setRating(new BigDecimal("4.80"));
        profile.setStatus(0);
        profile.setServiceStatus(0);
        profile.setAuditStatus(1);

        ExpertProfile created = expertProfileService.addExpertProfile(profile);

        assertNotNull(created.getId());
        assertNull(created.getUserId());
        assertEquals(profile.getOrganization(), created.getOrganization());
        assertNull(created.getCertificateName());
        assertEquals(profile.getCertificateUrl(), created.getCertificateUrl());
        ClientExpertListResponse clientExpert = clientExpertService.listExperts(suffix).getFirst();
        assertEquals(created.getId(), clientExpert.getId());
        assertEquals(false, clientExpert.getConsultable());

        expertProfileService.updateConsultationStatus(created.getId(), 1);
        assertEquals(true, clientExpertService.listExperts(suffix).getFirst().getConsultable());

        User clientUser = userMapper.selectActiveUsers().getFirst();
        ClientExpertChatRequest request = new ClientExpertChatRequest();
        request.setExpertId(created.getId());
        request.setContent("独立专家咨询测试");
        try {
            CurrentUserContext.set(clientUser);
            ClientExpertChatResponse response = consultationService.sendClientMessage(request);
            assertNotNull(response.getSessionId());
            assertNotNull(response.getMessageId());
            ClientExpertChatDetailResponse detail = consultationService.getClientChatDetail(
                    response.getSessionId(), created.getId());
            assertEquals(1, detail.getMessages().size());
            assertEquals(request.getContent(), detail.getMessages().getFirst().getContent());
        } finally {
            CurrentUserContext.clear();
        }
    }
}
