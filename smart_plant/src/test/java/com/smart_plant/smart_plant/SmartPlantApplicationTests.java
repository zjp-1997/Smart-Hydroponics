package com.smart_plant.smart_plant;

import com.smart_plant.smart_plant.controller.TestController;
import com.smart_plant.smart_plant.entity.TestInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartPlantApplicationTests {

    @Autowired
    private TestController testController;

    @Test
    void contextLoads() {
    }

    @Test
    void testInterfaceReturnsInfo() {
        TestInfo testInfo = testController.test();

        Assertions.assertEquals("success", testInfo.getStatus());
        Assertions.assertNotNull(testInfo.getMessage());
        Assertions.assertNotNull(testInfo.getCurrentTime());
    }

}
