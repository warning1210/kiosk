package com.kiosk.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 임시 스캐폴딩: 실제 인증(JWT/Spring Security) 도입 전까지, 요청 헤더({@code X-Admin-Id})로
 * 전달된 관리자를 컨트롤러 파라미터에 주입한다. 실제 인증이 도입되면
 * {@link CurrentAdminArgumentResolver} 내부만 SecurityContext 기반으로 교체하면 되고,
 * 이 애노테이션을 사용하는 컨트롤러 시그니처는 그대로 유지된다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentAdmin {
}
