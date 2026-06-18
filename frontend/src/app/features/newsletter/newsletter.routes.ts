import { Routes } from '@angular/router';

export const newsletterRoutes: Routes = [
  {
    path: ':slug',
    loadComponent: () =>
      import('./pages/newsletter-profile-page').then(
        (m) => m.NewsletterProfilePage,
      ),
  },
];
