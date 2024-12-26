package com.trabean.account.dto.response.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trabean.account.domain.Account.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountListResponseDTO {

    @JsonProperty("mainAccount")
    private Account mainAccount;

    @JsonProperty("accountList")
    private List<Account> accountList;

    @Builder
    @Getter
    public static class Account {
        private Long accountId;
        private String accountNo;
        private String accountName;
        private String bankName;
        private Long accountBalance;
        private AccountType accountType;
    }
}
