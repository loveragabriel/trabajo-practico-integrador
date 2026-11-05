package com.tpi.paseadores.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// single source of "now" for business rules: schedule timestamps are CABA local time (docs/arquitectura.md).
// Services take this Clock in their constructor so tests can pass Clock.fixed(...)
@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Argentina/Buenos_Aires"));
    }
}
