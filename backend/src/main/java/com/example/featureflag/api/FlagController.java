package com.example.featureflag.api;

import com.example.featureflag.api.dto.Dtos.AddRuleRequest;
import com.example.featureflag.api.dto.Dtos.AppResponse;
import com.example.featureflag.api.dto.Dtos.CreateAppRequest;
import com.example.featureflag.api.dto.Dtos.CreateFlagRequest;
import com.example.featureflag.api.dto.Dtos.FlagResponse;
import com.example.featureflag.api.dto.Dtos.RuleResponse;
import com.example.featureflag.api.dto.Dtos.UpdateFlagRequest;
import com.example.featureflag.application.FlagService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class FlagController {
    private final FlagService flagService;

    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    @PostMapping("/apps")
    public AppResponse createApp(@RequestBody CreateAppRequest request) {
        return flagService.createApp(request);
    }

    @GetMapping("/apps")
    public List<AppResponse> listApps() {
        return flagService.listApps();
    }

    @PostMapping("/flags")
    public FlagResponse createFlag(@RequestBody CreateFlagRequest request) {
        return flagService.createFlag(request);
    }

    @GetMapping("/flags")
    public List<FlagResponse> listFlags(@RequestParam String appKey, @RequestParam String environment) {
        return flagService.listFlags(appKey, environment);
    }

    @PutMapping("/flags/{flagKey}")
    public FlagResponse updateFlag(
            @PathVariable String flagKey,
            @RequestBody UpdateFlagRequest request
    ) {
        return flagService.updateFlag(flagKey, request);
    }

    @PostMapping("/flags/{flagKey}/rules")
    public RuleResponse addRule(@PathVariable String flagKey, @RequestBody AddRuleRequest request) {
        return flagService.addRule(flagKey, request);
    }

    @PostMapping("/flags/{flagKey}/archive")
    public FlagResponse archiveFlag(
            @PathVariable String flagKey,
            @RequestParam String appKey,
            @RequestParam String environment
    ) {
        return flagService.archiveFlag(flagKey, appKey, environment);
    }
}
