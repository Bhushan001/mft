import {
  Component, ChangeDetectionStrategy, inject, signal, OnInit
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TenantService } from '../../../../core/services/tenant.service';
import { ToastService } from '../../../../core/services/toast.service';
import { TenantResponse } from '../../../../core/models/tenant.models';
import { PageResponse } from '../../../../core/models/api.models';

@Component({
  selector: 'chrono-tenants',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Tenants</h2>
        <p class="text-secondary text-sm">Manage tenant organizations.</p>
      </div>
      <div class="section-header__actions">
        <button class="btn btn--primary" (click)="showCreate.set(true)">
          <span class="material-icons-outlined">add</span>
          New Tenant
        </button>
      </div>
    </div>

    @if (showCreate()) {
      <div class="modal-overlay" (click)="showCreate.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal__header">
            <h3 class="modal__title">New Tenant</h3>
            <button class="modal__close" (click)="showCreate.set(false)">
              <span class="material-icons-outlined">close</span>
            </button>
          </div>
          <form [formGroup]="createForm" (ngSubmit)="submitCreate()" class="modal__body">
            <div class="form-group">
              <label class="form-label">Name *</label>
              <input class="form-control" formControlName="name" placeholder="Acme Corp" />
            </div>
            <div class="form-group">
              <label class="form-label">Slug *</label>
              <input class="form-control" formControlName="slug" placeholder="acme-corp" />
              <span class="form-hint">Unique identifier, lowercase with hyphens</span>
            </div>
            <div class="form-group">
              <label class="form-label">Contact Email *</label>
              <input class="form-control" type="email" formControlName="contactEmail" placeholder="admin@acme.com" />
            </div>
            <div class="form-group">
              <label class="form-label">Plan</label>
              <select class="form-control" formControlName="plan">
                <option value="FREE">Free</option>
                <option value="STARTER">Starter</option>
                <option value="PROFESSIONAL">Professional</option>
                <option value="ENTERPRISE">Enterprise</option>
              </select>
            </div>
            <div class="modal__footer">
              <button type="button" class="btn btn--secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn--primary" [disabled]="saving()">
                @if (saving()) { <span class="spinner spinner--sm spinner--white"></span> }
                Create Tenant
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <div class="card">
      @if (loading()) {
        <div class="card__body">
          <div class="skeleton-list">
            @for (_ of skeletonRows; track $index) {
              <div class="skeleton skeleton--row"></div>
            }
          </div>
        </div>
      } @else if (page()?.content?.length === 0) {
        <div class="card__body">
          <div class="empty-state">
            <span class="material-icons-outlined empty-state__icon">business</span>
            <div class="empty-state__title">No tenants yet</div>
            <p class="empty-state__description">Create your first tenant to get started.</p>
          </div>
        </div>
      } @else {
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Name</th><th>Slug</th><th>Plan</th><th>Status</th><th>Contact</th><th>Created</th>
              </tr>
            </thead>
            <tbody>
              @for (tenant of page()?.content; track tenant.tenantId) {
                <tr>
                  <td class="font-medium">{{ tenant.name }}</td>
                  <td class="text-secondary text-sm">{{ tenant.slug }}</td>
                  <td>{{ tenant.plan }}</td>
                  <td><span [class]="'badge badge--' + statusBadge(tenant.status)">{{ tenant.status }}</span></td>
                  <td class="text-sm">{{ tenant.contactEmail }}</td>
                  <td class="text-secondary text-sm">{{ tenant.createdAt.slice(0, 10) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (page() && page()!.totalPages > 1) {
          <div class="card__footer pagination">
            <span class="text-sm text-secondary">{{ page()!.totalElements }} total</span>
            <div class="pagination__controls">
              <button class="btn btn--ghost btn--sm" [disabled]="currentPage() === 0" (click)="loadPage(currentPage() - 1)">
                <span class="material-icons-outlined">chevron_left</span>
              </button>
              <span class="text-sm">{{ currentPage() + 1 }} / {{ page()!.totalPages }}</span>
              <button class="btn btn--ghost btn--sm" [disabled]="page()!.last" (click)="loadPage(currentPage() + 1)">
                <span class="material-icons-outlined">chevron_right</span>
              </button>
            </div>
          </div>
        }
      }
    </div>
  `,
})
export class TenantsComponent implements OnInit {
  private readonly tenantService = inject(TenantService);
  private readonly toastService  = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading     = signal(true);
  readonly saving      = signal(false);
  readonly showCreate  = signal(false);
  readonly page        = signal<PageResponse<TenantResponse> | null>(null);
  readonly currentPage = signal(0);
  readonly skeletonRows = [1, 2, 3, 4, 5];

  readonly createForm = this.fb.group({
    name:         ['', Validators.required],
    slug:         ['', Validators.required],
    contactEmail: ['', [Validators.required, Validators.email]],
    plan:         ['FREE'],
  });

  ngOnInit(): void { this.loadPage(0); }

  loadPage(page: number): void {
    this.loading.set(true);
    this.currentPage.set(page);
    this.tenantService.list(page).subscribe({
      next: res => { this.page.set(res.data); this.loading.set(false); },
      error: ()  => { this.loading.set(false); this.toastService.error('Error', 'Failed to load tenants'); },
    });
  }

  submitCreate(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;
    this.saving.set(true);
    const { name, slug, contactEmail, plan } = this.createForm.getRawValue();
    this.tenantService.create({ name: name!, slug: slug!, contactEmail: contactEmail!, plan: plan! }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreate.set(false);
        this.createForm.reset({ plan: 'FREE' });
        this.toastService.success('Tenant created', name + ' has been provisioned.');
        this.loadPage(0);
      },
      error: err => {
        this.saving.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to create tenant');
      },
    });
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = { ACTIVE: 'success', SUSPENDED: 'warning', INACTIVE: 'neutral' };
    return map[status] ?? 'neutral';
  }
}
