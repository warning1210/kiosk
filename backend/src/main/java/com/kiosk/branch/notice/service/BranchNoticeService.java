package com.kiosk.branch.notice.service;

import com.kiosk.branch.notice.dto.BranchNoticeResponse;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.notice.NoticeRepository;
import com.kiosk.domain.notice.NoticeStatus;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지점 대시보드 공지사항 - notice(PUBLISHED) + event(ACTIVE이면서 지금이 start_at~end_at 사이)를 합친다.
// 별도 배치 없이 조회 시점에 기간을 걸러내므로, end_at이 지나면 다음 요청부터 자동으로 빠진다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchNoticeService {

    private final NoticeRepository noticeRepository;
    private final EventRepository eventRepository;

    public List<BranchNoticeResponse> list() {
        LocalDateTime now = LocalDateTime.now();

        Stream<BranchNoticeResponse> notices = noticeRepository.findByStatus(NoticeStatus.PUBLISHED).stream()
                .map(BranchNoticeResponse::fromNotice);

        Stream<BranchNoticeResponse> events = eventRepository.findByStatus(EventStatus.ACTIVE).stream()
                .filter(event -> !now.isBefore(event.getStartAt()) && !now.isAfter(event.getEndAt()))
                .map(BranchNoticeResponse::fromEvent);

        return Stream.concat(notices, events)
                .sorted(Comparator.comparing(BranchNoticeResponse::postedAt).reversed())
                .toList();
    }
}
