package com.tokenrealty.gateway.health;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/actuator")
@RequiredArgsConstructor
public class PlatformHealthController {

    private final PlatformHealthService platformHealthService;

    @GetMapping("/platform-health")
    public Map<String, Object> platformHealth() {
        return platformHealthService.checkPlatform();
    }
}
