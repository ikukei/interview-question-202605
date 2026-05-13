package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.AddRuleRequest;
import com.example.featureflag.api.dto.Dtos.ConditionRequest;
import com.example.featureflag.api.dto.Dtos.CreateAppRequest;
import com.example.featureflag.api.dto.Dtos.CreateFlagRequest;
import com.example.featureflag.api.dto.Dtos.PublishRequest;
import com.example.featureflag.infrastructure.repository.ApplicationRepository;
import com.example.featureflag.infrastructure.repository.FlagRepository;
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
            ApplicationRepository applicationRepository,
            FlagRepository flagRepository,
            FlagService flagService,
            PublishService publishService
    ) {
        return args -> {
            String appKey = "checkout-service";
            String environment = "local";
            String flagKey = "new-checkout";

            if (applicationRepository.findByAppKey(appKey).isEmpty()) {
                flagService.createApp(new CreateAppRequest(appKey, "Checkout Service", "Payments Platform"));
            }

            if (flagRepository.findByFlagKeyAndAppKeyAndEnvironment(flagKey, appKey, environment).isEmpty()) {
                flagService.createFlag(new CreateFlagRequest(
                        flagKey,
                        appKey,
                        environment,
                        "New Checkout",
                        "Enables the simplified checkout experience for selected users.",
                        "boolean",
                        "false",
                        true,
                        "release-2026-05-checkout"
                ));
                flagService.addRule(flagKey, new AddRuleRequest(
                        appKey,
                        environment,
                        1,
                        List.of(new ConditionRequest("region", "equals", "us-east")),
                        100,
                        "true",
                        true
                ));
                publishService.publish(new PublishRequest(appKey, environment, "demo-seed"));
            }
        };
    }
}
