import { registerPlugin } from '@capacitor/core';

import type { CapacitorPushPlugin } from './definitions';

const CapacitorPush = registerPlugin<CapacitorPushPlugin>('CapacitorPush', {
  web: () => import('./web').then((m) => new m.CapacitorPushWeb()),
});

export * from './definitions';
export { CapacitorPush };
