import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import CheckoutView from '../views/kiosk/CheckoutView.vue'
import PaymentSuccessView from '../views/kiosk/PaymentSuccessView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminAccountsView from '../views/admin/AdminAccountsView.vue'
import BranchLoginView from '../views/branch/BranchLoginView.vue'
import BranchJoinView from '../views/branch/BranchJoinView.vue'
import DashboardView from '../views/branch/DashboardView.vue'
import OrdersView from '../views/branch/OrdersView.vue'
import InventoryView from '../views/branch/InventoryView.vue'
import SalesView from '../views/branch/SalesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/pay/:token', name: 'payment-scan', component: CheckoutView },
    { path: '/payment/success', name: 'payment-success', component: PaymentSuccessView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/accounts', name: 'admin-accounts', component: AdminAccountsView },
    { path: '/branch/login', name: 'branch-login', component: BranchLoginView },
    { path: '/branch/join', name: 'branch-join', component: BranchJoinView },
    { path: '/branch/dashboard', name: 'branch-dashboard', component: DashboardView },
    { path: '/branch/orders', name: 'branch-orders', component: OrdersView },
    { path: '/branch/inventory', name: 'branch-inventory', component: InventoryView },
    { path: '/branch/sales', name: 'branch-sales', component: SalesView }
  ]
})

export default router
