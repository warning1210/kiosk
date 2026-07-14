package com.kiosk.domain.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link InventoryTransaction} 재고 이력을 저장하고 PK로 조회하는 Spring Data JPA 저장소이다.
 *
 * <p>현재 재고 신청 기능에는 별도의 조건 검색이 필요하지 않아 메서드를 추가하지 않았다. 빈 인터페이스처럼
 * 보여도 {@link JpaRepository}를 상속하므로 save, findById, findAll 같은 기본 구현을 사용할 수 있다.
 */
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}
