import {
  Component, ChangeDetectionStrategy, OnInit, inject, signal, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { WorkflowService } from '../../../../core/services/workflow.service';
import { EtlJobService } from '../../../../core/services/etl-job.service';
import { MappingService } from '../../../../core/services/mapping.service';
import { WorkflowResponse } from '../../../../core/models/workflow.models';
import { EtlJobResponse } from '../../../../core/models/etl.models';

@Component({
  selector: 'chrono-workspace-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Workspace</h2>
        <p class="text-secondary text-sm">Monitor jobs, mappings, and data flows.</p>
      </div>
      <div class="section-header__actions">
        <a routerLink="/workspace/jobs" class="btn btn--primary">
          <span class="material-icons-outlined">add</span>
          New Job
        </a>
      </div>
    </div>

    <!-- KPI row -->
    <div class="grid grid--4 mb-6" *ngIf="!loading(); else kpiSkeleton">
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Running Workflows</p>
          <p class="text-2xl font-bold">{{ stats().runningWorkflows }}</p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Completed Today</p>
          <p class="text-2xl font-bold">{{ stats().completedWorkflows }}</p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Failed</p>
          <p class="text-2xl font-bold">{{ stats().failedWorkflows }}</p>
          <p class="text-secondary text-xs mt-1" *ngIf="stats().failedWorkflows > 0">
            <span class="badge badge--danger">needs attention</span>
          </p>
        </div>
      </div>
      <div class="card">
        <div class="card__body">
          <p class="text-secondary text-sm mb-1">Active Mapping</p>
          <p class="text-2xl font-bold">{{ stats().activeMappings }}</p>
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

    <div class="grid grid--2">
      <!-- Recent Workflows -->
      <div class="card">
        <div class="card__header">
          <h3 class="card__title">Recent Workflows</h3>
          <a routerLink="/workspace/jobs" class="btn btn--ghost btn--sm">View all</a>
        </div>
        <div class="card__body p-0">
          <ng-container *ngIf="!loading(); else rowSkeleton">
            <table class="table" *ngIf="recentWorkflows().length; else emptyWorkflows">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Started</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let w of recentWorkflows()">
                  <td class="text-sm">{{ w.workflowType | titlecase }}</td>
                  <td><span class="badge" [ngClass]="workflowBadge(w.status)">{{ w.status }}</span></td>
                  <td class="text-secondary text-sm">{{ w.startedAt | date:'shortTime' }}</td>
                </tr>
              </tbody>
            </table>
            <ng-template #emptyWorkflows>
              <div class="empty-state">
                <span class="material-icons-outlined empty-state__icon">account_tree</span>
                <div class="empty-state__title">No workflows yet</div>
                <p class="empty-state__description">Start a workflow to see activity here.</p>
              </div>
            </ng-template>
          </ng-container>
          <ng-template #rowSkeleton>
            <div class="skeleton" style="height:160px;"></div>
          </ng-template>
        </div>
      </div>

      <!-- Recent ETL Jobs -->
      <div class="card">
        <div class="card__header">
          <h3 class="card__title">Recent ETL Jobs</h3>
          <a routerLink="/workspace/jobs" class="btn btn--ghost btn--sm">View all</a>
        </div>
        <div class="card__body p-0">
          <ng-container *ngIf="!loading(); else rowSkeleton2">
            <table class="table" *ngIf="recentEtlJobs().length; else emptyEtl">
              <thead>
                <tr>
                  <th>Source Ref</th>
                  <th>Status</th>
                  <th>Submitted</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let j of recentEtlJobs()">
                  <td class="text-sm">{{ j.sourceRef }}</td>
                  <td><span class="badge" [ngClass]="etlBadge(j.status)">{{ j.status }}</span></td>
                  <td class="text-secondary text-sm">{{ j.submittedAt | date:'shortTime' }}</td>
                </tr>
              </tbody>
            </table>
            <ng-template #emptyEtl>
              <div class="empty-state">
                <span class="material-icons-outlined empty-state__icon">storage</span>
                <div class="empty-state__title">No ETL jobs yet</div>
                <p class="empty-state__description">Submit an ETL job to see activity here.</p>
              </div>
            </ng-template>
          </ng-container>
          <ng-template #rowSkeleton2>
            <div class="skeleton" style="height:160px;"></div>
          </ng-template>
        </div>
      </div>
    </div>
  `,
})
export class WorkspaceDashboardComponent implements OnInit {
  private readonly workflowService = inject(WorkflowService);
  private readonly etlJobService = inject(EtlJobService);
  private readonly mappingService = inject(MappingService);
  private readonly cdr = inject(ChangeDetectorRef);

  loading = signal(true);
  recentWorkflows = signal<WorkflowResponse[]>([]);
  recentEtlJobs = signal<EtlJobResponse[]>([]);
  stats = signal({ runningWorkflows: 0, completedWorkflows: 0, failedWorkflows: 0, activeMappings: 0 });

  ngOnInit(): void {
    forkJoin({
      workflows: this.workflowService.list(0, 5),
      etlJobs: this.etlJobService.list(0, 5),
      mappings: this.mappingService.list(0, 1),
    }).subscribe({
      next: ({ workflows, etlJobs, mappings }) => {
        const wfItems = workflows.data?.content ?? [];
        this.recentWorkflows.set(wfItems);
        this.recentEtlJobs.set(etlJobs.data?.content ?? []);
        this.stats.set({
          runningWorkflows: wfItems.filter(w => w.status === 'RUNNING').length,
          completedWorkflows: wfItems.filter(w => w.status === 'COMPLETED').length,
          failedWorkflows: wfItems.filter(w => w.status === 'FAILED').length,
          activeMappings: mappings.data?.totalElements ?? 0,
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

  workflowBadge(status: string): string {
    const map: Record<string, string> = {
      RUNNING: 'badge--info',
      COMPLETED: 'badge--success',
      FAILED: 'badge--danger',
      PENDING: 'badge--neutral',
      COMPENSATING: 'badge--warning',
      COMPENSATED: 'badge--neutral',
    };
    return map[status] ?? 'badge--neutral';
  }

  etlBadge(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'badge--neutral',
      RUNNING: 'badge--info',
      COMPLETED: 'badge--success',
      FAILED: 'badge--danger',
      SKIPPED: 'badge--warning',
    };
    return map[status] ?? 'badge--neutral';
  }
}
