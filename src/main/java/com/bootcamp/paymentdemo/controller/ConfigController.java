package com.bootcamp.paymentdemo.controller;

import com.bootcamp.paymentdemo.config.AppProperties;
import com.bootcamp.paymentdemo.config.ClientApiProperties;
import com.bootcamp.paymentdemo.config.PortOneProperties;
import com.bootcamp.paymentdemo.dto.PublicConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class ConfigController {

    private final PortOneProperties portOneProperties;
    private final ClientApiProperties clientApiProperties;
    private final AppProperties appProperties;

    /**
     * GET /api/public/config
     * 프론트엔드 런타임 설정을 반환합니다.
     * - PortOne 채널 정보
     * - 클라이언트 API 계약 정보 (URL, 요청/응답 스키마)
     * - UI 브랜딩 정보
     */
    @GetMapping("/config")
    public ResponseEntity<PublicConfigResponse> getPublicConfig() {
        PublicConfigResponse response = PublicConfigResponse.builder()
            .portone(PublicConfigResponse.PortOneConfig.builder()
                .storeId(portOneProperties.getStore().getId())
                .channelKeys(portOneProperties.getChannel())
                .build())
            .api(PublicConfigResponse.ClientApiConfig.builder()
                .baseUrl(clientApiProperties.getBaseUrl())
                .endpoints(clientApiProperties.getEndpoints())
                .build())
            .branding(PublicConfigResponse.BrandingConfig.builder()
                .appName(appProperties.getAppName())
                .tagline(appProperties.getTagline())
                .logoText(appProperties.getLogoText())
                .build())
            .build();

        return ResponseEntity.ok(response);
    }
}
