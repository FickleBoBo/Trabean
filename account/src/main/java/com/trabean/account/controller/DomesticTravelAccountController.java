package com.trabean.account.controller;

import com.trabean.account.dto.request.domesticTravelAccount.CreateDomesticTravelAccountRequestDTO;
import com.trabean.account.dto.request.domesticTravelAccount.TransferDomesticTravelAccountRequestDTO;
import com.trabean.account.dto.request.common.VerifyAccountPasswordRequestDTO;
import com.trabean.account.dto.response.domesticTravelAccount.*;
import com.trabean.account.service.DomesticTravelAccountService;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/accounts/travel/domestic")
public class DomesticTravelAccountController {

    private final DomesticTravelAccountService domesticTravelAccountService;

    // 한화 여행통장 생성 API
    @PostMapping
    public ResponseEntity<SsafyApiResponseDTO> createDomesticTravelAccount(@RequestBody CreateDomesticTravelAccountRequestDTO requestDTO) {
        SsafyApiResponseDTO responseDTO = domesticTravelAccountService.createDomesticTravelAccount(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // 한화 여행통장 상세 조회 API
    @GetMapping("/{accountId}")
    public ResponseEntity<DomesticTravelAccountDetailResponseDTO> getDomesticTravelAccountDetail(@PathVariable Long accountId,
                                                                                                 @RequestParam(defaultValue = "19000101") String startDate,
                                                                                                 @RequestParam(defaultValue = "21000101") String endDate,
                                                                                                 @RequestParam(defaultValue = "A") String transactionType,
                                                                                                 @RequestParam(defaultValue = "-1") String selectedUserId) {
        DomesticTravelAccountDetailResponseDTO responseDTO = domesticTravelAccountService.getDomesticTravelAccountDetail(accountId, startDate, endDate, transactionType, selectedUserId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 생성일 조회 API
    @GetMapping("/{accountId}/created")
    public ResponseEntity<DomesticTravelAccountCreatedDateResponseDTO> getDomesticTravelAccountCreatedDate(@PathVariable Long accountId) {
        DomesticTravelAccountCreatedDateResponseDTO responseDTO = domesticTravelAccountService.getDomesticTravelAccountCreatedDate(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 잔액 조회 API
    @GetMapping("/{accountId}/accountBalance")
    public ResponseEntity<DomesticTravelAccountBalanceResponseDTO> getDomesticTravelAccountBalance(@PathVariable Long accountId) {
        DomesticTravelAccountBalanceResponseDTO responseDTO = domesticTravelAccountService.getDomesticTravelAccountBalance(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 계좌 이체 API
    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<SsafyApiResponseDTO> transferDomesticTravelAccount(@PathVariable Long accountId,
                                                                             @RequestBody TransferDomesticTravelAccountRequestDTO requestDTO) {
        SsafyApiResponseDTO responseDTO = domesticTravelAccountService.transferDomesticTravelAccount(accountId, requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 계좌 이체 비밀번호 검증 API
    @PostMapping("/{accountId}/verify")
    public ResponseEntity<InternalServerSuccessResponseDTO> verifyDomesticTravelAccountPassword(@PathVariable Long accountId,
                                                                                                @RequestBody VerifyAccountPasswordRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = domesticTravelAccountService.verifyDomesticTravelAccountPassword(accountId, requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 한화 여행통장 멤버 목록 조회 API (민채)
    @GetMapping("/{accountId}/members")
    public ResponseEntity<DomesticTravelAccountMemberListResponseDTO> getDomesticTravelAccountMemberList(@PathVariable Long accountId) {
        DomesticTravelAccountMemberListResponseDTO responseDTO = domesticTravelAccountService.getDomesticTravelAccountMemberList(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 통장 권한 조회 API
    @GetMapping("/{accountId}/userRole")
    public ResponseEntity<DomesticTravelAccountUserRoleResponseDTO> getDomesticTravelAccountUserRole(@PathVariable Long accountId) {
        DomesticTravelAccountUserRoleResponseDTO responseDTO = domesticTravelAccountService.getDomesticTravelAccountUserRole(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}
