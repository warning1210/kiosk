package com.kiosk.domain.branchapplication;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchApplicationRepository extends JpaRepository<BranchApplication, Long> {
    List<BranchApplication> findAllByOrderByAppliedAtDesc();
    Optional<BranchApplication> findByInviteToken(String inviteToken);
    boolean existsByEmailAndApprovalStatusIn(String email, List<ApprovalStatus> statuses);
}
