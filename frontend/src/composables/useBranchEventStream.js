import { onMounted, onUnmounted } from 'vue'

// 지점 대시보드 전용 SSE 연결 - 여러 컴포넌트(사이드바 알림, 대시보드)가 동시에 써도
// 실제 EventSource 연결은 탭당 하나만 유지한다 (useNewOrderAlert.js와 동일한 구독자 카운팅 패턴).
// 브라우저 기본 EventSource는 커스텀 헤더를 못 보내서 토큰을 쿼리 파라미터로 붙인다.
let eventSource = null
let subscribers = 0
const listeners = { order: new Set(), notice: new Set() }

// 탭이 백그라운드/비활성이어도(다른 탭 보는 중, 창 최소화 등) 새 주문을 놓치지 않도록
// 브라우저 데스크톱 알림을 띄운다. 탭이 아예 닫혀있으면 못 띄운다 - 그건 Service Worker 기반
// Web Push가 필요한 별개 작업(VAPID 키 발급, 백엔드 구독 저장/발송 인프라 필요).
function notifyNewOrder() {
  if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
  if (!document.hidden) return // 화면을 보고 있으면 뱃지 갱신만으로 충분, 알림은 안 띄운다

  const notification = new Notification('새 주문이 들어왔어요', {
    body: '지점 관리자 화면에서 확인해주세요.',
    tag: 'branch-new-order' // 같은 tag로 계속 덮어써서 알림이 쌓이지 않게 함
  })
  notification.onclick = () => {
    window.focus()
    notification.close()
  }
}

function connect() {
  const session = JSON.parse(localStorage.getItem('branch-session') || 'null')
  if (!session?.token) return

  if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
    Notification.requestPermission()
  }

  eventSource = new EventSource(`/api/branch/events/stream?token=${encodeURIComponent(session.token)}`)
  // 'order'는 상태변경/자동취소를 포함한 범용 갱신 신호, 'newOrder'는 실제 결제 확정 시에만 온다.
  eventSource.addEventListener('order', () => listeners.order.forEach(fn => fn()))
  eventSource.addEventListener('newOrder', notifyNewOrder)
  eventSource.addEventListener('notice', () => listeners.notice.forEach(fn => fn()))
  // EventSource는 연결이 끊기면 브라우저가 알아서 재연결을 시도한다 - 별도 처리 불필요.
}

function disconnect() {
  eventSource?.close()
  eventSource = null
}

export function useBranchEventStream() {
  onMounted(() => {
    subscribers++
    if (subscribers === 1) connect()
  })

  onUnmounted(() => {
    subscribers--
    if (subscribers === 0) disconnect()
  })

  return {
    onOrder(fn) {
      listeners.order.add(fn)
      return () => listeners.order.delete(fn)
    },
    onNotice(fn) {
      listeners.notice.add(fn)
      return () => listeners.notice.delete(fn)
    }
  }
}
