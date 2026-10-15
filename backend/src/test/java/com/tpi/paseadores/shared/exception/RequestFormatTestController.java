package com.tpi.paseadores.shared.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RequestFormatTestController {
    @GetMapping("/test/request/number")
    int number(@RequestParam int value) {
        return value;
    }

    @PostMapping(value = "/test/request/body", consumes = "application/json")
    Payload body(@RequestBody Payload payload) {
        return payload;
    }

    record Payload(int count) {
    }
}
