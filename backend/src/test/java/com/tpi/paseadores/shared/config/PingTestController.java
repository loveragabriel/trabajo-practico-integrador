package com.tpi.paseadores.shared.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PingTestController {
    @GetMapping("/api/ping")
    public String ping() {
        return "pong";
    }
}
