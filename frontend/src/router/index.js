import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import OrderTypeView from '../views/kiosk/OrderTypeView.vue'
import SizeView from '../views/kiosk/SizeView.vue'
import CheckoutView from '../views/kiosk/CheckoutView.vue'
import BenefitsView from '../views/kiosk/BenefitsView.vue'
import PaymentView from '../views/kiosk/PaymentView.vue'
import TossQrView from '../views/kiosk/TossQrView.vue'
import ReceiptView from '../views/kiosk/ReceiptView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import AdminAccountsView from '../views/admin/AdminAccountsView.vue'
import InventoryView from '../views/branch/InventoryView.vue'
import DashboardView from '../views/branch/DashboardView.vue'
import OrdersView from '../views/branch/OrdersView.vue'
import SalesView from '../views/branch/SalesView.vue'
import BranchLoginView from '../views/branch/BranchLoginView.vue'
import BranchApplyView from '../views/branch/BranchApplyView.vue'
import BranchJoinView from '../views/branch/BranchJoinView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/type', name: 'kiosk-type', component: OrderTypeView },
    { path: '/kiosk/size', name: 'kiosk-size', component: SizeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/kiosk/checkout', name: 'kiosk-checkout', component: CheckoutView },
    { path: '/kiosk/benefits', name: 'kiosk-benefits', component: BenefitsView },
    { path: '/kiosk/payment', name: 'kiosk-payment', component: PaymentView },
    { path: '/kiosk/toss-qr', name: 'kiosk-toss-qr', component: TossQrView },
    { path: '/kiosk/receipt', name: 'kiosk-receipt', component: ReceiptView },
    { path: '/admin', redirect: '/admin/accounts' },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/accounts', name: 'admin-accounts', component: AdminAccountsView },
    { path: '/branch', redirect: '/branch/login' },
    { path: '/branch/login', name: 'branch-login', component: BranchLoginView },
    { path: '/branch/apply', name: 'branch-apply', component: BranchApplyView },
    { path: '/branch/join', name: 'branch-join', component: BranchJoinView },
    { path: '/branch/dashboard', name: 'branch-dashboard', component: DashboardView },
    { path: '/branch/orders', name: 'branch-orders', component: OrdersView },
    { path: '/branch/sales', name: 'branch-sales', component: SalesView },
    { path: '/branch/inventory', name: 'branch-inventory', component: InventoryView }
  ]
})

export default router
