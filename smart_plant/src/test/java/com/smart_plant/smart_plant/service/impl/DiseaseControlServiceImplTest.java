package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.DiseaseControl;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.mapper.DiseaseControlMapper;
import com.smart_plant.smart_plant.mapper.DiseasePestMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证防治措施业务枚举与数据库支持的四类防治手段保持一致。 */
class DiseaseControlServiceImplTest {

    /** 生物防治（类别4）必须能够通过新增校验并写入 Mapper。 */
    @Test
    void shouldAcceptBiologicalControlCategory() {
        DiseaseControlMapper controlMapper = mock(DiseaseControlMapper.class);
        DiseasePestMapper pestMapper = mock(DiseasePestMapper.class);
        DiseaseControlServiceImpl service = new DiseaseControlServiceImpl(controlMapper, pestMapper);
        DiseaseControl control = new DiseaseControl();
        control.setDiseaseId(7L);
        control.setControlType(1);
        control.setControlCategory(4);
        control.setMethod("释放蚜茧蜂控制蚜虫");

        when(pestMapper.selectById(7L)).thenReturn(new DiseasePest());
        doAnswer(invocation -> {
            DiseaseControl inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        }).when(controlMapper).insert(any(DiseaseControl.class));
        when(controlMapper.selectById(99L)).thenReturn(control);

        assertEquals(4, service.addDiseaseControl(control).getControlCategory());
    }
}
