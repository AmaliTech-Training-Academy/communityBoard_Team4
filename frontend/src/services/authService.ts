import api from './api';

export interface AuthResponse {
  token: string;
  user?: {
    id: string;
    name: string;
    email: string;
  };
}

export const authService = {
  // `POST /api/auth/login` — sends back a JWT token with a 200 (OK) status
  login: async (email?: string, password?: string): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/auth/login', { email, password });
    return response.data;
  },

  // `POST /api/auth/register` — sends back a JWT token with a 201 (Created) status
  register: async (name: string, email: string, password: string): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/api/auth/register', { name, email, password });
    return response.data;
  }
};
