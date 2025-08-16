import Foundation
import Capacitor
import UserNotifications
import PushKit
import CallKit
import AVFAudio

@objc(CapacitorPushPlugin)
public class CapacitorPushPlugin: CAPPlugin, PKPushRegistryDelegate, CXProviderDelegate, UNUserNotificationCenterDelegate {
    private var pushRegistry: PKPushRegistry?
    private var voipToken: String?
    private var apnsToken: String?
    private var callProvider: CXProvider?
    private var callController: CXCallController?
    private var activeCalls: [UUID: String] = [:] // Track active calls
    private var sessionID: String?
    private var callType: String?
    
    // App readiness tracking
    private var isAppReady = false
    private var pendingNotifications: [(String, [String: Any])] = []
    private var webViewReadyObserver: NSObjectProtocol?
    
    override public func load() {
        super.load()
        print("🔧 CapacitorPushPlugin: Loading plugin...")
        setupAppReadinessObservers()
        setupNotificationObservers()
        setupVoIPPushRegistry()
        setupCallKit()
        setupUserNotifications()
    }
    
    private func setupUserNotifications() {
        print("🔧 Setting up User Notifications...")
        UNUserNotificationCenter.current().delegate = self
    }
    
    private func setupAppReadinessObservers() {
        print("🔧 Setting up app readiness observers...")
        
        // Listen for app state changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWillResignActive),
            name: UIApplication.willResignActiveNotification,
            object: nil
        )
        
