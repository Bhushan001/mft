export type EtlJobStatus = 'SUBMITTED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'SKIPPED';

export interface EtlJobResponse {
  jobId: string;
  tenantId: string;
  sourceRef: string;
  batchDate: string;
  status: EtlJobStatus;
  errorMessage?: string;
  submittedAt: string;
  completedAt?: string;
}

export interface SubmitEtlJobRequest {
  sourceRef: string;
  batchDate: string;
  inputPayload?: string;
}
