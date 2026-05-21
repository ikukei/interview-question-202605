package com.example.featuredemo.android

import com.example.featureflagsdk.android.FeatureClient
import com.example.featureflagsdk.android.FeatureContext

/**
 * Android demo — entry point placeholder.
 *
 * When implemented this Activity should:
 *  1. Build a [FeatureClient] pointing at the backend.
 *  2. Resolve the current user's context (userId, region from device locale, etc.).
 *  3. Call [FeatureClient.evaluateAll] inside a coroutine (lifecycleScope.launch).
 *  4. Display each flag key and enabled state in a RecyclerView or LazyColumn.
 *  5. Add a "Reload" button that repeats the evaluation on demand.
 *
 * TODO: implement once android-sdk is complete.
 */
class MainActivity {
    private val client = FeatureClient(
        baseUrl = "http://10.0.2.2:8080",   // localhost from Android emulator
        appKey = "android-demo",
        environment = "local",
    )

    private val context = FeatureContext(
        subjectKey = "android-demo-user",
        region = "Asia",
        subject = "vip",
        attributes = mapOf("platform" to "android"),
    )

    // TODO: override fun onCreate(savedInstanceState: Bundle?)
    // TODO: lifecycleScope.launch { val evals = client.evaluateAll(context); renderList(evals) }
}
