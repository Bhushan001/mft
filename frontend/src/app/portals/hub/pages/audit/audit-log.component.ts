import {
  Component, ChangeDetectionStrategy, OnInit, inject, signal, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { ApiResponse, PageResponse } from '../../../../core/models/api.models';

export interface AuditEntry {
  id: string;
  action: string;
  resourceType: string;
  resourceId: string;
  performedBy: string;
  tenantId: string;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

@Component({
  selector: 'chrono-audit-log',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Audit Log</h2>
        <p class="text-secondary text-sm">Platform-wide activity trail — all tenant and user operations.</p>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-4">
      <div class="card__body">
        <div class="grid grid--3">
          <div class="form-group">
            <label class="form-label">Tenant ID</label>
            <input class="form-input" [(ngModel)]="filterTenant"
                   placeholder="Filter by tenant…" (keyup.enter)="load()">
          </div>
          <div class="form-group">
            <label class="form-label">Action</label>
            <input class="form-input" [(ngModel)]="filterAction"
                   placeholder="e.g. CREATE, UPDATE, DELETE" (keyup.enter)="load()">
          </div>
          <div class="form-group">
            <label class="form-label">Resource Type</label>
            <input class="form-input" [(ngModel)]="filterResource"
                   placeholder="e.g. Tenant, User, MappingRule" (keyup.enter)="load()">
          </div>
        </div>
        <div class="mt-3" style="display:flex;gap:8px;justify-content:flex-end;">
          <button class="btn btn--ghost btn--sm" (click)="clearFilters()">Clear</button>
          <button class="btn btn--primary btn--sm" (click)="load()">Apply</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card__body p-0">
        <ng-container *ngIf="!loading(); else skeleton">
          <table class="table" *ngIf="entries().length; else empty">
            <thead>
              <tr>
                <th>Time</th>
                <th>Action</th>
                <th>Resource</th>
                <th>Performed By</th>
                <th>Tenant</th>
                <th>IP</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let e of entries()">
                <td class="text-secondary text-sm" style="white-space:nowrap">
                  {{ e.createdAt | date:'yyyy-MM-dd HH:mm:ss' }}
                </td>
                <td>
                  <span class="badge" [ngClass]="actionBadge(e.action)">{{ e.action }}</span>
                </td>
                <td class="text-sm">
                  <span class="text-secondary">{{ e.resourceType }}</span>
                  <span class="text-muted"> / </span>
                  <span>{{ e.resourceId | slice:0:12 }}…</span>
                </td>
                <td class="text-sm">{{ e.performedBy }}</td>
                <td class="text-secondary text-sm">{{ e.tenantId }}</td>
                <td class="text-secondary text-sm">{{ e.ipAddress || '—' }}</td>
              </tr>
            </tbody>
          </table>
          <ng-template #empty>
            <div class="empty-state">
              <span class="material-icons-outlined empty-state__icon">history</span>
              <div class="empty-state__title">No audit events found</div>
              <p class="empty-state__description">
                Audit logging will appear here once the audit service is connected.
              </p>
            </div>
          </ng-template>
        </ng-container>
        <ng-template #skeleton>
          <div class="skeleton" style="height:300px;"></div>
        </ng-template>
      </div>

      <!-- Pagination -->
      <div class="card__footer" *ngIf="totalPages() > 1" style="display:flex;align-items:center;justify-content:space-between;">
        <span class="text-secondary text-sm">
          Page {{ page() + 1 }} of {{ totalPages() }} ({{ totalElements() }} events)
        </span>
        <div style="display:flex;gap:8px;">
          <button class="btn btn--ghost btn--sm" [disabled]="page() === 0" (click)="prevPage()">
            <span class="material-icons-outlined" style="font-size:16px">chevron_left</span>
          </button>
          <button class="btn btn--ghost btn--sm" [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
            <span class="material-icons-outlined" style="font-size:16px">chevron_right</span>
          </button>
        </div>
      </div>
    </div>
  `,
})
export class AuditLogComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = signal(true);
  entries = signal<AuditEntry[]>([]);
  page = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);

  filterTenant = '';
  filterAction = '';
  filterResource = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    let params = new HttpParams()
      .set('page', this.page())
      .set('size', 20);
    if (this.filterTenant)  params = params.set('tenantId', this.filterTenant);
    if (this.filterAction)  params = params.set('action', this.filterAction);
    if (this.filterResource) params = params.set('resourceType', this.filterResource);

    this.http
      .get<ApiResponse<PageResponse<AuditEntry>>>(`${environment.apiBaseUrl}/audit/events`, { params })
      .subscribe({
        next: (res) => {
          this.entries.set(res.data?.content ?? []);
          this.totalPages.set(res.data?.totalPages ?? 0);
          this.totalElements.set(res.data?.totalElements ?? 0);
          this.loading.set(false);
          this.cdr.markForCheck();
        },
        error: () => {
          this.entries.set([]);
          this.loading.set(false);
          this.cdr.markForCheck();
        },
      });
  }

  clearFilters(): void {
    this.filterTenant = '';
    this.filterAction = '';
    this.filterResource = '';
    this.page.set(0);
    this.load();
  }

  prevPage(): void { this.page.update(p => p - 1); this.load(); }
  nextPage(): void { this.page.update(p => p + 1); this.load(); }

  actionBadge(action: string): string {
    const map: Record<string, string> = {
      CREATE: 'badge--success',
      UPDATE: 'badge--info',
      DELETE: 'badge--danger',
      LOGIN:  'badge--neutral',
      LOGOUT: 'badge--neutral',
    };
    return map[action] ?? 'badge--neutral';
  }
}
