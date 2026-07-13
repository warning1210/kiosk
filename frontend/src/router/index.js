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

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/kiosk/order', name: 'kiosk-order', component: OrderView },
    { path: '/admin/login', name: 'admin-login', component: AdminLoginView },
    { path: '/admin/select', name: 'admin-select', component: ActorPickerView },
    {
      path: '/branch',
      component: BranchLayout,
      meta: { requiresActor: true },
      children: [
        { path: 'inventory', name: 'branch-inventory', component: InventoryView },
        { path: 'stock-requests', name: 'branch-stock-requests', component: StockRequestStatusView }
      ]
    },
    {
      path: '/hq',
      component: HqLayout,
      meta: { requiresActor: true },
      children: [{ path: 'stock-requests', name: 'hq-stock-requests', component: StockRequestListView }]
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresActor) {
    const devActor = useDevActorStore()
    if (!devActor.isSelected) {
      return { name: 'admin-select' }
    }
  }
})

export default router
