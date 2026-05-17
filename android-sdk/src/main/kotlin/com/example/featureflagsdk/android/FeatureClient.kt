package com.example.featureflagsdk.android

/**
 * Entry point for feature flag evaluation on Android.
 *
 * Usage:
 * ```kotlin
 * val client = FeatureClient(
 *     baseUrl = "https://your-backend.example.com",
 *     appKey  = "my-android-app",
 *     environment = "prod",
 * )
 * val context = FeatureContext(subjectKey = userId, region = "Asia", subject = "vip")
 *
 * // Coroutine-based API (suspend functions)
 * val enabled: Boolean = client.boolVariation("new-checkout", context)
 * val all: List<FeatureEvaluation> = client.evaluateAll(context)
 * ```
 *
 * Implementation notes (TODO):
 *  - Use OkHttp for HTTP calls (suspend via okhttp3-coroutines or withContext(Dispatchers.IO)).
 *  - Parse responses with kotlinx.serialization or Gson.
 *  - Cache the latest snapshot locally (SharedPreferences or Room) for offline fallback.
 *  - Respect Android lifecycle: cancel in-flight requests when the owner scope is destroyed.
 */
class FeatureClient(
    private val baseUrl: String,
    private val appKey: String,
    private val environment: String,
) {
    // TODO: inject OkHttpClient

    /** Returns all flag keys available for this app + environment. */
    suspend fun listFlagKeys(): List<String> {
        TODO("Not yet implemented — requires OkHttp + JSON parsing")
    }

    /** Evaluates a single flag for the given context. */
    suspend fun evaluate(flagKey: String, context: FeatureContext): FeatureEvaluation {
        TODO("Not yet implemented — requires OkHttp + JSON parsing")
    }

    /** Evaluates all flags for the given context in a single batch request. */
    suspend fun evaluateAll(context: FeatureContext): List<FeatureEvaluation> {
        TODO("Not yet implemented — requires OkHttp + JSON parsing")
    }

    /**
     * Returns the boolean value of a flag, or [defaultValue] on any error.
     * Safe to call from a coroutine — never throws.
     */
    suspend fun boolVariation(
        flagKey: String,
        context: FeatureContext,
        defaultValue: Boolean = false,
    ): Boolean = runCatching { evaluate(flagKey, context).enabled }.getOrDefault(defaultValue)
}
