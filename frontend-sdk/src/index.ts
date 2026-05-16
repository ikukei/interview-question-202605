export type FeatureAttributes = Record<string, string>;

export interface FeatureContext {
  subjectKey: string;
  attributes?: FeatureAttributes;
}

export interface FeatureClientOptions {
  baseUrl: string;
  appKey: string;
  environment: string;
}

export interface FeatureEvaluation {
  flagKey: string;
  enabled: boolean;
  value: string;
  reasonCode: string;
  matchedRuleId?: string | null;
  snapshotVersion: number;
  releaseKey?: string | null;
}

export interface FeatureClient {
  evaluate(flagKey: string, context: FeatureContext, defaultValue?: string): Promise<FeatureEvaluation>;
  boolVariation(flagKey: string, context: FeatureContext, defaultValue?: boolean): Promise<boolean>;
  listFlagKeys(): Promise<string[]>;
  evaluateAll(context: FeatureContext, defaultValue?: string): Promise<FeatureEvaluation[]>;
}

export function createFeatureClient(options: FeatureClientOptions): FeatureClient {
  const baseUrl = options.baseUrl.replace(/\/$/, "");

  async function evaluate(flagKey: string, context: FeatureContext, defaultValue = "false"): Promise<FeatureEvaluation> {
    const response = await fetch(`${baseUrl}/api/v1/evaluations/flags/${encodeURIComponent(flagKey)}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        appKey: options.appKey,
        environment: options.environment,
        context,
        defaultValue
      })
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(`Feature evaluation failed with HTTP ${response.status}: ${message}`);
    }

    return response.json() as Promise<FeatureEvaluation>;
  }

  async function listFlagKeys(): Promise<string[]> {
    const response = await fetch(`${baseUrl}/api/v1/flags?appKey=${encodeURIComponent(options.appKey)}&environment=${encodeURIComponent(options.environment)}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json"
      }
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(`Failed to list flags with HTTP ${response.status}: ${message}`);
    }

    const flags = await response.json() as Array<{ flagKey: string }>;
    return flags.map(f => f.flagKey);
  }

  async function evaluateAll(context: FeatureContext, defaultValue = "false"): Promise<FeatureEvaluation[]> {
    const flagKeys = await listFlagKeys();
    if (flagKeys.length === 0) {
      return [];
    }

    const response = await fetch(`${baseUrl}/api/v1/evaluations:batch`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        appKey: options.appKey,
        environment: options.environment,
        flagKeys,
        context,
        defaultValue
      })
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(`Batch evaluation failed with HTTP ${response.status}: ${message}`);
    }

    return response.json() as Promise<FeatureEvaluation[]>;
  }

  return {
    evaluate,
    async boolVariation(flagKey: string, context: FeatureContext, defaultValue = false): Promise<boolean> {
      const result = await evaluate(flagKey, context, String(defaultValue));
      return result.value === "true";
    },
    listFlagKeys,
    evaluateAll
  };
}