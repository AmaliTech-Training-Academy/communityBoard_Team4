import React, { createContext, useState, useContext, useEffect, ReactNode } from "react";
import api from '../services/api';

export interface User {
  name: string;
  email: string;
  role: string;
  token?: string;
}

interface AuthResponse {
  token?: string | null;
  role: string;
  name: string;
  email: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email?: string, password?: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<AuthResponse>;
  logout: () => void;
  updateUserName: (name: string) => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

const readStoredToken = () => {
  const storedToken = localStorage.getItem("token");
  if (!storedToken || storedToken === "null" || storedToken === "undefined") {
    return null;
  }
  return storedToken;
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(() => {
    const token = readStoredToken();
    const role = localStorage.getItem("role");
    const name = localStorage.getItem("name");
    const email = localStorage.getItem("email");
    return token && role && name && email ? { token, role, name, email } : null;
  });
  const [token, setToken] = useState<string | null>(readStoredToken());

  useEffect(() => {
    setToken(readStoredToken());
  }, []);

  const login = async (email?: string, password?: string) => {
    const { data } = await api.post('/auth/login', { email, password });

    // Validate returned shape manually or inherently trust the API definition
    const { token: apiToken, role, name, email: apiEmail } = data;

    if (!apiToken) {
      throw new Error("Login succeeded without a token. Please try again.");
    }

    setToken(apiToken);
    localStorage.setItem("token", apiToken);
    localStorage.setItem("role", role);
    localStorage.setItem("name", name);
    localStorage.setItem("email", apiEmail);

    setUser({ token: apiToken, role, name, email: apiEmail });
  };

  const register = async (name: string, email: string, password: string) => {
    const { data } = await api.post('/auth/register', { name, email, password });

    const { token: apiToken, role, name: apiName, email: apiEmail } = data;

    if (!apiToken) {
      return data as AuthResponse;
    }

    setToken(apiToken);
    localStorage.setItem("token", apiToken);
    localStorage.setItem("role", role);
    localStorage.setItem("name", apiName);
    localStorage.setItem("email", apiEmail);

    setUser({ token: apiToken, role, name: apiName, email: apiEmail });

    return data as AuthResponse;
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("name");
    localStorage.removeItem("email");
    localStorage.removeItem("user"); // Clear legacy user objects if present
  };

  const updateUserName = (name: string) => {
    localStorage.setItem("name", name);
    setUser((prev) => (prev ? { ...prev, name } : prev));
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, updateUserName }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};
