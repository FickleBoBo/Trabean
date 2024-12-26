package com.trabean.account.controller;

import com.trabean.account.dto.request.personalAccount.CreatePersonalAccountRequestDTO;
import com.trabean.account.dto.request.personalAccount.TransferPersonalAccountRequestDTO;
import com.trabean.account.dto.request.common.VerifyAccountPasswordRequestDTO;
import com.trabean.account.dto.response.personalAccount.PersonalAccountCreatedDateResponseDTO;
import com.trabean.account.dto.response.personalAccount.PersonalAccountDetailResponseDTO;
import com.trabean.account.service.PersonalAccountService;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/accounts/personal")
public class PersonalAccountController {

    private final PersonalAccountService personalAccountService;

    // 개인 통장 생성 API
    @PostMapping
    public ResponseEntity<SsafyApiResponseDTO> createPersonalAccount(@RequestBody CreatePersonalAccountRequestDTO requestDTO) {
        SsafyApiResponseDTO responseDTO = personalAccountService.createPersonalAccount(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // 개인 통장 상세 조회 API
    @GetMapping("/{accountId}")
    public ResponseEntity<PersonalAccountDetailResponseDTO> getPersonalAccountDetail(@PathVariable Long accountId,
                                                                                     @RequestParam(defaultValue = "19000101") String startDate,
                                                                                     @RequestParam(defaultValue = "21000101") String endDate,
                                                                                     @RequestParam(defaultValue = "A") String transactionType) {
        PersonalAccountDetailResponseDTO responseDTO = personalAccountService.getPersonalAccountDetail(accountId, startDate, endDate, transactionType);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 개인 통장 생성일 조회 API
    @GetMapping("/{accountId}/created")
    public ResponseEntity<PersonalAccountCreatedDateResponseDTO> getPersonalAccountCreatedDate(@PathVariable Long accountId) {
        PersonalAccountCreatedDateResponseDTO responseDTO = personalAccountService.getPersonalAccountCreatedDate(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 개인 통장 계좌 이체 API
    @PostMapping("/{accountId}/transfer")
    public ResponseEntity<SsafyApiResponseDTO> transferPersonalAccount(@PathVariable Long accountId,
                                                                       @RequestBody TransferPersonalAccountRequestDTO requestDTO) {
        SsafyApiResponseDTO responseDTO = personalAccountService.transferPersonalAccount(accountId, requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 개인 통장 계좌 이체 비밀번호 검증 API
    @PostMapping("/{accountId}/verify")
    public ResponseEntity<InternalServerSuccessResponseDTO> verifyPersonalAccountPassword(@PathVariable Long accountId,
                                                                                          @RequestBody VerifyAccountPasswordRequestDTO requestDTO) {
        InternalServerSuccessResponseDTO responseDTO = personalAccountService.verifyPersonalAccountPassword(accountId, requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}
