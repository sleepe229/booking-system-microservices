package com.hotel.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/websocket/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowCredentials(true);
    }
}