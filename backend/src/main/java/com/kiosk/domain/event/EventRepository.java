package com.kiosk.domain.event;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventRepository {
    List<Event> findByStatus(EventStatus status);

    List<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status);

    List<Event> findByEventTypeInAndStatus(List<EventType> eventTypes, EventStatus status);
    List<Event> findAll();
    java.util.Optional<Event> findById(Long id);
    int insert(Event event);
    int update(Event event);
    default Event save(Event event) { if (event.getEventId() == null) insert(event); else update(event); return event; }
}
