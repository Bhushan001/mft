import { Component, ChangeDetectionStrategy, inject, signal, OnInit } from '@angular/core';
import { AuthService } from '../../../../core/services/auth.service';
import { WorkflowService } from '../../../../core/services/workflow.service';
import { MappingService } from '../../../../core/services/mapping.service';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

interface KpiStat {
  label: string;
  value: string;
  trend: 'up' | 'down' | 'flat';
  change: string;
  icon: string;
}

@Component({
  selector: 'chrono-hub-dashboard',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Welcome back, {{ firstName() }}</h2>
        <p class="text-secondary text-sm">Here's what's happening across your platform.</p>
      </div>
    </div>

    <div class="stats-grid">
      @for (stat of stats(); track stat.label) {
        <div class="stat-card">
          <div class="stat-card__label">{{ stat.label }}</div>
          @if (loading()) {
            <div class="skeleton" style="height:2rem;width:4rem;margin:var(--space-2) 0"></div>
          } @else {
            <div class="stat-card__value">{{ stat.value }}</div>
          }
          <div class="stat-card__trend" [class]="'stat-card__trend--' + stat.trend">
            <span class="material-icons-outlined">
              {{ stat.trend === 'up' ? 'trending_up' : stat.trend === 'down' ? 'trending_down' : 'trending_flat' }}
            </span>
            {{ stat.change }}
          </div>
        </div>
      }
    </div>

    <div class="card" style="margin-top:var(--space-xl)">
      <div class="card__header">
        <h3 class="card__header-title">Quick Actions</h3>
      </div>
      <div class="card__body">
        <div class="quick-actions">
          <a routerLink="/workspace/jobs" class="quick-action">
            <span class="material-icons-outlined quick-action__icon">play_circle_outline</span>
            <span class="quick-action__label">Start Job</span>
          </a>
          <a routerLink="/workspace/mappings" class="quick-action">
            <span class="material-icons-outlined quick-action__icon">account_tree</span>
            <span class="quick-action__label">Create Mapping</span>
          </a>
          <a routerLink="/console/users" class="quick-action">
            <span class="material-icons-outlined quick-action__icon">group_add</span>
            <span class="quick-action__label">Invite User</span>
          </a>
          <a routerLink="/console/tenants" class="quick-action">
            <span class="material-icons-outlined quick-action__icon">business</span>
            <span class="quick-action__label">Manage Tenants</span>
          </a>
        </div>
      </div>
    </div>
  `,
  styleUrl: './hub-dashboard.component.scss',
})
export class HubDashboardComponent implements OnInit {
  private readonly authService     = inject(AuthService);
  private readonly workflowService = inject(WorkflowService);
  private readonly mappingService  = inject(MappingService);

  readonly loading = signal(true);
  readonly stats = signal<KpiStat[]>([
    { label: 'Active Jobs',     value: '—', trend: 'flat', change: 'Loading...', icon: 'play_circle_outline' },
    { label: 'Completed Today', value: '—', trend: 'flat', change: 'Loading...', icon: 'check_circle_outline' },
    { label: 'Failed Jobs',     value: '—', trend: 'flat', change: 'Loading...', icon: 'error_outline' },
    { label: 'Mapped Schemas',  value: '—', trend: 'flat', change: 'Loading...', icon: 'account_tree' },
  ]);

  firstName(): string {
    return this.authService.currentUser()?.firstName ?? 'there';
  }

  ngOnInit(): void {
    forkJoin({
      running:   this.workflowService.list(0, 1, 'RUNNING'),
      completed: this.workflowService.list(0, 1, 'COMPLETED'),
      failed:    this.workflowService.list(0, 1, 'FAILED'),
      mappings:  this.mappingService.list(0, 1),
    }).subscribe({
      next: ({ running, completed, failed, mappings }) => {
        this.stats.set([
          {
            label: 'Active Jobs',
            value: String(running.data?.totalElements ?? 0),
            trend: 'flat',
            change: 'Running now',
            icon: 'play_circle_outline',
          },
          {
            label: 'Completed Today',
            value: String(completed.data?.totalElements ?? 0),
            trend: 'up',
            change: 'All time',
            icon: 'check_circle_outline',
          },
          {
            label: 'Failed Jobs',
            value: String(failed.data?.totalElements ?? 0),
            trend: (failed.data?.totalElements ?? 0) > 0 ? 'down' : 'flat',
            change: (failed.data?.totalElements ?? 0) > 0 ? 'Needs attention' : 'All clear',
            icon: 'error_outline',
          },
          {
            label: 'Mapped Schemas',
            value: String(mappings.data?.totalElements ?? 0),
            trend: 'flat',
            change: 'Total rules',
            icon: 'account_tree',
          },
        ]);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.stats.update(s => s.map(stat => ({ ...stat, value: 'N/A', change: 'Unavailable' })));
      },
    });
  }
}
