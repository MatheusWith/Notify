import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    (window as any).__RUNTIME_CONFIG = { apiUrl: 'http://test' };

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    delete (window as any).__RUNTIME_CONFIG;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should start with null tokens when sessionStorage is empty', () => {
    expect(service.accessToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should persist tokens to sessionStorage on login', () => {
    const mockResponse = {
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    };

    service
      .login({ email: 'a@b.com', password: 'Secret1!' })
      .subscribe();

    const req = httpMock.expectOne((request) =>
      request.url.includes('/auth/login'),
    );
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(sessionStorage.getItem('notify_access_token')).toBe(
      'test-access-token',
    );
    expect(sessionStorage.getItem('notify_refresh_token')).toBe(
      'test-refresh-token',
    );
  });

  it('should restore tokens from sessionStorage on creation', () => {
    sessionStorage.setItem('notify_access_token', 'stored-access');
    sessionStorage.setItem('notify_refresh_token', 'stored-refresh');

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const newService = TestBed.inject(AuthService);
    expect(newService.accessToken()).toBe('stored-access');
    expect(newService.isAuthenticated()).toBe(true);
  });

  it('should clear sessionStorage on logout', () => {
    // First set some tokens in sessionStorage
    sessionStorage.setItem('notify_access_token', 'some-token');
    sessionStorage.setItem('notify_refresh_token', 'some-refresh');
    // Ensure signals reflect the stored tokens
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const localService = TestBed.inject(AuthService);

    localService.logout();

    expect(sessionStorage.getItem('notify_access_token')).toBeNull();
    expect(sessionStorage.getItem('notify_refresh_token')).toBeNull();
    expect(localService.accessToken()).toBeNull();
    expect(localService.isAuthenticated()).toBe(false);
  });

  it('should send refresh token in refreshToken()', () => {
    const loginResponse = {
      accessToken: 'access-1',
      refreshToken: 'refresh-1',
      tokenType: 'Bearer',
      expiresIn: 900,
    };

    service.login({ email: 'a@b.com', password: 'Secret1!' }).subscribe();
    httpMock
      .expectOne((req) => req.url.includes('/auth/login'))
      .flush(loginResponse);

    const refreshResponse = {
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      tokenType: 'Bearer',
      expiresIn: 900,
    };

    service.refreshToken().subscribe();

    const refreshReq = httpMock.expectOne((req) =>
      req.url.includes('/auth/refresh'),
    );
    expect(refreshReq.request.method).toBe('POST');
    expect(refreshReq.request.body).toEqual({ refreshToken: 'refresh-1' });
    refreshReq.flush(refreshResponse);
  });
});
