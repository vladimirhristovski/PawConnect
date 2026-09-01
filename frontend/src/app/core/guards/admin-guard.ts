import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = auth.currentUser();
  if (auth.isAuthenticated() && user?.roles.includes('ADMIN')) return true;
  router.navigate(['/']);
  return false;
};
