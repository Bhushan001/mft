import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'chrono-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="login">
      <h2 class="login__title">Welcome back</h2>
      <p class="login__subtitle">Sign in to your account</p>

      <form [formGroup]="form" (ngSubmit)="submit()" class="login__form">
        <div class="form-group">
          <label class="form-label" for="email">
            Email <span class="required">*</span>
          </label>
          <input
            id="email"
            type="email"
            class="form-control"
            [class.is-invalid]="isFieldInvalid('email')"
            formControlName="email"
            placeholder="you@company.com"
            autocomplete="email"
          />
          @if (isFieldInvalid('email')) {
            <span class="form-error">
              <span class="material-icons-outlined">error_outline</span>
              Please enter a valid email address
            </span>
          }
        </div>

        <div class="form-group">
          <div style="display:flex;align-items:center;justify-content:space-between">
            <label class="form-label" for="password">
              Password <span class="required">*</span>
            </label>
            <a routerLink="../forgot-password" class="login__forgot">Forgot password?</a>
          </div>
          <div class="input-group">
            <input
              id="password"
              [type]="showPassword() ? 'text' : 'password'"
              class="form-control"
              [class.is-invalid]="isFieldInvalid('password')"
              formControlName="password"
              placeholder="Enter your password"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="input-group__suffix"
              style="pointer-events:all;cursor:pointer"
              (click)="showPassword.set(!showPassword())"
            >
              <span class="material-icons-outlined" style="font-size:18px;color:var(--color-text-secondary)">
                {{ showPassword() ? 'visibility_off' : 'visibility' }}
              </span>
            </button>
          </div>
          @if (isFieldInvalid('password')) {
            <span class="form-error">
              <span class="material-icons-outlined">error_outline</span>
              Password is required
            </span>
          }
        </div>

        <button
          type="submit"
          class="btn btn--primary btn--block"
          [disabled]="loading()"
        >
          @if (loading()) {
            <span class="spinner spinner--sm spinner--white"></span>
            Signing in...
          } @else {
            Sign in
          }
        </button>
      </form>
    </div>
  `,
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(false);
  readonly showPassword = signal(false);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  isFieldInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading.set(true);
    const { email, password } = this.form.getRawValue();

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/hub';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err) => {
        this.loading.set(false);
        const message = err.error?.message ?? 'Invalid credentials. Please try again.';
        this.toastService.error('Login Failed', message);
      },
      complete: () => this.loading.set(false),
    });
  }
}
