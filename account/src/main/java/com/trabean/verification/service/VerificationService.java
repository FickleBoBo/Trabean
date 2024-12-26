package com.trabean.verification.service;

import com.trabean.account.repository.AccountRepository;
import com.trabean.external.msa.notification.client.NotificationClient;
import com.trabean.external.msa.notification.dto.request.NotificationRequestDTO;
import com.trabean.external.ssafy.api.domestic.client.DomesticClient;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireTransactionHistoryRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireTransactionHistoryResponseDTO;
import com.trabean.external.ssafy.api.verification.client.VerificationClient;
import com.trabean.external.ssafy.api.verification.dto.request.CheckAuthCodeRequestDTO;
import com.trabean.external.ssafy.api.verification.dto.request.OpenAccountAuthRequestDTO;
import com.trabean.external.ssafy.api.verification.dto.response.CheckAuthCodeResponseDTO;
import com.trabean.external.ssafy.api.verification.dto.response.OpenAccountAuthResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTOFactory;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.external.ssafy.util.RequestHeader;
import com.trabean.util.ValidationUtil;
import com.trabean.verification.dto.request.AccountVerificationRequestDTO;
import com.trabean.verification.dto.request.OneWonVerificationRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.AUTH;
import static com.trabean.external.ssafy.constant.ApiName.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VerificationService {

    private final AccountRepository accountRepository;

    private final DomesticClient domesticClient;
    private final VerificationClient verificationClient;

    private final NotificationClient notificationClient;

    // 1원 인증(1원 송금) 서비스 로직
    public SsafyApiResponseDTO getAccountVerification(AccountVerificationRequestDTO requestDTO) {

        // SSAFY 금융 API 1원 송금 요청
        OpenAccountAuthRequestDTO openAccountAuthRequestDTO = OpenAccountAuthRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(openAccountAuth)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .build();
        OpenAccountAuthResponseDTO openAccountAuthResponseDTO = verificationClient.openAccountAuth(openAccountAuthRequestDTO);

        // SSAFY 금융 API 계좌 거래 내역 조회 (단건) 요청
        InquireTransactionHistoryRequestDTO inquireTransactionHistoryRequestDTO = InquireTransactionHistoryRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireTransactionHistory)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(openAccountAuthResponseDTO.getRec().getAccountNo())
                .transactionUniqueNo(openAccountAuthResponseDTO.getRec().getTransactionUniqueNo())
                .build();
        InquireTransactionHistoryResponseDTO inquireTransactionHistoryResponseDTO = domesticClient.inquireTransactionHistory(inquireTransactionHistoryRequestDTO);

        // Notification 서버 인증 번호 알림 생성 요청
        NotificationRequestDTO notificationRequestDTO = NotificationRequestDTO.builder()
                    .senderId(-1L)
                    .receiverIdList(List.of(UserHeaderInterceptor.userId.get()))
                    .accountId(ValidationUtil.validateAccount(accountRepository.findByAccountNo(requestDTO.getAccountNo())).getAccountId())
                    .notificationType(AUTH)
                    .amount(Long.valueOf(inquireTransactionHistoryResponseDTO.getRec().getTransactionSummary().split(" ")[1]))
                .build();
        notificationClient.sendNotification(notificationRequestDTO);

        return SsafyApiResponseDTOFactory.create(openAccountAuthResponseDTO.getHeader());
    }

    // 1원 인증(인증번호검증) 서비스 로직
    public SsafyApiResponseDTO getOneWonVerification(OneWonVerificationRequestDTO requestDTO) {

        // SSAFY 금융 API 1원 송금 검증 요청
        CheckAuthCodeRequestDTO checkAuthCodeRequestDTO = CheckAuthCodeRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(checkAuthCode)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(requestDTO.getAccountNo())
                .authCode(requestDTO.getOtp())
                .build();
        CheckAuthCodeResponseDTO checkAuthCodeResponseDTO = verificationClient.checkAuthCode(checkAuthCodeRequestDTO);

        return SsafyApiResponseDTOFactory.create(checkAuthCodeResponseDTO.getHeader());
    }

}
