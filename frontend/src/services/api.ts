import axios from "axios";

const api = axios.create({
  baseURL: "/api",
});

// Attach the token automatically to every request if it exists
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 globally — only redirect to login when a token existed and expired.
// Never redirect on auth-endpoint 401s (wrong password, unverified account, etc.)
// so callers can surface a proper error message to the user.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const requestUrl = error.config?.url ?? "";
    const isAuthEndpoint = requestUrl.startsWith("/auth/");
    const storedToken = localStorage.getItem("token");
    const hadToken = !!storedToken && storedToken !== "null" && storedToken !== "undefined";
    if (error.response?.status === 401 && hadToken && !isAuthEndpoint) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      localStorage.removeItem("role");
      localStorage.removeItem("name");
      localStorage.removeItem("email");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);

export default api;
