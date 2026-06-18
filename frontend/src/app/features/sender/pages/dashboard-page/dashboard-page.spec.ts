import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { DashboardPage } from './dashboard-page';

const API_URL = '/api/v1';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let component: DashboardPage;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideRouter([]),
        provideAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    fixture.detectChanges();
    httpMock.expectOne(`${API_URL}/newsletter/my`);
    expect(component).toBeTruthy();
  });

  it('should show loading state initially', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    expect(
      fixture.debugElement.query(By.css('mat-card p'))?.nativeElement.textContent,
    ).toContain('Loading your newsletters...');
    req.flush([]);
  });

  it('should show empty state when no newsletters', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    req.flush([]);
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('mat-card p'))?.nativeElement.textContent,
    ).toContain('No newsletters yet');
  });

  it('should show error state on failure', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    req.flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.css('mat-card h2'))?.nativeElement.textContent).toBe(
      'Failed to load newsletters',
    );
  });

  it('should render newsletter cards when data is loaded', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    req.flush([
      { id: '1', name: 'Tech Weekly', slug: 'tech-weekly', subscriberCount: 42 },
      { id: '2', name: 'Fashion Monthly', slug: 'fashion-monthly', subscriberCount: 7 },
    ]);
    fixture.detectChanges();

    const cards = fixture.debugElement.queryAll(By.css('.newsletter-card'));
    expect(cards.length).toBe(2);
    expect(cards[0].nativeElement.textContent).toContain('Tech Weekly');
    expect(cards[1].nativeElement.textContent).toContain('Fashion Monthly');
  });

  it('should render action buttons for each newsletter', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    req.flush([
      { id: '1', name: 'Tech Weekly', slug: 'tech-weekly', subscriberCount: 42 },
    ]);
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('a[mat-stroked-button]'));
    expect(buttons.length).toBe(2);
    expect(buttons[0].nativeElement.textContent).toContain('Campaigns');
    expect(buttons[1].nativeElement.textContent).toContain('Subscribers');
  });

  it('should display subscriber count with correct pluralization', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(`${API_URL}/newsletter/my`);
    req.flush([
      { id: '1', name: 'One', slug: 'one', subscriberCount: 1 },
      { id: '2', name: 'Many', slug: 'many', subscriberCount: 10 },
    ]);
    fixture.detectChanges();

    const texts = fixture.debugElement.queryAll(By.css('.subscriber-count'));
    expect(texts[0].nativeElement.textContent).toContain('1 subscriber');
    expect(texts[1].nativeElement.textContent).toContain('10 subscribers');
  });
});
