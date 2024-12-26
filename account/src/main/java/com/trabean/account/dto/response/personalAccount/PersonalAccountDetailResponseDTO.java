package com.trabean.account.dto.response.personalAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalAccountDetailResponseDTO {

    @JsonProperty("accountId")
    private Long accountId;

    @JsonProperty("accountNo")
    private String accountNo;

    @JsonProperty("accountBalance")
    private Long accountBalance;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("transactionList")
    private List<Transaction> transactionList;

    @Builder
    @Getter
    public static class Transaction {
        private String transactionType;
        private String transactionSummary;
        private String transactionDate;
        private String transactionTime;
        private Long transactionBalance;
        private Long transactionAfterBalance;
        private String transactionMemo;
    }
}
