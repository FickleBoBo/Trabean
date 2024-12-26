package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.dto.request.domesticTravelAccount.CreateDomesticTravelAccountRequestDTO;
import com.trabean.account.dto.request.domesticTravelAccount.TransferDomesticTravelAccountRequestDTO;
import com.trabean.account.dto.request.common.VerifyAccountPasswordRequestDTO;
import com.trabean.account.dto.response.domesticTravelAccount.*;
import com.trabean.account.dto.response.domesticTravelAccount.DomesticTravelAccountMemberListResponseDTO.Member;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.exception.custom.ExternalServerErrorException;
import com.trabean.external.msa.notification.client.NotificationClient;
import com.trabean.external.msa.notification.dto.request.NotificationRequestDTO;
import com.trabean.external.msa.travel.client.TravelClient;
import com.trabean.external.msa.travel.dto.request.SaveDomesticTravelAccountRequestDTO;
import com.trabean.external.msa.travel.dto.response.DomesticTravelAccountInfoResponseDTO;
import com.trabean.external.msa.user.client.UserClient;
import com.trabean.external.ssafy.api.domestic.client.DomesticClient;
import com.trabean.external.ssafy.api.domestic.dto.request.*;
import com.trabean.external.ssafy.api.domestic.dto.response.*;
import com.trabean.external.ssafy.api.memo.client.MemoClient;
import com.trabean.external.ssafy.api.memo.dto.request.TransactionMemoRequestDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTOFactory;
import com.trabean.external.ssafy.util.RequestHeader;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.util.ValidateInputDTO;
import com.trabean.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.trabean.account.domain.Account.AccountType.DOMESTIC;
import static com.trabean.account.domain.UserAccountRelation.UserRole.ADMIN;
import static com.trabean.constant.Constant.DOMESTIC_TRAVEL_ACCOUNT_TYPE_UNIQUE_NO;
import static com.trabean.constant.Constant.PEPPER;
import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.DEPOSIT;
import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.WITHDRAW;
import static com.trabean.external.ssafy.constant.ApiName.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DomesticTravelAccountService {

    private final AccountHelperService accountHelperService;

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final DomesticClient domesticClient;
    private final MemoClient memoClient;

    private final UserClient userClient;
    private final TravelClient travelClient;
    private final NotificationClient notificationClient;

    private final PasswordEncoder passwordEncoder;

    // 한화 여행통장 생성 서비스 로직
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SsafyApiResponseDTO createDomesticTravelAccount(CreateDomesticTravelAccountRequestDTO requestDTO) {

        // SSAFY 금융 API 계좌 생성 요청
        CreateDemandDepositAccountRequestDTO createDemandDepositAccountRequestDTO = CreateDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(createDemandDepositAccount)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountTypeUniqueNo(DOMESTIC_TRAVEL_ACCOUNT_TYPE_UNIQUE_NO)
                .build();
        CreateDemandDepositAccountResponseDTO createDemandDepositAccountResponseDTO = domesticClient.createDemandDepositAccount(createDemandDepositAccountRequestDTO);

        String accountNo = createDemandDepositAccountResponseDTO.getRec().getAccountNo();
        String hashedPassword = passwordEncoder.encode(requestDTO.getPassword() + PEPPER);

        Account savedAccount = null;
        UserAccountRelation savedUserAccountRelation = null;

        try {
            // Account 테이블에 저장
            Account account = Account.builder()
                    .accountNo(accountNo)
                    .password(hashedPassword)
                    .accountType(DOMESTIC)
                    .build();
            savedAccount = accountRepository.save(account);

            // UserAccountRelation 테이블에 저장
            UserAccountRelation userAccountRelation = UserAccountRelation.builder()
                    .userId(UserHeaderInterceptor.userId.get())
                    .account(savedAccount)
                    .userRole(ADMIN)
                    .build();
            savedUserAccountRelation = userAccountRelationRepository.save(userAccountRelation);

            // Travel 서버에 원화계좌 생성시 원화여행계좌 테이블에 정보 저장 요청
            SaveDomesticTravelAccountRequestDTO saveDomesticTravelAccountRequestDTO = SaveDomesticTravelAccountRequestDTO.builder()
                    .accountId(savedAccount.getAccountId())
                    .accountName(requestDTO.getAccountName())
                    .targetAmount(requestDTO.getTargetAmount())
                    .build();
            travelClient.saveDomesticTravelAccount(saveDomesticTravelAccountRequestDTO);

        } catch (ExternalServerErrorException e) {
            if (savedUserAccountRelation != null) {
                userAccountRelationRepository.delete(savedUserAccountRelation);
            }

            if (savedAccount != null) {
                accountRepository.delete(savedAccount);
            }

            throw e;
        }

        return SsafyApiResponseDTOFactory.create(createDemandDepositAccountResponseDTO.getHeader());
    }

    // 한화 여행통장 상세 조회 서비스 로직
    public DomesticTravelAccountDetailResponseDTO getDomesticTravelAccountDetail(Long accountId, String startDate, String endDate, String transactionType, String selectedUserId) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(DOMESTIC)
                        .build())
                .getAccountNo();

        // Travel 서버에 한화 여행통장 ID로 이름과 목표금액 반환 요청
        DomesticTravelAccountInfoResponseDTO domesticTravelAccountInfo = travelClient.getDomesticTravelAccountInfo(accountId);

        // SSAFY 금융 API 계좌 조회 (단건) 요청
        InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccount)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .build();
        InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

        // SSAFY 금융 API 계좌 거래 내역 조회 요청
        InquireTransactionHistoryListRequestDTO inquireTransactionHistoryListRequestDTO = InquireTransactionHistoryListRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireTransactionHistoryList)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .orderByType("DESC")
                .build();
        InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO = domesticClient.inquireTransactionHistoryList(inquireTransactionHistoryListRequestDTO);

        return DomesticTravelAccountDetailResponseDTO.builder()
                .accountName(domesticTravelAccountInfo.getAccountName())
                .targetAmount(domesticTravelAccountInfo.getTargetAmount())
                .accountId(accountId)
                .accountNo(accountNo)
                .accountBalance(inquireDemandDepositAccountResponseDTO.getRec().getAccountBalance())
                .bankName(inquireDemandDepositAccountResponseDTO.getRec().getBankName())
                .transactionList(getDomesticTravelAccountTransactionList(inquireTransactionHistoryListResponseDTO, selectedUserId))
                .build();
    }

    // SSAFY 금융 API 계좌 거래 내역 responseDTO -> 한화 여행 통장 거래 내역 리스트
    private List<DomesticTravelAccountDetailResponseDTO.Transaction> getDomesticTravelAccountTransactionList(InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO, String selectedUserId) {
        return inquireTransactionHistoryListResponseDTO.getRec().getList().stream()
                .filter(transaction -> "-1".equals(selectedUserId) || selectedUserId.equals(transaction.getTransactionMemo()))
                .map(transaction -> DomesticTravelAccountDetailResponseDTO.Transaction.builder()
                        .transactionType(transaction.getTransactionType())
                        .transactionSummary(transaction.getTransactionSummary())
                        .transactionDate(transaction.getTransactionDate())
                        .transactionTime(transaction.getTransactionTime())
                        .transactionBalance(transaction.getTransactionBalance())
                        .transactionAfterBalance(transaction.getTransactionAfterBalance())
                        .transactionMemo(transaction.getTransactionMemo())
                        .build())
                .collect(Collectors.toList());
    }

    // 한화 여행통장 생성일 조회 서비스 로직
    public DomesticTravelAccountCreatedDateResponseDTO getDomesticTravelAccountCreatedDate(Long accountId) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(DOMESTIC)
                        .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 조회 (단건) 요청
        InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccount)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .build();
        InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

        return DomesticTravelAccountCreatedDateResponseDTO.builder()
                .accountCreatedDate(inquireDemandDepositAccountResponseDTO.getRec().getAccountCreatedDate())
                .build();
    }

    // 한화 여행통장 잔액 조회 서비스 로직
    public DomesticTravelAccountBalanceResponseDTO getDomesticTravelAccountBalance(Long accountId) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(DOMESTIC)
                        .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 잔액 조회 요청
        InquireDemandDepositAccountBalanceRequestDTO inquireDemandDepositAccountBalanceRequestDTO = InquireDemandDepositAccountBalanceRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccountBalance)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .build();
        InquireDemandDepositAccountBalanceResponseDTO inquireDemandDepositAccountBalanceResponseDTO = domesticClient.inquireDemandDepositAccountBalance(inquireDemandDepositAccountBalanceRequestDTO);

        return DomesticTravelAccountBalanceResponseDTO.builder()
                .accountBalance(inquireDemandDepositAccountBalanceResponseDTO.getRec().getAccountBalance())
                .build();
    }

    // 한화 여행통장 계좌 이체 서비스 로직
    public SsafyApiResponseDTO transferDomesticTravelAccount(Long accountId, TransferDomesticTravelAccountRequestDTO requestDTO) {

        ValidationUtil.validateInput(ValidateInputDTO.builder()
                .account(accountRepository.findById(accountId))
                .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                .accountType(DOMESTIC)
                .isPayable(true)
                .build());

        String depositTransactionSummary = accountHelperService.getAccountName(requestDTO.getWithdrawalAccountNo());
        String withdrawalTransactionSummary = accountHelperService.getAccountName(requestDTO.getDepositAccountNo());

        // SSAFY 금융 API 계좌 이체 요청
        UpdateDemandDepositAccountTransferRequestDTO updateDemandDepositAccountTransferRequestDTO = UpdateDemandDepositAccountTransferRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(updateDemandDepositAccountTransfer)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .depositAccountNo(requestDTO.getDepositAccountNo())
                .depositTransactionSummary(depositTransactionSummary)
                .transactionBalance(requestDTO.getTransactionBalance())
                .withdrawalAccountNo(requestDTO.getWithdrawalAccountNo())
                .withdrawalTransactionSummary(withdrawalTransactionSummary)
                .build();
        UpdateDemandDepositAccountTransferResponseDTO updateDemandDepositAccountTransferResponseDTO = domesticClient.updateDemandDepositAccountTransfer(updateDemandDepositAccountTransferRequestDTO);

        Long depositAccountId = ValidationUtil.validateAccount(accountRepository.findByAccountNo(requestDTO.getDepositAccountNo())).getAccountId();
        Long withdrawalAccountId = ValidationUtil.validateAccount(accountRepository.findByAccountNo(requestDTO.getWithdrawalAccountNo())).getAccountId();

        // SSAFY 금융 API 거래내역 메모 요청 (출금 메모)
        TransactionMemoRequestDTO transactionWithdrawalMemoRequestDTO = TransactionMemoRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(transactionMemo)
                        .userKey(accountHelperService.getAdminUserKey(withdrawalAccountId))
                        .build())
                .accountNo(requestDTO.getWithdrawalAccountNo())
                .transactionUniqueNo(updateDemandDepositAccountTransferResponseDTO.getRec().get(0).getTransactionUniqueNo())
                .transactionMemo(String.valueOf(UserHeaderInterceptor.userId.get()))
                .build();
        memoClient.transactionMemo(transactionWithdrawalMemoRequestDTO);

        // SSAFY 금융 API 거래내역 메모 요청 (입금 메모)
        TransactionMemoRequestDTO transactionDepositMemoRequestDTO = TransactionMemoRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(transactionMemo)
                        .userKey(accountHelperService.getAdminUserKey(depositAccountId))
                        .build())
                .accountNo(requestDTO.getDepositAccountNo())
                .transactionUniqueNo(updateDemandDepositAccountTransferResponseDTO.getRec().get(1).getTransactionUniqueNo())
                .transactionMemo(String.valueOf(UserHeaderInterceptor.userId.get()))
                .build();
        memoClient.transactionMemo(transactionDepositMemoRequestDTO);

        // Notification 서버 입출금 시 알림 생성 요청
        NotificationRequestDTO notificationWithdrawalRequestDTO = NotificationRequestDTO.builder()
                .senderId(UserHeaderInterceptor.userId.get())
                .receiverIdList(List.of(UserHeaderInterceptor.userId.get()))
                .accountId(withdrawalAccountId)
                .notificationType(WITHDRAW)
                .amount(requestDTO.getTransactionBalance())
                .build();
        notificationClient.sendNotification(notificationWithdrawalRequestDTO);

        // Notification 서버 입출금 시 알림 생성 요청
        NotificationRequestDTO notificationDepositRequestDTO = NotificationRequestDTO.builder()
                .senderId(UserHeaderInterceptor.userId.get())
                .receiverIdList(accountHelperService.getAccountMemberIdList(depositAccountId))
                .accountId(depositAccountId)
                .notificationType(DEPOSIT)
                .amount(requestDTO.getTransactionBalance())
                .build();
        notificationClient.sendNotification(notificationDepositRequestDTO);

        return SsafyApiResponseDTOFactory.create(updateDemandDepositAccountTransferResponseDTO.getHeader());
    }

    // 한화 여행통장 계좌 이체 비밀번호 검증 서비스 로직
    public InternalServerSuccessResponseDTO verifyDomesticTravelAccountPassword(Long accountId, VerifyAccountPasswordRequestDTO requestDTO) {

        String savedPassword = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(DOMESTIC)
                        .isPayable(true)
                        .build())
                .getPassword();

        return InternalServerSuccessResponseDTO.builder()
                .message(accountHelperService.verifyAccountPassword(requestDTO.getPassword(), savedPassword))
                .build();
    }

    // 한화 여행통장 멤버 목록 조회 서비스 로직 (민채)
    public DomesticTravelAccountMemberListResponseDTO getDomesticTravelAccountMemberList(Long accountId) {

        ValidationUtil.validateInput(ValidateInputDTO.builder()
                .account(accountRepository.findById(accountId))
                .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                .accountType(DOMESTIC)
                .build());

        List<Member> members = ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(accountId))
                .stream()
                .map(member -> Member.builder()
                        .userId(member.getUserId())
                        .userName(userClient.getUserName(member.getUserId()).getUserName())
                        .role(member.getUserRole())
                        .build())
                .collect(Collectors.toList());

        // 응답 DTO 생성
        return DomesticTravelAccountMemberListResponseDTO.builder()
                .userId(UserHeaderInterceptor.userId.get())
                .memberCount((long) members.size())
                .members(members)
                .build();
    }

    // 통장 권한 조회 서비스 로직
    public DomesticTravelAccountUserRoleResponseDTO getDomesticTravelAccountUserRole(Long accountId) {
        return DomesticTravelAccountUserRoleResponseDTO.builder()
                .userRole(ValidationUtil.validateUserAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId)).getUserRole())
                .build();
    }

}
