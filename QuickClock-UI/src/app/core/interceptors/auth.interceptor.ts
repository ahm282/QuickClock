import {
    HttpInterceptorFn,
    HttpErrorResponse,
    HttpRequest,
    HttpHandlerFn,
    HttpEvent,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import {
    catchError,
    switchMap,
    throwError,
    BehaviorSubject,
    filter,
    take,
    Observable,
} from 'rxjs';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (
    req: HttpRequest<unknown>,
    next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
    const authService = inject(AuthService);
    const token = authService.accessToken();

    let authReq = req;
    if (token) {
        authReq = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`,
            },
        });
    }

    return next(authReq).pipe(
        catchError((error: unknown) => {
            if (error instanceof HttpErrorResponse && error.status === 401) {
                return handle401Error(authReq, next, authService);
            }
            return throwError(() => error);
        })
    );
};

const handle401Error = (
    request: HttpRequest<unknown>,
    next: HttpHandlerFn,
    authService: AuthService
) => {
    if (!isRefreshing) {
        isRefreshing = true;
        refreshTokenSubject.next(null);

        return authService.refresh().pipe(
            switchMap((response) => {
                isRefreshing = false;
                refreshTokenSubject.next(response.accessToken);
                return next(addToken(request, response.accessToken));
            }),
            catchError((err) => {
                isRefreshing = false;
                return throwError(() => err);
            })
        );
    } else {
        return refreshTokenSubject.pipe(
            filter((token) => token != null),
            take(1),
            switchMap((token) => next(addToken(request, token!)))
        );
    }
};

function addToken<T>(request: HttpRequest<T>, token: string): HttpRequest<T> {
    return request.clone({
        setHeaders: { Authorization: `Bearer ${token}` },
    });
}
