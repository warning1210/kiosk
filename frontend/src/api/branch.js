import axios from 'axios';
import { firebaseAuth } from '../firebase';

const api = axios.create({
    baseURL: '/api/branch',
    headers: {
        'Content-Type': 'application/json'
    }
});

// branchId는 URL로 받지 않고, 로그인된 Firebase 토큰으로부터 서버가 직접 확인한다 (IDOR 방지)
api.interceptors.request.use(async (config) => {
    const idToken = await firebaseAuth.currentUser?.getIdToken();
    console.log('Firebase token:', idToken);
    if (idToken) config.headers.Authorization = `Bearer ${idToken}`;
    return config;
});

export default api;