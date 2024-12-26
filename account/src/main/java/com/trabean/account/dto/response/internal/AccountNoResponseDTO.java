package com.trabean.account.dto.response.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountNoResponseDTO {

    @JsonProperty("message")
    private String message;

    @JsonProperty("accountNo")
    private String accountNo;
}
