import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || '/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 errors (but not for auth endpoints)
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthEndpoint = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth endpoints
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
};

// Image endpoints
export const imageApi = {
  upload: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/images/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// Prompt endpoints
export const promptApi = {
  submit: (data) => api.post('/prompt', data),
  getHistory: () => api.get('/prompt/history'),
};

export default api;
