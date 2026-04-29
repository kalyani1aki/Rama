import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';

import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { AuthService, UserRole } from './auth.service';

interface Order {
  id?: string;
  userEmail?: string;
  name: string;
  address: string;
  phone: string;
  quantity: number;
  createdAt?: string;
}

interface BackendUser {
  email: string;
  name: string;
  role: UserRole;
}

@Component({
  selector: 'app-root',
  imports: [
    CommonModule, ReactiveFormsModule,
    MatButtonModule, MatInputModule, MatFormFieldModule,
    MatCardModule, MatIconModule, MatTableModule,
    MatSnackBarModule, MatDividerModule, MatProgressSpinnerModule,
    MatTooltipModule, MatChipsModule,
    GoogleSigninButtonModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private socialAuthService = inject(SocialAuthService);
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  user = this.authService.user;

  orderForm: FormGroup = this.fb.group({
    name:     ['', [Validators.required, Validators.minLength(2)]],
    address:  ['', [Validators.required]],
    phone:    ['', [Validators.required, Validators.pattern(/^\+?[0-9\s\-]{7,15}$/)]],
    quantity: [1,  [Validators.required, Validators.min(1), Validators.max(999)]],
  });

  orders: Order[] = [];
  loading = false;
  submitting = false;
  private authSub?: Subscription;
  private apiUrl = '/api';

  get isAdmin(): boolean { return this.user()?.role === 'ADMIN'; }

  get displayedColumns(): string[] {
    return this.isAdmin
      ? ['userEmail', 'name', 'address', 'phone', 'quantity', 'createdAt']
      : ['name', 'address', 'phone', 'quantity', 'createdAt'];
  }

  ngOnInit() {
    this.authSub = this.socialAuthService.authState.subscribe((googleUser) => {
      if (googleUser) {
        const email = googleUser.email ?? '';
        const name = googleUser.name ?? 'Google User';
        this.http.post<BackendUser>(`${this.apiUrl}/users/login`, { email, name })
          .subscribe({
            next: (backendUser) => {
              this.authService.loginWithGoogle(
                { name, email, photoUrl: googleUser.photoUrl ?? '' },
                backendUser.role
              );
              this.orderForm.patchValue({ name });
              this.loadOrders();
            },
            error: () => {
              this.authService.loginWithGoogle(
                { name, email, photoUrl: googleUser.photoUrl ?? '' },
                'USER'
              );
              this.loadOrders();
            },
          });
      }
    });
  }

  ngOnDestroy() { this.authSub?.unsubscribe(); }

  loginAsGuest() {
    this.authService.loginAsGuest();
    this.loadOrders();
  }

  logout() {
    if (this.user()?.provider === 'GOOGLE') {
      this.socialAuthService.signOut().catch(() => {});
    }
    this.authService.logout();
    this.orders = [];
    this.orderForm.reset({ quantity: 1 });
  }

  loadOrders() {
    this.loading = true;
    this.http.get<Order[]>(`${this.apiUrl}/orders`).subscribe({
      next: (data) => { this.orders = data.reverse(); this.loading = false; },
      error: () => { this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 }); this.loading = false; },
    });
  }

  submitOrder() {
    if (this.orderForm.invalid) return;
    this.submitting = true;
    this.http.post<Order>(`${this.apiUrl}/orders`, this.orderForm.value).subscribe({
      next: () => {
        this.snackBar.open('Order placed successfully!', 'OK', { duration: 3000 });
        this.orderForm.patchValue({ address: '', phone: '', quantity: 1 });
        this.loadOrders();
        this.submitting = false;
      },
      error: () => {
        this.snackBar.open('Failed to place order', 'Close', { duration: 3000 });
        this.submitting = false;
      },
    });
  }

  downloadExcel() {
    this.http.get(`${this.apiUrl}/orders/export`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'orders.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snackBar.open('Failed to download Excel', 'Close', { duration: 3000 }),
    });
  }
}
