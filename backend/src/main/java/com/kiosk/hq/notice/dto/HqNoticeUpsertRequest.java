package com.kiosk.hq.notice.dto;

public record HqNoticeUpsertRequest(
        String title,
        String content,
        String imageUrl,
        String status
) {
}
