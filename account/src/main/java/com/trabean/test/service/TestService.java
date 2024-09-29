package com.trabean.test.service;

import com.trabean.common.SsafySuccessResponseDTO;
import com.trabean.external.ssafy.domestic.client.DomesticClient;
import com.trabean.external.ssafy.domestic.dto.request.UpdateDemandDepositAccountDepositRequestDTO;
import com.trabean.external.ssafy.domestic.dto.response.UpdateDemandDepositAccountDepositResponseDTO;
import com.trabean.external.ssafy.memo.client.MemoClient;
import com.trabean.external.ssafy.memo.dto.request.TransactionMemoRequestDTO;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.test.dto.request.DepositRequestDTO;
import com.trabean.util.RequestHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestService {

    private final DomesticClient domesticClient;
    private final MemoClient memoClient;

    // 계좌 입금 서비스 로직
    public SsafySuccessResponseDTO deposit(DepositRequestDTO requestDTO) {

        // SSAFY 금융 API 계좌 입금 요청
        UpdateDemandDepositAccountDepositRequestDTO updateDemandDepositAccountDepositRequestDTO = UpdateDemandDepositAccountDepositRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName("updateDemandDepositAccountDeposit")
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionBalance(requestDTO.getTransactionBalance())
                .transactionSummary(requestDTO.getTransactionSummary())
                .build();
        UpdateDemandDepositAccountDepositResponseDTO updateDemandDepositAccountDepositResponseDTO = domesticClient.updateDemandDepositAccountDeposit(updateDemandDepositAccountDepositRequestDTO);

        // SSAFY 금융 API 거래내역 메모 요청
        TransactionMemoRequestDTO transactionMemoRequestDTO = TransactionMemoRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName("transactionMemo")
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionUniqueNo(updateDemandDepositAccountDepositResponseDTO.getRec().getTransactionUniqueNo())
                .transactionMemo(String.valueOf(UserHeaderInterceptor.userId.get()))
                .build();
        memoClient.transactionMeno(transactionMemoRequestDTO);

        return SsafySuccessResponseDTO.builder()
                .responseCode(updateDemandDepositAccountDepositResponseDTO.getHeader().getResponseCode())
                .responseMessage(updateDemandDepositAccountDepositResponseDTO.getHeader().getResponseMessage())
                .build();
    }

}
