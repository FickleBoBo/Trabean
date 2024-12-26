package com.trabean.account.dto.request.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class UpdateAccountTransferLimitRequestDTO {

    @JsonProperty("oneTimeTransferLimit")
    private Long oneTimeTransferLimit;

    @JsonProperty("dailyTransferLimit")
    private Long dailyTransferLimit;
}
