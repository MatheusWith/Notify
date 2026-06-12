import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { SubscriberResponse } from '../../../../shared/models/newsletter.types';
import { TuiBlockStatusComponent } from '@taiga-ui/layout';

@Component({
  selector: 'app-subscribers-page',
  imports: [TuiBlockStatusComponent, DatePipe],
  templateUrl: './subscribers-page.html',
  styleUrl: './subscribers-page.scss',
})
export class SubscribersPage {
  readonly subscribers = signal<SubscriberResponse[]>([]);
  readonly totalCount = signal(0);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor(
    private readonly route: ActivatedRoute,
    private readonly http: HttpClient,
  ) {
    const slug = this.route.snapshot.paramMap.get('slug') ?? '';
    if (slug) {
      this.loadSubscribers(slug);
    }
  }

  private loadSubscribers(slug: string): void {
    this.loading.set(true);
    this.error.set('');

    this.http
      .get<{
        content: SubscriberResponse[];
        totalElements: number;
      }>(`${environment.apiUrl}/newsletter/${slug}/subscribers`)
      .subscribe({
        next: (page) => {
          this.subscribers.set(page.content);
          this.totalCount.set(page.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.loading.set(false);
          if (err.status === 404) {
            this.error.set('Newsletter not found');
          } else if (err.status === 403) {
            this.error.set('You are not the owner of this newsletter');
          } else {
            this.error.set('Failed to load subscribers. Please try again.');
          }
        },
      });
  }
}
