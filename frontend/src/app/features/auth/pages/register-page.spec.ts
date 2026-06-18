import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RegisterPage } from './register-page';
import { AuthService } from '../../../core/services/auth.service';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { provideAnimations } from '@angular/platform-browser/animations';

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;
  let component: RegisterPage;
  let authServiceMock: {
    login: ReturnType<typeof vi.fn>;
    register: ReturnType<typeof vi.fn>;
    getCurrentUser: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceMock = {
      login: vi.fn(),
      register: vi.fn(),
      getCurrentUser: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([]),
        provideAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render name, email and password input fields', () => {
    const nameInput = fixture.debugElement.query(
      By.css('input[formControlName="name"]'),
    );
    const emailInput = fixture.debugElement.query(
      By.css('input[formControlName="email"]'),
    );
    const passwordInput = fixture.debugElement.query(
      By.css('input[formControlName="password"]'),
    );

    expect(nameInput).toBeTruthy();
    expect((nameInput.nativeElement as HTMLInputElement).type).toBe('text');
    expect(emailInput).toBeTruthy();
    expect(emailInput.nativeElement.getAttribute('type')).toBe('email');
    expect(passwordInput).toBeTruthy();
    expect((passwordInput.nativeElement as HTMLInputElement).type).toBe('password');
  });

  it('should render submit button', () => {
    const submitButton = fixture.debugElement.query(
      By.css('button[type="submit"]'),
    );
    expect(submitButton).toBeTruthy();
    expect(submitButton.nativeElement.textContent.trim()).toBe(
      'Create Account',
    );
  });

  it('should render error notification when error property is set', () => {
    component.error = 'Something went wrong';
    fixture.detectChanges();

    expect(component.error).toBe('Something went wrong');
  });

  it('should call authService.register with form data on submit', () => {
    authServiceMock.register.mockReturnValue(
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
        name: 'Alice',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(authServiceMock.register).toHaveBeenCalledTimes(1);
    expect(authServiceMock.register).toHaveBeenCalledWith({
      email: 'a@b.com',
      name: 'Alice',
      password: 'Secret1!',
    });
  });

  it('should fetch current user after successful registration', () => {
    authServiceMock.register.mockReturnValue(
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
        name: 'Alice',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(authServiceMock.getCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('should show field validation errors on 400 response with violations', () => {
    const violationsError = {
      error: {
        violations: [
          { field: 'email', message: 'must be a valid email address' },
          { field: 'password', message: 'must be at least 8 characters' },
        ],
      },
    };
    authServiceMock.register.mockReturnValue(
      throwError(() => violationsError),
    );

    component.form.controls.email.setValue('existing@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('longenough');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.error).toContain('must be a valid email address');
    expect(component.error).toContain('must be at least 8 characters');
  });

  it('should show detail error on non-violation error response', () => {
    const detailError = { error: { detail: 'Email already registered' } };
    authServiceMock.register.mockReturnValue(
      throwError(() => detailError),
    );

    component.form.controls.email.setValue('existing@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('ValidPass1!');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.error).toBe('Email already registered');
  });

  it('should show generic error on unknown error', () => {
    authServiceMock.register.mockReturnValue(throwError(() => ({})));

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('ValidPass1!');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.error).toBe('Registration failed. Please try again.');
  });

  it('should clear previous error on new submit attempt', () => {
    authServiceMock.register.mockReturnValue(
      throwError(() => ({ error: { detail: 'fail' } })),
    );
    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('ValidPass1!');
    component.onSubmit();
    expect(component.error).toBe('fail');

    authServiceMock.register.mockReturnValue(
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
        name: 'Alice',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('ValidPass1!');
    component.onSubmit();
    expect(component.error).toBe('');
  });

  it('should not display error notification when there is no error', () => {
    expect(component.error).toBe('');
  });

  it('should have a link to the login page', () => {
    const loginLink = fixture.debugElement.query(
      By.css('a[routerLink="/auth/login"]'),
    );
    expect(loginLink).toBeTruthy();
    expect(loginLink.nativeElement.textContent.trim().toLowerCase()).toContain(
      'sign in',
    );
  });

  it('should mark all fields as touched on submit with invalid form', () => {
    component.onSubmit();
    expect(component.form.controls.name.touched).toBe(true);
    expect(component.form.controls.email.touched).toBe(true);
    expect(component.form.controls.password.touched).toBe(true);
  });

  it('should set loading to true during register and false after success', () => {
    authServiceMock.register.mockReturnValue(
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
        name: 'Alice',
        roles: ['USER'],
        enabled: true,
        createdAt: '2024-01-01T00:00:00',
      }),
    );

    component.form.controls.email.setValue('a@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('Secret1!');
    component.onSubmit();

    expect(component.loading()).toBe(false);
  });

  it('should reset loading to false on register error', () => {
    authServiceMock.register.mockReturnValue(
      throwError(() => ({ error: { detail: 'Email already registered' } })),
    );

    component.form.controls.email.setValue('existing@b.com');
    component.form.controls.name.setValue('Alice');
    component.form.controls.password.setValue('ValidPass1!');
    component.onSubmit();

    expect(component.loading()).toBe(false);
  });
});
