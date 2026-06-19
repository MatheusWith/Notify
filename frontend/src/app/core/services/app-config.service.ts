import { Injectable } from '@angular/core';

interface WindowWithConfig {
  __RUNTIME_CONFIG?: { apiUrl: string };
}

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  readonly apiUrl: string;

  constructor() {
    const runtime = (window as unknown as WindowWithConfig).__RUNTIME_CONFIG;
    const baseUrl = runtime?.apiUrl ?? '';
    this.apiUrl = baseUrl + '/api/v1';
  }
}
