import SwiftUI
import FeatureFlagSDK

/// iOS demo — SwiftUI entry point placeholder.
///
/// When implemented this view should:
///  1. Build a FeatureClient pointing at the backend.
///  2. Resolve the current user's context (userId, region from Locale, etc.).
///  3. Call client.evaluateAll(context:) inside a Task on appear.
///  4. Display each flag key and enabled state in a List.
///  5. Add a "Reload" button that repeats the evaluation on demand.
///
/// TODO: implement once ios-sdk is complete.
struct ContentView: View {
    private let client = FeatureClient(
        baseURL: URL(string: "http://localhost:8080")!,
        appKey: "ios-demo",
        environment: "local"
    )

    private let context = FeatureContext(
        subjectKey: "ios-demo-user",
        region: "Asia",
        subject: "vip",
        attributes: ["platform": "ios"]
    )

    // TODO: @State private var evaluations: [FeatureEvaluation] = []
    // TODO: @State private var isLoading = false

    var body: some View {
        // TODO: replace with real flag list
        Text("iOS Feature Flag Demo — not yet implemented")
            .padding()
    }

    // TODO:
    // func load() async {
    //     isLoading = true
    //     evaluations = (try? await client.evaluateAll(context: context)) ?? []
    //     isLoading = false
    // }
}
