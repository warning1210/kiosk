package com.kiosk.branch.dashboard.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// 지점 대시보드(useNewOrderAlert.js, DashboardView.vue)가 3초 폴링 대신 구독하는 SSE 브로드캐스터.
// 이벤트 내용은 신경 안 쓰고 "뭔가 바뀌었다"만 신호로 보낸다 - 프론트는 받으면 그냥 다시 조회(load)한다.
// ponytail: 단일 인스턴스 메모리 저장이라 서버를 여러 대로 늘리면(수평 확장) 다른 인스턴스에 붙은
// 클라이언트에는 이벤트가 안 간다. 지점 관리자용 내부 대시보드라 트래픽 규모상 지금은 충분하고,
// 필요해지면 Redis pub/sub 등으로 브로드캐스트 계층을 넣으면 된다.
@Slf4j
@Component
public class BranchEventPublisher {

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByBranch = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long branchId) {
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> emitters = emittersByBranch.computeIfAbsent(branchId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        Runnable remove = () -> emitters.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    /** 특정 지점(주문 결제 등)에게만 알린다. */
    public void publish(Long branchId, String eventName) {
        send(emittersByBranch.getOrDefault(branchId, new CopyOnWriteArrayList<>()), eventName);
    }

    /** 공지사항처럼 전 지점에 알려야 하는 이벤트. */
    public void broadcast(String eventName) {
        emittersByBranch.values().forEach(emitters -> send(emitters, eventName));
    }

    // 실제 이벤트가 한참 없으면(새벽 시간대 등) 죽은 연결이 다음 진짜 이벤트가 올 때까지 감지 안 되고
    // 계속 남아있는다. 그리고 nginx 등 중간 프록시는 보통 유휴 커넥션을 몇십 초 만에 끊어버리는데,
    // 그 전에 뭐라도 보내주지 않으면 프록시가 조용히 연결을 끊고 브라우저는 한참 뒤에야 재연결한다 -
    // 그래서 25초마다 빈 핑을 보내 죽은 연결은 여기서 걸러내고, 프록시 idle timeout도 피한다.
    @Scheduled(fixedRate = 25000)
    void heartbeat() {
        emittersByBranch.values().forEach(emitters -> send(emitters, "ping"));
    }

    private void send(List<SseEmitter> emitters, String eventName) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data("update"));
            } catch (Exception e) {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // 톰캣 내부에서 이미 에러 상태로 처리된 경우 IllegalStateException이 발생할 수 있으므로 무시한다.
                }
                emitters.remove(emitter);
            }
        }
    }
}
