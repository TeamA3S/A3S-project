package com.example.a3sproject.domain.payments.portone;

import com.example.a3sproject.config.PortOneProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class PortOneConfig { // RestClient Bean 생성
    private final PortOneProperties portOneProperties;

    @Bean
    public RestClient portOneRestClient() {
        return RestClient.builder()
                .baseUrl(portOneProperties.getApi().getBaseUrl())
                .defaultHeader("Authorization", "PortOne " + portOneProperties.getApi().getSecret())
        .build();
    }
}
