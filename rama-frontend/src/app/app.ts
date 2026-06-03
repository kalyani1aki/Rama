import { Component, OnInit, OnDestroy, inject, signal, computed, ViewChild, TemplateRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription, finalize } from 'rxjs';

import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatTabsModule } from '@angular/material/tabs';
import { MatRadioModule } from '@angular/material/radio';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';

import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';
import { AuthService, UserRole } from './auth.service';

import { SelectionModel } from '@angular/cdk/collections';

interface Order {
  id?: string;
  userEmail?: string;
  name: string;
  address: string;
  phone: string;
  quantity: number;
  pickupLocation: string;
  createdAt?: string;
  status?: 'CONFIRMED' | 'WAITING';
  isPaid?: boolean;
  isPickedUp?: boolean;
}

interface OrderStats {
  totalOrders: number;
  totalBoxes: number;
  confirmedOrders: number;
  confirmedBoxes: number;
  waitingOrders: number;
  waitingBoxes: number;
  paidBoxes: number;
  pickedUpBoxes: number;
  totalRevenue: number;
  paidRevenue: number;
  ordersPerLocation: { [key: string]: number };
  boxesPerLocation: { [key: string]: number };
  confirmedBoxesPerLocation: { [key: string]: number };
  waitingBoxesPerLocation: { [key: string]: number };
  paidBoxesPerLocation: { [key: string]: number };
  pickedUpBoxesPerLocation: { [key: string]: number };
}

