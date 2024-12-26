package com.trabean.account.controller;

import com.trabean.account.dto.request.foreignTravelAccount.CreateForeignTravelAccountRequestDTO;
import com.trabean.account.dto.response.foreignTravelAccount.CreateForeignTravelAccountResponseDTO;
import com.trabean.account.dto.response.foreignTravelAccount.ForeignTravelAccountCreatedDateResponseDTO;
import com.trabean.account.dto.response.foreignTravelAccount.TravelAccountCoupleResponseDTO;
import com.trabean.account.service.ForeignTravelAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/accounts/travel/foreign")
public class ForeignTravelAccountController {

    private final ForeignTravelAccountService foreignTravelAccountService;

    // 외화 여행통장 생성 API
    @PostMapping
    public ResponseEntity<CreateForeignTravelAccountResponseDTO> createForeignTravelAccount(@RequestBody CreateForeignTravelAccountRequestDTO requestDTO) {
        CreateForeignTravelAccountResponseDTO responseDTO = foreignTravelAccountService.createForeignTravelAccount(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    // 외화 여행통장 생성일 조회 API
    @GetMapping("/{accountId}/created")
    public ResponseEntity<ForeignTravelAccountCreatedDateResponseDTO> getForeignTravelAccountCreatedDate(@PathVariable Long accountId) {
        ForeignTravelAccountCreatedDateResponseDTO responseDTO = foreignTravelAccountService.getForeignTravelAccountCreatedDate(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    // 외화 여행통장 및 연결된 한화 여행통장 식별자와 계좌번호 조회 API
    @GetMapping("/{accountId}/couple")
    public ResponseEntity<TravelAccountCoupleResponseDTO> getTravelAccountCoupleResponseDTO(@PathVariable Long accountId) {
        TravelAccountCoupleResponseDTO responseDTO = foreignTravelAccountService.getTravelAccountCoupleResponseDTO(accountId);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }
}
