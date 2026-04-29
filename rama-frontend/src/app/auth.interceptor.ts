import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const email = authService.getUserEmail();
  const cloned = req.clone({
    setHeaders: { 'X-User-Email': email },
  });
  return next(cloned);
};
