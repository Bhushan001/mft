import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'chrono-toast-container',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toast-container" role="region" aria-live="polite" aria-label="Notifications">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="'toast--' + toast.type" role="alert">
          <div class="toast__icon">
            <span class="material-icons-outlined">{{ iconFor(toast.type) }}</span>
          </div>
          <div class="toast__content">
            <div class="toast__title">{{ toast.title }}</div>
            @if (toast.message) {
              <div class="toast__message">{{ toast.message }}</div>
            }
          </div>
          <button
            class="toast__close"
            (click)="toastService.dismiss(toast.id)"
            aria-label="Dismiss notification"
          >
            <span class="material-icons-outlined">close</span>
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastContainerComponent {
  protected readonly toastService = inject(ToastService);

  iconFor(type: string): string {
    const icons: Record<string, string> = {
      success: 'check_circle_outline',
      error: 'error_outline',
      warning: 'warning_amber',
      info: 'info_outline',
    };
    return icons[type] ?? 'info_outline';
  }
}
