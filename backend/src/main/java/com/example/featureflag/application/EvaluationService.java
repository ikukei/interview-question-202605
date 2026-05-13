package com.example.featureflag.application;

import com.example.featureflag.api.dto.Dtos.BatchEvaluationRequest;
import com.example.featureflag.api.dto.Dtos.EvaluationRequest;
import com.example.featureflag.api.dto.Dtos.EvaluationResponse;
import com.example.featureflag.api.dto.Dtos.ExplainResponse;
import com.example.featureflag.application.EvaluationEngine.EvaluationDecision;
import com.example.featureflag.application.model.SnapshotModels.Snapshot;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {
    private final PublishService publishService;
    private final EvaluationEngine evaluationEngine;

    public EvaluationService(PublishService publishService, EvaluationEngine evaluationEngine) {
        this.publishService = publishService;
        this.evaluationEngine = evaluationEngine;
    }

    public EvaluationResponse evaluate(String flagKey, EvaluationRequest request) {
        EvaluationDecision decision = decide(flagKey, request);
        return toEvaluationResponse(decision);
    }

    public List<EvaluationResponse> evaluateBatch(BatchEvaluationRequest request) {
        Snapshot snapshot = publishService.loadLatestSnapshot(request.appKey(), request.environment());
        return request.flagKeys().stream()
                .map(flagKey -> evaluationEngine.evaluate(snapshot, flagKey, request.context(), request.defaultValue()))
                .map(this::toEvaluationResponse)
                .toList();
    }

    public ExplainResponse explain(String flagKey, EvaluationRequest request) {
        EvaluationDecision decision = decide(flagKey, request);
        return new ExplainResponse(
                decision.flagKey(),
                decision.finalValue(),
                decision.reasonCode(),
                decision.appKey(),
                decision.environment(),
                decision.subjectKeyHash(),
                decision.matchedRuleId(),
                decision.matchedConditions(),
                decision.rolloutBucket(),
                decision.rolloutPercentage(),
                decision.releaseKey(),
                decision.snapshotVersion(),
                decision.evaluatedAt()
        );
    }

    private EvaluationDecision decide(String flagKey, EvaluationRequest request) {
        Snapshot snapshot = publishService.loadLatestSnapshot(request.appKey(), request.environment());
        return evaluationEngine.evaluate(snapshot, flagKey, request.context(), request.defaultValue());
    }

    private EvaluationResponse toEvaluationResponse(EvaluationDecision decision) {
        return new EvaluationResponse(
                decision.flagKey(),
                Boolean.parseBoolean(decision.finalValue()),
                decision.finalValue(),
                decision.reasonCode(),
                decision.matchedRuleId(),
                decision.snapshotVersion(),
                decision.releaseKey()
        );
    }
}
