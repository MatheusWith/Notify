import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { RegisterRequest } from '../../../shared/models/auth.types';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-register-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './register-page.html',
  styleUrl: './register-page.scss',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  error = '';
  loading = signal(false);

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.error = '';
    this.loading.set(true);
    const request: RegisterRequest = this.form.getRawValue();

    this.authService.register(request).subscribe({
      next: () => {
        this.loading.set(false);
        this.authService.getCurrentUser().subscribe({
          next: () => this.router.navigate(['/']),
        });
      },
      error: (err) => {
        this.loading.set(false);
        if (err.error?.violations?.length) {
          this.error = err.error.violations.map((v: { message: string }) => v.message).join(', ');
        } else if (err.error?.detail) {
          this.error = err.error.detail;
        } else {
          this.error = 'Registration failed. Please try again.';
        }
      },
    });
  }
}
