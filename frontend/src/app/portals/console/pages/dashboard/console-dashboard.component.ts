import {
  Component, ChangeDetectionStrategy, OnInit, inject, signal, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { TenantService } from '../../../../core/services/tenant.service';
import { UserService } from '../../../../core/services/user.service';
import { TenantResponse } from '../../../../core/models/tenant.models';

@Component({
  selector: 'chrono-console-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Admin Console</h2>
        <p class="text-secondary text-sm">Platform overview — tenants, users, and system health.</p>
      </div>
      <div class="section-header__actions">
        <a routerLink="/console/tenants" class="btn btn--primary">
          <span class="material-icons-outlined">add</span>
          New Tenant
        </a>
      </div>
    </div>

    <!-- KPI row -->
    <div class="grid grid--4 mb-6" *ngIf="!loading(); else kpiSkeleton">
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Total Tenants</p>
          <p class="text-2xl font-bold">{{ stats().totalTenants }}</p>
          <p class="text-secondary text-xs mt-1">
            <span class="badge badge--success">{{ stats().activeTenants }} active</span>
          </p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Total Users</p>
          <p class="text-2xl font-bold">{{ stats().totalUsers }}</p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Suspended</p>
          <p class="text-2xl font-bold">{{ stats().suspendedTenants }}</p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Inactive</p>
          <p class="text-2xl font-bold">{{ stats().inactiveTenants }}</p>
        </div>
      </div>
    </div>

    <ng-template #kpiSkeleton>
      <div class="grid grid--4 mb-6">
        <div class="card skeleton" *ngFor="let i of [1,2,3,4]">
          <div class="card__body" style="height:72px;"></div>
        </div>
      </div>
    </ng-template>

    <!-- Recent tenants -->
    <div class="card">
      <div class="card__header">
        <h3 class="card__title">Recent Tenants</h3>
        <a routerLink="/console/tenants" class="btn btn--ghost btn--sm">View all</a>
      </div>
      <div class="card__body p-0">
        <ng-container *ngIf="!loading(); else tableSkeleton">
          <table class="table" *ngIf="recentTenants().length; else emptyTenants">
            <thead>
              <tr>
                <th>Name</th>
                <th>Slug</th>
                <th>Plan</th>
                <th>Status</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let t of recentTenants()">
                <td>{{ t.name }}</td>
                <td class="text-secondary">{{ t.slug }}</td>
                <td>{{ t.plan || '—' }}</td>
                <td><span class="badge" [ngClass]="statusBadge(t.status)">{{ t.status }}</span></td>
                <td class="text-secondary text-sm">{{ t.createdAt | date:'mediumDate' }}</td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyTenants>
            <div class="empty-state">
              <span class="material-icons-outlined empty-state__icon">business</span>
              <div class="empty-state__title">No tenants yet</div>
              <p class="empty-state__description">Create the first tenant to get started.</p>
            </div>
          </ng-template>
        </ng-container>
        <ng-template #tableSkeleton>
          <div class="skeleton" style="height:200px;"></div>
        </ng-template>
      </div>
    </div>
  `,
})
export class ConsoleDashboardComponent implements OnInit {
  private readonly tenantService = inject(TenantService);
  private readonly userService = inject(UserService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = signal(true);
  recentTenants = signal<TenantResponse[]>([]);
  stats = signal({ totalTenants: 0, activeTenants: 0, suspendedTenants: 0, inactiveTenants: 0, totalUsers: 0 });

  ngOnInit(): void {
    forkJoin({
      tenants: this.tenantService.list(0, 5),
      users: this.userService.list(0, 1),
    }).subscribe({
      next: ({ tenants, users }) => {
        const items = tenants.data?.content ?? [];
        this.recentTenants.set(items);
        this.stats.set({
          totalTenants: tenants.data?.totalElements ?? 0,
          activeTenants: items.filter(t => t.status === 'ACTIVE').length,
          suspendedTenants: items.filter(t => t.status === 'SUSPENDED').length,
          inactiveTenants: items.filter(t => t.status === 'INACTIVE').length,
          totalUsers: users.data?.totalElements ?? 0,
        });
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge--success',
      SUSPENDED: 'badge--warning',
      INACTIVE: 'badge--neutral',
    };
    return map[status] ?? 'badge--neutral';
  }
}
