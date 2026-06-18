import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { QuillModule } from 'ngx-quill';
import { CampaignFormPage } from './campaign-form-page';

const API_URL = '/api/v1';

describe('CampaignFormPage (create mode)', () => {
  let fixture: ComponentFixture<CampaignFormPage>;
  let component: CampaignFormPage;
  let httpMock: HttpTestingController;
  const slug = 'test-newsletter';

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CampaignFormPage, QuillModule],
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

    fixture = TestBed.createComponent(CampaignFormPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show create mode and not load campaign', () => {
    fixture.detectChanges();

    expect(component.isEditMode()).toBe(false);
    expect(component.pageTitle()).toBe('New Campaign');
  });

  it('should show validation errors when submitting empty form', () => {
    fixture.detectChanges();

    component.onSubmit();

    expect(component.form.controls.subject.invalid).toBe(true);
    expect(component.form.controls.content.invalid).toBe(true);
  });

  it('should create campaign on valid submission and redirect', () => {
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();

    component.form.patchValue({
      subject: 'Test Subject',
      content: 'Test content for the campaign',
    });

    component.onSubmit();

    const req = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      subject: 'Test Subject',
      content: 'Test content for the campaign',
    });
    req.flush({
      id: '1',
      newsletterId: 'n1',
      subject: 'Test Subject',
      content: 'Test content',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    expect(component.saving()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith([
      '/sender/newsletters',
      slug,
      'campaigns',
    ]);
  });

  it('should include scheduledAt when provided', () => {
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();

    component.form.patchValue({
      subject: 'Scheduled',
      content: 'Content',
      scheduledAt: '2025-06-01T10:00',
    });

    component.onSubmit();

    const req = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns`,
    );
    const body = req.request.body as Record<string, string>;
    expect(body['scheduledAt']).toBeDefined();
    expect(body['subject']).toBe('Scheduled');
    req.flush({} as any);
  });

  it('should treat empty Quill content as invalid', () => {
    fixture.detectChanges();

    component.form.patchValue({
      subject: 'Valid Subject',
      content: '<p><br></p>',
    });

    component.onSubmit();

    expect(component.form.controls.content.invalid).toBe(true);
  });

  it('should show error notification on API failure', () => {
    fixture.detectChanges();

    component.form.patchValue({
      subject: 'Test',
      content: 'Content',
    });

    component.onSubmit();

    const req = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns`,
    );
    req.flush(
      { message: 'Error' },
      { status: 400, statusText: 'Bad Request' },
    );

    fixture.detectChanges();

    expect(component.error()).toBe(
      'Failed to save campaign. Please check your input and try again.',
    );
  });
});

describe('CampaignFormPage (edit mode)', () => {
  let fixture: ComponentFixture<CampaignFormPage>;
  let component: CampaignFormPage;
  let httpMock: HttpTestingController;
  const slug = 'test-newsletter';
  const campaignId = 'camp-1';

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [CampaignFormPage, QuillModule],
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
        provideAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CampaignFormPage);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load existing campaign in edit mode', () => {
    fixture.detectChanges();

    expect(component.isEditMode()).toBe(true);
    expect(component.pageTitle()).toBe('Edit Campaign');

    const req = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns/${campaignId}`,
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Existing Subject',
      content: 'Existing content',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    expect(component.loading()).toBe(false);
    expect(component.form.controls.subject.value).toBe('Existing Subject');
  });

  it('should send PUT request when updating existing campaign', () => {
    fixture.detectChanges();

    // Load
    const loadReq = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns/${campaignId}`,
    );
    loadReq.flush({
      id: campaignId,
      newsletterId: 'n1',
      subject: 'Old Subject',
      content: 'Old content',
      status: 'DRAFT',
      scheduledAt: null,
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-01T00:00:00Z',
    });

    fixture.detectChanges();

    // Update
    component.form.patchValue({
      subject: 'Updated Subject',
      content: 'Updated content',
    });

    component.onSubmit();

    const updateReq = httpMock.expectOne(
      `${API_URL}/newsletter/${slug}/campaigns/${campaignId}`,
    );
    expect(updateReq.request.method).toBe('PUT');
    expect(updateReq.request.body).toEqual({
      subject: 'Updated Subject',
      content: 'Updated content',
    });
    updateReq.flush({} as any);

    fixture.detectChanges();

    expect(component.saving()).toBe(false);
    expect(component.success()).toBe(true);
  });
});
