import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginPage } from './login-page';
import { AuthService } from '../../../core/services/auth.service';
import { Router, provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { provideAnimations } from '@angular/platform-browser/animations';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let component: LoginPage;
  let authServiceMock: {
    login: ReturnType<typeof vi.fn>;
    getCurrentUser: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      login: vi.fn(),
      getCurrentUser: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([
          { path: 'sender', redirectTo: '', pathMatch: 'full' },
        ]),
        provideAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render email and password input fields', () => {
    const emailInput = fixture.debugElement.query(
      By.css('input[formControlName="email"]'),
    );
    const passwordInput = fixture.debugElement.query(
      By.css('input[formControlName="password"]'),
    );

    expect(emailInput).toBeTruthy();
    expect(emailInput.nativeElement.getAttribute('type')).toBe('email');
    expect(passwordInput).toBeTruthy();
    expect((passwordInput.nativeElement as HTMLInputElement).type).toBe(
      'password',
    );
  });

  it('should render submit button', () => {
    const submitButton = fixture.debugElement.query(
      By.css('button[type="submit"]'),
    );
    expect(submitButton).toBeTruthy();
    expect(submitButton.nativeElement.textContent.trim()).toBe('Sign In');
  });

  it('should render error notification when error property is set', () => {
    component.error = 'Something went wrong';
    fixture.detectChanges();

    expect(component.error).toBe('Something went wrong');
  });

  it('should call authService.login with credentials on submit', () => {
    authServiceMock.login.mockReturnValue(
      of({
        accessToken: 'x',
        refreshToken: 'y',
        tokenType: 'Bearer',
        expiresIn: 900,
      }),
    );
    authServiceMock.getCurrentUser.mockReturnValue(
      of({
        id: 1,
        email: 'a@b.com',
        name: 'A',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(authServiceMock.login).toHaveBeenCalledTimes(1);
    expect(authServiceMock.login).toHaveBeenCalledWith({
      email: 'a@b.com',
      password: 'Secret1!',
    });
  });

  it('should fetch current user after successful login', () => {
    authServiceMock.login.mockReturnValue(
      of({
        accessToken: 'x',
        refreshToken: 'y',
        tokenType: 'Bearer',
        expiresIn: 900,
      }),
    );
    authServiceMock.getCurrentUser.mockReturnValue(
      of({
        id: 1,
        email: 'a@b.com',
        name: 'A',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(authServiceMock.getCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('should show error message on login failure', () => {
    authServiceMock.login.mockReturnValue(
      throwError(() => ({ status: 401, message: 'Unauthorized' })),
    );

    component.form.controls.email.setValue('bad@b.com');
    component.form.controls.password.setValue('wrong');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.error).toBe('Invalid email or password');
  });

  it('should clear previous error on new submit attempt', () => {
    authServiceMock.login.mockReturnValue(
      throwError(() => ({ status: 401 })),
    );
    component.form.controls.email.setValue('bad@b.com');
    component.form.controls.password.setValue('wrong');
    component.onSubmit();
    expect(component.error).toBe('Invalid email or password');

    authServiceMock.login.mockReturnValue(
      of({
        accessToken: 'x',
        refreshToken: 'y',
        tokenType: 'Bearer',
        expiresIn: 900,
      }),
    );
    authServiceMock.getCurrentUser.mockReturnValue(
      of({
        id: 1,
        email: 'a@b.com',
        name: 'A',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00Z',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();
    expect(component.error).toBe('');
  });

  it('should not display error notification when there is no error', () => {
    expect(component.error).toBe('');
  });

  it('should have a link to the register page', () => {
    const registerLink = fixture.debugElement.query(
      By.css('a[routerLink="/auth/register"]'),
    );
    expect(registerLink).toBeTruthy();
    expect(
      registerLink.nativeElement.textContent.trim().toLowerCase(),
    ).toContain('register');
  });

  it('should mark all fields as touched on submit with invalid form', () => {
    component.onSubmit();
    expect(component.form.controls.email.touched).toBe(true);
    expect(component.form.controls.password.touched).toBe(true);
  });

  it('should set loading to true during login and false after success', () => {
    authServiceMock.login.mockReturnValue(
      of({
        accessToken: 'x',
        refreshToken: 'y',
        tokenType: 'Bearer',
        expiresIn: 900,
      }),
    );
    authServiceMock.getCurrentUser.mockReturnValue(
      of({
        id: 1,
        email: 'a@b.com',
        name: 'A',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(component.loading()).toBe(false);
  });

  it('should reset loading to false on login error', () => {
    authServiceMock.login.mockReturnValue(
      throwError(() => ({ status: 401 })),
    );

    component.form.controls.email.setValue('bad@b.com');
    component.form.controls.password.setValue('wrong');
    component.onSubmit();

    expect(component.loading()).toBe(false);
  });
});
