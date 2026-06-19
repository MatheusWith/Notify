import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { SubscribersPage } from './subscribers-page';

const API_URL = '/api/v1';

describe('SubscribersPage', () => {
  let fixture: ComponentFixture<SubscribersPage>;
  let component: SubscribersPage;
  let httpMock: HttpTestingController;
  const slug = 'test-newsletter';

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SubscribersPage],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'slug' ? slug : null),
              },
            },
          },
        },
        provideAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscribersPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/subscribers`)
      .flush({ content: [], totalElements: 0 });
  });

  it('should have loading state initially and show loading indicator', () => {
    expect(component.loading()).toBe(true);

    fixture.detectChanges();
    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('Loading');

    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/subscribers`)
      .flush({ content: [], totalElements: 0 });
  });

  it('should display subscribers when data loads successfully', () => {
    fixture.detectChanges();

    const mockResponse = {
      content: [
        { name: 'Alice', status: 'CONFIRMED', createdAt: '2025-01-01T00:00:00Z' },
        { name: 'Bob', status: 'PENDING', createdAt: '2025-01-02T00:00:00Z' },
      ],
      totalElements: 2,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('');
    expect(component.subscribers()).toHaveLength(2);
    expect(component.totalCount()).toBe(2);

    const cards = fixture.debugElement.queryAll(By.css('.subscriber-card'));
    expect(cards).toHaveLength(2);
    expect(cards[0].nativeElement.textContent).toContain('Alice');

    const badge = fixture.debugElement.query(By.css('.total-badge'));
    expect(badge).toBeTruthy();
    expect(badge.nativeElement.textContent.trim()).toBe('2');
  });

  it('should show empty state when no subscribers exist', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    req.flush({ content: [], totalElements: 0 });

    fixture.detectChanges();

    expect(component.subscribers()).toHaveLength(0);
    expect(component.totalCount()).toBe(0);

    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('No subscribers yet');
  });

  it('should show newsletter not found on 404 error', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Newsletter not found');

    const errorMsg = fixture.debugElement.query(By.css('mat-card h2'));
    expect(errorMsg).toBeTruthy();
    expect(errorMsg.nativeElement.textContent).toBe('Newsletter not found');
  });

  it('should show forbidden error on 403 error', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    fixture.detectChanges();

    expect(component.error()).toBe('You are not the owner of this newsletter');
  });

  it('should show generic error on unexpected server error', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    req.flush({ message: 'Server error' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();

    expect(component.error()).toBe('Failed to load subscribers. Please try again.');
  });

  it('should display total count badge matching totalElements', () => {
    fixture.detectChanges();

    const mockResponse = {
      content: [
        { name: 'Alice', status: 'CONFIRMED', createdAt: '2025-01-01T00:00:00Z' },
        { name: 'Bob', status: 'PENDING', createdAt: '2025-01-02T00:00:00Z' },
        { name: 'Charlie', status: 'EXPIRED', createdAt: '2025-01-03T00:00:00Z' },
      ],
      totalElements: 3,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/subscribers`);
    req.flush(mockResponse);

    fixture.detectChanges();

    const badge = fixture.debugElement.query(By.css('.total-badge'));
    expect(badge).toBeTruthy();
    expect(badge.nativeElement.textContent.trim()).toBe('3');
  });
});
