import axios from 'axios';

const apiClient = axios.create({
    baseURL: 'http://backend:8080', // Backend URL
    withCredentials: true, // Important for sending cookies (session)
});

apiClient.interceptors.request.use(config => {
    const token = document.cookie
        .split('; ')
        .find(row => row.startsWith('XSRF-TOKEN='))
        ?.split('=')[1];
    if (token) {
        config.headers['X-XSRF-TOKEN'] = token;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

export const getCurrentUser = async () => {
    try {
        const response = await apiClient.get('/api/user/me');
        return response.data;
    } catch (error) {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            return null;
        }
        throw error;
    }
};

export const logoutUser = async () => {
    try {
        await apiClient.post('/api/logout');
    } catch (error) {
        console.error('Logout failed:', error);
        throw error;
    }
};

export default apiClient;
