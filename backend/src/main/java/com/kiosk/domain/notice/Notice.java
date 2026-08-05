package com.kiosk.domain.notice;

import com.kiosk.domain.admin.Admin;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {

    private Long noticeId;

    private String title;

    private String content;

    private String imageUrl;

    @Builder.Default
    private NoticeStatus status = NoticeStatus.DRAFT;

    private Admin authorAdmin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
