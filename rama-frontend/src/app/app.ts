import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, finalize } from 'rxjs';

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
import { MatSelectModule } from '@angular/material/select';

import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { AuthService, UserRole } from './auth.service';

interface Order {
  id?: string;
  userEmail?: string;
  name: string;
  address: string;
  phone: string;
  quantity: number;
  pickupLocation: string;
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
    MatTooltipModule, MatChipsModule, MatSelectModule,
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

  pickupLocations = ['Zurich', 'Bern', 'Baden', 'Lausanne'];

  orderForm: FormGroup = this.fb.group({
    name:           ['', [Validators.required, Validators.minLength(2)]],
    address:        [''],
    phone:          ['', [Validators.required, Validators.pattern(/^\+?[0-9\s\-]{7,15}$/)]],
    quantity:       [1,  [Validators.required, Validators.min(1), Validators.max(999)]],
    pickupLocation: ['', [Validators.required]],
  });

  orders = signal<Order[] | undefined>(undefined);
  loading = signal(false);
  submitting = signal(false);
  private authSub?: Subscription;
  private apiUrl = '/api';

  get isAdmin(): boolean { return this.user()?.role === 'ADMIN'; }

  get displayedColumns(): string[] {
    const cols = this.isAdmin
      ? ['userEmail', 'name', 'address', 'phone', 'quantity', 'pickupLocation', 'createdAt']
      : ['name', 'address', 'phone', 'quantity', 'pickupLocation', 'createdAt'];
    return [...cols, 'actions'];
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
    this.orders.set(undefined);
    this.orderForm.reset({ quantity: 1 });
  }

  loadOrders() {
    // Only show the loading spinner if we haven't fetched orders yet
    if (this.orders() === undefined) {
      this.loading.set(true);
    }

    this.http.get<Order[]>(`${this.apiUrl}/orders`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => {
          const reversed = (data || []).reverse();
          this.orders.set(reversed);

          // Pre-fill form from the most recent order if the form is currently empty
          if (reversed.length > 0) {
            const lastOrder = reversed[0];
            const currentVal = this.orderForm.value;
            this.orderForm.patchValue({
              phone:          currentVal.phone          || lastOrder.phone,
              pickupLocation: currentVal.pickupLocation || lastOrder.pickupLocation,
              address:        currentVal.address        || lastOrder.address,
            });
          }
        },
        error: () => {
          this.orders.set([]); // Set to empty array to resolve loading state on error
          this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 });
        },
      });
  }

  submitOrder() {
    if (this.orderForm.invalid) return;
    this.submitting.set(true);
    this.http.post<Order>(`${this.apiUrl}/orders`, this.orderForm.value)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.snackBar.open('Order placed successfully!', 'OK', { duration: 3000 });
          // Keep phone and pickupLocation for the next order, but reset address and quantity
          this.orderForm.patchValue({ address: '', quantity: 1 });
          this.loadOrders();
        },
        error: () => {
          this.snackBar.open('Failed to place order', 'Close', { duration: 3000 });
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

  deleteOrder(id: string) {
    if (!confirm('Are you sure you want to delete this order?')) return;

    this.http.delete(`${this.apiUrl}/orders/${id}`).subscribe({
      next: () => {
        this.snackBar.open('Order deleted successfully', 'OK', { duration: 3000 });
        this.loadOrders();
      },
      error: () => this.snackBar.open('Failed to delete order', 'Close', { duration: 3000 }),
    });
  }
}
