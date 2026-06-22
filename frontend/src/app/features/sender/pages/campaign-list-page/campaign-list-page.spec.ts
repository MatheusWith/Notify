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

  function createMockCampaign(id: string, overrides: Partial<{
    subject: string; content: string; status: string; scheduledAt: string | null;
  }> = {}): Record<string, unknown> {
    return {
      id,
      newsletterId: 'n1',
      subject: `Campaign ${id}`,
      content: '<p>Content</p>',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
      ...overrides,
    };
  }

  function expectCampaignsRequest(page = 0, size = 20, search?: string, status?: string) {
    return httpMock.expectOne(
      (req) => {
        if (req.url !== `${API_URL}/newsletter/${slug}/campaigns`) return false;
        if (req.params.get('page') !== String(page)) return false;
        if (req.params.get('size') !== String(size)) return false;
        if (search !== undefined && req.params.get('search') !== search) return false;
        if (status !== undefined && req.params.get('status') !== status) return false;
        return true;
      }
    );
  }

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
    expectCampaignsRequest().flush({ content: [], totalElements: 0 });
  });

  it('should have loading state initially', () => {
    expect(component.loading()).toBe(true);
    fixture.detectChanges();

    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('Loading');

    expectCampaignsRequest().flush({ content: [], totalElements: 0 });
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

    const req = expectCampaignsRequest();
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.campaigns()).toHaveLength(2);
    expect(component.totalCount()).toBe(2);

    const rows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(rows).toHaveLength(2);
    expect(rows[0].nativeElement.textContent).toContain('Campaign 1');

    const indexCells = fixture.debugElement.queryAll(By.css('tbody td:first-child'));
    expect(indexCells[0].nativeElement.textContent.trim()).toBe('1');
    expect(indexCells[1].nativeElement.textContent.trim()).toBe('2');

    const header = fixture.debugElement.query(By.css('h2'));
    expect(header.nativeElement.textContent).toContain('Campaigns (2)');
  });

  it('should show empty state when no campaigns', () => {
    fixture.detectChanges();

    const req = expectCampaignsRequest();
    req.flush({ content: [], totalElements: 0 });

    fixture.detectChanges();

    expect(component.campaigns()).toHaveLength(0);

    const header = fixture.debugElement.query(By.css('h2'));
    expect(header.nativeElement.textContent).toContain('Campaigns (0)');

    const statusCard = fixture.debugElement.query(By.css('mat-card'));
    expect(statusCard).toBeTruthy();
    expect(statusCard.nativeElement.textContent).toContain('No campaigns yet');
  });

  it('should show error state on API failure', () => {
    fixture.detectChanges();

    const req = expectCampaignsRequest();
    req.flush({ message: 'Error' }, { status: 500, statusText: 'Server Error' });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Failed to load campaigns. Please try again.');
  });

  it('should extract plain text preview from HTML content', () => {
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: [], totalElements: 0 });

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

  it('should show View button for all campaigns and Edit/Delete only for editable campaigns', () => {
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

    const req = expectCampaignsRequest();
    req.flush(mockResponse);

    fixture.detectChanges();

    // View button should be on every row
    const viewButtons = fixture.debugElement.queryAll(By.css('button'));
    const viewBtnTexts = viewButtons.map((b) => b.nativeElement.textContent.trim());
    const visibleViewButtons = viewBtnTexts.filter((t) => t === 'View');
    expect(visibleViewButtons.length).toBe(2); // one per row

    expect(component.canEdit('DRAFT')).toBe(true);
    expect(component.canEdit('PUBLISHED')).toBe(false);
    expect(component.canDelete('DRAFT')).toBe(true);
    expect(component.canDelete('PUBLISHED')).toBe(false);

    const headers = fixture.debugElement.queryAll(By.css('thead th'));
    expect(headers[0].nativeElement.textContent.trim()).toBe('#');
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

    const req = expectCampaignsRequest();
    req.flush(mockResponse);

    fixture.detectChanges();

    component.deleteCampaign('1');

    const deleteReq = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    // After delete, it reloads: expect the list call
    expectCampaignsRequest().flush({ content: [], totalElements: 0 });
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

    const req = expectCampaignsRequest();
    req.flush(mockResponse);

    fixture.detectChanges();

    component.publishCampaign('1');

    const patchReq = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/1/status`);
    expect(patchReq.request.method).toBe('PATCH');
    expect(patchReq.request.body).toEqual({ status: 'PUBLISHED' });
    patchReq.flush({});

    // After publish, it reloads
    expectCampaignsRequest().flush({ content: [], totalElements: 0 });
  });

  it('should show Previous as disabled on first page', () => {
    component.page.set(0);
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: [createMockCampaign('1'), createMockCampaign('2')], totalElements: 25 });
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('.pagination button'));
    expect(buttons[0].nativeElement.disabled).toBe(true);
  });

  it('should show Next as disabled on last page', () => {
    const items = Array.from({ length: 20 }, (_, i) => createMockCampaign(String(i + 1)));
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: items, totalElements: 20 });
    fixture.detectChanges();

    const pagination = fixture.debugElement.query(By.css('.pagination'));
    expect(pagination).toBeFalsy();
  });

  it('should navigate to next page', () => {
    const items = Array.from({ length: 20 }, (_, i) => createMockCampaign(String(i + 1)));
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: items, totalElements: 40 });
    fixture.detectChanges();

    component.nextPage();
    expectCampaignsRequest(1).flush({ content: [createMockCampaign('21')], totalElements: 40 });
    fixture.detectChanges();

    expect(component.page()).toBe(1);
  });

  it('should navigate to previous page', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [createMockCampaign('1')], totalElements: 40 });
    component.page.set(1);
    fixture.detectChanges();

    component.previousPage();
    expectCampaignsRequest(0).flush({ content: [createMockCampaign('1')], totalElements: 40 });
    fixture.detectChanges();

    expect(component.page()).toBe(0);
  });

  it('should show page info text', () => {
    const items = Array.from({ length: 20 }, (_, i) => createMockCampaign(String(i + 1)));
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: items, totalElements: 40 });
    fixture.detectChanges();

    const pageInfo = fixture.debugElement.query(By.css('.page-info'));
    expect(pageInfo.nativeElement.textContent).toContain('Page 1 of 2');
  });

  it('should hide pagination when only one page', () => {
    fixture.detectChanges();
    expectCampaignsRequest().flush({ content: [createMockCampaign('1')], totalElements: 1 });
    fixture.detectChanges();

    const pagination = fixture.debugElement.query(By.css('.pagination'));
    expect(pagination).toBeFalsy();
  });

  it('should reload same page after delete', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [createMockCampaign('1')], totalElements: 25 });

    component.page.set(1);

    component.deleteCampaign('1');
    const deleteReq = httpMock.expectOne(`${API_URL}/newsletter/${slug}/campaigns/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    expectCampaignsRequest(1).flush({ content: [], totalElements: 24 });
  });

  it('should include search param when searching', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.searchQuery.set('test');
    component.onSearch();

    const req = expectCampaignsRequest(0, 20, 'test');
    expect(req.request.params.get('search')).toBe('test');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should include status param when filtering by status', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.onFilterChange('DRAFT');
    const req = expectCampaignsRequest(0, 20, undefined, 'DRAFT');
    expect(req.request.params.get('status')).toBe('DRAFT');
    req.flush({ content: [], totalElements: 0 });
  });

  it('should trigger search on Enter key', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.searchQuery.set('query');
    component.page.set(2);

    const enterEvent = new KeyboardEvent('keydown', { key: 'Enter' });
    component.onSearchKeydown(enterEvent);

    expect(component.page()).toBe(0);
    expectCampaignsRequest(0, 20, 'query').flush({ content: [], totalElements: 0 });
  });

  it('should reset to page 0 on filter change', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.page.set(3);
    component.onFilterChange('PUBLISHED');

    expect(component.page()).toBe(0);
    expectCampaignsRequest(0, 20, undefined, 'PUBLISHED').flush({ content: [], totalElements: 0 });
  });

  it('should not trigger search on non-Enter key', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.searchQuery.set('abc');
    component.page.set(1);

    const tabEvent = new KeyboardEvent('keydown', { key: 'Tab' });
    component.onSearchKeydown(tabEvent);

    expect(component.page()).toBe(1);
  });

  it('should clear search when clearSearch is called', () => {
    fixture.detectChanges();
    expectCampaignsRequest(0).flush({ content: [], totalElements: 0 });

    component.searchQuery.set('some search');
    component.page.set(1);
    component.clearSearch();

    expect(component.searchQuery()).toBe('');
    expect(component.page()).toBe(0);
    expectCampaignsRequest(0, 20).flush({ content: [], totalElements: 0 });
  });

  it('should update searchQuery on input', () => {
    expectCampaignsRequest().flush({ content: [], totalElements: 0 });
    expect(component.searchQuery()).toBe('');

    component.onSearchInput('hello');

    expect(component.searchQuery()).toBe('hello');
  });
});
