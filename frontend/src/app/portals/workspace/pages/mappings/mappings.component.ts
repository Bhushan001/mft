import {
  Component, ChangeDetectionStrategy, inject, signal, OnInit
} from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MappingService } from '../../../../core/services/mapping.service';
import { ToastService } from '../../../../core/services/toast.service';
import { MappingRuleResponse } from '../../../../core/models/mapping.models';
import { PageResponse } from '../../../../core/models/api.models';

@Component({
  selector: 'chrono-mappings',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="section-header">
      <div class="section-header__title">
        <h2>Mapping Rules</h2>
        <p class="text-secondary text-sm">Define LOS-to-Strategy-One transformation rules.</p>
      </div>
      <div class="section-header__actions">
        <button class="btn btn--primary" (click)="showCreate.set(true)">
          <span class="material-icons-outlined">add</span>
          New Rule
        </button>
      </div>
    </div>

    @if (showCreate()) {
      <div class="modal-overlay" (click)="showCreate.set(false)">
        <div class="modal modal--lg" (click)="$event.stopPropagation()">
          <div class="modal__header">
            <h3 class="modal__title">New Mapping Rule</h3>
            <button class="modal__close" (click)="showCreate.set(false)">
              <span class="material-icons-outlined">close</span>
            </button>
          </div>
          <form [formGroup]="createForm" (ngSubmit)="submitCreate()" class="modal__body">
            <div class="form-group">
              <label class="form-label">Name *</label>
              <input class="form-control" formControlName="name" placeholder="LOS v2 → Strategy One" />
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <input class="form-control" formControlName="description" placeholder="Optional description" />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Source System</label>
                <input class="form-control" formControlName="sourceSystem" placeholder="LOS" />
              </div>
              <div class="form-group">
                <label class="form-label">Target System</label>
                <input class="form-control" formControlName="targetSystem" placeholder="StrategyOne" />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">Rule Definition (JSON) *</label>
              <textarea class="form-control form-control--mono" rows="6"
                formControlName="ruleDefinition"
                placeholder='{ "fields": [ { "from": "loanAmount", "to": "loan_amount" } ] }'></textarea>
              <span class="form-hint">JSON mapping specification</span>
            </div>
            <div class="modal__footer">
              <button type="button" class="btn btn--secondary" (click)="showCreate.set(false)">Cancel</button>
              <button type="submit" class="btn btn--primary" [disabled]="saving()">
                @if (saving()) { <span class="spinner spinner--sm spinner--white"></span> }
                Create Rule
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
            <span class="material-icons-outlined empty-state__icon">account_tree</span>
            <div class="empty-state__title">No mapping rules defined</div>
            <p class="empty-state__description">Create your first mapping rule to start processing loans.</p>
          </div>
        </div>
      } @else {
        <div class="table-wrapper">
          <table class="table">
            <thead>
              <tr>
                <th>Name</th><th>Source</th><th>Target</th><th>Version</th><th>Status</th><th>Updated</th><th></th>
              </tr>
            </thead>
            <tbody>
              @for (rule of page()?.content; track rule.ruleId) {
                <tr>
                  <td class="font-medium">{{ rule.name }}</td>
                  <td class="text-sm text-secondary">{{ rule.sourceSystem ?? '—' }}</td>
                  <td class="text-sm text-secondary">{{ rule.targetSystem ?? '—' }}</td>
                  <td class="text-sm">v{{ rule.version }}</td>
                  <td>
                    @if (rule.active) {
                      <span class="badge badge--success">Active</span>
                    } @else {
                      <span class="badge badge--neutral">Draft</span>
                    }
                  </td>
                  <td class="text-secondary text-sm">{{ rule.updatedAt.slice(0, 10) }}</td>
                  <td>
                    @if (!rule.active) {
                      <button class="btn btn--ghost btn--sm" [disabled]="publishing()" (click)="publish(rule.ruleId)">
                        Publish
                      </button>
                    }
                  </td>
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
export class MappingsComponent implements OnInit {
  private readonly mappingService = inject(MappingService);
  private readonly toastService   = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading     = signal(true);
  readonly saving      = signal(false);
  readonly publishing  = signal(false);
  readonly showCreate  = signal(false);
  readonly page        = signal<PageResponse<MappingRuleResponse> | null>(null);
  readonly currentPage = signal(0);
  readonly skeletonRows = [1, 2, 3];

  readonly createForm = this.fb.group({
    name:           ['', Validators.required],
    description:    [''],
    ruleDefinition: ['{}', Validators.required],
    sourceSystem:   [''],
    targetSystem:   [''],
  });

  ngOnInit(): void { this.loadPage(0); }

  loadPage(page: number): void {
    this.loading.set(true);
    this.currentPage.set(page);
    this.mappingService.list(page).subscribe({
      next: res => { this.page.set(res.data); this.loading.set(false); },
      error: ()  => { this.loading.set(false); this.toastService.error('Error', 'Failed to load mappings'); },
    });
  }

  submitCreate(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;
    this.saving.set(true);
    const v = this.createForm.getRawValue();
    this.mappingService.create({
      name: v.name!, description: v.description || undefined,
      ruleDefinition: v.ruleDefinition!,
      sourceSystem: v.sourceSystem || undefined,
      targetSystem: v.targetSystem || undefined,
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.showCreate.set(false);
        this.createForm.reset({ ruleDefinition: '{}' });
        this.toastService.success('Rule created', v.name + ' has been saved as a draft.');
        this.loadPage(0);
      },
      error: err => {
        this.saving.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to create rule');
      },
    });
  }

  publish(ruleId: string): void {
    this.publishing.set(true);
    this.mappingService.publish(ruleId).subscribe({
      next: () => {
        this.publishing.set(false);
        this.toastService.success('Rule published', 'Mapping rule is now active.');
        this.loadPage(this.currentPage());
      },
      error: err => {
        this.publishing.set(false);
        this.toastService.error('Error', err.error?.message ?? 'Failed to publish rule');
      },
    });
  }
}
