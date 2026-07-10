package com.example.kiosksim.repository;

import com.example.kiosksim.domain.KioskOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KioskOrderRepository extends JpaRepository<KioskOrder, Long> {
}
