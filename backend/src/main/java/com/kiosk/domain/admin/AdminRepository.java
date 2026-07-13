package com.kiosk.domain.admin;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    @Query("""
            SELECT a FROM Admin a
            LEFT JOIN FETCH a.branch
            WHERE a.accountStatus = :accountStatus
            ORDER BY a.role, a.name
            """)
    List<Admin> findByAccountStatusOrderByRoleAscNameAsc(@Param("accountStatus") AccountStatus accountStatus);

    /**
     * {@code branch}까지 즉시 로딩한다. 이 Admin은 {@code CurrentAdminArgumentResolver}에서
     * 컨트롤러 서비스 메서드의 트랜잭션과 별개로 조회되므로, 지연 연관관계를 그대로 두면
     * 이후 서비스 트랜잭션에서 세션이 달라 LazyInitializationException이 발생한다.
     */
    @Query("""
            SELECT a FROM Admin a
            LEFT JOIN FETCH a.branch
            WHERE a.adminId = :adminId
            """)
    Optional<Admin> findByIdWithBranch(@Param("adminId") Long adminId);
}
