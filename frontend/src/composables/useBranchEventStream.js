import { onMounted, onUnmounted } from 'vue'
import http from '../api/branch'

// 지점 대시보드 전용 SSE 연결 - 여러 컴포넌트(사이드바 알림, 대시보드)가 동시에 써도
// 실제 EventSource 연결은 탭당 하나만 유지한다 (useNewOrderAlert.js와 동일한 구독자 카운팅 패턴).
// 브라우저 기본 EventSource는 커스텀 헤더를 못 보내서 토큰을 쿼리 파라미터로 붙여야 하는데,
// 그러면 액세스 로그에 그대로 남는다 - 로그인 토큰(12시간, 풀 권한)을 직접 쓰지 않고, 헤더 인증으로
// 먼저 1분짜리 전용 티켓을 받아 그것만 쿼리 파라미터에 싣는다.
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

async function connect() {
  // 토큰이 httpOnly 쿠키로 옮겨가 JS에서 안 보이므로 세션 객체 존재로만 로그인 여부를 본다.
  const session = JSON.parse(localStorage.getItem('branch-session') || 'null')
  if (!session?.branchId) return

  if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
    Notification.requestPermission()
  }

  let ticket
  try {
    ticket = (await http.get('/events/stream-ticket')).data.ticket
  } catch (e) {
    console.error('SSE 티켓 발급 실패:', e)
    return
  }
  // connect()가 비동기로 티켓을 기다리는 동안 컴포넌트가 unmount돼 disconnect()가 먼저
  // 불렸을 수 있다 - 그 경우 subscribers는 이미 0이라 여기서 연결을 열면 안 된다.
  if (subscribers === 0) return

  eventSource = new EventSource(`/api/branch/events/stream?token=${encodeURIComponent(ticket)}`)
  // 'order'는 상태변경/자동취소를 포함한 범용 갱신 신호, 'newOrder'는 실제 결제 확정 시에만 온다.
  eventSource.addEventListener('order', () => listeners.order.forEach(fn => fn()))
  eventSource.addEventListener('newOrder', notifyNewOrder)
  eventSource.addEventListener('notice', () => listeners.notice.forEach(fn => fn()))
  // EventSource는 연결이 끊기면 브라우저가 알아서 재연결을 시도한다 - 별도 처리 불필요.
  // (티켓은 1분 만료라 재연결 시 새로 안 받으면 계속 실패한다 - onerror에서 다시 connect()한다.)
  // 티켓 만료(401) 등으로 연결 자체가 거부되면 브라우저는 CLOSED로 두고 자동 재시도를 안 한다
  // (한 번이라도 연결에 성공했다 끊긴 경우만 브라우저가 알아서 재시도함) - 그 경우만 새 티켓으로
  // 직접 재연결한다. 백엔드가 계속 죽어있는 상황에서 바로 재시도를 반복하지 않도록 3초 지연을 둔다.
  eventSource.onerror = () => {
    if (eventSource?.readyState === EventSource.CLOSED && subscribers > 0) {
      setTimeout(() => { if (subscribers > 0) connect() }, 3000)
    }
  }
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
