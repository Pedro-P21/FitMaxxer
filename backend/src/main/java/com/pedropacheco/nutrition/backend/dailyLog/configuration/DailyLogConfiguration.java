package com.pedropacheco.nutrition.backend.dailyLog.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class DailyLogConfiguration {
    @Bean
    public Clock applicationClock() { return Clock.systemDefaultZone(); }
}
