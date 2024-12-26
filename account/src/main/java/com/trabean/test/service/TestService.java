package com.trabean.test.service;

import com.trabean.external.msa.notification.client.NotificationClient;
import com.trabean.external.msa.notification.dto.request.NotificationRequestDTO;
import com.trabean.external.ssafy.api.domestic.client.DomesticClient;
import com.trabean.external.ssafy.api.domestic.dto.request.UpdateDemandDepositAccountDepositRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.UpdateDemandDepositAccountWithdrawalRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.UpdateDemandDepositAccountDepositResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.UpdateDemandDepositAccountWithdrawalResponseDTO;
import com.trabean.external.ssafy.api.memo.client.MemoClient;
import com.trabean.external.ssafy.api.memo.dto.request.TransactionMemoRequestDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTOFactory;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.test.dto.request.DepositRequestDTO;
import com.trabean.test.dto.request.WithdrawalRequestDTO;
import com.trabean.external.ssafy.util.RequestHeader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.DEPOSIT;
import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.WITHDRAW;
import static com.trabean.external.ssafy.constant.ApiName.*;

@Service
@RequiredArgsConstructor
public class TestService {

    private final DomesticClient domesticClient;
    private final MemoClient memoClient;

    private final NotificationClient notificationClient;

    // 계좌 입금(테스트용) 서비스 로직
    public SsafyApiResponseDTO depositTest(DepositRequestDTO requestDTO) {

        // SSAFY 금융 API 계좌 입금 요청
        UpdateDemandDepositAccountDepositRequestDTO updateDemandDepositAccountDepositRequestDTO = UpdateDemandDepositAccountDepositRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(updateDemandDepositAccountDeposit)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionBalance(requestDTO.getTransactionBalance())
                .transactionSummary(requestDTO.getTransactionSummary())
                .build();
        UpdateDemandDepositAccountDepositResponseDTO updateDemandDepositAccountDepositResponseDTO = domesticClient.updateDemandDepositAccountDeposit(updateDemandDepositAccountDepositRequestDTO);

        // SSAFY 금융 API 거래내역 메모 요청 (입금 메모)
        TransactionMemoRequestDTO transactionMemoRequestDTO = TransactionMemoRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(transactionMemo)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionUniqueNo(updateDemandDepositAccountDepositResponseDTO.getRec().getTransactionUniqueNo())
                .transactionMemo("-1")
                .build();
        memoClient.transactionMemo(transactionMemoRequestDTO);

        // Notification 서버 입출금 시 알림 생성 요청
        NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                    .senderId(-1L)
                    .receiverIdList(List.of(UserHeaderInterceptor.userId.get()))
                    .accountId(requestDTO.getAccountId())
                    .notificationType(DEPOSIT)
                    .amount(requestDTO.getTransactionBalance())
                .build();
        notificationClient.sendNotification(notificationRequestDTO);

        return SsafyApiResponseDTOFactory.create(updateDemandDepositAccountDepositResponseDTO.getHeader());
    }

    // 계좌 출금(테스트용) 서비스 로직
    public SsafyApiResponseDTO withdrawalTest(WithdrawalRequestDTO requestDTO) {

        // SSAFY 금융 API 계좌 출금 요청 (출금 메모)
        UpdateDemandDepositAccountWithdrawalRequestDTO updateDemandDepositAccountWithdrawalRequestDTO = UpdateDemandDepositAccountWithdrawalRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(updateDemandDepositAccountWithdrawal)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionBalance(requestDTO.getTransactionBalance())
                .transactionSummary(requestDTO.getTransactionSummary())
                .build();
        UpdateDemandDepositAccountWithdrawalResponseDTO updateDemandDepositAccountWithdrawalResponseDTO = domesticClient.updateDemandDepositAccountWithdrawal(updateDemandDepositAccountWithdrawalRequestDTO);

        // SSAFY 금융 API 거래내역 메모 요청
        TransactionMemoRequestDTO transactionMemoRequestDTO = TransactionMemoRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(transactionMemo)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .transactionUniqueNo(updateDemandDepositAccountWithdrawalResponseDTO.getRec().getTransactionUniqueNo())
                .transactionMemo(String.valueOf(UserHeaderInterceptor.userId.get()))
                .build();
        memoClient.transactionMemo(transactionMemoRequestDTO);

        // Notification 서버 입출금 시 알림 생성 요청
        NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                .senderId(-1L)
                .receiverIdList(List.of(UserHeaderInterceptor.userId.get()))
                .accountId(requestDTO.getAccountId())
                .notificationType(WITHDRAW)
                .amount(requestDTO.getTransactionBalance())
                .build();
        notificationClient.sendNotification(notificationRequestDTO);

        return SsafyApiResponseDTOFactory.create(updateDemandDepositAccountWithdrawalResponseDTO.getHeader());
    }

}
