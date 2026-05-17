package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.ConfigureFlagRequest;
import com.example.featureflag.api.dto.Dtos.CreateFlagRequest;
import com.example.featureflag.api.dto.Dtos.PublishRequest;
import com.example.featureflag.infrastructure.repository.FlagRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class DemoDataInitializer {
    @Bean
    CommandLineRunner seedDemoData(
            FlagRepository flagRepository,
            FlagService flagService,
            PublishService publishService,
            AuditService auditService
    ) {
        return args -> {
            String flagKey = "google-sso";
            String release = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            if (flagRepository.findByFlagKey(flagKey).isEmpty()) {
                flagService.createFlag(new CreateFlagRequest(
                        flagKey,
                        "Enables the Google SSO",
                        "boolean",
                        release
                ));
                auditService.record("demo-seed", "CREATE", "flag", flagKey,
                        null,
                        "{\"flagKey\":\"" + flagKey + "\",\"type\":\"boolean\",\"release\":\"" + release + "\"}");
            }

            flagService.configureFlag(flagKey, new ConfigureFlagRequest(
                    List.of("vue-demo", "java-demo", "python-demo"),
                    "local",
                    List.of("Asia", "North America"),
                    List.of("vip"),
                    true,
                    100,
                    null
            ));
            auditService.record("demo-seed", "CONFIGURE", "flag", flagKey,
                    null,
                    "{\"apps\":[\"vue-demo\",\"java-demo\",\"python-demo\"],\"environment\":\"local\",\"regions\":[\"Asia\",\"North America\"],\"subjects\":[\"vip\"],\"rollout\":100}");

            publishService.publish(new PublishRequest("vue-demo", "local", "demo-seed"));
            publishService.publish(new PublishRequest("java-demo", "local", "demo-seed"));
            publishService.publish(new PublishRequest("python-demo", "local", "demo-seed"));
            auditService.record("demo-seed", "PUBLISH", "snapshot", flagKey,
                    null,
                    "{\"apps\":[\"vue-demo\",\"java-demo\",\"python-demo\"],\"environment\":\"local\"}");
        };
    }
}
