import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/api.models';
import { EtlJobResponse, SubmitEtlJobRequest, EtlJobStatus } from '../models/etl.models';

@Injectable({ providedIn: 'root' })
export class EtlJobService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/etl/jobs`;

  list(page = 0, size = 20, status?: EtlJobStatus): Observable<ApiResponse<PageResponse<EtlJobResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<EtlJobResponse>>>(this.base, { params });
  }

  getById(jobId: string): Observable<ApiResponse<EtlJobResponse>> {
    return this.http.get<ApiResponse<EtlJobResponse>>(`${this.base}/${jobId}`);
  }

  submit(request: SubmitEtlJobRequest): Observable<ApiResponse<EtlJobResponse>> {
    return this.http.post<ApiResponse<EtlJobResponse>>(this.base, request);
  }
}
