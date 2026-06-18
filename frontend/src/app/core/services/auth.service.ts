import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AppConfigService } from './app-config.service';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  UpdateUserRequest,
  UserResponse,
} from '../../shared/models/auth.types';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly appConfig = inject(AppConfigService);
  private readonly apiUrl = this.appConfig.apiUrl;

  private readonly ACCESS_TOKEN_KEY = 'notify_access_token';
  private readonly REFRESH_TOKEN_KEY = 'notify_refresh_token';

  private readonly accessTokenSignal = signal<string | null>(
    sessionStorage.getItem(this.ACCESS_TOKEN_KEY),
  );
  private readonly refreshTokenSignal = signal<string | null>(
    sessionStorage.getItem(this.REFRESH_TOKEN_KEY),
  );
  private readonly userSignal = signal<UserResponse | null>(null);

  readonly isAuthenticated = computed(() => this.accessTokenSignal() !== null);
  readonly currentUser = this.userSignal.asReadonly();
  readonly accessToken = this.accessTokenSignal.asReadonly();

  constructor(private readonly http: HttpClient) {}

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/login`, request)
      .pipe(tap((response) => this.setTokens(response)));
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/register`, request)
      .pipe(tap((response) => this.setTokens(response)));
  }

  refreshToken(): Observable<AuthResponse> {
    const refresh = this.refreshTokenSignal();
    if (!refresh) {
      throw new Error('No refresh token available');
    }
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/auth/refresh`, {
        refreshToken: refresh,
      })
      .pipe(tap((response) => this.setTokens(response)));
  }

  getCurrentUser(): Observable<UserResponse> {
    return this.http
      .get<UserResponse>(`${this.apiUrl}/users/me`)
      .pipe(tap((user) => this.userSignal.set(user)));
  }

  updateProfile(request: UpdateUserRequest): Observable<UserResponse> {
    return this.http
      .put<UserResponse>(`${this.apiUrl}/users/me`, request)
      .pipe(tap((user) => this.userSignal.set(user)));
  }

  logout(): void {
    this.accessTokenSignal.set(null);
    this.refreshTokenSignal.set(null);
    this.userSignal.set(null);
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  private setTokens(response: AuthResponse): void {
    this.accessTokenSignal.set(response.accessToken);
    this.refreshTokenSignal.set(response.refreshToken);
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
  }
}
