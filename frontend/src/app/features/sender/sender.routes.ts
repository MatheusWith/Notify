import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const senderRoutes: Routes = [
  {
    path: 'newsletters/:slug',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/subscribers-page/subscribers-page').then(
        (m) => m.SubscribersPage,
      ),
  },
];
