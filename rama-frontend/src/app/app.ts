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
import { MatMenuModule } from '@angular/material/menu';

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
    MatMenuModule, GoogleSigninButtonModule,
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
    userEmail:      ['', [Validators.email]], // only for guest during order placement
    name:           ['', [Validators.required, Validators.minLength(2), Validators.pattern(/^[a-zA-Z\s]*$/)]],
    address:        [''],
    phone:          ['', [Validators.required, Validators.minLength(9), Validators.maxLength(15), Validators.pattern(/^\+?[0-9\s\-]*$/)]],
    quantity:       [1,  [Validators.required, Validators.min(1), Validators.max(999)]],
    pickupLocation: ['', [Validators.required]],
  });


  orders = signal<Order[] | undefined>(undefined);
  loading = signal(false);
  submitting = signal(false);
  editingOrder = signal<Order | null>(null);

  private authSub?: Subscription;
  private apiUrl = '/api';

  get isAdmin(): boolean { return this.user()?.role === 'ADMIN'; }
  get isGuest(): boolean { return this.user()?.provider === 'GUEST'; }

  get maxQuantity(): number {
    if (this.isAdmin) return 999;
    return this.user()?.provider === 'GOOGLE' ? 15 : 2;
  }

  get websiteUrl(): string {
    return window.location.origin;
  }

  get showQuantityWarning(): boolean {
    if (this.isAdmin) return false;
    return (this.orderForm.get('quantity')?.value || 0) > this.maxQuantity;
  }

  get displayedColumns(): string[] {
    const cols = this.isAdmin
      ? ['userEmail', 'name', 'address', 'phone', 'quantity', 'pickupLocation', 'createdAt']
      : ['name', 'address', 'phone', 'quantity', 'pickupLocation', 'createdAt'];
    return [...cols, 'actions'];
  }

  ngOnInit() {
    // If a session was restored from localStorage, load orders immediately
    if (this.user()) {
      this.updateValidators();
      this.loadOrders();
      if (this.user()?.provider === 'GOOGLE') {
        this.orderForm.patchValue({ name: this.user()?.name });
      }
    }

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
              this.updateValidators();
              this.loadOrders();
            },
            error: () => {
              this.authService.loginWithGoogle(
                { name, email, photoUrl: googleUser.photoUrl ?? '' },
                'USER'
              );
              this.updateValidators();
              this.loadOrders();
            },
          });
      }
    });
  }

  ngOnDestroy() { this.authSub?.unsubscribe(); }

  private updateValidators() {
    const qtyControl = this.orderForm.get('quantity');
    if (qtyControl) {
      qtyControl.setValidators([Validators.required, Validators.min(1), Validators.max(this.maxQuantity)]);
      qtyControl.updateValueAndValidity();
    }

    const emailControl = this.orderForm.get('userEmail');
    if (emailControl) {
      if (this.isGuest) {
        emailControl.setValidators([Validators.required, Validators.email]);
      } else {
        emailControl.clearValidators();
      }
      emailControl.updateValueAndValidity();
    }
  }

  loginAsGuest() {
    this.authService.loginAsGuest();
    this.updateValidators();
    this.loadOrders();
  }

  logout() {
    if (this.user()?.provider === 'GOOGLE') {
      this.socialAuthService.signOut().catch(() => {});
    }
    this.authService.logout();
    this.orders.set(undefined);
    this.editingOrder.set(null);
    this.orderForm.reset({ quantity: 1 });
    this.updateValidators();
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
          if (reversed.length > 0 && this.user()?.provider === 'GOOGLE') {
            const lastOrder = reversed[0];
            const currentVal = this.orderForm.value;
            this.orderForm.patchValue({
              name:           currentVal.name           || lastOrder.name,
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

    const orderData = { ...this.orderForm.value };

    const isEdit = !!this.editingOrder();
    const request = isEdit
      ? this.http.put<Order>(`${this.apiUrl}/orders/${this.editingOrder()!.id}`, orderData)
      : this.http.post<Order>(`${this.apiUrl}/orders`, orderData);

    request.pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (newOrder) => {
          this.snackBar.open(isEdit ? 'Order updated successfully!' : 'Order placed successfully!', 'OK', { duration: 3000 });
          this.editingOrder.set(null);

          if (this.isGuest) {
            // For guests, we manually update the orders signal to show the just-placed order
            // since the backend getOrders returns empty for guests.
            this.orders.set([newOrder]);
          } else {
            this.loadOrders();
          }
        },
        error: (err) => {
          const msg = typeof err.error === 'string' ? err.error : 'Failed to process order';
          this.snackBar.open(msg, 'Close', { duration: 7000 });
        },
      });
  }

  editOrder(order: Order) {
    this.editingOrder.set(order);
    this.orderForm.patchValue({
      name: order.name,
      address: order.address,
      phone: order.phone,
      quantity: order.quantity,
      pickupLocation: order.pickupLocation,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() {
    this.editingOrder.set(null);
    this.orderForm.reset({ quantity: 1 });
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
