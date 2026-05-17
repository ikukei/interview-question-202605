package com.example.featureflagsdk.android

/**
 * Result returned by the backend for a single feature flag evaluation.
 *
 * @param flagKey         The flag identifier.
 * @param enabled         Whether the flag is active for the given context.
 * @param reasonCode      Why the flag resolved to this value (e.g. RULE_MATCH, FLAG_DISABLED).
 * @param matchedRuleId   ID of the rule that produced the result, if any.
 * @param snapshotVersion Version of the snapshot used during evaluation.
 * @param releaseKey      Release tag associated with the flag config.
 */
data class FeatureEvaluation(
    val flagKey: String,
    val enabled: Boolean,
    val reasonCode: String,
    val matchedRuleId: String? = null,
    val snapshotVersion: Long,
    val releaseKey: String? = null,
)
