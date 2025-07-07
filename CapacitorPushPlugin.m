#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

CAP_PLUGIN(CapacitorPushPlugin, "CapacitorPush",
  CAP_PLUGIN_METHOD(echo, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(register, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(getToken, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(getVoIPToken, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(enableVoIP, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(setBadgeCount, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(requestPermissions, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(checkPermissions, CAPPluginReturnPromise);
  CAP_PLUGIN_METHOD(testVoIPSetup, CAPPluginReturnPromise);
)
