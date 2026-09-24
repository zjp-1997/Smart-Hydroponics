package com.smart_plant.smart_plant.service.impl;

import com.smart_plant.smart_plant.entity.TestInfo;
import com.smart_plant.smart_plant.service.TestService;
import com.smart_plant.smart_plant.utils.TimeUtils;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

    @Override
    public TestInfo getTestInfo() {
        return TestInfo.builder()
                .status("success")
                .message("smart_plant test interface is running")
                .currentTime(TimeUtils.now())
                .build();
    }
}
