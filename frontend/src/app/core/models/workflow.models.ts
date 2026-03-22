export type WorkflowStatus = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'COMPENSATING' | 'COMPENSATED';
export type StepStatus   = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'COMPENSATING' | 'COMPENSATED' | 'SKIPPED';
export type WorkflowType = 'LOAN_PROCESSING' | 'ETL_PIPELINE';

export interface WorkflowStepResponse {
  stepOrder: number;
  stepName: string;
  status: StepStatus;
  outputPayload?: string;
  errorMessage?: string;
  executedAt?: string;
}

export interface WorkflowResponse {
  workflowId: string;
  tenantId: string;
  workflowType: WorkflowType;
  status: WorkflowStatus;
  correlationId?: string;
  outputPayload?: string;
  errorMessage?: string;
  startedAt: string;
  completedAt?: string;
  steps: WorkflowStepResponse[];
}

export interface StartWorkflowRequest {
  workflowType: WorkflowType;
  correlationId?: string;
  inputPayload: string;
}
