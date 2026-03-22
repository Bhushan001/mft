import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';
import { ErrorResponse } from '../models/api.models';

export const errorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 0) {
        toastService.error('Network Error', 'Unable to reach the server. Please check your connection.');
      } else if (err.status >= 500) {
        toastService.error('Server Error', 'An unexpected error occurred. Please try again later.');
      } else if (err.status === 403) {
        toastService.error('Access Denied', 'You do not have permission to perform this action.');
      } else if (err.status === 404) {
        // 404s are usually handled by the calling component — don't toast globally
      } else if (err.status === 409) {
        const body = err.error as ErrorResponse;
        toastService.warning('Conflict', body?.message ?? 'A conflict occurred.');
      } else if (err.status === 422) {
        const body = err.error as ErrorResponse;
        toastService.warning('Validation Error', body?.message ?? 'Please check your input.');
      }

      return throwError(() => err);
    }),
  );
};