interface BackendUser {
  email: string;
  name: string;
  role: UserRole;
}

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatSnackBarModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
    MatSelectModule,
    MatMenuModule,
    GoogleSigninButtonModule,
    MatTabsModule,
    BaseChartDirective,
    MatRadioModule,
    MatDialogModule,
    MatCheckboxModule,
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
  private dialog = inject(MatDialog);

  @ViewChild('confirmSoldOutDialog') confirmSoldOutDialog!: TemplateRef<any>;

  user = this.authService.user;

  pickupLocations = [
    'Zurich (Wallisellerstrasse 1, 8302 Kloten) ',
    'Bern (Bollhölzliweg 17, 3067 Boll)',
    'Baden (Blumenweg 2C, 5300 Turgi)',
    'Aarau (Büelisackerstrasse 19a, 5622 Waltenschwil)',
    'Laussane (Chemin des Lentillières 3c, 1023 Crissier)'
  ];

  orderForm: FormGroup = this.fb.group({
    userEmail: ['', [Validators.email]], // only for guest during order placement
    name: ['', [Validators.required, Validators.minLength(2), Validators.pattern(/^[a-zA-Z\s]*$/)]],
    address: [''],
    phone: [
      '',
      [
        Validators.required,
        Validators.minLength(9),
        Validators.maxLength(15),
        Validators.pattern(/^\+?[0-9\s\-]*$/),
      ],
    ],
    quantity: [1, [Validators.required, Validators.min(1), Validators.max(999)]],
    pickupLocation: ['', [Validators.required]],
  });

  orders = signal<Order[] | undefined>(undefined);
  dataSource = new MatTableDataSource<Order>([]);
  filterForm: FormGroup = this.fb.group({
    userEmail: [''],
    name: [''],
    address: [''],
    phone: [''],
    quantity: [''],
    pickupLocation: [''],
  });

  loading = signal(false);
  submitting = signal(false);
  editingOrder = signal<Order | null>(null);

  selection = new SelectionModel<Order>(true, []);

  stats = signal<OrderStats | null>(null);
  soldOut = signal(false);
  logisticsMode = signal(false);
  seasonClosed = signal(false);
  showWaitingListForm = signal(false);
  adminView = signal<'orders' | 'stats'>('orders');

  pickupEmailForms: FormGroup[] = [];

  @ViewChild('confirmSendGenericEmailDialog') confirmSendGenericEmailDialog!: TemplateRef<any>;
  @ViewChild('confirmSendPickupEmailsDialog') confirmSendPickupEmailsDialog!: TemplateRef<any>;

  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    scales: { x: {}, y: { min: 0 } },
    plugins: { legend: { display: true } },
  };
  public barChartType: ChartType = 'bar';
  public barChartData: ChartData<'bar'> = { labels: [], datasets: [] };

  private authSub?: Subscription;
  private apiUrl = '/api';

  isAdmin = computed(() => this.user()?.role === 'ADMIN');
  isGuest = computed(() => this.user()?.provider === 'GUEST');

  get maxQuantity(): number {
    if (this.isAdmin()) return 999;
    return this.user()?.provider === 'GOOGLE' ? 15 : 5;
  }

  get websiteUrl(): string {
    return window.location.origin;
  }

  get showQuantityWarning(): boolean {
    if (this.isAdmin()) return false;
    return (this.orderForm.get('quantity')?.value || 0) > this.maxQuantity;
  }

  get displayedColumns(): string[] {
    return [
      'select',
      'userEmail',
      'name',
      'address',
      'phone',
      'quantity',
      'pickupLocation',
      'createdAt',
      'status',
      'isPaid',
      'isPickedUp',
      'actions'
    ];
  }

  confirmOrder(id: string) {
    this.http.put(`${this.apiUrl}/orders/${id}/confirm`, {}).subscribe({
      next: () => {
        this.snackBar.open('Order confirmed from waiting list!', 'OK', { duration: 3000 });
        this.loadOrders();
      },
      error: () => this.snackBar.open('Failed to confirm order', 'Close', { duration: 3000 }),
    });
  }

  toggleStatus(order: Order, type: 'isPaid' | 'isPickedUp') {
    const newVal = !order[type];
    const payload = { [type]: newVal };
    this.http.put(`${this.apiUrl}/orders/${order.id}/status`, payload).subscribe({
      next: () => {
        order[type] = newVal;
        this.snackBar.open(`${type === 'isPaid' ? 'Payment' : 'Pickup'} status updated`, 'OK', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to update status', 'Close', { duration: 3000 })
    });
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.filteredData.length;
    return numSelected === numRows && numRows > 0;
  }

  masterToggle() {
    this.isAllSelected() ?
        this.selection.clear() :
        this.dataSource.filteredData.forEach(row => this.selection.select(row));
  }

  bulkUpdateStatus(type: 'isPaid' | 'isPickedUp', value: boolean) {
    const ids = this.selection.selected.map(o => o.id);
    if (ids.length === 0) return;

    const payload = { ids, [type]: value };
    this.http.put(`${this.apiUrl}/orders/bulk-status`, payload).subscribe({
      next: () => {
        this.selection.selected.forEach(o => o[type] = value);
        this.selection.clear();
        this.snackBar.open(`Bulk ${type === 'isPaid' ? 'payment' : 'pickup'} status updated`, 'OK', { duration: 3000 });
      },
      error: () => this.snackBar.open('Failed to perform bulk update', 'Close', { duration: 3000 })
    });
  }

  ngOnInit() {
    this.dataSource.filterPredicate = (data: Order, filter: string) => {
      const searchTerms = JSON.parse(filter);
      return (
        (data.userEmail || '').toLowerCase().includes(searchTerms.userEmail) &&
        (data.name || '').toLowerCase().includes(searchTerms.name) &&
        (data.address || '').toLowerCase().includes(searchTerms.address) &&
        (data.phone || '').toLowerCase().includes(searchTerms.phone) &&
        (data.pickupLocation || '').toLowerCase().includes(searchTerms.pickupLocation) &&
        data.quantity.toString().toLowerCase().includes(searchTerms.quantity)
      );
    };

    this.filterForm.valueChanges.subscribe((values) => {
      const searchTerms = {
        userEmail: (values.userEmail || '').toLowerCase(),
        name: (values.name || '').toLowerCase(),
        address: (values.address || '').toLowerCase(),
        phone: (values.phone || '').toLowerCase(),
        pickupLocation: (values.pickupLocation || '').toLowerCase(),
        quantity: (values.quantity || '').toString().toLowerCase(),
      };
      this.dataSource.filter = JSON.stringify(searchTerms);
    });

    // If a session was restored from localStorage, load orders immediately
    if (this.user()) {
      this.updateValidators();
      this.loadOrders();
      if (this.user()?.provider === 'GOOGLE') {
        this.orderForm.patchValue({ name: this.user()?.name });
      }
    }
    this.fetchSoldOutStatus();

    this.pickupEmailForms = this.pickupLocations.map(loc => this.fb.group({
      selected: [false],
      location: [loc],
      time: ['', [Validators.required]],
      contactPerson: ['', [Validators.required]],
      contactPhone: ['', [Validators.required]]
    }));

    this.authSub = this.socialAuthService.authState.subscribe((googleUser) => {
      if (googleUser) {
        const email = googleUser.email ?? '';
        const name = googleUser.name ?? 'Google User';
        this.http.post<BackendUser>(`${this.apiUrl}/users/login`, { email, name }).subscribe({
          next: (backendUser) => {
            this.authService.loginWithGoogle(
              { name, email, photoUrl: googleUser.photoUrl ?? '' },
              backendUser.role,
            );
            this.orderForm.patchValue({ name });
            this.updateValidators();
            this.loadOrders();
          },
          error: () => {
            this.authService.loginWithGoogle(
              { name, email, photoUrl: googleUser.photoUrl ?? '' },
              'USER',
            );
            this.updateValidators();
            this.loadOrders();
          },
        });
      }
    });
  }

  ngOnDestroy() {
    this.authSub?.unsubscribe();
  }

  private updateValidators() {
    const qtyControl = this.orderForm.get('quantity');
    if (qtyControl) {
      qtyControl.setValidators([
        Validators.required,
        Validators.min(1),
        Validators.max(this.maxQuantity),
      ]);
      qtyControl.updateValueAndValidity();
    }

    const emailControl = this.orderForm.get('userEmail');
    if (emailControl) {
      if (this.isGuest()) {
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

    this.http
      .get<Order[]>(`${this.apiUrl}/orders`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => {
          const reversed = (data || []).reverse();
          this.orders.set(reversed);
          this.dataSource.data = reversed;

          // Pre-fill form from the most recent order if the form is currently empty
          if (reversed.length > 0 && this.user()?.provider === 'GOOGLE') {
            const lastOrder = reversed[0];
            const currentVal = this.orderForm.value;
            this.orderForm.patchValue({
              name: currentVal.name || lastOrder.name,
              phone: currentVal.phone || lastOrder.phone,
              pickupLocation: currentVal.pickupLocation || lastOrder.pickupLocation,
              address: currentVal.address || lastOrder.address,
            });
          }
          if (this.isAdmin()) {
            this.loadStats();
          }
        },
        error: () => {
          this.orders.set([]); // Set to empty array to resolve loading state on error
          this.snackBar.open('Failed to load orders', 'Close', { duration: 3000 });
        },
      });
  }

  loadStats() {
    this.http.get<OrderStats>(`${this.apiUrl}/orders/stats`).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.prepareChartData(data);
      },
      error: () => this.snackBar.open('Failed to load statistics', 'Close', { duration: 3000 }),
    });
  }

  private prepareChartData(stats: OrderStats) {
    const labels = this.pickupLocations;
    const data = labels.map((loc) => stats.boxesPerLocation[loc] || 0);

    this.barChartData = {
      labels,
      datasets: [
        {
          data,
          label: 'Boxes per Location',
          backgroundColor: '#ff8f00',
          borderColor: '#e65100',
          borderWidth: 1,
        },
      ],
    };
  }

  submitOrder() {
    if (this.orderForm.invalid) return;
    this.submitting.set(true);

    const orderData = { ...this.orderForm.value };

    const isEdit = !!this.editingOrder();
    const request = isEdit
      ? this.http.put<Order>(`${this.apiUrl}/orders/${this.editingOrder()!.id}`, orderData)
      : this.http.post<Order>(`${this.apiUrl}/orders`, orderData);

    request.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (newOrder) => {
        this.snackBar.open(
          isEdit ? 'Order updated successfully!' : 'Order placed successfully!',
          'OK',
          { duration: 3000 },
        );
        this.editingOrder.set(null);

        if (this.isGuest()) {
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

  downloadEmails() {
    this.http.get(`${this.apiUrl}/orders/export-emails`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'all_emails_2026.csv';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snackBar.open('Failed to download Email List', 'Close', { duration: 3000 }),
    });
  }

  submitNotification(email: string) {
    if (!email || !email.includes('@')) {
      this.snackBar.open('Please enter a valid email address', 'OK', { duration: 3000 });
      return;
    }
    this.submitting.set(true);
    this.http.post(`${this.apiUrl}/users/notify-me`, email).subscribe({
      next: () => {
        this.snackBar.open('Thank you! We will notify you for the 2027 season.', 'OK', { duration: 5000 });
        this.submitting.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to save notification request', 'Close', { duration: 3000 });
        this.submitting.set(false);
      }
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

  fetchSoldOutStatus() {
    this.http
      .get<boolean>(`${this.apiUrl}/config/sold-out`)
      .subscribe((status) => this.soldOut.set(status));
    this.http
      .get<boolean>(`${this.apiUrl}/config/logistics-mode`)
      .subscribe((status) => this.logisticsMode.set(status));
    this.http
      .get<boolean>(`${this.apiUrl}/config/season-closed`)
      .subscribe((status) => this.seasonClosed.set(status));
  }

  onSoldOutToggle(status: boolean) {
    if (status === this.soldOut()) return;

    if (status) {
      // Only show confirmation when setting to SOLD OUT
      const dialogRef = this.dialog.open(this.confirmSoldOutDialog);
      dialogRef.afterClosed().subscribe((result) => {
        if (result) {
          this.setSoldOut(true);
        } else {
          // Reset signal to refresh UI radio button state
          const current = this.soldOut();
          this.soldOut.set(!current);
          setTimeout(() => this.soldOut.set(current));
        }
      });
    } else {
      this.setSoldOut(false);
    }
  }

  setSoldOut(status: boolean) {
    this.http.post(`${this.apiUrl}/config/sold-out`, status).subscribe({
      next: () => {
        this.soldOut.set(status);
        this.snackBar.open(`Sold out status updated to: ${status}`, 'OK', { duration: 3000 });
      },
      error: () =>
        this.snackBar.open('Failed to update sold out status', 'Close', { duration: 3000 }),
    });
  }

  onLogisticsModeToggle(status: boolean) {
    if (status === this.logisticsMode()) return;
    this.http.post(`${this.apiUrl}/config/logistics-mode`, status).subscribe({
      next: () => {
        this.logisticsMode.set(status);
        this.snackBar.open(`Logistics mode updated to: ${status}`, 'OK', { duration: 3000 });
      },
      error: () =>
        this.snackBar.open('Failed to update logistics mode', 'Close', { duration: 3000 }),
    });
  }

  onSeasonClosedToggle(status: boolean) {
    if (status === this.seasonClosed()) return;
    this.http.post(`${this.apiUrl}/config/season-closed`, status).subscribe({
      next: () => {
        this.seasonClosed.set(status);
        this.snackBar.open(`Season closed status updated to: ${status}`, 'OK', { duration: 3000 });
      },
      error: () =>
        this.snackBar.open('Failed to update season closed status', 'Close', { duration: 3000 }),
    });
  }

  onSendGenericEmail() {
    const dialogRef = this.dialog.open(this.confirmSendGenericEmailDialog);
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.sendGenericEmail();
      }
    });
  }

  sendGenericEmail() {
    this.submitting.set(true);
    const payload = {
      subject: 'Mangoes Arrival Update',
      body: 'Hello,\n\nAs an update to your reserved mango boxes:\n\nThe mangoes are on their way and are scheduled to arrive on 29.05.2026. Pickup details will be shared via email by 30.05.2026.\n\nFor your order details please visit: https://ramaswiss.ch'
    };
    this.http.post(`${this.apiUrl}/admin/emails/generic`, payload).subscribe({
      next: () => {
        this.snackBar.open('Generic email sent successfully!', 'OK', { duration: 3000 });
        this.submitting.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to send generic email', 'Close', { duration: 3000 });
        this.submitting.set(false);
      }
    });
  }

  onSendPickupEmails() {
    const selectedForms = this.pickupEmailForms.filter(f => f.get('selected')?.value);
    const validData = selectedForms.filter(f => f.valid);

    if (validData.length === 0) {
      this.snackBar.open('Please select and fill in details for at least one pickup location', 'Close', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(this.confirmSendPickupEmailsDialog);
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.sendPickupEmails();
      }
    });
  }

  sendPickupEmails() {
    const selectedForms = this.pickupEmailForms.filter(f => f.get('selected')?.value);
    const validData = selectedForms.filter(f => f.valid).map(f => {
      const { selected, ...rest } = f.value;
      return rest;
    });

    this.submitting.set(true);
    this.http.post(`${this.apiUrl}/admin/emails/pickup`, validData).subscribe({
      next: () => {
        this.snackBar.open('Individual pickup emails sent successfully!', 'OK', { duration: 3000 });
        this.submitting.set(false);
      },
      error: () => {
        this.snackBar.open('Failed to send individual pickup emails', 'Close', { duration: 3000 });
        this.submitting.set(false);
      }
    });
  }
}
