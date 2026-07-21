package com.kiosk.domain.event;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStatus(EventStatus status);

    List<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status);
}
