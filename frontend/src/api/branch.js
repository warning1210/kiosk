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
    // Firebase 장애 시의 DB 폴백 로그인을 했다면 branch-session에 자체 서명 토큰이 같이 저장돼 있다 -
    // 이 경우 Firebase는 아예 건드리지 않아야 폴백의 의미가 있다.
    const session = JSON.parse(localStorage.getItem('branch-session') || 'null');
    if (session?.token) {
        config.headers.Authorization = `Bearer ${session.token}`;
        return config;
    }

    // 새로고침 직후에는 Firebase가 저장된 로그인 세션을 아직 복원하는 중이라
    // currentUser가 잠깐 null일 수 있다. authStateReady()로 그 복원이 끝날 때까지
    // 기다린 뒤 currentUser를 읽어야, 로그인된 상태에서도 첫 요청이 401로 새지 않는다.
    await firebaseAuth.authStateReady();
    const idToken = await firebaseAuth.currentUser?.getIdToken();
    if (idToken) config.headers.Authorization = `Bearer ${idToken}`;
    return config;
});

export default api;