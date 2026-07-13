import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import PaymentScanView from '../views/kiosk/PaymentScanView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import BranchDashboardView from '../views/admin/BranchDashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/pay/:token', name: 'payment-scan', component: PaymentScanView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/branch/:branchId/dashboard', name: 'branch-dashboard', component: BranchDashboardView }
  ]
})

export default router
