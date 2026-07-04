package com.commerce.interfaces.api.security;

import java.security.Principal;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.commerce.support.error.CoreException;
import com.commerce.support.error.ErrorType;

@Component
public class AuthenticatedMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AuthenticatedMember.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = currentAuthentication(webRequest);
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "인증 정보가 없습니다.");
        }
        return new AuthenticatedMember(extractMemberId(authentication));
    }

    private Authentication currentAuthentication(NativeWebRequest webRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication;
        }

        Principal principal = webRequest.getUserPrincipal();
        if (principal instanceof Authentication webAuthentication) {
            return webAuthentication;
        }
        return null;
    }

    private Long extractMemberId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedMember authenticatedMember) {
            return authenticatedMember.memberId();
        }
        if (principal instanceof Jwt jwt) {
            return parseMemberId(jwt.getSubject());
        }
        return parseMemberId(authentication.getName());
    }

    private Long parseMemberId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "인증 회원 ID가 올바르지 않습니다.");
        }
    }
}
