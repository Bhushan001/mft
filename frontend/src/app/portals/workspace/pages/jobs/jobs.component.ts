import {
  Component, ChangeDetectionStrategy, inject, signal, OnInit
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { WorkflowService } from '../../../../core/services/workflow.service';
import { EtlJobService } from '../../../../core/services/etl-job.service';
import { ToastService } from '../../../../core/services/toast.service';
import { WorkflowResponse } from '../../../../core/models/workflow.models';
import { EtlJobResponse } from '../../../../core/models/etl.models';
import { PageResponse } from '../../../../core/models/api.models';

type Tab = 'workflows' | 'etl';

@Component({
  selector: 'chrono-jobs',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Jobs</h2>
        <p class="text-secondary text-sm">View and manage processing workflows and ETL jobs.</p>
      </div>
      <div class="section-header__actions">
        <button class="btn btn--primary" (click)="openNewJob()">
          <span class="material-icons-outlined">add</span>
          New Job
        </button>
      </div>
    </div>

    <!-- Tab bar -->
    <div class="tab-bar" style="margin-bottom:var(--space-xl)">
      <button [class]="'tab-bar__tab' + (activeTab() === 'workflows' ? ' tab-bar__tab--active' : '')" (click)="activeTab.set('workflows')">
        Workflows
      </button>
      <button [class]="'tab-bar__tab' + (activeTab() === 'etl' ? ' tab-bar__tab--active' : '')" (click)="activeTab.set('etl')">
        ETL Jobs
      </button>
    </div>

    <!-- New Workflow modal -->
    @if (showWorkflow()) {
      <div class="modal-overlay" (click)="showWorkflow.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal__header">
            <h3 class="modal__title">Start Workflow</h3>
            <button class="modal__close" (click)="showWorkflow.set(false)">
              <span class="material-icons-outlined">close</span>
            </button>
          </div>
          <form [formGroup]="workflowForm" (ngSubmit)="submitWorkflow()" class="modal__body">
            <div class="form-group">
              <label class="form-label">Workflow Type *</label>
              <select class="form-control" formControlName="workflowType">
                <option value="LOAN_PROCESSING">Loan Processing</option>
                <option value="ETL_PIPELINE">ETL Pipeline</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Correlation ID</label>
              <input class="form-control" formControlName="correlationId" placeholder="Optional external reference" />
            </div>
            <div class="form-group">
              <label class="form-label">Input Payload (JSON) *</label>
              <textarea class="form-control form-control--mono" rows="4"
                formControlName="inputPayload" placeholder='{ "loanId": "L-001" }'></textarea>
            </div>
            <div class="modal__footer">
              <button type="button" class="btn btn--secondary" (click)="showWorkflow.set(false)">Cancel</button>
              <button type="submit" class="btn btn--primary" [disabled]="saving()">
                @if (saving()) { <span class="spinner spinner--sm spinner--white"></span> }
                Start
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- New ETL Job modal -->
    @if (showEtl()) {
      <div class="modal-overlay" (click)="showEtl.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal__header">
            <h3 class="modal__title">Submit ETL Job</h3>
            <button class="modal__close" (click)="showEtl.set(false)">
              <span class="material-icons-outlined">close</span>
            </button>
          </div>
          <form [formGroup]="etlForm" (ngSubmit)="submitEtl()" class="modal__body">
            <div class="form-group">
              <label class="form-label">Source Reference *</label>
              <input class="form-control" formControlName="sourceRef" placeholder="batch-2024-001" />
            </div>
            <div class="form-group">
              <label class="form-label">Batch Date *</label>
              <input class="form-control" type="date" formControlName="batchDate" />
            </div>
            <div class="modal__footer">
              <button type="button" class="btn btn--secondary" (click)="showEtl.set(false)">Cancel</button>
              <button type="submit" class="btn btn--primary" [disabled]="saving()">
                @if (saving()) { <span class="spinner spinner--sm spinner--white"></span> }
                Submit
              </button>
            </div>
          </form>
        </div>
      </div>
    }

    <!-- Workflows tab -->
    @if (activeTab() === 'workflows') {
      <div class="card">
        @if (loadingWf()) {
          <div class="card__body">
            <div class="skeleton-list">
              @for (_ of skeletonRows; track $index) { <div class="skeleton skeleton--row"></div> }
            </div>
          </div>
        } @else if (workflows()?.content?.length === 0) {
          <div class="card__body">
            <div class="empty-state">
              <span class="material-icons-outlined empty-state__icon">play_circle_outline</span>
              <div class="empty-state__title">No workflows yet</div>
              <p class="empty-state__description">Start a workflow to process loan applications.</p>
            </div>
          </div>
        } @else {
          <div class="table-wrapper">
            <table class="table">
              <thead>
                <tr><th>Workflow ID</th><th>Type</th><th>Status</th><th>Correlation</th><th>Started</th></tr>
              </thead>
              <tbody>
                @for (wf of workflows()?.content; track wf.workflowId) {
                  <tr>
                    <td class="text-sm font-mono">{{ wf.workflowId.slice(0, 8) }}...</td>
                    <td class="text-sm">{{ wf.workflowType }}</td>
                    <td><span [class]="'badge badge--' + statusBadge(wf.status)">{{ wf.status }}</span></td>
                    <td class="text-sm text-secondary">{{ wf.correlationId ?? '—' }}</td>
                    <td class="text-secondary text-sm">{{ wf.startedAt.slice(0, 16).replace('T', ' ') }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    }

    <!-- ETL tab -->
    @if (activeTab() === 'etl') {
      <div class="card">
        @if (loadingEtl()) {
          <div class="card__body">
            <div class="skeleton-list">
              @for (_ of skeletonRows; track $index) { <div class="skeleton skeleton--row"></div> }
            </div>
          </div>
        } @else if (etlJobs()?.content?.length === 0) {
          <div class="card__body">
            <div class="empty-state">
              <span class="material-icons-outlined empty-state__icon">sync</span>
              <div class="empty-state__title">No ETL jobs submitted</div>
              <p class="empty-state__description">Submit a batch ETL job to load data into Strategy One.</p>
            </div>
          </div>
        } @else {
          <div class="table-wrapper">
            <table class="table">
              <thead>
                <tr><th>Job ID</th><th>Source Ref</th><th>Batch Date</th><th>Status</th><th>Submitted</th></tr>
              </thead>
              <tbody>
                @for (job of etlJobs()?.content; track job.jobId) {
                  <tr>
                    <td class="text-sm font-mono">{{ job.jobId.slice(0, 8) }}...</td>
                    <td class="text-sm">{{ job.sourceRef }}</td>
                    <td class="text-sm text-secondary">{{ job.batchDate }}</td>
                    <td><span [class]="'badge badge--' + etlStatusBadge(job.status)">{{ job.status }}</span></td>
                    <td class="text-secondary text-sm">{{ job.submittedAt.slice(0, 16).replace('T', ' ') }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    }
  `,
})
export class JobsComponent implements OnInit {
  private readonly workflowService = inject(WorkflowService);
  private readonly etlJobService   = inject(EtlJobService);
  private readonly toastService    = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab   = signal<Tab>('workflows');
  readonly loadingWf   = signal(true);
  readonly loadingEtl  = signal(true);
  readonly saving      = signal(false);
  readonly showWorkflow = signal(false);
  readonly showEtl     = signal(false);
  readonly workflows   = signal<PageResponse<WorkflowResponse> | null>(null);
  readonly etlJobs     = signal<PageResponse<EtlJobResponse> | null>(null);
  readonly skeletonRows = [1, 2, 3, 4];

  readonly workflowForm = this.fb.group({
    workflowType:  ['LOAN_PROCESSING', Validators.required],
    correlationId: [''],
    inputPayload:  ['{}', Validators.required],
  });

  readonly etlForm = this.fb.group({
    sourceRef:  ['', Validators.required],
    batchDate:  [new Date().toISOString().slice(0, 10), Validators.required],
  });

  ngOnInit(): void {
    this.loadWorkflows();
    this.loadEtlJobs();
  }

  openNewJob(): void {
    if (this.activeTab() === 'etl') {
      this.showEtl.set(true);
    } else {
      this.showWorkflow.set(true);
    }
  }

  loadWorkflows(): void {
    this.loadingWf.set(true);
    this.workflowService.list().subscribe({
      next: res => { this.workflows.set(res.data); this.loadingWf.set(false); },
      error: ()  => { this.loadingWf.set(false); this.toastService.error('Error', 'Failed to load workflows'); },
    });
  }

  loadEtlJobs(): void {
    this.loadingEtl.set(true);
    this.etlJobService.list().subscribe({
      next: res => { this.etlJobs.set(res.data); this.loadingEtl.set(false); },
      error: ()  => { this.loadingEtl.set(false); this.toastService.error('Error', 'Failed to load ETL jobs'); },
    });
  }

  submitWorkflow(): void {
    this.workflowForm.markAllAsTouched();
    if (this.workflowForm.invalid) return;
    this.saving.set(true);
    const v = this.workflowForm.getRawValue();
    this.workflowService.start({
      workflowType: v.workflowType as any,
      correlationId: v.correlationId || undefined,
      inputPayload: v.inputPayload!,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showWorkflow.set(false);
        this.workflowForm.reset({ workflowType: 'LOAN_PROCESSING', inputPayload: '{}' });
        this.toastService.success('Workflow started', 'Processing has been queued.');
        this.loadWorkflows();
      },
      error: err => {
        this.saving.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to start workflow');
      },
    });
  }

  submitEtl(): void {
    this.etlForm.markAllAsTouched();
    if (this.etlForm.invalid) return;
    this.saving.set(true);
    const v = this.etlForm.getRawValue();
    this.etlJobService.submit({ sourceRef: v.sourceRef!, batchDate: v.batchDate! }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showEtl.set(false);
        this.etlForm.reset({ batchDate: new Date().toISOString().slice(0, 10) });
        this.toastService.success('ETL job submitted', 'Batch processing has been queued.');
        this.loadEtlJobs();
      },
      error: err => {
        this.saving.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to submit ETL job');
      },
    });
  }

  statusBadge(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'neutral', RUNNING: 'info', COMPLETED: 'success',
      FAILED: 'danger', COMPENSATING: 'warning', COMPENSATED: 'neutral',
    };
    return map[status] ?? 'neutral';
  }

  etlStatusBadge(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'neutral', RUNNING: 'info', COMPLETED: 'success',
      FAILED: 'danger', SKIPPED: 'warning',
    };
    return map[status] ?? 'neutral';
  }
}
