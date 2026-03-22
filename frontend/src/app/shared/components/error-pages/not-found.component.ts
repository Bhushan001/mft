import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'chrono-not-found',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="error-page">
      <div class="error-page__code">404</div>
      <h1 class="error-page__title">Page not found</h1>
      <p class="error-page__description">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <a routerLink="/hub" class="btn btn--primary">Go to Dashboard</a>
    </div>
  `,
  styleUrl: './not-found.component.scss',
})
export class NotFoundComponent {}
