import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, minLength, required } from '@angular/forms/signals';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/api-error';

@Component({
  selector: 'app-reset-password',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css',
})
export class ResetPassword {
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  token = this.route.snapshot.queryParamMap.get('token') ?? '';
  resetModel = signal({ newPassword: '' });
  error = signal<string | null>(null);
  success = signal(false);

  resetForm = form(
    this.resetModel,
    (path) => {
      required(path.newPassword, { message: 'Password is required' });
      minLength(path.newPassword, 6, { message: 'Password must be at least 6 characters' });
    },
    {
      submission: {
        action: async (form) => {
          this.error.set(null);
          try {
            await firstValueFrom(
              this.auth.resetPassword(this.token, form().value().newPassword),
            );
            this.success.set(true);
            setTimeout(() => this.router.navigate(['/login']), 1200);
          } catch (err) {
            this.error.set(apiErrorMessage(err, 'Reset failed — the link may have expired.'));
          }
          return;
        },
      },
    },
  );
}
