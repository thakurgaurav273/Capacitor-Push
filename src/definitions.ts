export interface CapacitorPushPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
