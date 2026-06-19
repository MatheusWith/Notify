import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppConfigService } from '../../../core/services/app-config.service';
import { NewsletterProfile } from '../../../shared/models/newsletter.types';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-newsletter-profile-page',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './newsletter-profile-page.html',
  styleUrl: './newsletter-profile-page.scss',
})
export class NewsletterProfilePage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);
  private readonly appConfig = inject(AppConfigService);

  newsletter = signal<NewsletterProfile | null>(null);
  loading = signal(true);
  error = signal('');

  subscribeSuccess = signal(false);
  subscribeLoading = signal(false);
  subscribeError = signal('');

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  constructor() {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (slug) {
      this.loadNewsletter(slug);
    }
  }

  private loadNewsletter(slug: string): void {
    this.http.get<NewsletterProfile>(`${this.appConfig.apiUrl}/newsletter/${slug}`).subscribe({
      next: (profile) => {
        this.newsletter.set(profile);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Newsletter not found');
        this.loading.set(false);
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.subscribeError.set('');
    this.subscribeSuccess.set(false);
    this.subscribeLoading.set(true);

    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.http
      .post(`${this.appConfig.apiUrl}/newsletter/subscribe`, {
        email: this.form.getRawValue().email,
        slug,
      })
      .subscribe({
        next: () => {
          this.subscribeLoading.set(false);
          this.subscribeSuccess.set(true);
          this.form.reset();
        },
        error: () => {
          this.subscribeLoading.set(false);
          this.subscribeError.set('Something went wrong. Please try again later.');
        },
      });
  }
}
