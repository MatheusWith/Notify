import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
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
  readonly page = signal(0);
  readonly size = signal(20);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.totalCount() / this.size())));

  readonly searchQuery = signal('');
  readonly filterStatus = signal('');

  readonly filterStatusList = ['DRAFT', 'PENDING', 'PUBLISHED', 'SENT'];

  constructor() {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (slug) {
      this.slug.set(slug);
      this.loadCampaigns(slug);
    }
  }

  onSearchInput(value: string): void {
    this.searchQuery.set(value);
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      this.onSearch();
    }
  }

  onSearch(): void {
    this.page.set(0);
    this.loadCampaigns(this.slug());
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.page.set(0);
    this.loadCampaigns(this.slug());
  }

  onFilterChange(status: string): void {
    this.filterStatus.set(status);
    this.page.set(0);
    this.loadCampaigns(this.slug());
  }

  private loadCampaigns(slug: string): void {
    this.loading.set(true);
    this.error.set('');

    let params = new HttpParams()
      .set('page', this.page())
      .set('size', this.size());

    const search = this.searchQuery();
    if (search) {
      params = params.set('search', search);
    }

    const status = this.filterStatus();
    if (status) {
      params = params.set('status', status);
    }

    this.http
      .get<{
        content: Campaign[];
        totalElements: number;
      }>(`${this.apiUrl}/newsletter/${slug}/campaigns`, { params })
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

    this.http.delete(`${this.apiUrl}/newsletter/${this.slug()}/campaigns/${id}`).subscribe({
      next: () => this.loadCampaigns(this.slug()),
      error: () => alert('Failed to delete campaign.'),
    });
  }

  publishCampaign(id: string): void {
    this.http
      .patch<Campaign>(`${this.apiUrl}/newsletter/${this.slug()}/campaigns/${id}/status`, {
        status: 'PUBLISHED',
      })
      .subscribe({
        next: () => this.loadCampaigns(this.slug()),
        error: () => alert('Failed to publish campaign.'),
      });
  }

  nextPage(): void {
    if (this.page() < this.totalPages() - 1) {
      this.page.update((p) => p + 1);
      this.loadCampaigns(this.slug());
    }
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.loadCampaigns(this.slug());
    }
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
