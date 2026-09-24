package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.entity.TestInfo;
import com.smart_plant.smart_plant.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @GetMapping
    public TestInfo test() {
        return testService.getTestInfo();
    }
}
