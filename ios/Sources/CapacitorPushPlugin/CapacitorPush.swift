import Foundation

@objc public class CapacitorPush: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
