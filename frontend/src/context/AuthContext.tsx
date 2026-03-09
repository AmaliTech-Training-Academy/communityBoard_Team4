import React, { createContext, useState, useContext, useEffect, ReactNode } from "react";

interface User {
  id?: string;
  name?: string;
  email?: string;
  [key: string]: any;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email?: string, password?: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem("token"));

  useEffect(() => {
    const savedUser = localStorage.getItem("user");
    if (savedUser && token) {
      try { setUser(JSON.parse(savedUser)); } catch (e) {}
    }
  }, [token]);

  const login = async (email?: string, password?: string) => {
    // Mock login simulating API request
    await new Promise(res => setTimeout(res, 800));
    if (email === 'admin@amalitech.com' && password === 'password123') {
      const mockUser = { id: '1', name: 'Admin User', email };
      setUser(mockUser);
      setToken('mock-auth-token');
      localStorage.setItem("token", 'mock-auth-token');
      localStorage.setItem("user", JSON.stringify(mockUser));
    } else {
      throw new Error("Invalid credentials");
    }
  };

  const register = async (name: string, email: string, password: string) => {
    // Mock registration API call
    await new Promise(res => setTimeout(res, 800));
    // For now, assume successful
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
};
