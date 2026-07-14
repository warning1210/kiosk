import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import OrderView from '../views/kiosk/OrderView.vue'
import AdminLoginView from '../views/admin/AdminLoginView.vue'
import ActorPickerView from '../views/admin/ActorPickerView.vue'
import BranchLayout from '../views/branch/BranchLayout.vue'
import InventoryView from '../views/branch/InventoryView.vue'
import StockRequestStatusView from '../views/branch/StockRequestStatusView.vue'
import HqLayout from '../views/hq/HqLayout.vue'
import StockRequestListView from '../views/hq/StockRequestListView.vue'
import { useDevActorStore } from '../stores/devActor'

// URL과 화면 컴포넌트의 관계를 정의한다.
// createWebHistory를 사용하므로 주소가 # 없이 일반적인 /branch/inventory 형태로 보인다.
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/select', name: 'admin-select', component: ActorPickerView },
    // /branch 아래에서는 BranchLayout을 공통 틀로 유지한다.
    // children의 상대 경로가 붙어 /branch/inventory와 /branch/stock-requests가 되고,
    // 선택된 자식 화면은 BranchLayout 안의 <router-view> 위치에 렌더링된다.
    {
      path: '/branch',
      component: BranchLayout,
      meta: { requiresActor: true },
      children: [
        { path: 'inventory', name: 'branch-inventory', component: InventoryView },
        { path: 'stock-requests', name: 'branch-stock-requests', component: StockRequestStatusView }
      ]
    },
    // 본점도 같은 중첩 라우트 방식으로 HqLayout 안에 재고신청 목록을 표시한다.
    {
      path: '/hq',
      component: HqLayout,
      meta: { requiresActor: true },
      children: [{ path: 'stock-requests', name: 'hq-stock-requests', component: StockRequestListView }]
    }
  ]
})

// requiresActor가 붙은 지점/본점 경로는 개발용 관리자 선택이 먼저 필요하다.
// 이 가드는 선택 여부만 확인하며, 지점/본점 역할에 따른 실제 API 권한은 백엔드가 검증한다.
// 선택 정보가 없으면 원래 화면 대신 관리자 선택 화면으로 이동시킨다.
router.beforeEach((to) => {
  if (to.meta.requiresActor) {
    const devActor = useDevActorStore()
    if (!devActor.isSelected) {
      return { name: 'admin-select' }
    }
  }
})

export default router
