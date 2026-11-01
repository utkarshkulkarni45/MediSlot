package com.example.medislot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    // Clinic wall-clock time, independent of the server's own timezone (Render runs in UTC)
    @Bean
    public Clock clinicClock(@Value("${app.zone-id:Asia/Kolkata}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }
}
