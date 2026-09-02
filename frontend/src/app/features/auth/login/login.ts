import { Component, inject, signal } from '@angular/core';
import { form, FormField, FormRoot, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  loginModel = signal({ username: '', password: '' });
  error = signal<string | null>(null);

  loginForm = form(
    this.loginModel,
    (path) => {
      required(path.username, { message: 'Username is required' });
      required(path.password, { message: 'Password is required' });
    },
    {
      submission: {
        action: async (form) => {
          this.error.set(null);
          try {
            await firstValueFrom(this.auth.login(form().value()));
            this.router.navigate(['/']);
          } catch (err) {
            const detail = (err as { error?: { detail?: string } }).error?.detail;
            this.error.set(detail ?? 'Invalid username or password.');
          }
          return;
        },
      },
    },
  );
}
