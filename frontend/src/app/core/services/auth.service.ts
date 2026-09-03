import { Service, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, switchMap, catchError, share, finalize, of, throwError, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User, LoginRequest, RegisterRequest } from '../models/user';

const ACCESS_KEY = 'pc_access_token';
const REFRESH_KEY = 'pc_refresh_token';

@Service()
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private baseUrl = `${environment.apiUrl}/auth`;

  accessToken = signal<string | null>(localStorage.getItem(ACCESS_KEY));
  currentUser = signal<User | null>(null);
  isAuthenticated = computed(() => this.accessToken() !== null);

  private refresh$: Observable<AuthResponse> | null = null;

  register(request: RegisterRequest) {
    return this.http.post<User>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest) {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((res) => this.storeTokens(res)),
      switchMap(() => this.fetchCurrentUser()),
      catchError((err) => {
        this.clearTokens();
        return throwError(() => err);
      }),
    );
  }

  refresh(): Observable<AuthResponse> {
    if (!this.refresh$) {
      const refreshToken = this.getRefreshToken();
      this.refresh$ = this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, { refreshToken }).pipe(
        tap((res) => this.storeTokens(res)),
        finalize(() => (this.refresh$ = null)),
        share(),
      );
    }
    return this.refresh$;
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    this.http.post(`${this.baseUrl}/logout`, { refreshToken }).subscribe({
      complete: () => this.clearSession(),
      error: () => this.clearSession(),
    });
  }

  deleteAccount() {
    return this.http.delete<void>(`${this.baseUrl}/me`);
  }

  forgotPassword(email: string) {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string) {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, { token, newPassword });
  }

  restoreSession(): Observable<User | null> {
    if (!this.accessToken()) return of(null);
    return this.fetchCurrentUser().pipe(
      catchError(() => {
        this.clearTokens();
        return of(null);
      }),
    );
  }

  private fetchCurrentUser() {
    return this.http
      .get<User>(`${this.baseUrl}/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  private storeTokens(res: AuthResponse): void {
    localStorage.setItem(ACCESS_KEY, res.accessToken);
    localStorage.setItem(REFRESH_KEY, res.refreshToken);
    this.accessToken.set(res.accessToken);
  }

  clearSession(): void {
    this.clearTokens();
    this.router.navigate(['/login']);
  }

  private clearTokens(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    this.accessToken.set(null);
    this.currentUser.set(null);
  }
}
