package com.kiosk.hq.notice.dto;

import com.kiosk.domain.notice.Notice;
import java.time.LocalDateTime;

public record HqNoticeResponse(
        Long noticeId,
        String title,
        String content,
        String imageUrl,
        String status,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HqNoticeResponse from(Notice notice) {
        return new HqNoticeResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImageUrl(),
                notice.getStatus().name(),
                notice.getAuthorAdmin() == null ? null : notice.getAuthorAdmin().getName(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
