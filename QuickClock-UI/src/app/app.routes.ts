import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { loginGuard } from './core/guards/login.guard';
import { LogoutComponent } from './features/auth/logout/logout.component';
import { rolesGuard } from './core/guards/roles.guard';
import { KioskPageComponent } from './features/kiosk/kiosk-page.component';
import { KioskLayoutComponent } from './layout/kiosk-layout/kiosk-layout.component';
import { UserHomeComponent } from './features/user/user-home/user-home.component';

export const routes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        canActivate: [authGuard],
        children: [],
    },

    { path: 'login', component: LoginComponent, canActivate: [loginGuard] },
    { path: 'logout', component: LogoutComponent },
    {
        path: '',
        component: MainLayoutComponent,
        canActivate: [authGuard],
        children: [
            { path: 'home', component: UserHomeComponent },
            { path: 'dashboard', redirectTo: 'home', pathMatch: 'full' },
        ],
    },
    {
        path: 'kiosk',
        component: KioskLayoutComponent,
        canActivate: [authGuard, rolesGuard],
        data: { roles: ['KIOSK', 'ADMIN', 'SUPER_ADMIN'] },
        children: [{ path: '', component: KioskPageComponent }],
    },
];
