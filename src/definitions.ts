import { PluginListenerHandle } from "@capacitor/core";

export interface CapacitorPushPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;

  /**
   * Register for push notifications
   */
  register(): Promise<void>;
  flushPending(): Promise<void>;


  /**
   * Get all available tokens (APNs, VoIP, FCM)
   */
  getToken(): Promise<{ [key: string]: string }>;

  /**
   * Get VoIP token specifically (iOS only)
   */
  getVoIPToken(): Promise<{ voipToken: string }>;

  /**
   * Enable/disable VoIP notifications (iOS only)
   */
  enableVoIP(options: { enable: boolean }): Promise<void>;

  /**
   * Set badge count (iOS only)
   */
  setBadgeCount(options: { count: number }): Promise<void>;

  /**
   * Request permissions
   */
  requestPermissions(): Promise<PermissionStatus>;

  /**
   * Check permission status
   */
  checkPermissions(): Promise<PermissionStatus>;

  /**
   * Test VoIP setup (iOS only)
   */
  testVoIPSetup(): Promise<{
    isVoIPSupported: boolean;
    hasVoIPToken: boolean;
    voipToken: string;
  }>;

    /**
   * Set custom payload key mappings for flexible payload parsing
   */
 setKeyMappings(options: {
    sessionKey?: string;
    senderKey?: string;
    titleKey?: string;
    bodyKey?: string;
    senderAvatarKey?: string;
    receiverTypeKey?: string;
    receiverKey?: string;
    tagKey?: string;
  }): Promise<{
    sessionKey: string;
    senderKey: string;
    titleKey: string;
    bodyKey: string;
    senderAvatarKey: string;
    receiverTypeKey: string;
    receiverKey: string;
    tagKey: string;
  }>;

  /**
   * Add listeners for various notification events
   */
  addListener(
    eventName: 'registration',
    listenerFunc: (token: { token: string }) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'voipTokenReceived',
    listenerFunc: (data: { token: string; type: string }) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'apnsTokenReceived',
    listenerFunc: (data: { token: string; type: string }) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'voipCallAccepted',
    listenerFunc: (data: { sessionId: string; type: string }) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'voipCallRejected',
    listenerFunc: (data: { sessionId: string; type: string }) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'registrationError',
    listenerFunc: (error: any) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'pushNotificationReceived',
    listenerFunc: (notification: PushNotificationSchema) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'pushNotificationActionPerformed',
    listenerFunc: (data: { id: string, convType: string}) => void,
  ): Promise<PluginListenerHandle>;
  addListener(eventName: 'voipRegistration', listenerFunc: (data: {
    token: string;
    type: string;
  }) => void): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'voipNotificationReceived',
    listenerFunc: (notification: VoIPNotificationSchema) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: 'voipTokenInvalidated',
    listenerFunc: (data: any) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
  eventName: 'voipCallCancelled',
  listenerFunc: (data: { sessionId: string; type: string }) => void,
): Promise<PluginListenerHandle>;


  removeAllListeners(): Promise<void>;
}

export interface PermissionStatus {
  receive: PermissionState;
  voip?: PermissionState;
}

export interface PushNotificationSchema {
  title?: string;
  body?: string;
  id: string;
  badge?: number;
  notification?: any;
  data: any;
  click_action?: string;
  link?: string;
  group?: string;
  groupSummary?: boolean;
}

export interface VoIPNotificationSchema {
  id: string;
  data: any;
  callerName?: string;
  callerId?: string;
  callType?: 'audio' | 'video';
}

export interface ActionPerformed {
  actionId: string;
  inputValue?: string;
  notification: PushNotificationSchema;
}

export type PermissionState = 'prompt' | 'prompt-with-rationale' | 'granted' | 'denied';