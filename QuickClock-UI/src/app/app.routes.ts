import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { MainLayoutComponent } from './layout/main-layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { LogoutComponent } from './features/auth/logout/logout.component';
import { rolesGuard } from './core/guards/roles.guard';
import { KioskPageComponent } from './features/kiosk/kiosk-page.component';
import { KioskLayoutComponent } from './layout/kiosk-layout/kiosk-layout.component';

export const routes: Routes = [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

    { path: 'login', component: LoginComponent },
    { path: 'logout', component: LogoutComponent },
    {
        path: 'dashboard',
        component: MainLayoutComponent,
        canActivate: [authGuard],
    },
    {
        path: 'kiosk',
        component: KioskLayoutComponent,
        canActivate: [authGuard, rolesGuard],
        data: { roles: ['KIOSK', 'ADMIN', 'SUPER_ADMIN'] },
        children: [{ path: '', component: KioskPageComponent }],
    },
];
