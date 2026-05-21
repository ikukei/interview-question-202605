import Foundation

/// Errors thrown by FeatureClient.
public enum FeatureClientError: Error {
    case httpError(Int, String)
    case networkError(Error)
    case decodingError(Error)
}

/// Entry point for feature flag evaluation on iOS / macOS.
///
/// Usage:
/// ```swift
/// let client = FeatureClient(
///     baseURL: URL(string: "https://your-backend.example.com")!,
///     appKey: "my-ios-app",
///     environment: "prod"
/// )
/// let context = FeatureContext(subjectKey: userId, region: "Asia", subject: "vip")
///
/// // async/await API
/// let enabled = try await client.boolVariation("new-checkout", context: context)
/// let all     = try await client.evaluateAll(context: context)
/// ```
///
/// Implementation notes (TODO):
///  - Use URLSession.shared for HTTP calls.
///  - Parse JSON responses with JSONDecoder (FeatureEvaluation is already Decodable).
///  - Cache the latest snapshot in UserDefaults or a local file for offline fallback.
///  - Support Combine publishers in addition to async/await for UIKit compatibility.
public actor FeatureClient {
    private let baseURL: URL
    private let appKey: String
    private let environment: String

    public init(baseURL: URL, appKey: String, environment: String) {
        self.baseURL = baseURL
        self.appKey = appKey
        self.environment = environment
    }

    /// Returns all flag keys available for this app + environment.
    public func listFlagKeys() async throws -> [String] {
        // TODO: GET /api/v1/flags?appKey=&environment=
        throw FeatureClientError.networkError(
            NSError(domain: "FeatureFlagSDK", code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Not yet implemented"])
        )
    }

    /// Evaluates a single flag for the given context.
    public func evaluate(_ flagKey: String, context: FeatureContext) async throws -> FeatureEvaluation {
        // TODO: POST /api/v1/evaluations/flags/{flagKey}
        throw FeatureClientError.networkError(
            NSError(domain: "FeatureFlagSDK", code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Not yet implemented"])
        )
    }

    /// Evaluates all flags for the given context in a single batch request.
    public func evaluateAll(context: FeatureContext) async throws -> [FeatureEvaluation] {
        // TODO: POST /api/v1/evaluations:batch
        throw FeatureClientError.networkError(
            NSError(domain: "FeatureFlagSDK", code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Not yet implemented"])
        )
    }

    /// Returns the boolean value of a flag, or `defaultValue` on any error.
    public func boolVariation(
        _ flagKey: String,
        context: FeatureContext,
        defaultValue: Bool = false
    ) async -> Bool {
        do {
            return try await evaluate(flagKey, context: context).enabled
        } catch {
            return defaultValue
        }
    }
}
