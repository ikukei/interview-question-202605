package com.example.featureflag.api;

import com.example.featureflag.api.dto.Dtos.BatchEvaluationRequest;
import com.example.featureflag.api.dto.Dtos.EvaluationRequest;
import com.example.featureflag.api.dto.Dtos.EvaluationResponse;
import com.example.featureflag.api.dto.Dtos.ExplainResponse;
import com.example.featureflag.application.EvaluationService;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EvaluationController {
    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluations/flags/{flagKey}")
    public EvaluationResponse evaluate(@PathVariable String flagKey, @RequestBody EvaluationRequest request) {
        return evaluationService.evaluate(flagKey, request);
    }

    @PostMapping("/evaluations:batch")
    public List<EvaluationResponse> batch(@RequestBody BatchEvaluationRequest request) {
        return evaluationService.evaluateBatch(request);
    }

    @PostMapping("/evaluations:explain/{flagKey}")
    public ExplainResponse explain(@PathVariable String flagKey, @RequestBody EvaluationRequest request) {
        return evaluationService.explain(flagKey, request);
    }
}
