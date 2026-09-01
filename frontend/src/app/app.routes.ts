import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { adminGuard } from './core/guards/admin-guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
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
  {
    path: 'businesses/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/businesses/business-form/business-form').then((m) => m.BusinessForm),
  },
  {
    path: 'businesses/:id/edit',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/businesses/business-form/business-form').then((m) => m.BusinessForm),
  },
  {
    path: 'businesses/:id',
    loadComponent: () =>
      import('./features/businesses/business-detail/business-detail').then((m) => m.BusinessDetail),
  },
  {
    path: 'businesses',
    loadComponent: () =>
      import('./features/businesses/business-search/business-search').then((m) => m.BusinessSearch),
  },
  {
    path: 'pets/:id/photos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/pets/pet-photo-manager/pet-photo-manager').then((m) => m.PetPhotoManager),
  },
  {
    path: 'businesses/:id/photos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/businesses/business-photo-manager/business-photo-manager').then(
        (m) => m.BusinessPhotoManager,
      ),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account').then((m) => m.Account),
  },
  {
    path: 'admin',
    pathMatch: 'full',
    redirectTo: 'admin/users',
  },
  {
    path: 'admin/users',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-users/admin-users').then((m) => m.AdminUsers),
  },
  {
    path: 'admin/listings',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-listings/admin-listings').then((m) => m.AdminListings),
  },
  {
    path: 'admin/applications',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./features/admin/admin-applications/admin-applications').then(
        (m) => m.AdminApplications,
      ),
  },
  { path: '**', redirectTo: '' },
];
