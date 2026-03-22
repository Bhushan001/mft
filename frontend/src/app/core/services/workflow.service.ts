import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';
import { WorkflowResponse, StartWorkflowRequest, WorkflowStatus } from '../models/workflow.models';

@Injectable({ providedIn: 'root' })
export class WorkflowService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/workflows`;

  list(page = 0, size = 20, status?: WorkflowStatus): Observable<ApiResponse<PageResponse<WorkflowResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<WorkflowResponse>>>(this.base, { params });
  }

  getById(workflowId: string): Observable<ApiResponse<WorkflowResponse>> {
    return this.http.get<ApiResponse<WorkflowResponse>>(`${this.base}/${workflowId}`);
  }

  start(request: StartWorkflowRequest): Observable<ApiResponse<WorkflowResponse>> {
    return this.http.post<ApiResponse<WorkflowResponse>>(this.base, request);
  }
}
