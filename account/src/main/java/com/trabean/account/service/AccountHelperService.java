package com.trabean.account.service;

import com.trabean.account.domain.Account;
import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.repository.AccountRepository;
import com.trabean.account.repository.UserAccountRelationRepository;
import com.trabean.exception.custom.InvalidPasswordException;
import com.trabean.exception.custom.UserAccountRelationNotFoundException;
import com.trabean.external.msa.travel.client.TravelClient;
import com.trabean.external.msa.user.client.UserClient;
import com.trabean.external.msa.user.dto.request.UserKeyRequestDTO;
import com.trabean.util.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.trabean.account.domain.UserAccountRelation.UserRole.ADMIN;
import static com.trabean.constant.Constant.PEPPER;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountHelperService {

    private final AccountRepository accountRepository;
    private final UserAccountRelationRepository userAccountRelationRepository;

    private final UserClient userClient;
    private final TravelClient travelClient;

    private final PasswordEncoder passwordEncoder;

    // 통장 주인의 userId를 반환하는 메서드
    public Long getAdminUserId(Long accountId) {
        return ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(accountId))
                .stream()
                .filter(relation -> relation.getUserRole() == ADMIN)
                .map(UserAccountRelation::getUserId)
                .findFirst()
                .orElseThrow(UserAccountRelationNotFoundException::getInstance);
    }

    // 통장 주인의 userId를 반환하는 메서드
    public Long getAdminUserId(String accountNo) {
        Long accountId = ValidationUtil.validateAccount(accountRepository.findByAccountNo(accountNo)).getAccountId();
        return ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(accountId))
                .stream()
                .filter(relation -> relation.getUserRole() == ADMIN)
                .map(UserAccountRelation::getUserId)
                .findFirst()
                .orElseThrow(UserAccountRelationNotFoundException::getInstance);
    }

    // 통장 주인의 userKey 조회 메서드
    public String getAdminUserKey(Long accountId) {

        // User 서버에 userKey 조회 요청
        return userClient.getUserKey(UserKeyRequestDTO.builder()
                        .userId(getAdminUserId(accountId))
                        .build())
                .getUserKey();
    }

    // 통장 주인의 userKey 조회 메서드
    public String getAdminUserKey(String accountNo) {
        Long accountId = ValidationUtil.validateAccount(accountRepository.findByAccountNo(accountNo)).getAccountId();

        // User 서버에 userKey 조회 요청
        return userClient.getUserKey(UserKeyRequestDTO.builder()
                        .userId(getAdminUserId(accountId))
                        .build())
                .getUserKey();
    }

    // 통장에 가입된 userId 조회 메서드
    public List<Long> getAccountMemberIdList(Long accountId) {
        return ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(accountId))
                .stream()
                .map(UserAccountRelation::getUserId)
                .collect(Collectors.toList());
    }

    // 통장에 가입된 userId 조회 메서드
    public List<Long> getAccountMemberIdList(String accountNo) {
        Long accountId = ValidationUtil.validateAccount(accountRepository.findByAccountNo(accountNo)).getAccountId();
        return ValidationUtil.validateUserAccountRelationList(userAccountRelationRepository.findAllByAccountId(accountId))
                .stream()
                .map(UserAccountRelation::getUserId)
                .collect(Collectors.toList());
    }

    // 통장 주인 이름 조회 메서드
    public String getAccountName(Long accountId) {
        return getAccountNameByType(ValidationUtil.validateAccount(accountRepository.findById(accountId)));
    }

    // 통장 주인 이름 조회 메서드
    public String getAccountName(String accountNo) {
        return getAccountNameByType(ValidationUtil.validateAccount(accountRepository.findByAccountNo(accountNo)));
    }

    // 개인 통장은 통장 주인 이름, 여행통장들은 통장 이름 반환
    private String getAccountNameByType(Account account) {
        switch (account.getAccountType()) {
            case PERSONAL -> {
                return userClient.getUserName(getAdminUserId(account.getAccountId())).getUserName();
            }
            case DOMESTIC -> {
                return travelClient.getDomesticTravelAccountInfo(account.getAccountId()).getAccountName();
            }
            case FOREIGN -> {
                Long parentAccountId = travelClient.getParentAccountId(getAdminUserId(account.getAccountId())).getParentAccountId();

                return travelClient.getDomesticTravelAccountInfo(parentAccountId).getAccountName();
            }
            default -> throw UserAccountRelationNotFoundException.getInstance();
        }
    }

    // 통장 비밀번호가 일치하는지 판단
    public String verifyAccountPassword(String password, String savedPassword) {
        if (!passwordEncoder.matches(password + PEPPER, savedPassword)) {
            throw InvalidPasswordException.getInstance();
        }

        return "통장 비밀번호 검증 성공";
    }

}
