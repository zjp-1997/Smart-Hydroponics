package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.DiseaseControl;
import com.smart_plant.smart_plant.entity.DiseasePest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 验证 V20 病虫害扩展字段、JSON 图片类型处理器和防治手段字段的真实 MySQL 映射。
 * 测试运行在事务中，结束后自动回滚，不向业务库遗留测试数据。
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class DiseaseKnowledgeMapperIntegrationTest {

    @Autowired
    private DiseasePestMapper diseasePestMapper;

    @Autowired
    private DiseaseControlMapper diseaseControlMapper;

    /** 新增后重新查询，所有扩展字段都应无损返回。 */
    @Test
    void extendedFieldsAndJsonImagesRoundTrip() {
        DiseasePest disease = new DiseasePest();
        disease.setName("集成测试病害-" + UUID.randomUUID());
        disease.setType(1);
        disease.setAffectedCrops("叶菜类、瓜类");
        disease.setSymptom("叶片出现测试病斑");
        disease.setSuitableStage("苗期");
        disease.setOccurrencePeriod("春秋季");
        disease.setLivingHabits("测试生活习性");
        disease.setSuitableEnvironment("温暖高湿");
        disease.setTransmissionRoute("种苗传播");
        disease.setCoverImage("/uploads/disease-images/cover.webp");
        disease.setImageUrls(List.of("/uploads/disease-images/a.webp", "/uploads/disease-images/b.webp"));
        disease.setStatus(1);
        disease.setSortOrder(12);
        assertEquals(1, diseasePestMapper.insert(disease));
        assertNotNull(disease.getId());

        DiseasePest savedDisease = diseasePestMapper.selectById(disease.getId());
        assertEquals(disease.getImageUrls(), savedDisease.getImageUrls());
        assertEquals("春秋季", savedDisease.getOccurrencePeriod());
        assertEquals(12, savedDisease.getSortOrder());

        DiseaseControl control = new DiseaseControl();
        control.setDiseaseId(disease.getId());
        control.setControlType(2);
        control.setControlCategory(3);
        control.setMethod("按监测结果适时施药");
        control.setDrugName("测试药剂");
        control.setUsageMethod("均匀喷雾");
        control.setDosageSpec("5%乳油1500倍液");
        control.setSafetyIntervalDays(7);
        control.setPrecautions("轮换用药");
        control.setStatus(1);
        control.setSortOrder(2);
        assertEquals(1, diseaseControlMapper.insert(control));

        DiseaseControl savedControl = diseaseControlMapper.selectById(control.getId());
        assertEquals(3, savedControl.getControlCategory());
        assertEquals("5%乳油1500倍液", savedControl.getDosageSpec());
        assertEquals(7, savedControl.getSafetyIntervalDays());
        assertEquals(2, savedControl.getSortOrder());
    }
}
