# capacitor-push

Push plugin

## Install

```bash
npm install capacitor-push
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`register()`](#register)
* [`flushPending()`](#flushpending)
* [`getToken()`](#gettoken)
* [`getVoIPToken()`](#getvoiptoken)
* [`enableVoIP(...)`](#enablevoip)
* [`setBadgeCount(...)`](#setbadgecount)
* [`requestPermissions()`](#requestpermissions)
* [`checkPermissions()`](#checkpermissions)
* [`testVoIPSetup()`](#testvoipsetup)
* [`addListener('registration', ...)`](#addlistenerregistration-)
* [`addListener('voipTokenReceived', ...)`](#addlistenervoiptokenreceived-)
* [`addListener('apnsTokenReceived', ...)`](#addlistenerapnstokenreceived-)
* [`addListener('voipCallAccepted', ...)`](#addlistenervoipcallaccepted-)
* [`addListener('voipCallRejected', ...)`](#addlistenervoipcallrejected-)
* [`addListener('registrationError', ...)`](#addlistenerregistrationerror-)
* [`addListener('pushNotificationReceived', ...)`](#addlistenerpushnotificationreceived-)
* [`addListener('pushNotificationActionPerformed', ...)`](#addlistenerpushnotificationactionperformed-)
* [`addListener('voipRegistration', ...)`](#addlistenervoipregistration-)
* [`addListener('voipNotificationReceived', ...)`](#addlistenervoipnotificationreceived-)
* [`addListener('voipTokenInvalidated', ...)`](#addlistenervoiptokeninvalidated-)
* [`addListener('voipCallCancelled', ...)`](#addlistenervoipcallcancelled-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### register()

```typescript
register() => Promise<void>
```

Register for push notifications

--------------------


### flushPending()

```typescript
flushPending() => Promise<void>
```

--------------------


### getToken()

```typescript
getToken() => Promise<{ [key: string]: string; }>
```

Get all available tokens (APNs, VoIP, FCM)

**Returns:** <code>Promise&lt;{ [key: string]: string; }&gt;</code>

--------------------


### getVoIPToken()

```typescript
getVoIPToken() => Promise<{ voipToken: string; }>
```

Get VoIP token specifically (iOS only)

**Returns:** <code>Promise&lt;{ voipToken: string; }&gt;</code>

--------------------


### enableVoIP(...)

```typescript
enableVoIP(options: { enable: boolean; }) => Promise<void>
```

Enable/disable VoIP notifications (iOS only)

| Param         | Type                              |
| ------------- | --------------------------------- |
| **`options`** | <code>{ enable: boolean; }</code> |

--------------------


### setBadgeCount(...)

```typescript
setBadgeCount(options: { count: number; }) => Promise<void>
```

Set badge count (iOS only)

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ count: number; }</code> |

--------------------


### requestPermissions()

```typescript
requestPermissions() => Promise<PermissionStatus>
```

Request permissions

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### checkPermissions()

```typescript
checkPermissions() => Promise<PermissionStatus>
```

Check permission status

**Returns:** <code>Promise&lt;<a href="#permissionstatus">PermissionStatus</a>&gt;</code>

--------------------


### testVoIPSetup()

```typescript
testVoIPSetup() => Promise<{ isVoIPSupported: boolean; hasVoIPToken: boolean; voipToken: string; }>
```

Test VoIP setup (iOS only)

**Returns:** <code>Promise&lt;{ isVoIPSupported: boolean; hasVoIPToken: boolean; voipToken: string; }&gt;</code>

--------------------


### addListener('registration', ...)

```typescript
addListener(eventName: 'registration', listenerFunc: (token: { token: string; }) => void) => Promise<PluginListenerHandle>
```

Add listeners for various notification events

| Param              | Type                                                |
| ------------------ | --------------------------------------------------- |
| **`eventName`**    | <code>'registration'</code>                         |
| **`listenerFunc`** | <code>(token: { token: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipTokenReceived', ...)

```typescript
addListener(eventName: 'voipTokenReceived', listenerFunc: (data: { token: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                             |
| ------------------ | ---------------------------------------------------------------- |
| **`eventName`**    | <code>'voipTokenReceived'</code>                                 |
| **`listenerFunc`** | <code>(data: { token: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('apnsTokenReceived', ...)

```typescript
addListener(eventName: 'apnsTokenReceived', listenerFunc: (data: { token: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                             |
| ------------------ | ---------------------------------------------------------------- |
| **`eventName`**    | <code>'apnsTokenReceived'</code>                                 |
| **`listenerFunc`** | <code>(data: { token: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipCallAccepted', ...)

```typescript
addListener(eventName: 'voipCallAccepted', listenerFunc: (data: { sessionId: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| **`eventName`**    | <code>'voipCallAccepted'</code>                                      |
| **`listenerFunc`** | <code>(data: { sessionId: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipCallRejected', ...)

```typescript
addListener(eventName: 'voipCallRejected', listenerFunc: (data: { sessionId: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| **`eventName`**    | <code>'voipCallRejected'</code>                                      |
| **`listenerFunc`** | <code>(data: { sessionId: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('registrationError', ...)

```typescript
addListener(eventName: 'registrationError', listenerFunc: (error: any) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                 |
| ------------------ | ------------------------------------ |
| **`eventName`**    | <code>'registrationError'</code>     |
| **`listenerFunc`** | <code>(error: any) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('pushNotificationReceived', ...)

```typescript
addListener(eventName: 'pushNotificationReceived', listenerFunc: (notification: PushNotificationSchema) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'pushNotificationReceived'</code>                                                              |
| **`listenerFunc`** | <code>(notification: <a href="#pushnotificationschema">PushNotificationSchema</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('pushNotificationActionPerformed', ...)

```typescript
addListener(eventName: 'pushNotificationActionPerformed', listenerFunc: (data: { id: string; convType: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                              |
| ------------------ | ----------------------------------------------------------------- |
| **`eventName`**    | <code>'pushNotificationActionPerformed'</code>                    |
| **`listenerFunc`** | <code>(data: { id: string; convType: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipRegistration', ...)

```typescript
addListener(eventName: 'voipRegistration', listenerFunc: (data: { token: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                             |
| ------------------ | ---------------------------------------------------------------- |
| **`eventName`**    | <code>'voipRegistration'</code>                                  |
| **`listenerFunc`** | <code>(data: { token: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipNotificationReceived', ...)

```typescript
addListener(eventName: 'voipNotificationReceived', listenerFunc: (notification: VoIPNotificationSchema) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'voipNotificationReceived'</code>                                                              |
| **`listenerFunc`** | <code>(notification: <a href="#voipnotificationschema">VoIPNotificationSchema</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipTokenInvalidated', ...)

```typescript
addListener(eventName: 'voipTokenInvalidated', listenerFunc: (data: any) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                |
| ------------------ | ----------------------------------- |
| **`eventName`**    | <code>'voipTokenInvalidated'</code> |
| **`listenerFunc`** | <code>(data: any) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('voipCallCancelled', ...)

```typescript
addListener(eventName: 'voipCallCancelled', listenerFunc: (data: { sessionId: string; type: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| **`eventName`**    | <code>'voipCallCancelled'</code>                                     |
| **`listenerFunc`** | <code>(data: { sessionId: string; type: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### PermissionStatus

| Prop          | Type                                                        |
| ------------- | ----------------------------------------------------------- |
| **`receive`** | <code><a href="#permissionstate">PermissionState</a></code> |
| **`voip`**    | <code><a href="#permissionstate">PermissionState</a></code> |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PushNotificationSchema

| Prop               | Type                 |
| ------------------ | -------------------- |
| **`title`**        | <code>string</code>  |
| **`body`**         | <code>string</code>  |
| **`id`**           | <code>string</code>  |
| **`badge`**        | <code>number</code>  |
| **`notification`** | <code>any</code>     |
| **`data`**         | <code>any</code>     |
| **`click_action`** | <code>string</code>  |
| **`link`**         | <code>string</code>  |
| **`group`**        | <code>string</code>  |
| **`groupSummary`** | <code>boolean</code> |


#### VoIPNotificationSchema

| Prop             | Type                            |
| ---------------- | ------------------------------- |
| **`id`**         | <code>string</code>             |
| **`data`**       | <code>any</code>                |
| **`callerName`** | <code>string</code>             |
| **`callerId`**   | <code>string</code>             |
| **`callType`**   | <code>'audio' \| 'video'</code> |


### Type Aliases


#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>

</docgen-api>
