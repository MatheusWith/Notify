import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from '../../../../core/services/app-config.service';
import { Campaign } from '../../../../shared/models/newsletter.types';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-campaign-view-page',
  imports: [RouterLink, DatePipe, MatButtonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './campaign-view-page.html',
  styleUrl: './campaign-view-page.scss',
})
export class CampaignViewPage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(AppConfigService);
  private readonly apiUrl = this.appConfig.apiUrl;

  readonly campaign = signal<Campaign | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  readonly slug: string;
  private readonly campaignId: string;

  constructor() {
    this.slug = this.route.snapshot.paramMap.get('slug') ?? '';
    this.campaignId = this.route.snapshot.paramMap.get('id') ?? '';

    if (this.slug && this.campaignId) {
      this.loadCampaign();
    } else {
      this.loading.set(false);
      this.error.set('Invalid campaign URL.');
    }
  }

  private loadCampaign(): void {
    this.loading.set(true);
    this.error.set('');

    this.http
      .get<Campaign>(`${this.apiUrl}/newsletter/${this.slug}/campaigns/${this.campaignId}`)
      .subscribe({
        next: (campaign) => {
          this.campaign.set(campaign);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Failed to load campaign. Please try again.');
        },
      });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'DRAFT':
        return 'status-draft';
      case 'PENDING':
        return 'status-pending';
      case 'PUBLISHED':
        return 'status-published';
      case 'SENT':
        return 'status-sent';
      default:
        return '';
    }
  }
}
