import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { MainLayout } from '../../layouts/main-layout/main-layout';

export const senderRoutes: Routes = [
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./pages/dashboard-page/dashboard-page').then(
            (m) => m.DashboardPage,
          ),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./pages/profile-page/profile-page').then(
            (m) => m.ProfilePage,
          ),
      },
      {
        path: 'newsletters/:slug',
        loadComponent: () =>
          import('./pages/subscribers-page/subscribers-page').then(
            (m) => m.SubscribersPage,
          ),
      },
      {
        path: 'newsletters/:slug/campaigns',
        loadComponent: () =>
          import('./pages/campaign-list-page/campaign-list-page').then(
            (m) => m.CampaignListPage,
          ),
      },
      {
        path: 'newsletters/:slug/campaigns/new',
        loadComponent: () =>
          import('./pages/campaign-form-page/campaign-form-page').then(
            (m) => m.CampaignFormPage,
          ),
      },
      {
        path: 'newsletters/:slug/campaigns/:id/edit',
        loadComponent: () =>
          import('./pages/campaign-form-page/campaign-form-page').then(
            (m) => m.CampaignFormPage,
          ),
      },
    ],
  },
];
