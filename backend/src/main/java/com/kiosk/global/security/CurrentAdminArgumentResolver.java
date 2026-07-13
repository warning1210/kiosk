package com.kiosk.global.security;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AccountStatus;
import com.kiosk.domain.admin.AdminRepository;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * 임시 스캐폴딩: {@code X-Admin-Id} 헤더를 읽어 {@link Admin}을 조회한다.
 * 실제 인증이 도입되면 이 리졸버만 SecurityContext 기반으로 교체하면 된다.
 */
@Component
public class CurrentAdminArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_NAME = "X-Admin-Id";

    private final AdminRepository adminRepository;

    public CurrentAdminArgumentResolver(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAdmin.class)
                && Admin.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(HEADER_NAME);
        if (header == null || header.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, HEADER_NAME + " 헤더가 필요합니다");
        }

        Long adminId;
        try {
            adminId = Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, HEADER_NAME + " 값이 올바르지 않습니다");
        }

        Admin admin = adminRepository.findByIdWithBranch(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "존재하지 않는 관리자입니다"));

        if (admin.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비활성화된 관리자 계정입니다");
        }

        return admin;
    }
}
