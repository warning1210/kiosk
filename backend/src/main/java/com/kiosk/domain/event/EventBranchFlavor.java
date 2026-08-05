package com.kiosk.domain.event;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
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
public class EventBranchFlavor {

    private Long eventBranchFlavorId;

    private Event event;

    private Branch branch;

    private Flavor flavor;

    private LocalDateTime createdAt;

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
