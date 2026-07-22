package com.kiosk.hq.notice.service;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.notice.Notice;
import com.kiosk.domain.notice.NoticeRepository;
import com.kiosk.domain.notice.NoticeStatus;
import com.kiosk.hq.notice.dto.HqNoticeResponse;
import com.kiosk.hq.notice.dto.HqNoticeUpsertRequest;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HqNoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<HqNoticeResponse> list() {
        return noticeRepository.findAll().stream()
                .sorted(Comparator.comparing(Notice::getNoticeId).reversed())
                .map(HqNoticeResponse::from)
                .toList();
    }

    public HqNoticeResponse create(HqNoticeUpsertRequest request, Admin author) {
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }

        Notice notice = Notice.builder()
                .title(request.title())
                .content(request.content())
                .imageUrl(request.imageUrl())
                .status(parseStatus(request.status()))
                .authorAdmin(author)
                .build();

        return HqNoticeResponse.from(noticeRepository.save(notice));
    }

    public HqNoticeResponse update(Long noticeId, HqNoticeUpsertRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }

        notice.setTitle(request.title());
        notice.setContent(request.content());
        notice.setImageUrl(request.imageUrl());
        notice.setStatus(parseStatus(request.status()));

        return HqNoticeResponse.from(notice);
    }

    private NoticeStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return NoticeStatus.DRAFT;
        }
        try {
            return NoticeStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("올바르지 않은 상태입니다: " + value);
        }
    }
}
