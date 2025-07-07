import { WebPlugin } from '@capacitor/core';

import type { CapacitorPushPlugin, PermissionStatus } from './definitions';

export class CapacitorPushWeb extends WebPlugin implements CapacitorPushPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }

  async flushPending(): Promise<void> {
    console.warn('flushPending is not applicable on web');
    return;
  }

  private registration?: ServiceWorkerRegistration;
  private vapidPublicKey?: string;

  async register(): Promise<void> {
    if ('serviceWorker' in navigator && 'PushManager' in window) {
      try {
        this.registration = await navigator.serviceWorker.register('/sw.js');
        console.log('Service Worker registered');
      } catch (error) {
        console.error('Service Worker registration failed:', error);
        throw error;
      }
    } else {
      throw new Error('Push notifications not supported');
    }
  }

  async getToken(): Promise<{ [key: string]: string }> {
    if (!this.registration) {
      throw new Error('Service Worker not registered');
    }

    const subscription = await this.registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: this.vapidPublicKey
    });

    return {
      webPushToken: JSON.stringify(subscription),
      fcmToken: JSON.stringify(subscription) // For web, it's the same
    };
  }

  async getVoIPToken(): Promise<{ voipToken: string }> {
    // VoIP not available on web
    throw new Error('VoIP tokens not supported on web platform');
  }

  async enableVoIP(): Promise<void> {
    // VoIP not available on web
    throw new Error('VoIP notifications not supported on web');
  }

  async setBadgeCount(): Promise<void> {
    // Badge count not available on web
    console.warn('Badge count not supported on web');
  }

  async requestPermissions(): Promise<PermissionStatus> {
    const result = await Notification.requestPermission();
    return {
      receive: result === 'granted' ? 'granted' : 'denied'
    };
  }

  async checkPermissions(): Promise<PermissionStatus> {
    const result = Notification.permission;
    return {
      receive: result === 'granted' ? 'granted' : 'denied'
    };
  }

  async testVoIPSetup(): Promise<{
    isVoIPSupported: boolean;
    hasVoIPToken: boolean;
    voipToken: string;
  }> {
    // VoIP not supported on web
    return {
      isVoIPSupported: false,
      hasVoIPToken: false,
      voipToken: 'Not supported on web'
    };
  }
}
