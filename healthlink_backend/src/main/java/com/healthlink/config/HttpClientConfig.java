package com.healthlink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Central HTTP client configuration to ensure consistent timeouts and
 * prevent silent swallowing of security-relevant errors.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate(clientHttpRequestFactory());
        // Use default handler (throws on 4xx/5xx) rather than silent ignore
        rt.setErrorHandler(new DefaultResponseErrorHandler());
        return rt;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        // Conservative timeouts to avoid thread exhaustion
        f.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return f;
    }
}
