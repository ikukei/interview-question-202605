// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "FeatureFlagSDK",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    products: [
        .library(name: "FeatureFlagSDK", targets: ["FeatureFlagSDK"]),
    ],
    targets: [
        .target(
            name: "FeatureFlagSDK",
            path: "Sources/FeatureFlagSDK"
        ),
        .testTarget(
            name: "FeatureFlagSDKTests",
            dependencies: ["FeatureFlagSDK"],
            path: "Tests/FeatureFlagSDKTests"
        ),
    ]
)
