import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AppConfigService } from '../../../../core/services/app-config.service';
import { Campaign } from '../../../../shared/models/newsletter.types';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-campaign-list-page',
  imports: [MatButtonModule, MatCardModule, RouterLink, DatePipe],
  templateUrl: './campaign-list-page.html',
  styleUrl: './campaign-list-page.scss',
})
export class CampaignListPage {
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(AppConfigService);
  private readonly apiUrl = this.appConfig.apiUrl;

  readonly campaigns = signal<Campaign[]>([]);
  readonly totalCount = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly slug = signal('');

  constructor() {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (slug) {
      this.slug.set(slug);
      this.loadCampaigns(slug);
    }
  }

  private loadCampaigns(slug: string): void {
    this.loading.set(true);
    this.error.set('');

    this.http
      .get<{ content: Campaign[]; totalElements: number }>(
        `${this.apiUrl}/newsletter/${slug}/campaigns`,
      )
      .subscribe({
        next: (page) => {
          this.campaigns.set(page.content);
          this.totalCount.set(page.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Failed to load campaigns. Please try again.');
        },
      });
  }

  deleteCampaign(id: string): void {
    if (!confirm('Are you sure you want to delete this campaign?')) {
      return;
    }

    this.http
      .delete(`${this.apiUrl}/newsletter/${this.slug()}/campaigns/${id}`)
      .subscribe({
        next: () => this.loadCampaigns(this.slug()),
        error: () => alert('Failed to delete campaign.'),
      });
  }

  publishCampaign(id: string): void {
    this.http
      .patch<Campaign>(
        `${this.apiUrl}/newsletter/${this.slug()}/campaigns/${id}/status`,
        { status: 'PUBLISHED' },
      )
      .subscribe({
        next: () => this.loadCampaigns(this.slug()),
        error: () => alert('Failed to publish campaign.'),
      });
  }

  getPlainTextPreview(html: string): string {
    if (!html) return '';
    const doc = new DOMParser().parseFromString(html, 'text/html');
    const text = doc.body.textContent || '';
    return text.length > 100 ? text.slice(0, 100) + '\u2026' : text;
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

  canEdit(status: string): boolean {
    return status === 'DRAFT' || status === 'PENDING';
  }

  canPublish(status: string): boolean {
    return status === 'DRAFT' || status === 'PENDING';
  }

  canDelete(status: string): boolean {
    return status === 'DRAFT' || status === 'PENDING';
  }
}
