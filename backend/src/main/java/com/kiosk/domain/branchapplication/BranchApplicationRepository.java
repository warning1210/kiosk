package com.kiosk.domain.branchapplication;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchApplicationRepository extends JpaRepository<BranchApplication, Long> {
    List<BranchApplication> findAllByOrderByAppliedAtDesc();
    Optional<BranchApplication> findByInviteToken(String inviteToken);
    boolean existsByEmailAndApprovalStatusIn(String email, List<ApprovalStatus> statuses);
    // 같은 이메일의 가장 최근 초대 또는 신청을 찾아 재발급 여부를 판단한다.
    Optional<BranchApplication> findFirstByEmailOrderByCreatedAtDesc(String email);
}
