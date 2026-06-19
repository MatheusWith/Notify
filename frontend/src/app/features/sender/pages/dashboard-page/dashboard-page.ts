import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AppConfigService } from '../../../../core/services/app-config.service';
import { NewsletterSummary } from '../../../../shared/models/newsletter.types';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, MatCardModule, MatButtonModule],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(AppConfigService);

  newsletters = signal<NewsletterSummary[]>([]);
  loading = signal(true);
  error = signal('');

  constructor() {
    this.http.get<NewsletterSummary[]>(`${this.appConfig.apiUrl}/newsletter/my`).subscribe({
      next: (list) => {
        this.newsletters.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load newsletters');
        this.loading.set(false);
      },
    });
  }
}
