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
  private readonly STORAGE_KEY = 'rama_user_session';
  private _user = signal<RamaUser | null>(null);
  readonly user = this._user.asReadonly();

  constructor() {
    this.restoreSession();
  }

  private restoreSession() {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved) {
      try {
        this._user.set(JSON.parse(saved));
      } catch {
        localStorage.removeItem(this.STORAGE_KEY);
      }
    }
  }

  private saveSession(user: RamaUser | null) {
    if (user) {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  loginAsGuest() {
    const guest: RamaUser = {
      name: 'Guest',
      email: 'guest@rama.local',
      photoUrl: '',
      provider: 'GUEST',
      role: 'USER',
    };
    this._user.set(guest);
    this.saveSession(guest);
  }

  loginWithGoogle(googleUser: { name: string; email: string; photoUrl: string }, role: UserRole) {
    const user: RamaUser = {
      name: googleUser.name,
      email: googleUser.email,
      photoUrl: googleUser.photoUrl,
      provider: 'GOOGLE',
      role,
    };
    this._user.set(user);
    this.saveSession(user);
  }

  updateRole(role: UserRole) {
    const current = this._user();
    if (current) {
      const updated = { ...current, role };
      this._user.set(updated);
      this.saveSession(updated);
    }
  }

  logout() {
    this._user.set(null);
    this.saveSession(null);
  }

  isLoggedIn(): boolean {
    return this._user() !== null;
  }

  getUserEmail(): string {
    return this._user()?.email ?? 'anonymous';
  }
}
