package com.kiosk.global.staffcall;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// 키오스크 "직원 호출" 상태를 지점별로 메모리에만 들고 있는 저장소.
// DB에 남길 필요 없는 실시간 신호라 서버가 재시작되면 초기화된다.
@Component
public class StaffCallRegistry {

    private final Map<Long, LocalDateTime> calledAtByBranch = new ConcurrentHashMap<>();

    public void call(Long branchId) {
        calledAtByBranch.put(branchId, LocalDateTime.now());
    }

    public LocalDateTime getCalledAt(Long branchId) {
        return calledAtByBranch.get(branchId);
    }

    public void acknowledge(Long branchId) {
        calledAtByBranch.remove(branchId);
    }
}
