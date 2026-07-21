package com.kiosk.branch.notice.dto;

import com.kiosk.domain.event.Event;
import com.kiosk.domain.notice.Notice;
import java.time.LocalDateTime;

// notice(공지)와 event(이벤트/프로모션)를 지점 대시보드에서 하나의 공지 목록으로 합쳐 보여주기 위한 통합 응답
public record BranchNoticeResponse(
        String noticeType, // NOTICE | EVENT
        Long id,
        String title,
        String content,
        String imageUrl,
        LocalDateTime postedAt,
        LocalDateTime endAt // NOTICE는 기간이 없어 항상 null
) {
    public static BranchNoticeResponse fromNotice(Notice notice) {
        return new BranchNoticeResponse("NOTICE", notice.getNoticeId(), notice.getTitle(), notice.getContent(),
                notice.getImageUrl(), notice.getCreatedAt(), null);
    }

    public static BranchNoticeResponse fromEvent(Event event) {
        return new BranchNoticeResponse("EVENT", event.getEventId(), event.getEventName(), event.getDescription(),
                event.getImageUrl(), event.getStartAt(), event.getEndAt());
    }
}
