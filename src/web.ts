import { WebPlugin } from '@capacitor/core';

import type { CapacitorPushPlugin } from './definitions';

export class CapacitorPushWeb extends WebPlugin implements CapacitorPushPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
