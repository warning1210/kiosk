package com.kiosk.domain.branchapplication;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BranchApplicationRepository extends JpaRepository<BranchApplication, Long> {
    List<BranchApplication> findAllByOrderByAppliedAtDesc();
    Optional<BranchApplication> findByInviteToken(String inviteToken);
}
