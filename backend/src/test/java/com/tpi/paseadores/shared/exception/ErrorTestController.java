package com.tpi.paseadores.shared.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ErrorTestController {

    record Payload(@NotBlank String name) {
    }

    @PostMapping("/test/validate")
    public String validate(@Valid @RequestBody Payload payload) {
        return "ok";
    }

    @GetMapping("/test/not-found")
    public String notFound() {
        throw new ResourceNotFoundException("pet not found");
    }

    @GetMapping("/test/forbidden")
    public String forbidden() {
        throw new ForbiddenActionException("not your pet");
    }

    @GetMapping("/test/conflict")
    public String conflict() {
        throw new BusinessRuleViolationException("walk is full");
    }

    @GetMapping("/test/boom")
    public String boom() {
        throw new RuntimeException("boom");
    }
}
