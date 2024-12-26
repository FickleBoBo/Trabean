package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.dto.request.internal.*;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.common.InternalServerSuccessResponseDTO;
import com.trabean.external.msa.user.client.UserClient;
import com.trabean.account.dto.response.internal.AccountNoResponseDTO;
import com.trabean.account.dto.response.internal.AdminUserKeyResponseDTO;
import com.trabean.account.dto.response.internal.TravelAccountMembersResponseDTO;
import com.trabean.account.dto.response.internal.TravelAccountMembersResponseDTO.Member;
import com.trabean.account.dto.response.internal.UserRoleResponseDTO;
import com.trabean.util.ValidateInputDTO;
import com.trabean.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.trabean.account.domain.Account.AccountType.DOMESTIC;
import static com.trabean.account.domain.UserAccountRelation.UserRole.NONE_PAYER;
import static com.trabean.constant.Constant.PEPPER;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InternalService {

    private final AccountHelperService accountHelperService;

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final UserClient userClient;

    private final PasswordEncoder passwordEncoder;

    // 통장 계좌번호 조회 서비스 로직
    public AccountNoResponseDTO getAccountNo(AccountNoRequestDTO requestDTO) {
        return AccountNoResponseDTO.builder()
                .message("통장 계좌번호 조회 성공")
                .accountNo(ValidationUtil.validateAccount(accountRepository.findById(requestDTO.getAccountId()))
                        .getAccountNo())
                .build();
    }

    // 통장 권한 조회 서비스 로직
    public UserRoleResponseDTO getUserRole(UserRoleRequestDTO requestDTO) {
        return UserRoleResponseDTO.builder()
                .message("통장 권한 조회 성공")
                .userRole(ValidationUtil.validateUserAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(requestDTO.getUserId(), requestDTO.getAccountId()))
                        .getUserRole())
                .build();
    }

    // 여행통장 결제 권한 변경 서비스 로직
    @Transactional
    public InternalServerSuccessResponseDTO updateUserRole(UpdateUserRoleRequestDTO requestDTO) {

        userAccountRelationRepository.updateUserRoleByUserIdAndAccountId(requestDTO.getUserId(), requestDTO.getDomesticAccountId(), requestDTO.getUserRole());

        for (Long foreignAccountId : requestDTO.getForeignAccountIdList()) {
            userAccountRelationRepository.updateUserRoleByUserIdAndAccountId(requestDTO.getUserId(), foreignAccountId, requestDTO.getUserRole());
        }

        return InternalServerSuccessResponseDTO.builder()
                .message("여행통장 결제 권한 변경 성공")
                .build();
    }

    // 결제 비밀번호 검증 서비스 로직
    public InternalServerSuccessResponseDTO verifyPassword(VerifyPasswordRequestDTO requestDTO) {

        String savedPassword = ValidationUtil.validateInput(ValidateInputDTO.builder()
                        .account(accountRepository.findById(requestDTO.getAccountId()))
                        .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(requestDTO.getUserId(), requestDTO.getAccountId()))
                        .isPayable(true)
                        .build())
                .getPassword();

        return InternalServerSuccessResponseDTO.builder()
                .message(accountHelperService.verifyAccountPassword(requestDTO.getPassword(), savedPassword))
                .build();
    }

    // 여행통장 가입 서비스 로직
    @Transactional
    public InternalServerSuccessResponseDTO joinTravelAccount(JoinTravelAccountRequestDTO requestDTO) {

        // 한화 여행통장에 가입
        UserAccountRelation domesticUserAccountRelation = UserAccountRelation.builder()
                .userId(requestDTO.getUserId())
                .account(Account.builder()
                        .accountId(requestDTO.getDomesticAccountId())
                        .build())
                .userRole(NONE_PAYER)
                .build();
        userAccountRelationRepository.save(domesticUserAccountRelation);

        // 한화 여행통장에 연결된 외화 여행통장에 가입
        for (Long foreignAccountId : requestDTO.getForeignAccountIdList()) {
            UserAccountRelation foreignUserAccountRelation = UserAccountRelation.builder()
                    .userId(requestDTO.getUserId())
                    .account(Account.builder()
                            .accountId(foreignAccountId)
                            .build())
                    .userRole(NONE_PAYER)
                    .build();
            userAccountRelationRepository.save(foreignUserAccountRelation);
        }

        return InternalServerSuccessResponseDTO.builder()
                .message("여행통장 가입 성공")
                .build();
    }

    // 통장 주인의 userKey 조회 서비스 로직
    public AdminUserKeyResponseDTO getAdminUserKey(AdminUserKeyRequestDTO requestDTO) {
        return AdminUserKeyResponseDTO.builder()
                .userKey(accountHelperService.getAdminUserKey(requestDTO.getAccountId()))
                .build();
    }

    // 여행통장 비밀번호 변경 서비스 로직
    @Transactional
    public InternalServerSuccessResponseDTO updateTravelAccountPassword(UpdateTravelAccountPasswordRequestDTO requestDTO) {

        String hashedPassword = passwordEncoder.encode(requestDTO.getPassword() + PEPPER);

        accountRepository.updatePasswordById(requestDTO.getDomesticAccountId(), hashedPassword);

        for (Long foreignAccountId : requestDTO.getForeignAccountIdList()) {
            accountRepository.updatePasswordById(foreignAccountId, hashedPassword);
        }

        return InternalServerSuccessResponseDTO.builder()
                .message("비밀번호 변경 성공")
                .build();
    }

    // 한화 여행통장 멤버 목록 조회 API
    public TravelAccountMembersResponseDTO getTravelAccountMembers(TravelAccountMembersRequestDTO requestDTO) {

        ValidationUtil.validateInput(ValidateInputDTO.builder()
                .account(accountRepository.findById(requestDTO.getAccountId()))
                .userAccountRelation(userAccountRelationRepository.findByUserIdAndAccountId(requestDTO.getUserId(), requestDTO.getAccountId()))
                .accountType(DOMESTIC)
                .build());

        List<Member> members = ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(requestDTO.getAccountId()))
                .stream()
                .map(member -> Member.builder()
                        .userId(member.getUserId())
                        .userName(userClient.getUserName(member.getUserId()).getUserName())
                        .role(member.getUserRole())
                        .build())
                .collect(Collectors.toList());

        return TravelAccountMembersResponseDTO.builder()
                .userId(requestDTO.getUserId())
                .memberCount((long) members.size())
                .members(members).build();
    }

}
