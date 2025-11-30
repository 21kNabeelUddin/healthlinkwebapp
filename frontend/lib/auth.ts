import { User } from '@/types';

export const getStoredUser = (): User | null => {
  if (typeof window === 'undefined') return null;
  const userStr = localStorage.getItem('user');
  if (!userStr) return null;
  try {
    return JSON.parse(userStr);
  } catch {
    return null;
  }
};

export const setStoredUser = (user: User | null): void => {
  if (typeof window === 'undefined') return;
  if (user) {
    localStorage.setItem('user', JSON.stringify(user));
  } else {
    localStorage.removeItem('user');
  }
};

export const getStoredToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('token');
};

export const setStoredToken = (token: string | null): void => {
  if (typeof window === 'undefined') return;
  if (token) {
    localStorage.setItem('token', token);
  } else {
    localStorage.removeItem('token');
  }
};

export const clearAuth = (): void => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('user');
  localStorage.removeItem('token');
};

