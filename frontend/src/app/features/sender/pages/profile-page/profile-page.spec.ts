import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { ProfilePage } from './profile-page';
import { AuthService } from '../../../../core/services/auth.service';

describe('ProfilePage', () => {
  let fixture: ComponentFixture<ProfilePage>;
  let component: ProfilePage;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(async () => {
    (window as any).__RUNTIME_CONFIG = { apiUrl: 'http://test' };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ProfilePage],
      providers: [
        provideAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfilePage);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    (window as any).__RUNTIME_CONFIG = undefined;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show loading spinner initially', () => {
    fixture.detectChanges();
    expect(component.loading()).toBe(true);

    const spinner = fixture.debugElement.query(By.css('mat-spinner'));
    expect(spinner).toBeTruthy();

    httpMock.expectOne((req) => req.url.includes('/users/me')).flush({
      id: '1',
      name: 'Test',
      email: 'test@test.com',
      roles: ['USER'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });
  });

  it('should display user info after loading', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((req) => req.url.includes('/users/me'));
    expect(req.request.method).toBe('GET');
    req.flush({
      id: '1',
      name: 'Alice',
      email: 'alice@example.com',
      roles: ['USER', 'ADMIN'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);

    const cardContent = fixture.debugElement.query(By.css('mat-card-content'));
    expect(cardContent).toBeTruthy();
    expect(cardContent.nativeElement.textContent).toContain('Alice');
    expect(cardContent.nativeElement.textContent).toContain('alice@example.com');
    expect(cardContent.nativeElement.textContent).toContain('ADMIN');

    const editButton = fixture.debugElement.query(By.css('button[mat-raised-button]'));
    expect(editButton).toBeTruthy();
  });

  it('should show error on API failure', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((req) => req.url.includes('/users/me'));
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Failed to load profile.');

    const errorMsg = fixture.debugElement.query(By.css('.error-notification'));
    expect(errorMsg).toBeTruthy();
    expect(errorMsg.nativeElement.textContent).toContain('Failed to load profile');
  });

  it('should switch to edit mode on click', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((req) => req.url.includes('/users/me'));
    req.flush({
      id: '1',
      name: 'Alice',
      email: 'alice@example.com',
      roles: ['USER'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    const editButton = fixture.debugElement.query(By.css('button[mat-raised-button]'));
    expect(editButton).toBeTruthy();
    editButton.nativeElement.click();

    fixture.detectChanges();

    expect(component.isEditing()).toBe(true);

    const nameInput = fixture.debugElement.query(By.css('input[formControlName="name"]'));
    expect(nameInput).toBeTruthy();
    expect((nameInput.nativeElement as HTMLInputElement).value).toBe('Alice');

    const saveButton = fixture.debugElement.query(By.css('button[type="submit"]'));
    expect(saveButton).toBeTruthy();
    expect(saveButton.nativeElement.textContent).toContain('Save');
  });

  it('should call updateProfile on form submit', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((req) => req.url.includes('/users/me'));
    req.flush({
      id: '1',
      name: 'Alice',
      email: 'alice@example.com',
      roles: ['USER'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    // Switch to edit mode
    const editButton = fixture.debugElement.query(By.css('button[mat-raised-button]'));
    editButton.nativeElement.click();

    fixture.detectChanges();

    // Submit the form
    const form = fixture.debugElement.query(By.css('form'));
    form.triggerEventHandler('ngSubmit', null);

    const putReq = httpMock.expectOne((req) => req.url.includes('/users/me'));
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body).toEqual({
      name: 'Alice',
      email: 'alice@example.com',
    });
    putReq.flush({
      id: '1',
      name: 'Alice Updated',
      email: 'alice@example.com',
      roles: ['USER'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    expect(component.saving()).toBe(false);
    expect(component.success()).toBe(true);
    expect(component.isEditing()).toBe(false);
  });

  it('should cancel editing and revert to view mode', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((req) => req.url.includes('/users/me'));
    req.flush({
      id: '1',
      name: 'Alice',
      email: 'alice@example.com',
      roles: ['USER'],
      enabled: true,
      createdAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    // Enter edit mode
    component.startEditing();
    fixture.detectChanges();

    // Cancel
    const cancelButton = fixture.debugElement.query(By.css('button[type="button"]'));
    cancelButton.nativeElement.click();
    fixture.detectChanges();

    expect(component.isEditing()).toBe(false);
    // Should show view mode again
    const editButton = fixture.debugElement.query(By.css('button[mat-raised-button]'));
    expect(editButton).toBeTruthy();
    expect(editButton.nativeElement.textContent).toContain('Edit Profile');
  });
});
