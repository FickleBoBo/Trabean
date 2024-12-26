package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.dto.request.account.UpdateAccountTransferLimitRequestDTO;
import com.trabean.account.dto.response.account.AccountAdminNameResponseDTO;
import com.trabean.account.dto.response.account.AccountListResponseDTO;
import com.trabean.account.dto.response.account.RecentTransactionListResponseDTO;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.external.msa.travel.client.TravelClient;
import com.trabean.external.msa.user.client.UserClient;
import com.trabean.external.ssafy.api.domestic.client.DomesticClient;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireDemandDepositAccountListRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireDemandDepositAccountRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.InquireTransactionHistoryListRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.request.UpdateTransferLimitRequestDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireDemandDepositAccountListResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireDemandDepositAccountResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.InquireTransactionHistoryListResponseDTO;
import com.trabean.external.ssafy.api.domestic.dto.response.UpdateTransferLimitResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTO;
import com.trabean.external.ssafy.common.SsafyApiResponseDTOFactory;
import com.trabean.external.ssafy.util.RequestHeader;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.util.ValidateInputDTO;
import com.trabean.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.trabean.account.domain.Account.AccountType.*;
import static com.trabean.account.domain.UserAccountRelation.UserRole.ADMIN;
import static com.trabean.external.ssafy.constant.ApiName.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService {

    private final AccountHelperService accountHelperService;

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final DomesticClient domesticClient;

    private final UserClient userClient;
    private final TravelClient travelClient;

    // 통장 목록 조회 서비스 로직
    public AccountListResponseDTO getAccountList() {

        // SSAFY 금융 API 계좌 목록 조회 요청
        InquireDemandDepositAccountListRequestDTO inquireDemandDepositAccountListRequestDTO = InquireDemandDepositAccountListRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireDemandDepositAccountList)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .build();
        InquireDemandDepositAccountListResponseDTO inquireDemandDepositAccountListResponseDTO = domesticClient.inquireDemandDepositAccountList(inquireDemandDepositAccountListRequestDTO);

        // 한화 여행통장(멤버)
        List<UserAccountRelation> adminRelationsForDomesticNonAdminAccounts = ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByUserId(UserHeaderInterceptor.userId.get()))
                .stream()
                .filter(relation -> relation.getAccount().getAccountType() == DOMESTIC && relation.getUserRole() != ADMIN)
                .map(relation -> userAccountRelationRepository.findByAccount_AccountIdAndUserRole(relation.getAccount().getAccountId(), ADMIN))
                .flatMap(List::stream)
                .toList();

        for (UserAccountRelation u : adminRelationsForDomesticNonAdminAccounts) {

            // SSAFY 금융 API 계좌 조회 (단건) 요청
            InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                    .header(RequestHeader.builder()
                            .apiName(inquireDemandDepositAccount)
                            .userKey(accountHelperService.getAdminUserKey(u.getAccount().getAccountId()))
                            .build())
                    .accountNo(ValidationUtil.validateAccount(accountRepository.findById(u.getAccount().getAccountId())).getAccountNo())
                    .build();
            InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

            inquireDemandDepositAccountListResponseDTO.getRec().add((inquireDemandDepositAccountResponseDTO.getRec()));
        }

        if (inquireDemandDepositAccountListResponseDTO.getRec() == null) {
            return AccountListResponseDTO.builder()
                    .mainAccount(null)
                    .accountList(new ArrayList<>())
                    .build();
        }
        return getAccountList(inquireDemandDepositAccountListResponseDTO);
    }

    // SSAFY 금융 API 게좌 목록 responseDTO -> 통장 목록 조회 responseDTO
    private AccountListResponseDTO getAccountList(InquireDemandDepositAccountListResponseDTO inquireDemandDepositAccountListResponseDTO) {
        AccountListResponseDTO.Account mainAccount = null;
        List<AccountListResponseDTO.Account> accountList = new ArrayList<>();

        // User 서버에 userId로 mainAccountId 반환 요청
        Long mainAccountId = userClient.getMainAccountId(UserHeaderInterceptor.userId.get()).getMainAccountId();

        // Travel 서버에 한화 여행통장이름 이름 반환 요청
        for (InquireDemandDepositAccountListResponseDTO.REC account : inquireDemandDepositAccountListResponseDTO.getRec()) {
            Account savedAccount = ValidationUtil.validateAccount(accountRepository.findByAccountNo(account.getAccountNo()));
            String savedAccountName = savedAccount.getAccountType() == DOMESTIC
                    ? travelClient.getDomesticTravelAccountInfo(savedAccount.getAccountId()).getAccountName()
                    : "개인 입출금 통장";

            AccountListResponseDTO.Account dtoAccount = AccountListResponseDTO.Account.builder()
                    .accountId(savedAccount.getAccountId())
                    .accountNo(account.getAccountNo())
                    .accountName(savedAccountName)
                    .bankName(account.getBankName())
                    .accountBalance(account.getAccountBalance())
                    .accountType(savedAccount.getAccountType())
                    .build();

            if (savedAccount.getAccountId().equals(mainAccountId)) {
                mainAccount = dtoAccount;
            } else {
                accountList.add(dtoAccount);
            }
        }

        return AccountListResponseDTO.builder()
                .mainAccount(mainAccount)
                .accountList(accountList)
                .build();
    }

    // 최근 이체 목록 조회 서비스 로직
    public RecentTransactionListResponseDTO getRecentTransactionList(Long accountId, String startDate, String endDate) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                    .account(accountRepository.findById(accountId))
                    .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                    .isPayable(true)
                    .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 거래 내역 조회 요청
        InquireTransactionHistoryListRequestDTO inquireTransactionHistoryListRequestDTO = InquireTransactionHistoryListRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireTransactionHistoryList)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .startDate(startDate)
                .endDate(endDate)
                .transactionType("D")
                .orderByType("DESC")
                .build();
        InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO = domesticClient.inquireTransactionHistoryList(inquireTransactionHistoryListRequestDTO);

        return getUniqueLastTransactionList(inquireTransactionHistoryListResponseDTO);
    }

    // SSAFY 금융 API 계좌 거래 내역 responseDTO -> 최근 이체 목록 조회 responseDTO
    public RecentTransactionListResponseDTO getUniqueLastTransactionList(InquireTransactionHistoryListResponseDTO inquireTransactionHistoryListResponseDTO) {
        List<RecentTransactionListResponseDTO.Info> accountList = inquireTransactionHistoryListResponseDTO.getRec().getList()
                .stream()
                .filter(transactionHistory -> !transactionHistory.getTransactionAccountNo().isEmpty())
                .map(transactionHistory -> {
                    Account account = ValidationUtil.validateAccount(accountRepository.findByAccountNo(transactionHistory.getTransactionAccountNo()));

                    // SSAFY 금융 API 계좌 조회 (단건) 요청
                    InquireDemandDepositAccountRequestDTO inquireDemandDepositAccountRequestDTO = InquireDemandDepositAccountRequestDTO.builder()
                            .header(RequestHeader.builder()
                                    .apiName(inquireDemandDepositAccount)
                                    .userKey(accountHelperService.getAdminUserKey(account.getAccountId()))
                                    .build())
                            .accountNo(account.getAccountNo())
                            .build();
                    InquireDemandDepositAccountResponseDTO inquireDemandDepositAccountResponseDTO = domesticClient.inquireDemandDepositAccount(inquireDemandDepositAccountRequestDTO);

                    return RecentTransactionListResponseDTO.Info.builder()
                            .accountId(account.getAccountId())
                            .accountNo(transactionHistory.getTransactionAccountNo())
                            .adminName(accountHelperService.getAccountName(account.getAccountId()))
                            .bankName(inquireDemandDepositAccountResponseDTO.getRec().getBankName())
                            .build();
                })
                .distinct()
                .limit(6)
                .collect(Collectors.toList());

        return RecentTransactionListResponseDTO.builder()
                .accountList(accountList)
                .build();
    }

    // 계좌 이체 한도 변경 서비스 로직
    public SsafyApiResponseDTO updateTransferLimit(Long accountId, UpdateAccountTransferLimitRequestDTO requestDTO) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .userRole(ADMIN)
                        .build())
                .getAccountNo();

        // SSAFY 금융 API 계좌 이체 한도 변경 요청
        UpdateTransferLimitRequestDTO updateTransferLimitRequestDTO = UpdateTransferLimitRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(updateTransferLimit)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountNo(accountNo)
                .oneTimeTransferLimit(requestDTO.getOneTimeTransferLimit())
                .dailyTransferLimit(requestDTO.getDailyTransferLimit())
                .build();
        UpdateTransferLimitResponseDTO updateTransferLimitResponseDTO = domesticClient.updateTransferLimit(updateTransferLimitRequestDTO);

        return SsafyApiResponseDTOFactory.create(updateTransferLimitResponseDTO.getHeader());
    }

    // 통장 주인 이름 조회 서비스 로직
    public AccountAdminNameResponseDTO getAccountName(String accountNo) {
        return AccountAdminNameResponseDTO.builder()
                .name(accountHelperService.getAccountName(accountNo))
                .build();
    }

}
