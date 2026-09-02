import { Component, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, maxLength, minLength, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { apiErrorMessage } from '../../../core/api-error';

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
      maxLength(path.username, 100, { message: 'Username must be at most 100 characters' });
      required(path.email, { message: 'Email is required' });
      email(path.email, { message: 'Enter a valid email' });
      maxLength(path.email, 255, { message: 'Email must be at most 255 characters' });
      required(path.password, { message: 'Password is required' });
      minLength(path.password, 6, { message: 'Password must be at least 6 characters' });
      maxLength(path.password, 100, { message: 'Password must be at most 100 characters' });
      maxLength(path.firstName, 100, { message: 'First name must be at most 100 characters' });
      maxLength(path.lastName, 100, { message: 'Last name must be at most 100 characters' });
      maxLength(path.phone, 30, { message: 'Phone must be at most 30 characters' });
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
            this.error.set(apiErrorMessage(err, 'Registration failed.'));
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
