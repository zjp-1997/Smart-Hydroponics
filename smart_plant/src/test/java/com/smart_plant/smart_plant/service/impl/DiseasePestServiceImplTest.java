package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.DiseaseControl;
import com.smart_plant.smart_plant.entity.DiseasePest;
import com.smart_plant.smart_plant.exception.BusinessException;
import com.smart_plant.smart_plant.mapper.CropTypeMapper;
import com.smart_plant.smart_plant.mapper.DiseaseControlMapper;
import com.smart_plant.smart_plant.mapper.DiseasePestMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证 farm 用户端病虫害详情的发布状态边界。 */
class DiseasePestServiceImplTest {

    private final DiseasePestMapper diseasePestMapper = mock(DiseasePestMapper.class);
    private final DiseaseControlMapper diseaseControlMapper = mock(DiseaseControlMapper.class);
    private final CropTypeMapper cropTypeMapper = mock(CropTypeMapper.class);
    private final DiseasePestServiceImpl service = new DiseasePestServiceImpl(
            diseasePestMapper, diseaseControlMapper, cropTypeMapper);

    /** 启用的病虫害详情中只应保留启用的防治措施。 */
    @Test
    void returnsOnlyEnabledControlsForClientDetail() {
        DiseasePest diseasePest = diseasePest(1);
        DiseaseControl enabled = control(1L, 1);
        DiseaseControl disabled = control(2L, 0);
        when(diseasePestMapper.selectById(7L)).thenReturn(diseasePest);
        when(diseaseControlMapper.selectList(7L, null, null, null, null))
                .thenReturn(List.of(enabled, disabled));

        DiseasePest result = service.getEnabledDiseasePestById(7L);

        assertEquals(1, result.getControls().size());
        assertEquals(1L, result.getControls().getFirst().getId());
    }

    /** 后台已停用的病虫害不能继续被 farm 用户端访问。 */
    @Test
    void rejectsDisabledDiseasePestForClientDetail() {
        when(diseasePestMapper.selectById(8L)).thenReturn(diseasePest(0));
        when(diseaseControlMapper.selectList(8L, null, null, null, null)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.getEnabledDiseasePestById(8L));
    }

    /** 构造最小病虫害数据，使测试聚焦状态过滤规则。 */
    private DiseasePest diseasePest(int status) {
        DiseasePest diseasePest = new DiseasePest();
        diseasePest.setId(status == 1 ? 7L : 8L);
        diseasePest.setName("测试病害");
        diseasePest.setStatus(status);
        return diseasePest;
    }

    /** 构造不同发布状态的防治措施。 */
    private DiseaseControl control(long id, int status) {
        DiseaseControl control = new DiseaseControl();
        control.setId(id);
        control.setStatus(status);
        return control;
    }
}
