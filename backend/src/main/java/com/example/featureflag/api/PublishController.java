package com.example.featureflag.api;

import com.example.featureflag.api.dto.Dtos.PublishRequest;
import com.example.featureflag.api.dto.Dtos.SnapshotResponse;
import com.example.featureflag.application.PublishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PublishController {
    private final PublishService publishService;

    public PublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/publish")
    public SnapshotResponse publish(@RequestBody PublishRequest request) {
        return publishService.publish(request);
    }

    @GetMapping("/snapshots/latest")
    public SnapshotResponse latest(@RequestParam String appKey, @RequestParam String environment) {
        return publishService.latest(appKey, environment);
    }
}
