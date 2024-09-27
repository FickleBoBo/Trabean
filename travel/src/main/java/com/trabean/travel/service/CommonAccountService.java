package com.trabean.travel.service;

import com.trabean.travel.callApi.client.AccountClient;
import com.trabean.travel.callApi.client.ForeignCurrencyClient;
import com.trabean.travel.callApi.dto.request.AccountBalanceApiRequestDto;
import com.trabean.travel.callApi.dto.request.AccountNumberApiRequestDto;
import com.trabean.travel.callApi.dto.response.AccountBalanceApiResponseDto;
import com.trabean.travel.callApi.dto.response.AccountNumberApiResponseDto;
import com.trabean.util.RequestHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonAccountService {

    private final AccountClient accountClient;
    private final ForeignCurrencyClient foreignCurrencyClient;

    private String userKey = "9e10349e-91e9-474d-afb4-564b24178d9f";

    /**
     * 계좌번호 조회
     */
    public String getAccountNo(Long accountId) {
        AccountNumberApiResponseDto accountNumberApiResponseDto = accountClient.getAccount(
                new AccountNumberApiRequestDto(accountId));
        return accountNumberApiResponseDto.getAccountNo();
    }

    /**
     * 외화 계좌 잔액 조회
     */
    public Double getForeignAccountBalance(String accountNo) {
        AccountBalanceApiRequestDto accountBalanceApiRequestDto = new AccountBalanceApiRequestDto(
                RequestHeader.builder()
                        .apiName("inquireForeignCurrencyDemandDepositAccountBalance")
                        .userKey(userKey)
                        .build(),
                accountNo);

        AccountBalanceApiResponseDto accountBalanceApiResponseDto
                = foreignCurrencyClient.getForeignAccountBalance(accountBalanceApiRequestDto);

        return (double) accountBalanceApiResponseDto.getRec().getAccountBalance();
    }

}