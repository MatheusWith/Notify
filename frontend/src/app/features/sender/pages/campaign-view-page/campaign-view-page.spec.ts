import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { CampaignViewPage } from './campaign-view-page';

const API_URL = '/api/v1';

describe('CampaignViewPage', () => {
  let fixture: ComponentFixture<CampaignViewPage>;
  let component: CampaignViewPage;
  let httpMock: HttpTestingController;
  const slug = 'test-newsletter';
  const campaignId = 'camp-1';

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CampaignViewPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => {
                  if (key === 'slug') return slug;
                  if (key === 'id') return campaignId;
                  return null;
                },
              },
            },
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CampaignViewPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`).flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Test Campaign',
      content: '<p>Test content</p>',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });
  });

  it('should have loading state initially', () => {
    expect(component.loading()).toBe(true);
    fixture.detectChanges();

    const card = fixture.debugElement.query(By.css('mat-card'));
    expect(card).toBeTruthy();
    expect(card.nativeElement.textContent).toContain('Loading campaign');

    httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`).flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Test',
      content: '<p>Content</p>',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });
  });

  it('should display campaign data when loaded successfully', () => {
    fixture.detectChanges();

    const mockCampaign = {
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Campaign Subject',
      content: '<p>Campaign <strong>content</strong> here</p>',
      status: 'PUBLISHED',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCampaign);

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.campaign()?.subject).toBe('Campaign Subject');

    const heading = fixture.debugElement.query(By.css('h2'));
    expect(heading.nativeElement.textContent).toContain('Campaign Subject');

    const statusBadge = fixture.debugElement.query(By.css('.status-badge'));
    expect(statusBadge).toBeTruthy();
    expect(statusBadge.nativeElement.textContent).toContain('PUBLISHED');

    const contentRender = fixture.debugElement.query(By.css('.content-render'));
    expect(contentRender).toBeTruthy();
    expect(contentRender.nativeElement.innerHTML).toContain(
      'Campaign <strong>content</strong> here',
    );
  });

  it('should show error state on API failure', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`);
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Failed to load campaign. Please try again.');

    const card = fixture.debugElement.query(By.css('mat-card'));
    expect(card).toBeTruthy();
    expect(card.nativeElement.textContent).toContain('Campaign not found');
  });

  it('should show back link to campaign list', () => {
    fixture.detectChanges();

    httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`).flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Test',
      content: '<p>Content</p>',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    const backLink = fixture.debugElement.query(By.css('.back-link'));
    expect(backLink).toBeTruthy();
    expect(backLink.nativeElement.textContent).toContain('Back to Campaigns');
  });

  it('should show Edit button for DRAFT campaigns', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`);
    req.flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Draft Campaign',
      content: '<p>Draft content</p>',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    const editLink = fixture.debugElement.query(By.css('.actions-bar a'));
    expect(editLink).toBeTruthy();
    expect(editLink.nativeElement.textContent).toContain('Edit Campaign');
  });

  it('should not show Edit button for PUBLISHED campaigns', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`);
    req.flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Published Campaign',
      content: '<p>Published content</p>',
      status: 'PUBLISHED',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    const editLink = fixture.debugElement.query(By.css('.actions-bar'));
    expect(editLink).toBeFalsy();
  });

  it('should display scheduled date when present', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/${campaignId}`);
    req.flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Scheduled',
      content: '<p>Content</p>',
      status: 'PUBLISHED',
      scheduledAt: '2025-06-01T10:00:00Z',
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    const metadataValues = fixture.debugElement.queryAll(By.css('.metadata-value'));
    const metadataTexts = metadataValues.map((el) => el.nativeElement.textContent);
    const hasSchedule = metadataTexts.some((t) => t.includes('Jun'));
    expect(hasSchedule).toBe(true);
  });
});

describe('CampaignViewPage with missing params', () => {
  let component: CampaignViewPage;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CampaignViewPage],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => null,
              },
            },
          },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(CampaignViewPage);
    component = fixture.componentInstance;
  });

  it('should show error when slug or id is missing', () => {
    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Invalid campaign URL.');
  });
});
