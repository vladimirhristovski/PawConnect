import { Component, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, minLength, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormField, FormRoot, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  registerModel = signal<RegisterForm>({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
  });
  error = signal<string | null>(null);
  success = signal(false);

  registerForm = form(
    this.registerModel,
    (path) => {
      required(path.username, { message: 'Username is required' });
      minLength(path.username, 3, { message: 'Username must be at least 3 characters' });
      required(path.email, { message: 'Email is required' });
      email(path.email, { message: 'Enter a valid email' });
      required(path.password, { message: 'Password is required' });
      minLength(path.password, 6, { message: 'Password must be at least 6 characters' });
    },
    {
      submission: {
        action: async (form) => {
          this.error.set(null);
          try {
            await firstValueFrom(this.auth.register(form().value()));
            this.success.set(true);
            setTimeout(() => this.router.navigate(['/login']), 1200);
          } catch (err) {
            const detail = (err as { error?: { detail?: string } }).error?.detail;
            this.error.set(detail ?? 'Registration failed.');
          }
          return;
        },
      },
    },
  );
}

interface RegisterForm {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
}
