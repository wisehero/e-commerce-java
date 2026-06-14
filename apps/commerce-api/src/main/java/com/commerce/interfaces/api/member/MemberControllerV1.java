package com.commerce.interfaces.api.member;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce.application.member.MemberInfo;
import com.commerce.application.member.MemberSignUpUseCase;
import com.commerce.interfaces.api.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberControllerV1 {

    private final MemberSignUpUseCase memberSignUpUseCase;

    @Operation(summary = "회원 가입")
    @PostMapping
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        MemberInfo info = memberSignUpUseCase.signUp(request.toCommand());
        return ApiResponse.success(SignUpResponse.from(info));
    }
}
