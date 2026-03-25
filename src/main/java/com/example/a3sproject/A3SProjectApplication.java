package com.example.a3sproject;

import com.example.a3sproject.config.DotenvInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableScheduling
public class A3SProjectApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(A3SProjectApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
