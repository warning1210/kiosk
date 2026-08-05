package com.kiosk.domain.auditlog;

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
public class AuditLog {

    private Long auditLogId;

    private String entityType;

    private Long entityId;

    private Admin admin;

    private String action;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String reason;

    private LocalDateTime changedAt;

    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
