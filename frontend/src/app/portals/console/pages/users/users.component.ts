import {
  Component, ChangeDetectionStrategy, inject, signal, OnInit
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { UserResponse } from '../../../../core/models/user.models';
import { PageResponse } from '../../../../core/models/api.models';

@Component({
  selector: 'chrono-users',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Users</h2>
        <p class="text-secondary text-sm">Manage user accounts and permissions.</p>
      </div>
      <div class="section-header__actions">
        <button class="btn btn--primary" (click)="showInvite.set(true)">
          <span class="material-icons-outlined">person_add</span>
          Invite User
        </button>
      </div>
    </div>

    @if (showInvite()) {
      <div class="modal-overlay" (click)="showInvite.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal__header">
            <h3 class="modal__title">Invite User</h3>
            <button class="modal__close" (click)="showInvite.set(false)">
              <span class="material-icons-outlined">close</span>
            </button>
          </div>
          <form [formGroup]="inviteForm" (ngSubmit)="submitInvite()" class="modal__body">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">First Name *</label>
                <input class="form-control" formControlName="firstName" placeholder="Jane" />
              </div>
              <div class="form-group">
                <label class="form-label">Last Name *</label>
                <input class="form-control" formControlName="lastName" placeholder="Smith" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Email *</label>
              <input class="form-control" type="email" formControlName="email" placeholder="jane@company.com" />
            </div>
            <div class="form-group">
              <label class="form-label">Temporary Password *</label>
              <input class="form-control" type="password" formControlName="password" placeholder="Min 8 characters" />
            </div>
            <div class="form-group">
              <label class="form-label">Role *</label>
              <select class="form-control" formControlName="role">
                <option value="TENANT_USER">Tenant User</option>
                <option value="TENANT_ADMIN">Tenant Admin</option>
              </select>
            </div>
            <div class="modal__footer">
              <button type="button" class="btn btn--secondary" (click)="showInvite.set(false)">Cancel</button>
              <button type="submit" class="btn btn--primary" [disabled]="saving()">
                @if (saving()) { <span class="spinner spinner--sm spinner--white"></span> }
                Send Invite
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
            @for (_ of skeletonRows; track $index) { <div class="skeleton skeleton--row"></div> }
          </div>
        </div>
      } @else if (page()?.content?.length === 0) {
        <div class="card__body">
          <div class="empty-state">
            <span class="material-icons-outlined empty-state__icon">people_outline</span>
            <div class="empty-state__title">No users found</div>
            <p class="empty-state__description">Invite your first team member to get started.</p>
          </div>
        </div>
      } @else {
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th>
              </tr>
            </thead>
            <tbody>
              @for (user of page()?.content; track user.userId) {
                <tr>
                  <td class="font-medium">{{ user.firstName }} {{ user.lastName }}</td>
                  <td class="text-sm">{{ user.email }}</td>
                  <td><span [class]="'badge badge--' + roleBadge(user.role)">{{ user.role }}</span></td>
                  <td><span [class]="'badge badge--' + statusBadge(user.status)">{{ user.status }}</span></td>
                  <td class="text-secondary text-sm">{{ user.createdAt.slice(0, 10) }}</td>
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
export class UsersComponent implements OnInit {
  private readonly userService  = inject(UserService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading     = signal(true);
  readonly saving      = signal(false);
  readonly showInvite  = signal(false);
  readonly page        = signal<PageResponse<UserResponse> | null>(null);
  readonly currentPage = signal(0);
  readonly skeletonRows = [1, 2, 3, 4, 5];

  readonly inviteForm = this.fb.group({
    firstName: ['', Validators.required],
    lastName:  ['', Validators.required],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8)]],
    role:      ['TENANT_USER', Validators.required],
  });

  ngOnInit(): void { this.loadPage(0); }

  loadPage(page: number): void {
    this.loading.set(true);
    this.currentPage.set(page);
    this.userService.list(page).subscribe({
      next: res => { this.page.set(res.data); this.loading.set(false); },
      error: ()  => { this.loading.set(false); this.toastService.error('Error', 'Failed to load users'); },
    });
  }

  submitInvite(): void {
    this.inviteForm.markAllAsTouched();
    if (this.inviteForm.invalid) return;
    this.saving.set(true);
    const v = this.inviteForm.getRawValue();
    this.userService.create({
      firstName: v.firstName!, lastName: v.lastName!,
      email: v.email!, password: v.password!, role: v.role as any,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showInvite.set(false);
        this.inviteForm.reset({ role: 'TENANT_USER' });
        this.toastService.success('User invited', v.email + ' has been added.');
        this.loadPage(0);
      },
      error: err => {
        this.saving.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to invite user');
      },
    });
  }

  roleBadge(role: string): string {
    const map: Record<string, string> = { PLATFORM_ADMIN: 'danger', TENANT_ADMIN: 'warning', TENANT_USER: 'info' };
    return map[role] ?? 'neutral';
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = { ACTIVE: 'success', INACTIVE: 'neutral', PENDING: 'warning' };
    return map[status] ?? 'neutral';
  }
}
