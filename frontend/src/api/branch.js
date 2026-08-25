import axios from 'axios';
import { firebaseAuth } from '../firebase';

const api = axios.create({
    baseURL: '/api/branch',
    headers: {
        'Content-Type': 'application/json'
    }
});

// branchId는 URL로 받지 않고, 로그인된 토큰으로부터 서버가 직접 확인한다 (IDOR 방지)
api.interceptors.request.use(async (config) => {
    // Firebase 장애 시의 DB 폴백 로그인을 했다면 토큰이 httpOnly 쿠키에 들어있다 - JS는 그 값을
    // 읽을 수 없고(읽히면 XSS로 유출되니까 그게 목적이다) 브라우저가 요청에 자동으로 실어 보낸다.
    // 여기서 할 일은 Firebase를 건드리지 않고 그냥 통과시키는 것뿐이다.
    const session = JSON.parse(localStorage.getItem('branch-session') || 'null');
    if (session?.auth === 'cookie') return config;

    // 새로고침 직후에는 Firebase가 저장된 로그인 세션을 아직 복원하는 중이라
    // currentUser가 잠깐 null일 수 있다. authStateReady()로 그 복원이 끝날 때까지
    // 기다린 뒤 currentUser를 읽어야, 로그인된 상태에서도 첫 요청이 401로 새지 않는다.
    await firebaseAuth.authStateReady();
    const idToken = await firebaseAuth.currentUser?.getIdToken();
    if (idToken) config.headers.Authorization = `Bearer ${idToken}`;
    return config;
});

// 지점 토큰이 없거나 만료(또는 로그아웃으로 무효화)되어 서버가 401을 반환하면
// 잘못 남은 세션을 제거하고 로그인 화면으로 리다이렉트한다.
// 쿠키 자체는 JS가 못 지우므로 서버가 /api/auth/logout에서 지운다.
api.interceptors.response.use(
    (response) => response,
    async (requestError) => {
        if (requestError.response?.status === 401) {
            localStorage.removeItem('branch-session');
            // Firebase 세션도 로그아웃 시켜서 다시 로그인하도록 유도한다.
            if (firebaseAuth.currentUser) {
                await firebaseAuth.signOut();
            }
            if (window.location.pathname !== '/branch/login') {
                window.location.replace('/branch/login');
            }
        }
        return Promise.reject(requestError);
    }
);

export default api;