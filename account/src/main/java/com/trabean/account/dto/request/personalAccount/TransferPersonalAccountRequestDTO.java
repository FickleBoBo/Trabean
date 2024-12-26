package com.trabean.account.dto.request.personalAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TransferPersonalAccountRequestDTO {

    @JsonProperty("depositAccountNo")
    private String depositAccountNo;

    @JsonProperty("withdrawalAccountNo")
    private String withdrawalAccountNo;

    @JsonProperty("transactionBalance")
    private Long transactionBalance;
}
