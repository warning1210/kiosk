package com.kiosk.hq.stockrequest;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class StockRequestSpecifications {

    private StockRequestSpecifications() {
    }

    static Specification<StockRequest> filter(StockRequestStatus status, Long branchId,
                                               LocalDateTime from, LocalDateTime to, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("requestStatus"), status));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("branchId"), branchId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"), to));
            }
            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.trim() + "%";
                jakarta.persistence.criteria.Join<StockRequest, Branch> branch = root.join("branch");
                predicates.add(cb.or(
                        cb.like(root.get("requestNumber"), likePattern),
                        cb.like(branch.get("branchName"), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
