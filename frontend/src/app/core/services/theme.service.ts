import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'notify_theme';
  private readonly darkSignal = signal<boolean>(this.loadInitial());

  readonly isDark = this.darkSignal.asReadonly();

  constructor() {
    this.apply();
  }

  toggle(): void {
    this.darkSignal.update((v) => !v);
    this.apply();
  }

  private apply(): void {
    const isDark = this.darkSignal();
    document.documentElement.classList.toggle('dark', isDark);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(this.STORAGE_KEY, isDark ? 'dark' : 'light');
    }
  }

  private loadInitial(): boolean {
    if (typeof localStorage !== 'undefined') {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored === 'dark') return true;
      if (stored === 'light') return false;
    }
    return false;
  }
}
