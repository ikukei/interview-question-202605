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
            PublishService publishService
    ) {
        return args -> {
            String flagKey = "new-checkout";
            String release = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

            if (flagRepository.findByFlagKey(flagKey).isEmpty()) {
                flagService.createFlag(new CreateFlagRequest(
                        flagKey,
                        null,
                        "Enables the simplified checkout experience for selected demo users.",
                        "boolean",
                        release,
                        true
                ));
            }

            flagService.configureFlag(flagKey, new ConfigureFlagRequest(
                    List.of("vue-demo", "java-demo"),
                    "local",
                    List.of("Asia", "North America"),
                    "vip",
                    true,
                    100,
                    null
            ));
            publishService.publish(new PublishRequest("vue-demo", "local", "demo-seed"));
            publishService.publish(new PublishRequest("java-demo", "local", "demo-seed"));
        };
    }
}
