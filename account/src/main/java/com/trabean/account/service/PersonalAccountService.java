package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.dto.request.personalAccount.CreatePersonalAccountRequestDTO;
import com.trabean.account.dto.request.personalAccount.TransferPersonalAccountRequestDTO;
import com.trabean.account.dto.request.common.VerifyAccountPasswordRequestDTO;
import com.trabean.account.dto.response.personalAccount.PersonalAccountCreatedDateResponseDTO;
import com.trabean.account.dto.response.personalAccount.PersonalAccountDetailResponseDTO;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.external.msa.notification.client.NotificationClient;
import com.trabean.external.msa.notification.dto.request.NotificationRequestDTO;
import com.trabean.external.msa.user.client.UserClient;
import com.trabean.external.msa.user.dto.request.UpdateMainAccountIdRequestDTO;
import com.trabean.external.ssafy.api.domestic.client.DomesticClient;
import com.trabean.external.ssafy.api.domestic.dto.request.CreateDemandDepositAccountRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireDemandDepositAccountRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireTransactionHistoryListRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.UpdateDemandDepositAccountTransferRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.CreateDemandDepositAccountResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireDemandDepositAccountResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireTransactionHistoryListResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.UpdateDemandDepositAccountTransferResponseDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.trabean.account.domain.Account.AccountType.PERSONAL;
import static com.trabean.account.domain.UserAccountRelation.UserRole.ADMIN;
import static com.trabean.constant.Constant.PEPPER;
import static com.trabean.constant.Constant.PERSONAL_ACCOUNT_TYPE_UNIQUE_NO;
import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.DEPOSIT;
import static com.trabean.external.msa.notification.dto.request.NotificationRequestDTO.Type.WITHDRAW;
import static com.trabean.external.ssafy.constant.ApiName.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PersonalAccountService {

    private final AccountHelperService accountHelperService;

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final DomesticClient domesticClient;
    private final MemoClient memoClient;

    private final UserClient userClient;
    private final NotificationClient notificationClient;

    private final PasswordEncoder passwordEncoder;

    // 개인 통장 생성 서비스 로직
    @Transactional
    public SsafyApiResponseDTO createPersonalAccount(CreatePersonalAccountRequestDTO requestDTO) {

        // SSAFY 금융 API 계좌 생성 요청
        CreateDemandDepositAccountRequestDTO createDemandDepositAccountRequestDTO = CreateDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(createDemandDepositAccount)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountTypeUniqueNo(PERSONAL_ACCOUNT_TYPE_UNIQUE_NO)
                .build();
        CreateDemandDepositAccountResponseDTO createDemandDepositAccountResponseDTO = domesticClient.createDemandDepositAccount(createDemandDepositAccountRequestDTO);

        String accountNo = createDemandDepositAccountResponseDTO.getRec().getAccountNo();
        String hashedPassword = passwordEncoder.encode(requestDTO.getPassword() + PEPPER);

        // Account 테이블에 저장
        Account account = Account.builder()
                .accountNo(accountNo)
                .password(hashedPassword)
                .accountType(PERSONAL)
                .build();
        Account savedAccount = accountRepository.save(account);

        // UserAccountRelation 테이블에 저장
        UserAccountRelation userAccountRelation = UserAccountRelation.builder()
                .userId(UserHeaderInterceptor.userId.get())
                .account(savedAccount)
                .userRole(ADMIN)
                .build();
        userAccountRelationRepository.save(userAccountRelation);

        // User 서버에 mainAccountId가 존재하는지 조회해서 존재 안하면 mainAccount로 저장
        if (userClient.getMainAccountId(UserHeaderInterceptor.userId.get()).getMainAccountId() == null) {
            UpdateMainAccountIdRequestDTO updateMainAccountIdRequestDTO = UpdateMainAccountIdRequestDTO.builder()
                    .userId(UserHeaderInterceptor.userId.get())
                    .mainAccountId(savedAccount.getAccountId())
                    .build();
            userClient.updateMainAccountId(updateMainAccountIdRequestDTO);
        }

        return SsafyApiResponseDTOFactory.create(createDemandDepositAccountResponseDTO.getHeader());
    }

    // 개인 통장 상세 조회 서비스 로직
    public PersonalAccountDetailResponseDTO getPersonalAccountDetail(Long accountId, String startDate, String endDate, String transactionType) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                    .account(accountRepository.findById(accountId))
                    .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                    .accountType(PERSONAL)
                    .userRole(ADMIN)
                    .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 조회 (단건) 요청
        InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccount)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(accountNo)
                .build();
        InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

        // SSAFY 금융 API 계좌 거래 내역 조회 요청
        InquireTransactionHistoryListRequestDTO inquireTransactionHistoryListRequestDTO = InquireTransactionHistoryListRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireTransactionHistoryList)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(accountNo)
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .orderByType("DESC")
                .build();
        InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO = domesticClient.inquireTransactionHistoryList(inquireTransactionHistoryListRequestDTO);

        return PersonalAccountDetailResponseDTO.builder()
                .accountId(accountId)
                .accountNo(accountNo)
                .accountBalance(inquireDemandDepositAccountResponseDTO.getRec().getAccountBalance())
                .accountName("개인 입출금 통장")
                .bankName(inquireDemandDepositAccountResponseDTO.getRec().getBankName())
                .transactionList(getPersonalAccountTransactionList(inquireTransactionHistoryListResponseDTO))
                .build();
    }

    // SSAFY 금융 API 계좌 거래 내역 responseDTO -> 개인 통장 거래 내역 리스트
    private List<PersonalAccountDetailResponseDTO.Transaction> getPersonalAccountTransactionList(InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO) {
        return inquireTransactionHistoryListResponseDTO.getRec().getList().stream()
                .map(transaction -> PersonalAccountDetailResponseDTO.Transaction.builder()
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

    // 개인 통장 생성일 조회 서비스 로직
    public PersonalAccountCreatedDateResponseDTO getPersonalAccountCreatedDate(Long accountId) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(PERSONAL)
                        .userRole(ADMIN)
                        .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 조회 (단건) 요청
        InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccount)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(accountNo)
                .build();
        InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

        return PersonalAccountCreatedDateResponseDTO.builder()
                .accountCreatedDate(inquireDemandDepositAccountResponseDTO.getRec().getAccountCreatedDate())
                .build();
    }

    // 개인 통장 계좌 이체 서비스 로직
    public SsafyApiResponseDTO transferPersonalAccount(Long accountId, TransferPersonalAccountRequestDTO requestDTO) {

        ValidationUtil.validateInput(ValidateInputDTO.builder()
                .account(accountRepository.findById(accountId))
                .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                .accountType(PERSONAL)
                .userRole(ADMIN)
                .isPayable(true)
                .build());

        String depositTransactionSummary = accountHelperService.getAccountName(requestDTO.getWithdrawalAccountNo());
        String withdrawalTransactionSummary = accountHelperService.getAccountName(requestDTO.getDepositAccountNo());

        // SSAFY 금융 API 계좌 이체 요청
        UpdateDemandDepositAccountTransferRequestDTO updateDemandDepositAccountTransferRequestDTO = UpdateDemandDepositAccountTransferRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(updateDemandDepositAccountTransfer)
                        .userKey(UserHeaderInterceptor.userKey.get())
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

    // 개인 통장 계좌 이체 비밀번호 검증 서비스 로직
    public InternalServerSuccessResponseDTO verifyPersonalAccountPassword(Long accountId, VerifyAccountPasswordRequestDTO requestDTO) {

        String savedPassword = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(PERSONAL)
                        .userRole(ADMIN)
                        .isPayable(true)
                        .build())
                .getPassword();

        return InternalServerSuccessResponseDTO.builder()
                .message(accountHelperService.verifyAccountPassword(requestDTO.getPassword(), savedPassword))
                .build();
    }

}
