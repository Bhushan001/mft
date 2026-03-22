import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';
import { MappingRuleResponse, CreateMappingRuleRequest, UpdateMappingRuleRequest } from '../models/mapping.models';

@Injectable({ providedIn: 'root' })
export class MappingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/mappings`;

  list(page = 0, size = 20, name?: string): Observable<ApiResponse<PageResponse<MappingRuleResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (name) params = params.set('name', name);
    return this.http.get<ApiResponse<PageResponse<MappingRuleResponse>>>(this.base, { params });
  }

  getActive(): Observable<ApiResponse<MappingRuleResponse>> {
    return this.http.get<ApiResponse<MappingRuleResponse>>(`${this.base}/active`);
  }

  getById(ruleId: string): Observable<ApiResponse<MappingRuleResponse>> {
    return this.http.get<ApiResponse<MappingRuleResponse>>(`${this.base}/${ruleId}`);
  }

  create(request: CreateMappingRuleRequest): Observable<ApiResponse<MappingRuleResponse>> {
    return this.http.post<ApiResponse<MappingRuleResponse>>(this.base, request);
  }

  update(ruleId: string, request: UpdateMappingRuleRequest): Observable<ApiResponse<MappingRuleResponse>> {
    return this.http.put<ApiResponse<MappingRuleResponse>>(`${this.base}/${ruleId}`, request);
  }

  publish(ruleId: string): Observable<ApiResponse<MappingRuleResponse>> {
    return this.http.post<ApiResponse<MappingRuleResponse>>(`${this.base}/${ruleId}/publish`, {});
  }

  delete(ruleId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${ruleId}`);
  }
}
