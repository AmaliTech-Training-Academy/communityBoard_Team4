import axios from 'axios';

// Get base URL from environment variables, fallback to localhost:8080/api/v1
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// We map /api/auth endpoints as described without the /v1 if they are just /api/auth.
// If the backend has them at /api/v1/auth, this base URL works.
// For now, setting it to the root until we clarify, but assuming standard /api structure.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: parseInt(import.meta.env.VITE_API_TIMEOUT || '30000', 10),
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const tokenStr = localStorage.getItem('token');
    if (tokenStr) {
      // In AuthContext, token is set as string directly ('mock-auth-token'), but later we'll have real JWT
      config.headers.Authorization = `Bearer ${tokenStr}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle global errors like 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If we receive a 401 Unauthorized, we could trigger a logout action
    if (error.response && error.response.status === 401) {
      console.error('Authentication expired. Please log in again.');
      // Optional: Clear token and redirect to login, or emit event that AuthContext listens to
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      // window.location.href = '/login'; 
    }
    return Promise.reject(error);
  }
);

export default api;
