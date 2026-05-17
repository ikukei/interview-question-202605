import Foundation

/// Result returned by the backend for a single feature flag evaluation.
public struct FeatureEvaluation: Decodable {
    /// The flag identifier.
    public let flagKey: String
    /// Whether the flag is active for the given context.
    public let enabled: Bool
    /// Why the flag resolved to this value (e.g. RULE_MATCH, FLAG_DISABLED).
    public let reasonCode: String
    /// ID of the rule that produced the result, if any.
    public let matchedRuleId: String?
    /// Version of the snapshot used during evaluation.
    public let snapshotVersion: Int
    /// Release tag associated with the flag config.
    public let releaseKey: String?
}
