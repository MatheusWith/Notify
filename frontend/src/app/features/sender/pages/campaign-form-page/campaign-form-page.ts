import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from '../../../../core/services/app-config.service';
import { Campaign } from '../../../../shared/models/newsletter.types';
import { QuillModule } from 'ngx-quill';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-campaign-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    QuillModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './campaign-form-page.html',
  styleUrl: './campaign-form-page.scss',
})
export class CampaignFormPage {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(AppConfigService);
  private readonly apiUrl = this.appConfig.apiUrl;

  readonly form = this.fb.nonNullable.group({
    subject: ['', [Validators.required, Validators.maxLength(200)]],
    content: ['', [Validators.required, Validators.maxLength(20000), quillRequiredValidator]],
    scheduledAt: [''],
  });

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly success = signal(false);
  readonly isEditMode = signal(false);
  readonly pageTitle = signal('New Campaign');

  readonly slug: string = '';
  private campaignId: string | null = null;

  readonly quillModules = {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      [{ header: [1, 2, 3, false] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['blockquote', 'code-block'],
      [{ align: [] }],
      ['link'],
      ['clean'],
    ],
  };

  constructor() {
    this.slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.campaignId = this.route.snapshot.paramMap.get('id');

    if (this.campaignId) {
      this.isEditMode.set(true);
      this.pageTitle.set('Edit Campaign');
      this.loadCampaign();
    }
  }

  private loadCampaign(): void {
    this.loading.set(true);

    this.http
      .get<Campaign>(
        `${this.apiUrl}/newsletter/${this.slug}/campaigns/${this.campaignId}`,
      )
      .subscribe({
        next: (campaign) => {
          this.form.patchValue({
            subject: campaign.subject,
            content: campaign.content,
            scheduledAt: campaign.scheduledAt
              ? campaign.scheduledAt.slice(0, 16)
              : '',
          });
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Failed to load campaign.');
        },
      });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.error.set('');
    this.success.set(false);
    this.saving.set(true);

    const body = this.buildBody();
    const request$ =
      this.isEditMode() && this.campaignId
        ? this.http.put<Campaign>(
            `${this.apiUrl}/newsletter/${this.slug}/campaigns/${this.campaignId}`,
            body,
          )
        : this.http.post<Campaign>(
            `${this.apiUrl}/newsletter/${this.slug}/campaigns`,
            body,
          );

    request$.subscribe({
      next: () => {
        this.saving.set(false);

        if (!this.isEditMode()) {
          this.router.navigate([
            '/sender/newsletters',
            this.slug,
            'campaigns',
          ]);
        } else {
          this.success.set(true);
        }
      },
      error: () => {
        this.saving.set(false);
        this.error.set(
          'Failed to save campaign. Please check your input and try again.',
        );
      },
    });
  }

  private buildBody() {
    const raw = this.form.getRawValue();
    const body: Record<string, string> = {
      subject: raw.subject.trim(),
      content: this.isContentEmpty(raw.content) ? '' : raw.content,
    };

    if (raw.scheduledAt) {
      body['scheduledAt'] = new Date(raw.scheduledAt).toISOString();
    }

    return body;
  }

  private isContentEmpty(html: string): boolean {
    return !html || html === '<p><br></p>' || html === '<p></p>';
  }
}

function quillRequiredValidator(control: AbstractControl): ValidationErrors | null {
  const value: string = control.value || '';
  if (!value || value === '<p><br></p>' || value === '<p></p>') {
    return { required: true };
  }
  return null;
}
