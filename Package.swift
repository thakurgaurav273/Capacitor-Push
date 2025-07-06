// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPush",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapacitorPush",
            targets: ["CapacitorPushPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CapacitorPushPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CapacitorPushPlugin"),
        .testTarget(
            name: "CapacitorPushPluginTests",
            dependencies: ["CapacitorPushPlugin"],
            path: "ios/Tests/CapacitorPushPluginTests")
    ]
)