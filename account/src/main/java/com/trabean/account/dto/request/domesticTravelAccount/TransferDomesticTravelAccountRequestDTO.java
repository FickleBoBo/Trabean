package com.trabean.account.dto.request.domesticTravelAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TransferDomesticTravelAccountRequestDTO {

    @JsonProperty("depositAccountNo")
    private String depositAccountNo;

    @JsonProperty("withdrawalAccountNo")
    private String withdrawalAccountNo;

    @JsonProperty("transactionBalance")
    private Long transactionBalance;
}
