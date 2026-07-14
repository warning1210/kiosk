import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import CheckoutView from '../views/CheckoutView.vue'
import PaymentSuccessView from '../views/PaymentSuccessView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    
    // QR 코드가 인코딩해야 하는 URL (Toss Checkout 연동용)
    { path: '/pay/:token', name: 'payment-scan', component: CheckoutView },
    { path: '/payment/success', name: 'payment-success', component: PaymentSuccessView },
    
    // 관리자 로그인 경로
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView }
  ]
})

export default router