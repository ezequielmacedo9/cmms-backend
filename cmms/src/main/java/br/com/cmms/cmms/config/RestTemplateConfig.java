package br.com.cmms.cmms.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides a single {@link RestTemplate} bean for outbound HTTP calls
 * (Google OAuth tokeninfo, keep-alive self-ping). Centralising it here
 * avoids per-request instantiation in services and lets us tune timeouts
 * in a single place.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
    }
}
