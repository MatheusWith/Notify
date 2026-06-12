import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.authRoutes),
  },
  {
    path: 'newsletter',
    loadChildren: () =>
      import('./features/newsletter/newsletter.routes').then(
        (m) => m.newsletterRoutes,
      ),
  },
  {
    path: 'sender',
    loadChildren: () =>
      import('./features/sender/sender.routes').then((m) => m.senderRoutes),
  },
  {
    path: '',
    redirectTo: '/auth/login',
    pathMatch: 'full',
  },
];
