package com.example.a3sproject;

import com.example.a3sproject.config.DotenvInitializer;
import io.github.cdimascio.dotenv.Dotenv;
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
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        boolean osEnvSet = System.getenv("SPRING_PROFILES_ACTIVE") != null;
        boolean jvmArgSet = System.getProperty("spring.profiles.active") != null;

        if (!osEnvSet && !jvmArgSet) {
            String profile = dotenv.get("SPRING_PROFILES_ACTIVE", "local");
            System.setProperty("spring.profiles.active", profile);
        }
        SpringApplication app = new SpringApplication(A3SProjectApplication.class);
        app.addInitializers(new DotenvInitializer());
        app.run(args);
    }
}
