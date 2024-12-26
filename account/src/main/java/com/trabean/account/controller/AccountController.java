package com.trabean.account.controller;

import com.trabean.account.dto.request.account.UpdateAccountTransferLimitRequestDTO;
import com.trabean.account.dto.response.account.AccountAdminNameResponseDTO;
import com.trabean.account.dto.response.account.AccountListResponseDTO;
import com.trabean.account.dto.response.account.RecentTransactionListResponseDTO;
import com.trabean.account.service.AccountService;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    // 통장 목록 조회 APi
    @GetMapping
    public ResponseEntity<AccountListResponseDTO> getAccountList() {
        AccountListResponseDTO responseDTO = accountService.getAccountList();
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 최근 이체 목록 조회 APi
    @GetMapping("/{accountId}/transfer")
    public ResponseEntity<RecentTransactionListResponseDTO> getRecentTransactionList(@PathVariable Long accountId,
                                                                                     @RequestParam(defaultValue = "19000101") String startDate,
                                                                                     @RequestParam(defaultValue = "21000101") String endDate) {
        RecentTransactionListResponseDTO responseDTO = accountService.getRecentTransactionList(accountId, startDate, endDate);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 계좌 이체 한도 변경 API
    @PutMapping("/{accountId}/transfer")
    public ResponseEntity<SsafyApiResponseDTO> updateTransferLimit(@PathVariable Long accountId,
                                                                   @RequestBody UpdateAccountTransferLimitRequestDTO requestDTO) {
        SsafyApiResponseDTO responseDTO = accountService.updateTransferLimit(accountId, requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 통장 주인 이름 조회 API
    @GetMapping("/{accountNo}/name")
    public ResponseEntity<AccountAdminNameResponseDTO> getAccountName(@PathVariable String accountNo) {
        AccountAdminNameResponseDTO responseDTO = accountService.getAccountName(accountNo);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}
