package com.trabean.account.dto.request.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class TravelAccountMembersRequestDTO {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("accountId")
    private Long accountId;
}
