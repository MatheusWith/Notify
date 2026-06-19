import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { CampaignListPage } from './campaign-list-page';

const API_URL = '/api/v1';

describe('CampaignListPage', () => {
  let fixture: ComponentFixture<CampaignListPage>;
  let component: CampaignListPage;
  let httpMock: HttpTestingController;
  const slug = 'test-newsletter';

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CampaignListPage],
      providers: [
        provideRouter([]),
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

    fixture = TestBed.createComponent(CampaignListPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/campaigns`)
      .flush({ content: [], totalElements: 0 });
  });

  it('should have loading state initially', () => {
    expect(component.loading()).toBe(true);
    fixture.detectChanges();

    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('Loading');

    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/campaigns`)
      .flush({ content: [], totalElements: 0 });
  });

  it('should display campaigns when data loads successfully', () => {
    fixture.detectChanges();

    const mockResponse = {
      content: [
        {
          id: '1',
          newsletterId: 'n1',
          subject: 'Campaign 1',
          content: '<p>Content 1</p>',
          status: 'DRAFT',
          scheduledAt: null,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        },
        {
          id: '2',
          newsletterId: 'n1',
          subject: 'Campaign 2',
          content: '<p>Content 2</p>',
          status: 'PUBLISHED',
          scheduledAt: '2025-02-01T00:00:00Z',
          createdAt: '2025-01-15T00:00:00Z',
          updatedAt: '2025-01-15T00:00:00Z',
        },
      ],
      totalElements: 2,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.campaigns()).toHaveLength(2);
    expect(component.totalCount()).toBe(2);

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(2);
    expect(rows[0].nativeElement.textContent).toContain('Campaign 1');
    expect(rows[0].nativeElement.textContent).toContain('Content 1');
  });

  it('should show empty state when no campaigns', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    req.flush({ content: [], totalElements: 0 });

    fixture.detectChanges();

    expect(component.campaigns()).toHaveLength(0);

    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('No campaigns yet');
  });

  it('should show error state on API failure', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Failed to load campaigns. Please try again.');
  });

  it('should extract plain text preview from HTML content', () => {
    fixture.detectChanges();
    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/campaigns`)
      .flush({ content: [], totalElements: 0 });

    expect(component.getPlainTextPreview('<p>Hello <strong>World</strong></p>')).toBe(
      'Hello World',
    );
    expect(component.getPlainTextPreview('Plain text')).toBe('Plain text');
    expect(component.getPlainTextPreview('')).toBe('');
    expect(component.getPlainTextPreview(null as unknown as string)).toBe('');

    const longHtml = '<p>' + 'A'.repeat(150) + '</p>';
    const preview = component.getPlainTextPreview(longHtml);
    expect(preview.length).toBe(101); // 100 chars + ellipsis
    expect(preview.endsWith('\u2026')).toBe(true);
  });

  it('should show Edit and Delete buttons only for editable campaigns', () => {
    fixture.detectChanges();

    const mockResponse = {
      content: [
        {
          id: '1',
          newsletterId: 'n1',
          subject: 'Draft',
          content: 'Content',
          status: 'DRAFT',
          scheduledAt: null,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        },
        {
          id: '2',
          newsletterId: 'n1',
          subject: 'Published',
          content: 'Content',
          status: 'PUBLISHED',
          scheduledAt: null,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        },
      ],
      totalElements: 2,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    req.flush(mockResponse);

    fixture.detectChanges();

    expect(component.canEdit('DRAFT')).toBe(true);
    expect(component.canEdit('PUBLISHED')).toBe(false);
    expect(component.canDelete('DRAFT')).toBe(true);
    expect(component.canDelete('PUBLISHED')).toBe(false);
  });

  it('should send DELETE request when deleting a campaign', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();

    const mockResponse = {
      content: [
        {
          id: '1',
          newsletterId: 'n1',
          subject: 'Delete Me',
          content: 'Content',
          status: 'DRAFT',
          scheduledAt: null,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    req.flush(mockResponse);

    fixture.detectChanges();

    component.deleteCampaign('1');

    const deleteReq = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    // After delete, it reloads: expect the list call
    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/campaigns`)
      .flush({ content: [], totalElements: 0 });
  });

  it('should send PATCH request when publishing a campaign', () => {
    fixture.detectChanges();

    const mockResponse = {
      content: [
        {
          id: '1',
          newsletterId: 'n1',
          subject: 'Publish Me',
          content: 'Content',
          status: 'DRAFT',
          scheduledAt: null,
          createdAt: '2025-01-01T00:00:00Z',
          updatedAt: '2025-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
    };

    const req = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns`);
    req.flush(mockResponse);

    fixture.detectChanges();

    component.publishCampaign('1');

    const patchReq = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/1/status`);
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({ status: 'PUBLISHED' });
    patchReq.flush({});

    // After publish, it reloads
    httpMock
      .expectOne(`${API_URL}/newsletter/${slug}/campaigns`)
      .flush({ content: [], totalElements: 0 });
  });
});
