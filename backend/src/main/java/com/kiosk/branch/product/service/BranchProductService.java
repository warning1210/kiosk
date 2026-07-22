package com.kiosk.branch.product.service;

import com.kiosk.branch.product.dto.BranchProductResponse;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.product.BranchProduct;
import com.kiosk.domain.product.BranchProductRepository;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchProductService {

    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchProductResponse> list(Long branchId) {
        List<Product> products = productRepository.findAll();
        Map<Long, BranchProduct> branchProducts = branchProductRepository.findByBranch_BranchId(branchId).stream()
                .collect(Collectors.toMap(bp -> bp.getProduct().getProductId(), Function.identity()));

        return products.stream()
                .sorted(Comparator.comparing(Product::getProductId).reversed())
                .map(product -> {
                    BranchProduct bp = branchProducts.get(product.getProductId());
                    return BranchProductResponse.from(product, bp != null ? bp.getIsVisible() : null);
                })
                .toList();
    }

    public BranchProductResponse toggleVisibility(Long branchId, Long productId, Boolean isVisible) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        BranchProduct branchProduct = branchProductRepository.findByBranch_BranchIdAndProduct_ProductId(branchId, productId)
                .orElseGet(() -> {
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));
                    return BranchProduct.builder()
                            .branch(branch)
                            .product(product)
                            .build();
                });

        branchProduct.setIsVisible(isVisible != null ? isVisible : true);
        branchProduct = branchProductRepository.save(branchProduct);

        return BranchProductResponse.from(product, branchProduct.getIsVisible());
    }
}
