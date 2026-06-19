import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { UpdateUserRequest } from '../../../../shared/models/auth.types';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe } from '@angular/common';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-profile-page',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
})
export class ProfilePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly user = this.authService.currentUser;
  loading = signal(false);
  saving = signal(false);
  error = signal('');
  success = signal(false);
  isEditing = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
  });

  ngOnInit(): void {
    this.loadUser();
  }

  private loadUser(): void {
    this.loading.set(true);
    this.error.set('');
    this.authService
      .getCurrentUser()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({ error: () => this.error.set('Failed to load profile.') });
  }

  startEditing(): void {
    this.isEditing.set(true);
    this.success.set(false);
    const u = this.user();
    if (u) {
      this.form.patchValue({ name: u.name, email: u.email });
    }
  }

  cancelEditing(): void {
    this.isEditing.set(false);
    this.form.reset();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.success.set(false);
    const request: UpdateUserRequest = this.form.getRawValue();
    this.authService
      .updateProfile(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.success.set(true);
          this.isEditing.set(false);
        },
        error: () => this.error.set('Failed to update profile.'),
      });
  }
}
