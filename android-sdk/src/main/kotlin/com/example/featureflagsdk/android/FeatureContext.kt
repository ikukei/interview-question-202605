package com.example.featureflagsdk.android

/**
 * Evaluation context passed by the calling app.
 *
 * @param subjectKey Unique identifier for the subject being evaluated (e.g. user ID).
 * @param region     Geographic region (e.g. "Asia", "North America").
 * @param subject    Subject group (e.g. "vip", "internal").
 * @param releaseKey Release tag (e.g. "20260518").
 * @param attributes Additional free-form attributes forwarded to the backend.
 */
data class FeatureContext(
    val subjectKey: String,
    val region: String? = null,
    val subject: String? = null,
    val releaseKey: String? = null,
    val attributes: Map<String, String> = emptyMap(),
)
