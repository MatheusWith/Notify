import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  readonly apiUrl: string;

  constructor() {
    const runtime = (window as any).__RUNTIME_CONFIG;
    this.apiUrl = runtime?.apiUrl ?? '/api/v1';
  }
}
