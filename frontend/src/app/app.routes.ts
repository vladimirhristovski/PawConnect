import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./features/auth/reset-password/reset-password').then((m) => m.ResetPassword),
  },

  {
    path: 'listings/mine',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/listings/my-listings/my-listings').then((m) => m.MyListings),
  },
  {
    path: 'listings/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/listings/listing-form/listing-form').then((m) => m.ListingForm),
  },
  {
    path: 'listings/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/listings/listing-form/listing-form').then((m) => m.ListingForm),
  },
  {
    path: 'listings/:id/applications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/listing-applications/listing-applications').then(
        (m) => m.ListingApplications,
      ),
  },
  {
    path: 'listings/:id',
    loadComponent: () =>
      import('./features/listings/listing-detail/listing-detail').then((m) => m.ListingDetail),
  },
  {
    path: 'listings',
    loadComponent: () =>
      import('./features/listings/listing-search/listing-search').then((m) => m.ListingSearch),
  },
  {
    path: 'applications/mine',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/applications/my-applications/my-applications').then(
        (m) => m.MyApplications,
      ),
  },
  { path: '**', redirectTo: '' },
];
