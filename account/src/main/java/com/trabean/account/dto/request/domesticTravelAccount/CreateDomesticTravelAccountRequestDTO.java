package com.trabean.account.dto.request.domesticTravelAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class CreateDomesticTravelAccountRequestDTO {

    @JsonProperty("password")
    private String password;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("targetAmount")
    private Long targetAmount;
}
