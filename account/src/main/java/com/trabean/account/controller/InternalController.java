package com.trabean.account.controller;

import com.trabean.account.dto.request.internal.*;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.account.dto.response.internal.AccountNoResponseDTO;
import com.trabean.account.dto.response.internal.AdminUserKeyResponseDTO;
import com.trabean.account.dto.response.internal.TravelAccountMembersResponseDTO;
import com.trabean.account.dto.response.internal.UserRoleResponseDTO;
import com.trabean.account.service.InternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/accounts/internal")
public class InternalController {

    private final InternalService internalService;

    // 통장 계좌번호 조회 API
    @PostMapping("/get-accountNo")
    public ResponseEntity<AccountNoResponseDTO> getAccountNo(@RequestBody AccountNoRequestDTO requestDTO) {
        AccountNoResponseDTO responseDTO = internalService.getAccountNo(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 통장 권한 조회 API
    @PostMapping("/get-userRole")
    public ResponseEntity<UserRoleResponseDTO> getUserRole(@RequestBody UserRoleRequestDTO requestDTO) {
        UserRoleResponseDTO responseDTO = internalService.getUserRole(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 여행통장 결제 권한 변경 API
    @PutMapping("/update-userRole")
    public ResponseEntity<InternalServerSuccessResponseDTO> updateUserRole(@RequestBody UpdateUserRoleRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = internalService.updateUserRole(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 결제 비밀번호 검증 API
    @PostMapping("/verify-password")
    public ResponseEntity<InternalServerSuccessResponseDTO> verifyPassword(@RequestBody VerifyPasswordRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = internalService.verifyPassword(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 여행통장 가입 API
    @PostMapping("join-member")
    public ResponseEntity<InternalServerSuccessResponseDTO> joinTravelAccount(@RequestBody JoinTravelAccountRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = internalService.joinTravelAccount(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 통장 주인의 userKey 조회 API
    @PostMapping("/get-admin-userKey")
    public ResponseEntity<AdminUserKeyResponseDTO> getAdminUserKey(@RequestBody AdminUserKeyRequestDTO requestDTO) {
        AdminUserKeyResponseDTO responseDTO = internalService.getAdminUserKey(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 여행통장 비밀번호 변경 API
    @PutMapping("/update-travel-account-password")
    public ResponseEntity<InternalServerSuccessResponseDTO> updateTravelAccountPassword(@RequestBody UpdateTravelAccountPasswordRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = internalService.updateTravelAccountPassword(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 멤버 목록 조회 API
    @PostMapping("/get-travel-account-members")
    public ResponseEntity<TravelAccountMembersResponseDTO> getTravelAccountMembers(@RequestBody TravelAccountMembersRequestDTO requestDTO) {
        TravelAccountMembersResponseDTO responseDTO = internalService.getTravelAccountMembers(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}
