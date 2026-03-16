package com.bootcamp.paymentdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ui.branding")
public class AppProperties {
    private String appName;
    private String tagline;
    private String logoText;
}