        // Listen for Capacitor webview ready
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(webViewDidLoad),
            name: .capacitorViewDidLoad,
            object: nil
        )
        
        // Check initial app state
        DispatchQueue.main.async {
            self.checkAppReadiness()
        }
    }
    
    @objc private func appDidBecomeActive() {
        print("📱 App became active")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.checkAppReadiness()
        }
    }
    
    @objc private func appWillResignActive() {
        print("📱 App will resign active")
        // Don't set isAppReady = false here for CallKit compatibility
    }
    
    @objc private func webViewDidLoad() {
        print("📱 WebView loaded")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            self.checkAppReadiness()
        }
    }
    
    private func checkAppReadiness() {
        let appState = UIApplication.shared.applicationState
        let hasWebView = self.webView != nil
        let hasBridge = self.bridge != nil
        
        print("📱 Checking app readiness - State: \(appState.rawValue), WebView: \(hasWebView), Bridge: \(hasBridge)")
        
        // App is ready if it's active OR background (for CallKit) and has webview/bridge
        let wasReady = isAppReady
        isAppReady = (appState == .active || appState == .background) && hasWebView && hasBridge
        
        if isAppReady && !wasReady {
            print("✅ App is now ready - flushing pending notifications")
            flushPendingNotifications()
        }
    }
    
    private func flushPendingNotifications() {
        guard !pendingNotifications.isEmpty else { return }
        
        print("📤 Flushing \(pendingNotifications.count) pending notifications")
        
        for (event, data) in pendingNotifications {
            print("📤 Sending delayed notification: \(event)")
            super.notifyListeners(event, data: data)
        }
        pendingNotifications.removeAll()
    }
    
    // Special method for CallKit notifications that need immediate delivery
    private func sendCallKitNotification(_ eventName: String, data: [String: Any]) {
        let dataToSend = data
        
        // For CallKit events, we need to be more lenient with app readiness
        let appState = UIApplication.shared.applicationState
        let hasWebView = self.webView != nil
        let hasBridge = self.bridge != nil
        
        let isCallKitReady = (appState == .active || appState == .background) && hasWebView && hasBridge
        
        if isCallKitReady {
            print("📤 Sending CallKit notification immediately: \(eventName)")
            super.notifyListeners(eventName, data: dataToSend)
        } else {
            print("📤 Queueing CallKit notification: \(eventName)")
            pendingNotifications.append((eventName, dataToSend))
            
            // Try to flush after a delay
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                self.checkAppReadiness()
            }
        }
    }
    
    // Override notifyListeners to handle app readiness for regular notifications
    override public func notifyListeners(_ eventName: String, data: [String : Any]?) {
        let dataToSend = data ?? [:]
        
        if isAppReady {
            print("📤 Sending notification immediately: \(eventName)")
            super.notifyListeners(eventName, data: dataToSend)
        } else {
            print("📤 Queueing notification (app not ready): \(eventName)")
            pendingNotifications.append((eventName, dataToSend))
            
            // Try to flush after a delay in case app becomes ready
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                self.checkAppReadiness()
            }
        }
    }
    
    private func setupNotificationObservers() {
        print("🔧 Setting up notification observers...")
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(didRegisterForRemoteNotifications(_:)),
            name: .init("CapacitorPushPluginDidRegister"),
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(didFailToRegisterForRemoteNotifications(_:)),
            name: .init("CapacitorPushPluginDidFail"),
            object: nil
        )
        
        // Add observer for push notification received
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(didReceiveRemoteNotification(_:)),
            name: .init("CapacitorPushPluginDidReceive"),
            object: nil
        )
    }
    
    @objc private func didReceiveRemoteNotification(_ notification: Notification) {
        guard let userInfo = notification.userInfo?["userInfo"] as? [String: Any] else {
            print("❌ Push: No userInfo in notification")
            return
        }
        
        print("📦 Push notification received: \(userInfo)")
        
        // Trigger pushNotificationReceived event
        notifyListeners("pushNotificationReceived", data: [
            "data": userInfo,
            "id": userInfo["id"] as? String ?? UUID().uuidString
        ])
    }
    
    private func setupVoIPPushRegistry() {
        print("🔧 Setting up VoIP Push Registry...")
        
        // Ensure we're on main queue
        DispatchQueue.main.async {
            self.pushRegistry = PKPushRegistry(queue: DispatchQueue.main)
            self.pushRegistry?.delegate = self
            self.pushRegistry?.desiredPushTypes = [.voIP]
            
            print("📱 VoIP Push Registry configured with delegate: \(String(describing: self.pushRegistry?.delegate))")
            print("📱 Desired push types: \(String(describing: self.pushRegistry?.desiredPushTypes))")
        }
    }
    
    private func setupCallKit() {
        print("🔧 Setting up CallKit...")
        let config: CXProviderConfiguration =  CXProviderConfiguration();
        config.supportsVideo = true
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.generic]
        config.ringtoneSound = "ringtone.caf" // Optional, add to your bundle
        
        callProvider = CXProvider(configuration: config)
        callProvider?.setDelegate(self, queue: nil)
        callController = CXCallController()
        
        print("📞 CallKit configured successfully")
    }
    
    @objc private func didRegisterForRemoteNotifications(_ notification: Notification) {
        guard let deviceToken = notification.userInfo?["deviceToken"] as? Data else {
            print("❌ APNS: No device token in notification")
            return
        }
        
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        self.apnsToken = tokenString
        print("✅ APNS Token received: \(tokenString)")
        
        notifyListeners("apnsTokenReceived", data: [
            "token": tokenString,
            "type": "apns"
        ])
        
        notifyListeners("registration", data: [
            "token": tokenString
        ])
    }
    
    @objc private func didFailToRegisterForRemoteNotifications(_ notification: Notification) {
        guard let error = notification.userInfo?["error"] as? Error else { return }
        print("❌ APNS Registration failed: \(error.localizedDescription)")
        
        notifyListeners("registrationError", data: [
            "error": error.localizedDescription
        ])
    }
    
    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve(["value": value])
    }
    
    @objc func register(_ call: CAPPluginCall) {
        print("📱 Register called - requesting notification permissions...")
        
        DispatchQueue.main.async {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
                if let error = error {
                    print("❌ Permission request failed: \(error.localizedDescription)")
                    call.reject("Permission request failed", error.localizedDescription)
                    return
                }
                
                print("📱 Notification permission granted: \(granted)")
                if granted {
                    DispatchQueue.main.async {
                        UIApplication.shared.registerForRemoteNotifications()
                        call.resolve()
                    }
                } else {
                    call.reject("Permission denied")
                }
            }
        }
    }
    
    @objc func getToken(_ call: CAPPluginCall) {
        var tokens: [String: String] = [:]
        
        if let apnsToken = self.apnsToken {
            tokens["apnsToken"] = apnsToken
            tokens["fcmToken"] = apnsToken // FCM on iOS uses APNs token
        }
        
        if let voipToken = self.voipToken {
            tokens["voipToken"] = voipToken
        }
        
        print("📱 Tokens requested - APNS: \(apnsToken != nil), VoIP: \(voipToken != nil)")
        
        if tokens.isEmpty {
            call.reject("No tokens available")
        } else {
            call.resolve(tokens)
        }
    }
    
    @objc func getVoIPToken(_ call: CAPPluginCall) {
        print("📱 VoIP token requested: \(voipToken != nil ? "Available" : "Not available")")
        
        if let voipToken = self.voipToken {
            call.resolve(["voipToken": voipToken])
        } else {
            call.reject("VoIP token not available")
        }
    }
    
    @objc func enableVoIP(_ call: CAPPluginCall) {
        let enable = call.getBool("enable") ?? true
        print("📱 VoIP enable/disable called: \(enable)")
        
        if enable {
            setupVoIPPushRegistry()
            call.resolve()
        } else {
            pushRegistry?.desiredPushTypes = []
            call.resolve()
        }
    }
    
    @objc func setBadgeCount(_ call: CAPPluginCall) {
        let count = call.getInt("count") ?? 0
        DispatchQueue.main.async {
            UIApplication.shared.applicationIconBadgeNumber = count
            call.resolve()
        }
    }
    
    @objc func endCall(_ call: CAPPluginCall) {
        guard let callUUID = call.getString("callId"),
              let uuid = UUID(uuidString: callUUID) else {
            call.reject("Invalid call ID")
            return
        }
        
        print("📞 Ending call: \(callUUID)")
        let endCallAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction(action: endCallAction)
        
        callController?.request(transaction) { error in
            if let error = error {
                print("❌ Failed to end call: \(error.localizedDescription)")
                call.reject("Failed to end call", error.localizedDescription)
            } else {
                print("✅ Call ended successfully")
                call.resolve()
            }
        }
    }
    
    @objc func answerCall(_ call: CAPPluginCall) {
        guard let callUUID = call.getString("callId"),
              let uuid = UUID(uuidString: callUUID) else {
            call.reject("Invalid call ID")
            return
        }
        
        print("📞 Answering call: \(callUUID)")
        let answerAction = CXAnswerCallAction(call: uuid)
        let transaction = CXTransaction(action: answerAction)
        
        callController?.request(transaction) { error in
            if let error = error {
                print("❌ Failed to answer call: \(error.localizedDescription)")
                call.reject("Failed to answer call", error.localizedDescription)
            } else {
                print("✅ Call answered successfully")
                call.resolve()
            }
        }
    }
    
    @objc public override func requestPermissions(_ call: CAPPluginCall) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error = error {
                call.reject("Permission request failed", error.localizedDescription)
                return
            }
            call.resolve(["receive": granted ? "granted" : "denied"])
        }
    }
    
    @objc public override func checkPermissions(_ call: CAPPluginCall) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let status = settings.authorizationStatus
            var permissionStatus = "denied"
            
            switch status {
            case .authorized, .provisional:
                permissionStatus = "granted"
            case .notDetermined:
                permissionStatus = "prompt"
            case .denied:
                permissionStatus = "denied"
            @unknown default:
                permissionStatus = "denied"
            }
            
            call.resolve(["receive": permissionStatus])
        }
    }
    
    // MARK: - UNUserNotificationCenterDelegate
    
    // Called when notification is received while app is in foreground
    public func userNotificationCenter(_ center: UNUserNotificationCenter,
                                     willPresent notification: UNNotification,
                                     withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        
        print("📦 Push notification received in foreground: \(notification.request.content.userInfo)")
        
        // Trigger pushNotificationReceived event
        notifyListeners("pushNotificationReceived", data: [
            "data": notification.request.content.userInfo,
            "id": notification.request.identifier,
            "title": notification.request.content.title,
            "body": notification.request.content.body
        ])
        
        // Show the notification even when app is in foreground
        completionHandler([.alert, .badge, .sound])
    }
    
    // Called when user taps on notification
    public func userNotificationCenter(_ center: UNUserNotificationCenter,
                                     didReceive response: UNNotificationResponse,
                                     withCompletionHandler completionHandler: @escaping () -> Void) {
        
        print("📱 Push notification tapped: \(response.notification.request.content.userInfo)")
        print("📱 Action identifier: \(response.actionIdentifier)")
        
        let notification = response.notification
        var actionData: [String: Any] = [
            "actionId": response.actionIdentifier,
            "inputValue": "",
            "notification": [
                "id": notification.request.identifier,
                "title": notification.request.content.title,
                "body": notification.request.content.body,
                "data": notification.request.content.userInfo,
                "tag": notification.request.content.userInfo["tag"] as? String ?? "",
                "badge": notification.request.content.badge?.intValue ?? 0
            ]
        ]
        
        // Handle text input response if available
        if let textResponse = response as? UNTextInputNotificationResponse {
            actionData["inputValue"] = textResponse.userText
            print("📱 Text input received: \(textResponse.userText)")
        }
        
        // Trigger pushNotificationActionPerformed event
        notifyListeners("pushNotificationActionPerformed", data: actionData)
        
        completionHandler()
    }
    
    // MARK: - PKPushRegistryDelegate
    public func pushRegistry(_ registry: PKPushRegistry, didUpdate pushCredentials: PKPushCredentials, for type: PKPushType) {
        print("📱 Push Registry - didUpdate called for type: \(type)")
        
        guard type == .voIP else {
            print("❌ Push type is not VoIP: \(type)")
            return
        }
        
        let tokenString = pushCredentials.token.reduce("", {$0 + String(format: "%02X", $1)})
        self.voipToken = tokenString
        print("✅ VoIP Token received: \(tokenString)")
        print("📱 VoIP Token length: \(tokenString.count)")
        
        notifyListeners("voipTokenReceived", data: [
            "token": tokenString,
            "type": "voip"
        ])
        
        notifyListeners("voipRegistration", data: [
            "token": tokenString,
            "type": "voip"
        ])
    }
    
    public func pushRegistry(_ registry: PKPushRegistry, didInvalidatePushTokenFor type: PKPushType) {
        print("📱 Push Registry - didInvalidatePushTokenFor called for type: \(type)")
        
        guard type == .voIP else {
            print("❌ Push type is not VoIP: \(type)")
            return
        }
        
        print("❌ VoIP Token invalidated")
        self.voipToken = nil
        notifyListeners("voipTokenInvalidated", data: [:])
    }
    
    public func pushRegistry(_ registry: PKPushRegistry,
                             didReceiveIncomingPushWith payload: PKPushPayload,
                             for type: PKPushType,
                             completion: @escaping () -> Void) {
        
        print("📦 VOIP PUSH RECEIVED! Type: \(type)")
        print("📦 Registry: \(registry)")
        print("📦 Payload: \(payload)")
        
        guard type == .voIP else {
            print("❌ Push type is not VoIP: \(type)")
            completion()
            return
        }
        
        print("📦 Full VoIP Payload Dictionary: \(payload.dictionaryPayload)")
        
        let uuid = UUID()
        print("📦 Generated UUID for call: \(uuid)")
        
        // Extract correct values based on actual payload
        let callerName = payload.dictionaryPayload["senderName"] as? String ??
                        payload.dictionaryPayload["title"] as? String ??
                        payload.dictionaryPayload["name"] as? String ?? "Unknown"
        
        let callerId = payload.dictionaryPayload["sender"] as? String ??
                      payload.dictionaryPayload["callerId"] as? String ??
                      payload.dictionaryPayload["from"] as? String ?? "unknown"
        
        let callType = payload.dictionaryPayload["callType"] as? String ?? "audio"
        self.callType = callType
        
        let callAction = payload.dictionaryPayload["callAction"] as? String ?? "initiated"
        self.sessionID = payload.dictionaryPayload["sessionId"] as? String ?? ""
        
        print("📦 Extracted - Caller: \(callerName), ID: \(callerId), Type: \(callType), Action: \(callAction)")
        
        if callAction != "initiated" {
            print("⚠️ Call action is not 'initiated': \(callAction) - Reporting dummy call to avoid crash")
            // 🔒 Ignore if it's a cancel/ended for a session we've already accepted
            if callAction == "cancelled" || callAction == "ended" {
                if self.sessionID == payload.dictionaryPayload["sessionId"] as? String {
                    print("ℹ️ Ignoring cancelled/ended push for already handled session: \(self.sessionID ?? "")")
                    completion()
                    return
                }
            }
            let update = CXCallUpdate()
            update.remoteHandle = CXHandle(type: .generic, value: callerId)
            update.localizedCallerName = callerName
            update.hasVideo = (callType == "video")

            callProvider?.reportNewIncomingCall(with: uuid, update: update) { error in
                if let error = error {
                    print("❌ Dummy CallKit error: \(error.localizedDescription)")
                } else {
                    print("✅ Dummy call reported successfully for compliance")
                }

                // End the dummy call immediately
                let endCallAction = CXEndCallAction(call: uuid)
                let transaction = CXTransaction(action: endCallAction)
                self.callController?.request(transaction) { err in
                    if let err = err {
                        print("❌ Failed to auto-end dummy call: \(err.localizedDescription)")
                    } else {
                        print("✅ Dummy call auto-ended")
                    }
                }

                completion()
            }

            return
        }
        
        let update = CXCallUpdate()
        update.remoteHandle = CXHandle(type: .generic, value: callerId)
        update.localizedCallerName = callerName
        update.hasVideo = (callType == "video")
        
        print("📞 CallKit update created - Handle: \(callerId), Name: \(callerName), Video: \(update.hasVideo)")
        
        // Store the call info
        activeCalls[uuid] = callerId
        print("📞 Stored active call: \(uuid) -> \(callerId)")
        
        // Report call to CallKit
        callProvider?.reportNewIncomingCall(with: uuid, update: update) { error in
            if let error = error {
                print("❌ CallKit error: \(error.localizedDescription)")
                // Remove from active calls if failed
                self.activeCalls.removeValue(forKey: uuid)
            } else {
                print("✅ CallKit: Incoming call successfully reported.")
            }
            
            // Notify JS side
            print("📤 Notifying JS side with voipNotificationReceived")
            self.notifyListeners("voipNotificationReceived", data: [
                "id": uuid.uuidString,
                "data": payload.dictionaryPayload,
                "callerName": callerName,
                "callerId": callerId,
                "callType": callType
            ])
            
            completion()
        }
    }
    
    // MARK: - CXProviderDelegate
    public func providerDidReset(_ provider: CXProvider) {
        print("📞 CallKit: Provider reset.")
        activeCalls.removeAll()
    }
    
    
    public func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        print("📞 CallKit: Answer Call - \(String(describing: self.sessionID))")
        
        // Create call update to connect the call and hide CallKit UI
        let update = CXCallUpdate()
        update.hasVideo = (self.callType == "video")
        
        // Safely unwrap sessionID and callType
        guard let sessionID = self.sessionID, let callType = self.callType else {
            print("❌ Missing sessionID or callType")
            action.fulfill()
            return
        }
        
        // Use the special CallKit notification method for immediate delivery
        self.sendCallKitNotification("voipCallAccepted", data: [
            "sessionId": sessionID,
            "type": callType
        ])
        
        print("📞 VOIP data sent to JS: Answer Call - \(sessionID)")
        
        // Report the call update to connect the call
        callProvider?.reportCall(with: action.callUUID, updated: update)
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        print("📞 CallKit: End Call - \(String(describing: self.sessionID))")
        
        // Safely unwrap sessionID and callType
        guard let sessionID = self.sessionID, let callType = self.callType else {
            print("❌ Missing sessionID or callType")
            action.fulfill()
            return
        }
        
        // Use the special CallKit notification method for immediate delivery
        self.sendCallKitNotification("voipCallRejected", data: [
            "sessionId": sessionID,
            "type": callType
        ])
        
        print("📞 VOIP data sent to JS: End Call - \(sessionID)")
        
        // Remove from active calls
        activeCalls.removeValue(forKey: action.callUUID)
        
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, perform action: CXSetHeldCallAction) {
        print("📞 CallKit: Set Call Hold - \(action.callUUID)")
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        print("📞 CallKit: Set Call Muted - \(action.callUUID)")
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        print("📞 CallKit: Start Call - \(action.callUUID)")
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        print("📞 CallKit: Audio session activated")
        notifyListeners("audioSessionActivated", data: [:])
    }
    
    public func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        print("📞 CallKit: Audio session deactivated")
        notifyListeners("audioSessionDeactivated", data: [:])
    }
    
    deinit {
        print("🔧 CapacitorPushPlugin: Deinitializing...")
        NotificationCenter.default.removeObserver(self)
    }
}

// Extension to add Capacitor-specific notification
extension Notification.Name {
    static let capacitorViewDidLoad = Notification.Name("CapacitorViewDidLoad")
}
