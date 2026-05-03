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

  loginAsGuest() {
    this._user.set({
      name: 'Guest',
      email: 'guest@rama.local', // Placeholder, will be replaced by form input
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
