package com.kiosk.domain.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findFirstByBranch_BranchIdOrderByAdminIdAsc(Long branchId);
    Optional<Admin> findFirstByOrderByAdminIdAsc();
    Optional<Admin> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    Optional<Admin> findByEmail(String email);
}
