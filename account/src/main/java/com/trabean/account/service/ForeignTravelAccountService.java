package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.dto.request.foreignTravelAccount.CreateForeignTravelAccountRequestDTO;
import com.trabean.account.dto.response.foreignTravelAccount.CreateForeignTravelAccountResponseDTO;
import com.trabean.account.dto.response.foreignTravelAccount.ForeignTravelAccountCreatedDateResponseDTO;
import com.trabean.account.dto.response.foreignTravelAccount.TravelAccountCoupleResponseDTO;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.exception.custom.ExternalServerErrorException;
import com.trabean.external.msa.travel.client.TravelClient;
import com.trabean.external.msa.travel.dto.request.SaveForeignTravelAccountRequestDTO;
import com.trabean.external.ssafy.api.foriegn.client.ForeignClient;
import com.trabean.external.ssafy.api.foriegn.dto.request.CreateForeignCurrencyDemandDepositAccountRequestDTO;
import com.trabean.external.ssafy.api.foriegn.dto.request.InquireForeignCurrencyDemandDepositAccountRequestDTO;
import com.trabean.external.ssafy.api.foriegn.dto.response.CreateForeignCurrencyDemandDepositAccountResponseDTO;
import com.trabean.external.ssafy.api.foriegn.dto.response.InquireForeignCurrencyDemandDepositAccountResponseDTO;
import com.trabean.external.ssafy.util.RequestHeader;
import com.trabean.interceptor.UserHeaderInterceptor;
import com.trabean.util.ValidateInputDTO;
import com.trabean.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.trabean.account.domain.Account.AccountType.DOMESTIC;
import static com.trabean.account.domain.Account.AccountType.FOREIGN;
import static com.trabean.account.domain.UserAccountRelation.UserRole.ADMIN;
import static com.trabean.constant.Constant.FOREIGN_TRAVEL_ACCOUNT_TYPE_UNIQUE_NO;
import static com.trabean.external.ssafy.constant.ApiName.createForeignCurrencyDemandDepositAccount;
import static com.trabean.external.ssafy.constant.ApiName.inquireForeignCurrencyDemandDepositAccount;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ForeignTravelAccountService {

    private final AccountHelperService accountHelperService;

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final ForeignClient foreignClient;

    private final TravelClient travelClient;

    // 외화 여행통장 생성 서비스 로직
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CreateForeignTravelAccountResponseDTO createForeignTravelAccount(CreateForeignTravelAccountRequestDTO requestDTO) {

        Account verifiedAccount = ValidationUtil.validateInput(ValidateInputDTO.builder()
                    .account(accountRepository.findById(requestDTO.getDomesticAccountId()))
                    .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), requestDTO.getDomesticAccountId()))
                    .accountType(DOMESTIC)
                    .userRole(ADMIN)
                .build());

        // SSAFY 금융 API 계좌 생성 요청
        CreateForeignCurrencyDemandDepositAccountRequestDTO createForeignCurrencyDemandDepositAccountRequestDTO = CreateForeignCurrencyDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(createForeignCurrencyDemandDepositAccount)
                        .userKey(UserHeaderInterceptor.userKey.get())
                        .build())
                .accountTypeUniqueNo(FOREIGN_TRAVEL_ACCOUNT_TYPE_UNIQUE_NO)
                .currency(requestDTO.getCurrency())
                .build();
        CreateForeignCurrencyDemandDepositAccountResponseDTO createForeignCurrencyDemandDepositAccountResponseDTO = foreignClient.createForeignCurrencyDemandDepositAccount(createForeignCurrencyDemandDepositAccountRequestDTO);

        String accountNo = createForeignCurrencyDemandDepositAccountResponseDTO.getRec().getAccountNo();
        String currency = createForeignCurrencyDemandDepositAccountResponseDTO.getRec().getCurrency().getCurrency();

        Account savedAccount = null;
        UserAccountRelation savedUserAccountRelation = null;
        List<UserAccountRelation> savedUserAccountRelationList = new ArrayList<>();

        try {
            // Account 테이블에 저장
            Account account = Account.builder()
                    .accountNo(accountNo)
                    .password(verifiedAccount.getPassword())
                    .accountType(FOREIGN)
                    .build();
            savedAccount = accountRepository.save(account);

            // UserAccountRelation 테이블에 저장
            UserAccountRelation userAccountRelation = UserAccountRelation.builder()
                    .userId(UserHeaderInterceptor.userId.get())
                    .account(savedAccount)
                    .userRole(ADMIN)
                    .build();
            savedUserAccountRelation = userAccountRelationRepository.save(userAccountRelation);

            List<UserAccountRelation> userAccountRelationList = ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(requestDTO.getDomesticAccountId()));

            for (UserAccountRelation u : userAccountRelationList) {
                if (u.getUserRole() == ADMIN) continue;

                // UserAccountRelation 테이블에 저장
                UserAccountRelation savedTempUserAccountRelation = userAccountRelationRepository.save(UserAccountRelation.builder()
                        .userId(u.getUserId())
                        .account(savedAccount)
                        .userRole(u.getUserRole())
                        .build());

                savedUserAccountRelationList.add(savedTempUserAccountRelation);
            }

            // Travel 서버에 외화계좌 생성시 외화여행계좌 테이블에 정보 저장 요청
            SaveForeignTravelAccountRequestDTO saveForeignTravelAccountRequestDTO = SaveForeignTravelAccountRequestDTO.builder()
                    .foreignAccountId(savedAccount.getAccountId())
                    .domesticAccountId(requestDTO.getDomesticAccountId())
                    .currency(currency)
                    .build();
            travelClient.saveForeignTravelAccount(saveForeignTravelAccountRequestDTO);

        } catch (ExternalServerErrorException e) {
            if (!savedUserAccountRelationList.isEmpty()) {
                userAccountRelationRepository.deleteAll(savedUserAccountRelationList);
            }

            if (savedUserAccountRelation != null) {
                userAccountRelationRepository.delete(savedUserAccountRelation);
            }

            if (savedAccount != null) {
                accountRepository.delete(savedAccount);
            }

            throw e;
        }

        return CreateForeignTravelAccountResponseDTO.builder()
                .domesticAccountId(verifiedAccount.getAccountId())
                .domesticAccountNo(verifiedAccount.getAccountNo())
                .foreignAccountId(savedAccount.getAccountId())
                .foreignAccountNo(savedAccount.getAccountNo())
                .build();
    }

    // 외화 여행통장 생성일 조회 서비스 로직
    public ForeignTravelAccountCreatedDateResponseDTO getForeignTravelAccountCreatedDate(Long accountId) {

        String accountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(accountId))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), accountId))
                        .accountType(FOREIGN)
                        .build())
                .getAccountNo();

        // SSAFY 금융 API 외화 계좌 조회 (단건) 요청
        InquireForeignCurrencyDemandDepositAccountRequestDTO inquireForeignCurrencyDemandDepositAccountRequestDTO = InquireForeignCurrencyDemandDepositAccountRequestDTO.builder()
                .header(RequestHeader.builder()
                        .apiName(inquireForeignCurrencyDemandDepositAccount)
                        .userKey(accountHelperService.getAdminUserKey(accountId))
                        .build())
                .accountNo(accountNo)
                .build();
        InquireForeignCurrencyDemandDepositAccountResponseDTO inquireForeignCurrencyDemandDepositAccountResponseDTO = foreignClient.inquireForeignCurrencyDemandDepositAccount(inquireForeignCurrencyDemandDepositAccountRequestDTO);

        return ForeignTravelAccountCreatedDateResponseDTO.builder()
                .accountCreatedDate(inquireForeignCurrencyDemandDepositAccountResponseDTO.getRec().getAccountCreatedDate())
                .build();
    }

    // 외화 여행통장 및 연결된 한화 여행통장 식별자와 계좌번호 조회 서비스 로직
    public TravelAccountCoupleResponseDTO getTravelAccountCoupleResponseDTO(Long foreignAccountId) {

        String foreignAccountNo = ValidationUtil.validateInput(ValidateInputDTO.builder()
                    .account(accountRepository.findById(foreignAccountId))
                    .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(UserHeaderInterceptor.userId.get(), foreignAccountId))
                    .accountType(FOREIGN)
                    .build())
                .getAccountNo();

        // Travel 서버에 외화 여행통장 ID로 한화 여행통장 ID 반환 요청
        Long domesticAccountId = travelClient.getParentAccountId(foreignAccountId).getParentAccountId();
        String domesticAccountNo = ValidationUtil.validateAccount(accountRepository.findById(domesticAccountId)).getAccountNo();

        return TravelAccountCoupleResponseDTO.builder()
                .domesticAccountId(domesticAccountId)
                .domesticAccountNo(domesticAccountNo)
                .foreignAccountId(foreignAccountId)
                .foreignAccountNo(foreignAccountNo)
                .build();
    }

}
