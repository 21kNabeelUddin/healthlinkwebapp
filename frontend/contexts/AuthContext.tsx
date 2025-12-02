'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { User } from '@/types';
import { getStoredUser, setStoredUser, getStoredToken, setStoredToken, clearAuth } from '@/lib/auth';
import { authApi } from '@/lib/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (user: User, token: string, refreshToken?: string) => void;
  logout: () => Promise<void>;
  updateUser: (user: User) => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const storedUser = getStoredUser() as any;
    const storedToken = getStoredToken();
    if (storedUser && storedToken) {
      // Normalize legacy stored users that may not have `userType` populated
      const normalizedRole = storedUser.role || storedUser.userType;
      const normalizedUser: User = {
        ...storedUser,
        id: String(storedUser.id),
        userType: normalizedRole,
        role: normalizedRole,
      };

      setUser(normalizedUser);
      setToken(storedToken);
    }
    setIsLoading(false);
  }, []);

  const login = (newUser: User, newToken: string, refreshToken?: string) => {
    setUser(newUser);
    setToken(newToken);
    setStoredUser(newUser);
    setStoredToken(newToken);
    if (refreshToken) {
      localStorage.setItem('refreshToken', refreshToken);
    }
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      setToken(null);
      clearAuth();
      localStorage.removeItem('refreshToken');
    }
  };

  const updateUser = (updatedUser: User) => {
    setUser(updatedUser);
    setStoredUser(updatedUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        login,
        logout,
        updateUser,
        isAuthenticated: !!user && !!token,
        isLoading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

