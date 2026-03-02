import axios from "axios";

const API = axios.create({ baseURL: "/api" });

API.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authAPI = {
  login: (data) => API.post("/auth/login", data),
  register: (data) => API.post("/auth/register", data),
};

export const postAPI = {
  getAll: (page = 0, size = 10) => API.get(`/posts?page=${page}&size=${size}`),
  getById: (id) => API.get(`/posts/${id}`),
  create: (data) => API.post("/posts", data),
  update: (id, data) => API.put(`/posts/${id}`, data),
  delete: (id) => API.delete(`/posts/${id}`),
};

export const categoryAPI = {
  getAll: () => API.get("/categories"),
};

// TODO: Add comment API calls
// TODO: Add search API calls

export default API;
