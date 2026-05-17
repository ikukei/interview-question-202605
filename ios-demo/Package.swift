// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "iOSDemo",
    platforms: [
        .iOS(.v16),
        .macOS(.v13),
    ],
    dependencies: [
        .package(path: "../ios-sdk"),
    ],
    targets: [
        .executableTarget(
            name: "iOSDemo",
            dependencies: [
                .product(name: "FeatureFlagSDK", package: "ios-sdk"),
            ],
            path: "Sources/iOSDemo"
        ),
    ]
)
