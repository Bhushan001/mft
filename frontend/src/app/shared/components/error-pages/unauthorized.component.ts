import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'chrono-unauthorized',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="error-page">
      <span class="material-icons-outlined error-page__icon">lock_outline</span>
      <h1 class="error-page__title">Access Denied</h1>
      <p class="error-page__description">
        You don't have permission to view this page. Contact your administrator if you believe this is an error.
      </p>
      <div style="display:flex;gap:var(--space-sm)">
        <button class="btn btn--secondary" (click)="goBack()">Go Back</button>
        <a routerLink="/hub" class="btn btn--primary">Go to Dashboard</a>
      </div>
    </div>
  `,
  styleUrl: './unauthorized.component.scss',
})
export class UnauthorizedComponent {
  goBack(): void {
    history.back();
  }
}
