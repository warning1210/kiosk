import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import CheckoutView from '../views/kiosk/CheckoutView.vue'
import PaymentSuccessView from '../views/kiosk/PaymentSuccessView.vue'
import AdminAccountsView from '../views/admin/AdminAccountsView.vue'
import AdminDashboardView from '../views/admin/AdminDashboardView.vue'
import AdminStockRequestsView from '../views/admin/AdminStockRequestsView.vue'
import AdminDeliveriesView from '../views/admin/AdminDeliveriesView.vue'
import AdminBranchesView from '../views/admin/AdminBranchesView.vue'
import AdminEventsView from '../views/admin/AdminEventsView.vue'
import AdminCouponsView from '../views/admin/AdminCouponsView.vue'
import AdminChatView from '../views/admin/AdminChatView.vue'
import AdminProductsView from '../views/admin/AdminProductsView.vue'
import AdminReportsView from '../views/admin/AdminReportsView.vue'
import AdminComingSoonView from '../views/admin/AdminComingSoonView.vue'
import AdminNoticesView from '../views/admin/AdminNoticesView.vue'
import BranchLoginView from '../views/branch/BranchLoginView.vue'
import BranchJoinView from '../views/branch/BranchJoinView.vue'
// 이메일 링크에서 열리는 예비 지점장 전용 신청서 화면을 가져온다.
import BranchApplicationFormView from '../views/branch/BranchApplicationFormView.vue'
import DashboardView from '../views/branch/DashboardView.vue'
import OrdersView from '../views/branch/OrdersView.vue'
import OrderListView from '../views/branch/OrderListView.vue'
import InventoryView from '../views/branch/InventoryView.vue'
import StockRequestsView from '../views/branch/StockRequestsView.vue'
import SalesView from '../views/branch/SalesView.vue'
import ChatView from '../views/branch/ChatView.vue'
import EventsView from '../views/branch/EventsView.vue'
import NoticeDetailView from '../views/branch/NoticeDetailView.vue'
import BranchProductView from '../views/branch/BranchProductView.vue'
import { resetKioskLocale } from '../composables/useKioskI18n'
import { resolveGuard } from './authGuard'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/pay/:token', name: 'payment-scan', component: CheckoutView },
    { path: '/payment/success', name: 'payment-success', component: PaymentSuccessView },
    // 본사/지점 로그인이 하나로 합쳐져서 BranchLoginView 하나로 통합됨 (로그인 성공 후 role로 목적지를 정함)
    { path: '/admin/login', redirect: '/branch/login' },
    { path: '/admin/dashboard', name: 'admin-dashboard', component: AdminDashboardView },
    { path: '/admin/stock-requests', name: 'admin-stock-requests', component: AdminStockRequestsView },
    { path: '/admin/deliveries', name: 'admin-deliveries', component: AdminDeliveriesView },
    { path: '/admin/branches', name: 'admin-branches', component: AdminBranchesView },
    { path: '/admin/reports', name: 'admin-reports', component: AdminReportsView },
    { path: '/admin/events', name: 'admin-events', component: AdminEventsView },
    { path: '/admin/products', name: 'admin-products', component: AdminProductsView },
    { path: '/admin/coupons', name: 'admin-coupons', component: AdminCouponsView },
    { path: '/admin/chat', name: 'admin-chat', component: AdminChatView },
    {
      path: '/admin/notices',
      name: 'admin-notices',
      component: AdminNoticesView,
      meta: { active: 'notices', title: '공지사항', subtitle: '본사 공지사항을 작성하고 지점 노출 여부를 관리할 수 있습니다.' }
    },
    {
      path: '/admin/accounts',
      name: 'admin-accounts',
      // 준비 중 화면 대신 실제 지점장 계정 관리 화면을 연결한다.
      component: AdminAccountsView,
      meta: { active: 'accounts', title: '계정 관리', subtitle: '본사·지점 관리자 계정 권한을 관리할 수 있습니다.' }
    },
    {
      path: '/admin/settings',
      name: 'admin-settings',
      component: AdminComingSoonView,
      meta: { active: 'settings', title: '시스템 설정', subtitle: '재고, 배송, 시스템 기본 정책을 설정할 수 있습니다.' }
    },
    { path: '/admin/branch-invites', name: 'admin-branch-invites', component: AdminAccountsView },
    { path: '/branch/login', name: 'branch-login', component: BranchLoginView },
    { path: '/branch/join', name: 'branch-join', component: BranchJoinView },
    // 본점이 발송한 토큰 URL로만 접근하는 지점 개설 신청서 경로다.
    { path: '/branch/application', name: 'branch-application', component: BranchApplicationFormView },
    { path: '/branch/dashboard', name: 'branch-dashboard', component: DashboardView },
    { path: '/branch/orders', name: 'branch-orders', component: OrdersView },
    { path: '/branch/orderlist', name: 'branch-orderlist', component: OrderListView },
    { path: '/branch/inventory', name: 'branch-inventory', component: InventoryView },
    { path: '/branch/stock-requests', name: 'branch-stock-requests', component: StockRequestsView },
    { path: '/branch/sales', name: 'branch-sales', component: SalesView },
    { path: '/branch/chat', name: 'branch-chat', component: ChatView },
    { path: '/branch/events', name: 'branch-events', component: EventsView },
    { path: '/branch/products', name: 'branch-products', component: BranchProductView },
    { path: '/branch/notices/:type/:id', name: 'branch-notice-detail', component: NoticeDetailView }
  ]
})

// 본사/지점 주소를 직접 입력해도 로그인 토큰이 없는 사용자는 로그인 화면으로 보낸다.
router.beforeEach((to) =>
  resolveGuard(to.path, to.fullPath, (key) => JSON.parse(localStorage.getItem(key) || 'null'))
)

// 주문 완료·취소·시간 초과 등 어떤 경로로든 광고 화면에 돌아오면
// 다음 고객은 항상 한국어에서 새 주문을 시작합니다.
router.afterEach((to) => {
  if (to.path === '/') resetKioskLocale()
})

export default router
