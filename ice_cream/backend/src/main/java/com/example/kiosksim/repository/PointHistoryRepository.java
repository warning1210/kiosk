package com.example.kiosksim.repository;

import com.example.kiosksim.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
