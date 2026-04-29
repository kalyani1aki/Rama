import { Injectable, signal } from '@angular/core';

export type UserRole = 'ADMIN' | 'USER';

export interface RamaUser {
  name: string;
  email: string;
  photoUrl: string;
  provider: 'GOOGLE' | 'GUEST';
  role: UserRole;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private _user = signal<RamaUser | null>(null);
  readonly user = this._user.asReadonly();

  // For guest users, use a stable session ID stored in localStorage
  private guestEmail: string = this.getOrCreateGuestEmail();

  private getOrCreateGuestEmail(): string {
    let id = localStorage.getItem('rama_guest_id');
    if (!id) {
      id = 'guest_' + (crypto.randomUUID?.() ?? this.generateUUID());
      localStorage.setItem('rama_guest_id', id);
    }
    return id + '@guest.rama';
  }

  private generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
      const r = (Math.random() * 16) | 0;
      return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
    });
  }

  loginAsGuest() {
    this._user.set({
      name: 'Guest',
      email: this.guestEmail,
      photoUrl: '',
      provider: 'GUEST',
      role: 'USER',
    });
  }

  loginWithGoogle(googleUser: { name: string; email: string; photoUrl: string }, role: UserRole) {
    this._user.set({
      name: googleUser.name,
      email: googleUser.email,
      photoUrl: googleUser.photoUrl,
      provider: 'GOOGLE',
      role,
    });
  }

  updateRole(role: UserRole) {
    const current = this._user();
    if (current) this._user.set({ ...current, role });
  }

  logout() {
    this._user.set(null);
  }

  isLoggedIn(): boolean {
    return this._user() !== null;
  }

  getUserEmail(): string {
    return this._user()?.email ?? 'anonymous';
  }
}
