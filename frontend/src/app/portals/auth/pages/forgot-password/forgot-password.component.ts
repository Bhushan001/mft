import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../../core/services/toast.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'chrono-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="forgot">
      <a routerLink="../login" class="forgot__back">
        <span class="material-icons-outlined" style="font-size:16px">arrow_back</span>
        Back to login
      </a>

      <h2 class="forgot__title">Reset password</h2>
      <p class="forgot__subtitle">
        Enter your email and we'll send you a reset link.
      </p>

      @if (!sent()) {
        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-group">
            <label class="form-label" for="email">Email address</label>
            <input
              id="email"
              type="email"
              class="form-control"
              [class.is-invalid]="isFieldInvalid('email')"
              formControlName="email"
              placeholder="you@company.com"
            />
            @if (isFieldInvalid('email')) {
              <span class="form-error">
                <span class="material-icons-outlined">error_outline</span>
                Please enter a valid email
              </span>
            }
          </div>

          <button type="submit" class="btn btn--primary btn--block" [disabled]="loading()">
            @if (loading()) {
              <span class="spinner spinner--sm spinner--white"></span>
              Sending...
            } @else {
              Send reset link
            }
          </button>
        </form>
      } @else {
        <div class="forgot__success">
          <span class="material-icons-outlined forgot__success-icon">mark_email_read</span>
          <p>Check your inbox — we've sent a reset link to <strong>{{ form.value.email }}</strong>.</p>
          <a routerLink="../login" class="btn btn--secondary btn--block">Back to login</a>
        </div>
      }
    </div>
  `,
  styleUrl: './forgot-password.component.scss',
})
export class ForgotPasswordComponent {
  private readonly http = inject(HttpClient);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly sent = signal(false);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  isFieldInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading.set(true);
    const { email } = this.form.getRawValue();

    this.http
      .post(`${environment.apiBaseUrl}/auth/forgot-password`, { email })
      .subscribe({
        next: () => this.sent.set(true),
        error: () => {
          this.loading.set(false);
          this.toastService.error('Error', 'Could not send reset email. Please try again.');
        },
        complete: () => this.loading.set(false),
      });
  }
}
