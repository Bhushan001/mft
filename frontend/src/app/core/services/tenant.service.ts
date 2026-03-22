import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';
import { TenantResponse, CreateTenantRequest, UpdateTenantRequest } from '../models/tenant.models';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/tenants`;

  list(page = 0, size = 20, search?: string): Observable<ApiResponse<PageResponse<TenantResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PageResponse<TenantResponse>>>(this.base, { params });
  }

  getById(tenantId: string): Observable<ApiResponse<TenantResponse>> {
    return this.http.get<ApiResponse<TenantResponse>>(`${this.base}/${tenantId}`);
  }

  create(request: CreateTenantRequest): Observable<ApiResponse<TenantResponse>> {
    return this.http.post<ApiResponse<TenantResponse>>(this.base, request);
  }

  update(tenantId: string, request: UpdateTenantRequest): Observable<ApiResponse<TenantResponse>> {
    return this.http.put<ApiResponse<TenantResponse>>(`${this.base}/${tenantId}`, request);
  }

  delete(tenantId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${tenantId}`);
  }
}
