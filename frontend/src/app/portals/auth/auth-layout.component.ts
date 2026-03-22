import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'chrono-auth-layout',
  standalone: true,
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './auth-layout.component.scss',
  template: `
    <div class="auth-shell">
      <div class="auth-shell__brand">
        <div class="auth-shell__logo">
          <span class="material-icons-outlined" style="font-size:40px;color:var(--color-primary)">
            schedule
          </span>
        </div>
        <h1 class="auth-shell__brand-name">Chrono</h1>
        <p class="auth-shell__tagline">Enterprise Data Integration Platform</p>
      </div>

      <div class="auth-shell__card">
        <router-outlet />
      </div>

      <footer class="auth-shell__footer">
        &copy; {{ year }} Chrono. All rights reserved.
      </footer>
    </div>
  `,
})
export class AuthLayoutComponent {
  readonly year = new Date().getFullYear();
}
