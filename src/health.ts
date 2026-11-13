import { Capacitor, registerPlugin } from '@capacitor/core';

export type HealthExercise = { id: string; title: string; kind: string; minutes: number; date: string; source: string };
export type HealthSummary = {
  stepsToday?: number;
  stepsByDay?: { date: string; steps: number }[];
  heartRateAvg?: number;
  heartRateMin?: number;
  heartRateMax?: number;
  sleepMinutes?: number;
  exercises?: HealthExercise[];
  grantedCount: number;
};
export type HealthStatus = { available: boolean; needsInstall: boolean; granted: boolean; grantedCount?: number };

/** Native side: android/app/src/main/java/cz/fittrack/app/HealthPlugin.kt (Health Connect, read-only). */
interface HealthPlugin {
  status(): Promise<HealthStatus>;
  authorize(): Promise<{ granted: boolean; grantedCount: number }>;
  openHealthConnect(): Promise<void>;
  readSummary(options: { days: number }): Promise<HealthSummary>;
}

export const Health = registerPlugin<HealthPlugin>('Health');
export const isNative = Capacitor.isNativePlatform();
export const isAndroid = Capacitor.getPlatform() === 'android';
