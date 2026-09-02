import { Component, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css',
})
export class ForgotPassword {
  private auth = inject(AuthService);

  forgotModel = signal({ email: '' });
  sent = signal(false);

  forgotForm = form(
    this.forgotModel,
    (path) => {
      required(path.email, { message: 'Email is required' });
      email(path.email, { message: 'Enter a valid email' });
    },
    {
      submission: {
        action: async (form) => {
          await firstValueFrom(this.auth.forgotPassword(form().value().email)).catch(
            () => undefined,
          );
          this.sent.set(true);
          return;
        },
      },
    },
  );
}
