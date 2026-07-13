package com.kiosk.domain.branch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findFirstByOrderByBranchIdAsc();
}
