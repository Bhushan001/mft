export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

export interface TenantResponse {
  tenantId: string;
  name: string;
  slug: string;
  status: TenantStatus;
  plan: string;
  contactEmail: string;
  maxUsers: number;
  timezone: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  name: string;
  slug: string;
  contactEmail: string;
  plan?: string;
  maxUsers?: number;
  timezone?: string;
}

export interface UpdateTenantRequest {
  name?: string;
  contactEmail?: string;
  plan?: string;
  maxUsers?: number;
  timezone?: string;
  status?: TenantStatus;
}
