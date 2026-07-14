import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import CheckoutView from '../views/CheckoutView.vue'
import PaymentSuccessView from '../views/PaymentSuccessView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import BranchDashboardView from '../views/admin/BranchDashboardView.vue'
 
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
 
    // QR 코드가 인코딩해야 하는 URL: https://your-frontend.com/pay/{qrToken}
    { path: '/pay/:token', name: 'payment-scan', component: CheckoutView },
    { path: '/payment/success', name: 'payment-success', component: PaymentSuccessView },
    // failUrl용 페이지도 필요하면 비슷하게 추가하세요.
 
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/branch/:branchId/dashboard', name: 'branch-dashboard', component: BranchDashboardView }
  ]
})
 
export default router