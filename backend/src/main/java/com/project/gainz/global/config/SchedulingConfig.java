package com.project.gainz.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 정해진 시각에 도는 작업 (오래된 알림 정리 등)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
