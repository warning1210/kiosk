package com.kiosk.domain.admin;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findFirstByBranch_BranchIdOrderByAdminIdAsc(Long branchId);
    Optional<Admin> findFirstByOrderByAdminIdAsc();
    // 모든 지점장 계정을 생성 순서대로 조회한다.
    List<Admin> findByRoleOrderByAdminIdAsc(AdminRole role);
}
