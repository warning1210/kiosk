package com.kiosk.domain.notice;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeRepository {
    List<Notice> findByStatus(NoticeStatus status);
    List<Notice> findAll();
    java.util.Optional<Notice> findById(Long id);
    int insert(Notice notice);
    int update(Notice notice);
    default Notice save(Notice notice) { if (notice.getNoticeId() == null) insert(notice); else update(notice); return notice; }
}
